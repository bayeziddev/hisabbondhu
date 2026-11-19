import { Hono } from 'hono';
import { z } from 'zod';
import type { AppEnv } from '../env';
import { audit } from '../lib/auth';
import { randomOtp, randomToken, safeEqual, sha256Hex, signJwt, uuid } from '../lib/crypto';
import { clientIp, fail, now, readJson } from '../lib/http';
import { maskPhone, normalizeBdPhone } from '../lib/phone';
import { SmsError, sendOtpSms } from '../lib/sms';

export const ACCESS_TTL_SECONDS = 15 * 60;
const REFRESH_TTL_MS = 60 * 24 * 3600 * 1000;
const OTP_TTL_MS = 5 * 60 * 1000;
const RESEND_AFTER_S = 60;
const MAX_PER_PHONE_HOUR = 5;
const MAX_PER_IP_HOUR = 20;
const MAX_ATTEMPTS = 5;

export const authRoutes = new Hono<AppEnv>();

const otpHash = (pepper: string, phone: string, code: string) => sha256Hex(`${pepper}:${phone}:${code}`);

async function issueTokens(env: AppEnv['Bindings'], userId: string, sessionId: string, secret: string) {
  return {
    access_token: await signJwt(userId, sessionId, env.JWT_SECRET, ACCESS_TTL_SECONDS),
    refresh_token: `${sessionId}.${secret}`,
    expires_in: ACCESS_TTL_SECONDS,
  };
}

authRoutes.post('/request-otp', async (c) => {
  const body = await readJson(c, z.object({ phone: z.string().max(30) }));
  const phone = normalizeBdPhone(body.phone);
  if (!phone) fail(422, 'invalid_phone');
  const ip = clientIp(c);
  const t = now();
  const db = c.env.DB;

  const recent = await db
    .prepare('SELECT COUNT(*) AS n, MAX(created_at) AS last FROM otp_codes WHERE phone = ? AND created_at > ?')
    .bind(phone, t - 3600_000)
    .first<{ n: number; last: number | null }>();
  if (recent && recent.last && t - recent.last < RESEND_AFTER_S * 1000) fail(429, 'rate_limited', 'Please wait before requesting another code');
  if (recent && recent.n >= MAX_PER_PHONE_HOUR) fail(429, 'rate_limited');
  const byIp = await db
    .prepare('SELECT COUNT(*) AS n FROM otp_codes WHERE ip = ? AND created_at > ?')
    .bind(ip, t - 3600_000)
    .first<{ n: number }>();
  if (byIp && byIp.n >= MAX_PER_IP_HOUR) fail(429, 'rate_limited');

  const reviewPhone = c.env.REVIEW_PHONE ? normalizeBdPhone(c.env.REVIEW_PHONE) : null;
  const isReview = reviewPhone !== null && reviewPhone === phone && !!c.env.REVIEW_OTP;
  const code = isReview ? c.env.REVIEW_OTP! : randomOtp();

  await db
    .prepare('INSERT INTO otp_codes (phone, code_hash, ip, created_at, expires_at) VALUES (?, ?, ?, ?, ?)')
    .bind(phone, await otpHash(c.env.OTP_PEPPER, phone, code), ip, t, t + OTP_TTL_MS)
    .run();

  if (!isReview) {
    try {
      await sendOtpSms(c.env, phone, code);
    } catch (e) {
      console.error('sms failed', maskPhone(phone), e instanceof SmsError ? e.message : e);
      fail(502, 'sms_failed', 'Could not send SMS. Please try again.');
    }
  }

  const devMode = c.env.OTP_DEV_MODE === 'true' && c.env.APP_ENV !== 'production';
  return c.json({
    ok: true,
    expires_in: OTP_TTL_MS / 1000,
    resend_after: RESEND_AFTER_S,
    ...(devMode ? { dev_code: code } : {}),
  });
});

authRoutes.post('/verify-otp', async (c) => {
  const body = await readJson(c, z.object({
    phone: z.string().max(30),
    code: z.string().regex(/^\d{6}$/),
    device_id: z.string().max(64).optional(),
    device_name: z.string().max(80).optional(),
  }));
  const phone = normalizeBdPhone(body.phone);
  if (!phone) fail(422, 'invalid_phone');
  const db = c.env.DB;
  const t = now();

  const otp = await db
    .prepare('SELECT id, code_hash, attempts, expires_at FROM otp_codes WHERE phone = ? AND consumed = 0 ORDER BY id DESC LIMIT 1')
    .bind(phone)
    .first<{ id: number; code_hash: string; attempts: number; expires_at: number }>();
  if (!otp || otp.expires_at < t) fail(400, 'code_expired');
  if (otp.attempts >= MAX_ATTEMPTS) fail(429, 'too_many_attempts');

  const hash = await otpHash(c.env.OTP_PEPPER, phone, body.code);
  if (!safeEqual(hash, otp.code_hash)) {
    await db.prepare('UPDATE otp_codes SET attempts = attempts + 1 WHERE id = ?').bind(otp.id).run();
    fail(400, otp.attempts + 1 >= MAX_ATTEMPTS ? 'too_many_attempts' : 'invalid_code');
  }

  // Single use: only one request can flip consumed 0 → 1.
  const consumed = await db.prepare('UPDATE otp_codes SET consumed = 1 WHERE id = ? AND consumed = 0').bind(otp.id).run();
  if (!consumed.meta.changes) fail(400, 'code_expired');

  let user = await db.prepare('SELECT id FROM users WHERE phone = ?').bind(phone).first<{ id: string }>();
  if (!user) {
    user = { id: uuid() };
    await db.prepare('INSERT INTO users (id, phone, created_at, last_login_at) VALUES (?, ?, ?, ?)').bind(user.id, phone, t, t).run();
  } else {
    await db.prepare('UPDATE users SET last_login_at = ? WHERE id = ?').bind(t, user.id).run();
  }

  const sessionId = uuid();
  const secret = randomToken(32);
  await db
    .prepare(`INSERT INTO sessions (id, user_id, refresh_hash, device_id, device_name, created_at, last_used_at, expires_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?)`)
    .bind(sessionId, user.id, await sha256Hex(secret), body.device_id ?? null, body.device_name ?? null, t, t, t + REFRESH_TTL_MS)
    .run();
  await audit(db, 'login', { userId: user.id, ip: clientIp(c), detail: body.device_name });

  return c.json({ ...(await issueTokens(c.env, user.id, sessionId, secret)), user: { id: user.id, phone } });
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
  const user = await db.prepare('SELECT phone FROM users WHERE id = ?').bind(s.user_id).first<{ phone: string }>();
  return c.json({ ...(await issueTokens(c.env, s.user_id, s.id, secret)), user: { id: s.user_id, phone: user?.phone ?? '' } });
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
