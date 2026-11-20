import type { Env } from '../env';
import { randomOtp, safeEqual, sha256Hex } from './crypto';
import { fail } from './http';

export const OTP_TTL_MS = 5 * 60 * 1000;
export const RESEND_AFTER_S = 60;
const MAX_PER_TARGET_HOUR = 5;
const MAX_PER_IP_HOUR = 20;
const MAX_ATTEMPTS = 5;

export const otpHash = (pepper: string, target: string, code: string) => sha256Hex(`${pepper}:${target}:${code}`);

/** Rate-limits, stores a hashed code and returns the plain code (to be sent by email/SMS). */
export async function createOtp(env: Env, channel: 'email' | 'sms', target: string, ip: string, fixedCode?: string): Promise<string> {
  const db = env.DB;
  const t = Date.now();
  const recent = await db
    .prepare('SELECT COUNT(*) AS n, MAX(created_at) AS last FROM otp_codes WHERE target = ? AND created_at > ?')
    .bind(target, t - 3600_000)
    .first<{ n: number; last: number | null }>();
  if (recent?.last && t - recent.last < RESEND_AFTER_S * 1000) fail(429, 'rate_limited', 'Please wait before requesting another code');
  if (recent && recent.n >= MAX_PER_TARGET_HOUR) fail(429, 'rate_limited');
  const byIp = await db
    .prepare('SELECT COUNT(*) AS n FROM otp_codes WHERE ip = ? AND created_at > ?')
    .bind(ip, t - 3600_000)
    .first<{ n: number }>();
  if (byIp && byIp.n >= MAX_PER_IP_HOUR) fail(429, 'rate_limited');

  const code = fixedCode ?? randomOtp();
  await db
    .prepare('INSERT INTO otp_codes (channel, target, code_hash, ip, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(channel, target, await otpHash(env.OTP_PEPPER, target, code), ip, t, t + OTP_TTL_MS)
    .run();
  return code;
}

/** Verifies and consumes the latest code for a target (single use, attempt-limited). */
export async function consumeOtp(env: Env, target: string, code: string): Promise<void> {
  const db = env.DB;
  const otp = await db
    .prepare('SELECT id, code_hash, attempts, expires_at FROM otp_codes WHERE target = ? AND consumed = 0 ORDER BY id DESC LIMIT 1')
    .bind(target)
    .first<{ id: number; code_hash: string; attempts: number; expires_at: number }>();
  if (!otp || otp.expires_at < Date.now()) fail(400, 'code_expired');
  if (otp.attempts >= MAX_ATTEMPTS) fail(429, 'too_many_attempts');
  if (!safeEqual(await otpHash(env.OTP_PEPPER, target, code), otp.code_hash)) {
    await db.prepare('UPDATE otp_codes SET attempts = attempts + 1 WHERE id = ?').bind(otp.id).run();
    fail(400, otp.attempts + 1 >= MAX_ATTEMPTS ? 'too_many_attempts' : 'invalid_code');
  }
  const consumed = await db.prepare('UPDATE otp_codes SET consumed = 1 WHERE id = ? AND consumed = 0').bind(otp.id).run();
  if (!consumed.meta.changes) fail(400, 'code_expired');
}

export const devCodeAllowed = (env: Env) => env.OTP_DEV_MODE === 'true' && env.APP_ENV !== 'production';
