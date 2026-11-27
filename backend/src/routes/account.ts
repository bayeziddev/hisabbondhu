import { Hono } from 'hono';
import { z } from 'zod';
import type { AppEnv, Env } from '../env';
import { audit, requireAuth } from '../lib/auth';
import { sha256Hex, uuid } from '../lib/crypto';
import { clientIp, fail, now, readJson } from '../lib/http';
import { consumeOtp, createOtp, devCodeAllowed } from '../lib/otp';
import { normalizeBdPhone } from '../lib/phone';
import { sendOtpSms } from '../lib/sms';
import { getUser, publicUser } from '../lib/users';
import { deleteBusinessFiles } from '../jobs';

export const accountRoutes = new Hono<AppEnv>();
accountRoutes.use('*', requireAuth);

/** Right of access: who am I, which businesses, which logged-in devices. */
accountRoutes.get('/', async (c) => {
  const db = c.env.DB;
  const userId = c.get('userId');
  const [user, businesses, sessions] = await db.batch([
    db.prepare('SELECT id, email, phone, name, phone_verified_at, marketing_opt_in, created_at, last_login_at FROM users WHERE id = ?').bind(userId),
    db.prepare(`SELECT b.id, b.name, m.role, b.created_at, b.last_sync_at FROM businesses b
                JOIN business_members m ON m.business_id = b.id WHERE m.user_id = ?`).bind(userId),
    db.prepare(`SELECT id, device_name, created_at, last_used_at FROM sessions
                WHERE user_id = ? AND revoked_at IS NULL AND expires_at > ? ORDER BY last_used_at DESC`).bind(userId, now()),
  ]);
  return c.json({ user: user.results?.[0] ?? null, businesses: businesses.results, devices: sessions.results });
});

/** Right to data portability: complete machine-readable export of everything we store about the user. */
accountRoutes.get('/export', async (c) => {
  const db = c.env.DB;
  const userId = c.get('userId');
  const user = await db.prepare('SELECT id, email, phone, name, marketing_opt_in, created_at, last_login_at, pro_until FROM users WHERE id = ?').bind(userId).first();
  const [code, invitedBy, points] = await db.batch([
    db.prepare('SELECT code, created_at FROM referral_codes WHERE user_id = ?').bind(userId),
    db.prepare('SELECT code, status, created_at FROM referrals WHERE referee_user_id = ?').bind(userId),
    db.prepare('SELECT delta, reason, created_at FROM points_ledger WHERE user_id = ? ORDER BY created_at').bind(userId),
  ]);
  const referral = { code: code.results[0] ?? null, invited_with: invitedBy.results[0] ?? null, points_ledger: points.results };
  const owned = await db.prepare('SELECT * FROM businesses WHERE owner_user_id = ?').bind(userId).all();
  const out: Record<string, unknown>[] = [];
  for (const b of owned.results as { id: string }[]) {
    const [parties, products, txs] = await db.batch([
      db.prepare('SELECT * FROM parties WHERE business_id = ? ORDER BY server_seq').bind(b.id),
      db.prepare('SELECT * FROM products WHERE business_id = ? ORDER BY server_seq').bind(b.id),
      db.prepare('SELECT * FROM transactions WHERE business_id = ? ORDER BY occurred_at').bind(b.id),
    ]);
    out.push({ business: b, parties: parties.results, products: products.results, transactions: txs.results });
  }
  c.header('content-disposition', 'attachment; filename="hisabbondhu-export.json"');
  return c.json({ format: 'hisabbondhu-export', version: 1, exported_at: now(), user, referral, data: out });
});

/** Complete / update the profile. Name and Bangladeshi mobile number are required for every account. */
accountRoutes.put('/profile', async (c) => {
  const body = await readJson(c, z.object({
    name: z.string().trim().min(1).max(100),
    phone: z.string().max(30),
    marketing_opt_in: z.boolean().default(false),
  }));
  const phone = normalizeBdPhone(body.phone);
  if (!phone) fail(422, 'invalid_phone');
  const userId = c.get('userId');
  const current = await getUser(c.env.DB, userId);
  if (!current) fail(401, 'session_expired');
  // Changing the number clears its verification.
  const verified = current.phone === phone ? current.phone_verified_at : null;
  await c.env.DB
    .prepare('UPDATE users SET name = ?, phone = ?, phone_verified_at = ?, marketing_opt_in = ? WHERE id = ?')
    .bind(body.name, phone, verified, body.marketing_opt_in ? 1 : 0, userId)
    .run();
  await audit(c.env.DB, 'profile_updated', { userId, ip: clientIp(c) });
  return c.json({ user: publicUser((await getUser(c.env.DB, userId))!) });
});

/*
 * Phone verification by SMS — built and ready, switched off until an SMS gateway is paid for.
 * Turn on with PHONE_OTP_ENABLED="true" + SMS_PROVIDER + SMS_API_KEY/SMS_SENDER_ID.
 */
const phoneOtpEnabled = (env: Env) => env.PHONE_OTP_ENABLED === 'true';

accountRoutes.post('/phone/request-otp', async (c) => {
  if (!phoneOtpEnabled(c.env)) fail(503, 'feature_disabled', 'Phone verification is not enabled yet');
  const user = await getUser(c.env.DB, c.get('userId'));
  if (!user?.phone) fail(422, 'invalid_phone');
  const code = await createOtp(c.env, 'sms', user.phone, clientIp(c));
  try {
    await sendOtpSms(c.env, user.phone, code);
  } catch {
    fail(502, 'sms_failed');
  }
  return c.json({ ok: true, ...(devCodeAllowed(c.env) ? { dev_code: code } : {}) });
});

accountRoutes.post('/phone/verify', async (c) => {
  if (!phoneOtpEnabled(c.env)) fail(503, 'feature_disabled', 'Phone verification is not enabled yet');
  const body = await readJson(c, z.object({ code: z.string().regex(/^\d{6}$/) }));
  const userId = c.get('userId');
  const user = await getUser(c.env.DB, userId);
  if (!user?.phone) fail(422, 'invalid_phone');
  await consumeOtp(c.env, user.phone, body.code);
  await c.env.DB.prepare('UPDATE users SET phone_verified_at = ? WHERE id = ?').bind(now(), userId).run();
  return c.json({ user: publicUser((await getUser(c.env.DB, userId))!) });
});

/** Sign out one device. */
accountRoutes.delete('/sessions/:sid', async (c) => {
  await c.env.DB.prepare('UPDATE sessions SET revoked_at = ? WHERE id = ? AND user_id = ?')
    .bind(now(), c.req.param('sid'), c.get('userId')).run();
  return c.json({ ok: true });
});

/** Right to erasure (Google Play account-deletion requirement). */
accountRoutes.delete('/', async (c) => {
  const count = await deleteUserEverything(c.env, c.get('userId'), 'api', clientIp(c), c.executionCtx);
  return c.json({ ok: true, deleted_businesses: count });
});

export async function deleteUserEverything(
  env: Env, userId: string, source: string, ip: string, ctx?: { waitUntil(p: Promise<unknown>): void },
): Promise<number> {
  const db = env.DB;
  const user = await db.prepare('SELECT email, phone FROM users WHERE id = ?').bind(userId).first<{ email: string; phone: string | null }>();
  if (!user) return 0;
  const owned = await db.prepare('SELECT id FROM businesses WHERE owner_user_id = ?').bind(userId).all<{ id: string }>();
  const ids = owned.results.map((b) => b.id);
  const t = now();

  const stmts: D1PreparedStatement[] = [];
  for (const id of ids) {
    stmts.push(
      db.prepare('DELETE FROM transactions WHERE business_id = ?').bind(id),
      db.prepare('DELETE FROM products WHERE business_id = ?').bind(id),
      db.prepare('DELETE FROM parties WHERE business_id = ?').bind(id),
      db.prepare('DELETE FROM business_members WHERE business_id = ?').bind(id),
      db.prepare('DELETE FROM audit_logs WHERE business_id = ?').bind(id),
      db.prepare('DELETE FROM businesses WHERE id = ?').bind(id),
    );
  }
  stmts.push(
    db.prepare('DELETE FROM business_members WHERE user_id = ?').bind(userId),
    db.prepare('DELETE FROM sessions WHERE user_id = ?').bind(userId),
    db.prepare('DELETE FROM otp_codes WHERE target = ? OR target = ?').bind(user.email, user.phone ?? ''),
    db.prepare('DELETE FROM audit_logs WHERE user_id = ?').bind(userId),
    db.prepare('DELETE FROM points_ledger WHERE user_id = ?').bind(userId),
    db.prepare('DELETE FROM referrals WHERE referee_user_id = ? OR referrer_user_id = ?').bind(userId, userId),
    db.prepare('DELETE FROM referral_codes WHERE user_id = ?').bind(userId),
    db.prepare('DELETE FROM users WHERE id = ?').bind(userId),
    db.prepare('INSERT INTO deletion_log (id, email_hash, source, businesses, requested_at, completed_at) VALUES (?, ?, ?, ?, ?, ?)')
      .bind(uuid(), await sha256Hex(`${env.OTP_PEPPER}:deleted:${user.email}`), source, ids.length, t, t),
  );
  await db.batch(stmts);

  // Backup files in R2 are removed in the background (queue if available, otherwise inline).
  for (const id of ids) {
    if (env.JOBS) await env.JOBS.send({ type: 'delete_business_files', businessId: id });
    else if (ctx) ctx.waitUntil(deleteBusinessFiles(env, id));
    else await deleteBusinessFiles(env, id);
  }
  console.log(`account deleted via ${source} (${ids.length} businesses) ip=${ip.slice(0, 7)}…`);
  return ids.length;
}
