import { Hono } from 'hono';
import { z } from 'zod';
import type { AppEnv, Env } from '../env';
import { audit, requireAuth } from '../lib/auth';
import { clientIp, fail, now, readJson } from '../lib/http';
import {
  REFERRAL, claimHash, decideForReferrer, decideReferral, getOrCreateCode, normalizeCode, pointsBalance,
  redeemEnabled, redemptionId, shareDevice, shareUrl,
} from '../lib/referral';

export const referralRoutes = new Hono<AppEnv>();
referralRoutes.use('*', requireAuth);

const DAY = 24 * 3600_000;

/** Everything the referral screen shows, in one response. */
export async function referralSummary(env: Env, userId: string) {
  const db = env.DB;
  const code = await getOrCreateCode(db, userId);
  const [user, mine, stats, history, points] = await Promise.all([
    db.prepare('SELECT created_at, pro_until FROM users WHERE id = ?').bind(userId).first<{ created_at: number; pro_until: number | null }>(),
    db.prepare('SELECT code, status FROM referrals WHERE referee_user_id = ?').bind(userId).first<{ code: string; status: string }>(),
    db.prepare(`SELECT
                  SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) AS pending,
                  SUM(CASE WHEN status = 'qualified' THEN 1 ELSE 0 END) AS qualified
                FROM referrals WHERE referrer_user_id = ?`).bind(userId).first<{ pending: number | null; qualified: number | null }>(),
    db.prepare('SELECT delta, reason, created_at FROM points_ledger WHERE user_id = ? ORDER BY created_at DESC LIMIT 30')
      .bind(userId).all<{ delta: number; reason: string; created_at: number }>(),
    pointsBalance(db, userId),
  ]);
  const t = now();
  const proUntil = user?.pro_until ?? null;
  const windowOpen = !!user && t - user.created_at <= REFERRAL.applyWindowDays * DAY;
  return {
    code,
    share_url: shareUrl(code),
    points,
    pro_until: proUntil,
    pro_active: !!proUntil && proUntil > t,
    redeem_enabled: redeemEnabled(env),
    can_apply: !mine && windowOpen,
    applied_code: mine?.code ?? null,
    applied_status: mine?.status ?? null,
    stats: { pending: stats?.pending ?? 0, qualified: stats?.qualified ?? 0 },
    rules: {
      referrer_points: REFERRAL.referrerPoints,
      welcome_points: REFERRAL.welcomePoints,
      min_transactions: REFERRAL.minTransactions,
      apply_window_days: REFERRAL.applyWindowDays,
      redeem_options: REFERRAL.redeemOptions,
    },
    history: history.results,
  };
}

referralRoutes.get('/', async (c) => {
  const userId = c.get('userId');
  await decideReferral(c.env, userId);        // my own invitation, if any
  await decideForReferrer(c.env, userId);     // friends I invited
  return c.json(await referralSummary(c.env, userId));
});

/** A new account enters the code it was invited with. */
referralRoutes.post('/apply', async (c) => {
  const body = await readJson(c, z.object({ code: z.string().max(20) }));
  const code = normalizeCode(body.code);
  if (!code) fail(422, 'invalid_code', 'Referral code must be 6 letters/digits');
  const db = c.env.DB;
  const userId = c.get('userId');
  const t = now();

  const owner = await db.prepare('SELECT user_id FROM referral_codes WHERE code = ?').bind(code).first<{ user_id: string }>();
  if (!owner) fail(404, 'code_not_found');
  if (owner.user_id === userId) fail(422, 'own_code');

  const me = await db.prepare('SELECT email, created_at FROM users WHERE id = ?').bind(userId).first<{ email: string; created_at: number }>();
  if (!me) fail(401, 'session_expired');
  if (await db.prepare('SELECT 1 FROM referrals WHERE referee_user_id = ?').bind(userId).first()) fail(409, 'already_applied');
  if (t - me.created_at > REFERRAL.applyWindowDays * DAY) fail(422, 'apply_window_closed');
  // No inviting each other back and forth.
  const reverse = await db.prepare('SELECT 1 FROM referrals WHERE referee_user_id = ? AND referrer_user_id = ?').bind(owner.user_id, userId).first();
  if (reverse) fail(422, 'mutual_referral');
  if (await shareDevice(db, userId, owner.user_id)) fail(422, 'own_code');

  const hash = await claimHash(c.env, me.email);
  const claim = await db.prepare('INSERT INTO referral_claims (email_hash, created_at) VALUES (?, ?) ON CONFLICT DO NOTHING').bind(hash, t).run();
  if (claim.meta.changes !== 1) fail(409, 'already_applied'); // this email was invited before (even on a deleted account)

  await db.batch([
    db.prepare(`INSERT INTO referrals (referee_user_id, referrer_user_id, code, status, created_at) VALUES (?, ?, ?, 'pending', ?)`)
      .bind(userId, owner.user_id, code, t),
    db.prepare(`INSERT INTO points_ledger (id, user_id, delta, reason, ref, created_at) VALUES (?, ?, ?, 'referral_welcome', ?, ?)
                ON CONFLICT (id) DO NOTHING`)
      .bind(`ref:${userId}:welcome`, userId, REFERRAL.welcomePoints, owner.user_id, t),
  ]);
  await audit(db, 'referral_applied', { userId, ip: clientIp(c) });
  await decideReferral(c.env, userId); // may already be active (e.g. entered the code late)
  return c.json(await referralSummary(c.env, userId));
});

/** Exchange points for days of Pro. Atomic: the points are only spent if the balance covers them. */
referralRoutes.post('/redeem', async (c) => {
  if (!redeemEnabled(c.env)) fail(503, 'redeem_disabled', 'Pro is not available yet — your points are kept');
  const body = await readJson(c, z.object({ option: z.string().max(40) }));
  const option = REFERRAL.redeemOptions.find((o) => o.id === body.option);
  if (!option) fail(422, 'invalid_option');
  const db = c.env.DB;
  const userId = c.get('userId');
  const t = now();
  const id = redemptionId();

  const [spend] = await db.batch([
    db.prepare(`INSERT INTO points_ledger (id, user_id, delta, reason, ref, created_at)
                SELECT ?1, ?2, ?3, 'redeem_pro', ?4, ?5
                WHERE (SELECT COALESCE(SUM(delta), 0) FROM points_ledger WHERE user_id = ?2) >= ?6`)
      .bind(id, userId, -option.points, option.id, t, option.points),
    db.prepare(`UPDATE users SET pro_until = MAX(COALESCE(pro_until, 0), ?1) + ?2
                WHERE id = ?3 AND EXISTS (SELECT 1 FROM points_ledger WHERE id = ?4)`)
      .bind(t, option.days * DAY, userId, id),
  ]);
  if (spend.meta.changes !== 1) fail(422, 'not_enough_points');
  await audit(db, 'points_redeemed', { userId, ip: clientIp(c), detail: option.id });
  return c.json(await referralSummary(c.env, userId));
});
