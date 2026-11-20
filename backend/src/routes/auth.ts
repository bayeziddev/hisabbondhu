import { Hono } from 'hono';
import { z } from 'zod';
import type { AppEnv } from '../env';
import { audit } from '../lib/auth';
import { randomToken, safeEqual, sha256Hex, signJwt, uuid } from '../lib/crypto';
import { EmailError, maskEmail, normalizeEmail, sendOtpEmail } from '../lib/email';
import { clientIp, fail, now, readJson } from '../lib/http';
import { OTP_TTL_MS, RESEND_AFTER_S, consumeOtp, createOtp, devCodeAllowed } from '../lib/otp';
import { getUser, publicUser } from '../lib/users';

export const ACCESS_TTL_SECONDS = 15 * 60;
const REFRESH_TTL_MS = 60 * 24 * 3600 * 1000;

export const authRoutes = new Hono<AppEnv>();

async function issueTokens(env: AppEnv['Bindings'], userId: string, sessionId: string, secret: string) {
  return {
    access_token: await signJwt(userId, sessionId, env.JWT_SECRET, ACCESS_TTL_SECONDS),
    refresh_token: `${sessionId}.${secret}`,
    expires_in: ACCESS_TTL_SECONDS,
  };
}

/** Step 1 — send a 6-digit login code to the email address (Resend). */
authRoutes.post('/request-otp', async (c) => {
  const body = await readJson(c, z.object({ email: z.string().max(254) }));
  const email = normalizeEmail(body.email);
  if (!email) fail(422, 'invalid_email');

  const review = c.env.REVIEW_EMAIL ? normalizeEmail(c.env.REVIEW_EMAIL) : null;
  const isReview = review !== null && review === email && !!c.env.REVIEW_OTP;
  const code = await createOtp(c.env, 'email', email, clientIp(c), isReview ? c.env.REVIEW_OTP : undefined);

  if (!isReview) {
    try {
      await sendOtpEmail(c.env, email, code);
    } catch (e) {
      console.error('email failed', maskEmail(email), e instanceof EmailError ? e.message : e);
      fail(502, 'email_failed', 'Could not send the email. Please try again.');
    }
  }
  return c.json({
    ok: true,
    expires_in: OTP_TTL_MS / 1000,
    resend_after: RESEND_AFTER_S,
    ...(devCodeAllowed(c.env) ? { dev_code: code } : {}),
  });
});

/** Step 2 — verify the code. Creates the account on first login. */
authRoutes.post('/verify-otp', async (c) => {
  const body = await readJson(c, z.object({
    email: z.string().max(254),
    code: z.string().regex(/^\d{6}$/),
    device_id: z.string().max(64).optional(),
    device_name: z.string().max(80).optional(),
  }));
  const email = normalizeEmail(body.email);
  if (!email) fail(422, 'invalid_email');
  await consumeOtp(c.env, email, body.code);

  const db = c.env.DB;
  const t = now();
  let existing = await db.prepare('SELECT id FROM users WHERE email = ?').bind(email).first<{ id: string }>();
  const isNew = !existing;
  if (!existing) {
    existing = { id: uuid() };
    await db.prepare('INSERT INTO users (id, email, created_at, last_login_at) VALUES (?, ?, ?, ?)').bind(existing.id, email, t, t).run();
  } else {
    await db.prepare('UPDATE users SET last_login_at = ? WHERE id = ?').bind(t, existing.id).run();
  }

  const sessionId = uuid();
  const secret = randomToken(32);
  await db
    .prepare(`INSERT INTO sessions (id, user_id, refresh_hash, device_id, device_name, created_at, last_used_at, expires_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
    .bind(sessionId, existing.id, await sha256Hex(secret), body.device_id ?? null, body.device_name ?? null, t, t, t + REFRESH_TTL_MS)
    .run();
  await audit(db, isNew ? 'signup' : 'login', { userId: existing.id, ip: clientIp(c), detail: body.device_name });

  const user = await getUser(db, existing.id);
  return c.json({ ...(await issueTokens(c.env, existing.id, sessionId, secret)), new_user: isNew, user: publicUser(user!) });
});

function parseRefresh(token: string): { sid: string; secret: string } | null {
  const i = token.indexOf('.');
  if (i < 10) return null;
  return { sid: token.slice(0, i), secret: token.slice(i + 1) };
}

authRoutes.post('/refresh', async (c) => {
  const body = await readJson(c, z.object({ refresh_token: z.string().min(20).max(300) }));
  const parsed = parseRefresh(body.refresh_token);
  if (!parsed) fail(401, 'invalid_refresh');
  const db = c.env.DB;
  const t = now();
  const s = await db
    .prepare('SELECT id, user_id, refresh_hash, expires_at, revoked_at FROM sessions WHERE id = ?')
    .bind(parsed.sid)
    .first<{ id: string; user_id: string; refresh_hash: string; expires_at: number; revoked_at: number | null }>();
  if (!s || s.revoked_at || s.expires_at < t) fail(401, 'session_expired');

  if (!safeEqual(await sha256Hex(parsed.secret), s.refresh_hash)) {
    // A rotated (old) refresh token was reused → likely stolen. Kill the session.
    await db.prepare('UPDATE sessions SET revoked_at = ? WHERE id = ?').bind(t, s.id).run();
    await audit(db, 'refresh_reuse_detected', { userId: s.user_id, ip: clientIp(c) });
    fail(401, 'session_expired');
  }

  const secret = randomToken(32);
  await db
    .prepare('UPDATE sessions SET refresh_hash = ?, last_used_at = ?, expires_at = ? WHERE id = ?')
    .bind(await sha256Hex(secret), t, t + REFRESH_TTL_MS, s.id)
    .run();
  const user = await getUser(db, s.user_id);
  if (!user) fail(401, 'session_expired');
  return c.json({ ...(await issueTokens(c.env, s.user_id, s.id, secret)), user: publicUser(user) });
});

authRoutes.post('/logout', async (c) => {
  const body = await readJson(c, z.object({ refresh_token: z.string().max(300) }));
  const parsed = parseRefresh(body.refresh_token);
  if (parsed) {
    const s = await c.env.DB.prepare('SELECT refresh_hash FROM sessions WHERE id = ?').bind(parsed.sid).first<{ refresh_hash: string }>();
    if (s && safeEqual(await sha256Hex(parsed.secret), s.refresh_hash)) {
      await c.env.DB.prepare('UPDATE sessions SET revoked_at = ? WHERE id = ?').bind(now(), parsed.sid).run();
    }
  }
  return c.json({ ok: true });
});
