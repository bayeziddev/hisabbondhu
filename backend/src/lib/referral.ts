import type { Env } from '../env';
import { sha256Hex, uuid } from './crypto';
import { now } from './http';

/**
 * Referral programme ("বন্ধুকে আনুন, হিসাব পয়েন্ট নিন").
 *
 * - Every account has a share code. A new account can enter one code within APPLY_WINDOW_DAYS.
 * - The new user gets WELCOME_POINTS at once. The inviter gets REFERRER_POINTS only when the new
 *   user really uses the app: MIN_TRANSACTIONS entries synced and the account older than the
 *   minimum age (24 h by default). Rewards are never paid for an install alone.
 * - Points are redeemed for days of Pro (REDEEM_OPTIONS) once REFERRAL_REDEEM_ENABLED is "true".
 */
export const REFERRAL = {
  referrerPoints: 100,
  welcomePoints: 50,
  minTransactions: 5,
  applyWindowDays: 7,
  pendingExpiryDays: 30,
  monthlyCap: 20,
  redeemOptions: [
    { id: 'pro_30', points: 100, days: 30 },
    { id: 'pro_365', points: 1000, days: 365 },
  ],
} as const;

export const PLAY_PACKAGE = 'com.hisabbondhu.myapp';
const DAY = 24 * 3600_000;

const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'; // 32 symbols, no 0/O/1/I
export const CODE_RE = /^[A-HJ-NP-Z2-9]{6}$/;

/** Uniform random code: 32 symbols divide 256 exactly, so masking a byte has no bias. */
export function generateCode(length = 6): string {
  const buf = new Uint8Array(length);
  crypto.getRandomValues(buf);
  return [...buf].map((b) => ALPHABET[b & 31]).join('');
}

/** Accepts "k7m-2qx", " K7M 2QX " etc. Returns null when it can't be a valid code. */
export function normalizeCode(input: string): string | null {
  const c = input.toUpperCase().replace(/[\s-]/g, '');
  return CODE_RE.test(c) ? c : null;
}

export const shareUrl = (code: string) =>
  `https://play.google.com/store/apps/details?id=${PLAY_PACKAGE}&referrer=${encodeURIComponent(`ref=${code}`)}`;

export const redeemEnabled = (env: Env) => env.REFERRAL_REDEEM_ENABLED === 'true';

/** Minimum referee account age before the inviter is rewarded (hours; tests use 0). */
export function minAgeMs(env: Env): number {
  const h = Number(env.REFERRAL_MIN_AGE_HOURS ?? '24');
  return Number.isFinite(h) && h >= 0 ? h * 3600_000 : DAY;
}

export const claimHash = (env: Env, email: string) => sha256Hex(`${env.OTP_PEPPER}:referral:${email.toLowerCase()}`);

export async function getOrCreateCode(db: D1Database, userId: string): Promise<string> {
  const existing = await db.prepare('SELECT code FROM referral_codes WHERE user_id = ?').bind(userId).first<{ code: string }>();
  if (existing) return existing.code;
  for (let i = 0; i < 8; i++) {
    const code = generateCode();
    const res = await db
      .prepare('INSERT INTO referral_codes (user_id, code, created_at) VALUES (?, ?, ?) ON CONFLICT DO NOTHING')
      .bind(userId, code, now())
      .run();
    if (res.meta.changes === 1) return code;
    // Either the code was taken (retry with a new one) or a parallel request created ours.
    const mine = await db.prepare('SELECT code FROM referral_codes WHERE user_id = ?').bind(userId).first<{ code: string }>();
    if (mine) return mine.code;
  }
  throw new Error('could not allocate a referral code');
}

export async function pointsBalance(db: D1Database, userId: string): Promise<number> {
  const r = await db.prepare('SELECT COALESCE(SUM(delta), 0) AS p FROM points_ledger WHERE user_id = ?').bind(userId).first<{ p: number }>();
  return r?.p ?? 0;
}

/** True when the two accounts have ever signed in on the same device. */
export async function shareDevice(db: D1Database, a: string, b: string): Promise<boolean> {
  const r = await db
    .prepare(`SELECT 1 AS x FROM sessions s1 JOIN sessions s2 ON s1.device_id = s2.device_id
              WHERE s1.user_id = ? AND s2.user_id = ? AND s1.device_id IS NOT NULL AND s1.device_id != '' LIMIT 1`)
    .bind(a, b)
    .first();
  return !!r;
}

type Pending = { referee_user_id: string; referrer_user_id: string; created_at: number; user_created_at: number };

/**
 * Decides one pending referral: reward, reject (same device), expire, or leave pending.
 * Safe to call any number of times and in parallel — the status update is conditional and the
 * award ids are deterministic.
 */
export async function decideReferral(env: Env, refereeUserId: string): Promise<'qualified' | 'rejected' | 'expired' | 'pending' | 'none'> {
  const db = env.DB;
  const r = await db
    .prepare(`SELECT r.referee_user_id, r.referrer_user_id, r.created_at, u.created_at AS user_created_at
              FROM referrals r JOIN users u ON u.id = r.referee_user_id
              WHERE r.referee_user_id = ? AND r.status = 'pending'`)
    .bind(refereeUserId)
    .first<Pending>();
  if (!r) return 'none';
  const t = now();

  const setStatus = (status: string, reason: string | null) =>
    db.prepare(`UPDATE referrals SET status = ?, reason = ?, decided_at = ? WHERE referee_user_id = ? AND status = 'pending'`)
      .bind(status, reason, t, r.referee_user_id);

  if (await shareDevice(db, r.referee_user_id, r.referrer_user_id)) {
    await setStatus('rejected', 'same_device').run();
    return 'rejected';
  }

  const active = await db
    .prepare(`SELECT COUNT(*) AS n FROM transactions t JOIN business_members m ON m.business_id = t.business_id
              WHERE m.user_id = ? AND m.role = 'owner' AND t.reversal_of IS NULL`)
    .bind(r.referee_user_id)
    .first<{ n: number }>();
  const oldEnough = t - r.user_created_at >= minAgeMs(env);

  if ((active?.n ?? 0) >= REFERRAL.minTransactions && oldEnough) {
    const recent = await db
      .prepare(`SELECT COUNT(*) AS n FROM referrals WHERE referrer_user_id = ? AND status = 'qualified'
                AND reason IS NULL AND decided_at > ?`)
      .bind(r.referrer_user_id, t - 30 * DAY)
      .first<{ n: number }>();
    if ((recent?.n ?? 0) >= REFERRAL.monthlyCap) {
      await setStatus('qualified', 'monthly_cap').run(); // counted, but no points this month
      return 'qualified';
    }
    await db.batch([
      setStatus('qualified', null),
      db.prepare(`INSERT INTO points_ledger (id, user_id, delta, reason, ref, created_at)
                  SELECT ?, ?, ?, 'referral_referrer', ?, ?
                  WHERE EXISTS (SELECT 1 FROM referrals WHERE referee_user_id = ? AND status = 'qualified' AND reason IS NULL)
                  ON CONFLICT (id) DO NOTHING`)
        .bind(`ref:${r.referee_user_id}:referrer`, r.referrer_user_id, REFERRAL.referrerPoints, r.referee_user_id, t, r.referee_user_id),
    ]);
    return 'qualified';
  }

  if (t - r.created_at > REFERRAL.pendingExpiryDays * DAY) {
    await setStatus('expired', null).run();
    return 'expired';
  }
  return 'pending';
}

/** Re-checks the pending invitations of one inviter (when they open the referral screen). */
export async function decideForReferrer(env: Env, referrerUserId: string): Promise<void> {
  const rows = await env.DB
    .prepare(`SELECT referee_user_id FROM referrals WHERE referrer_user_id = ? AND status = 'pending' ORDER BY created_at LIMIT 25`)
    .bind(referrerUserId)
    .all<{ referee_user_id: string }>();
  for (const row of rows.results) await decideReferral(env, row.referee_user_id);
}

/** Daily cron: decide every pending referral. */
export async function sweepReferrals(env: Env): Promise<void> {
  const rows = await env.DB
    .prepare(`SELECT referee_user_id FROM referrals WHERE status = 'pending' ORDER BY created_at LIMIT 2000`)
    .all<{ referee_user_id: string }>();
  for (const row of rows.results) {
    try {
      await decideReferral(env, row.referee_user_id);
    } catch (e) {
      console.error('referral sweep', e);
    }
  }
}

/** New ledger id for a redemption (random: one user may redeem many times). */
export const redemptionId = () => `redeem:${uuid()}`;
