import { Hono } from 'hono';
import type { AppEnv, Env } from '../env';
import { requireAuth } from '../lib/auth';
import { sha256Hex, uuid } from '../lib/crypto';
import { clientIp, now } from '../lib/http';
import { deleteBusinessFiles } from '../jobs';

export const accountRoutes = new Hono<AppEnv>();
accountRoutes.use('*', requireAuth);

/** Right of access: who am I, which businesses, which logged-in devices. */
accountRoutes.get('/', async (c) => {
  const db = c.env.DB;
  const userId = c.get('userId');
  const [user, businesses, sessions] = await db.batch([
    db.prepare('SELECT id, phone, created_at, last_login_at FROM users WHERE id = ?').bind(userId),
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
  const user = await db.prepare('SELECT id, phone, created_at, last_login_at FROM users WHERE id = ?').bind(userId).first();
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
  return c.json({ format: 'hisabbondhu-export', version: 1, exported_at: now(), user, data: out });
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
  const user = await db.prepare('SELECT phone FROM users WHERE id = ?').bind(userId).first<{ phone: string }>();
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
    db.prepare('DELETE FROM otp_codes WHERE phone = ?').bind(user.phone),
    db.prepare('DELETE FROM audit_logs WHERE user_id = ?').bind(userId),
    db.prepare('DELETE FROM users WHERE id = ?').bind(userId),
    db.prepare('INSERT INTO deletion_log (id, phone_hash, source, businesses, requested_at, completed_at) VALUES (?, ?, ?, ?, ?, ?)')
      .bind(uuid(), await sha256Hex(`${env.OTP_PEPPER}:deleted:${user.phone}`), source, ids.length, t, t),
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
