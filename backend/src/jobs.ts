import type { Env, Job } from './env';
import { sweepReferrals } from './lib/referral';

const SNAPSHOT_KEEP = 8; // weekly snapshots kept per business

/** Removes every R2 object of a business (backups, exports, attachments). */
export async function deleteBusinessFiles(env: Env, businessId: string): Promise<void> {
  if (!env.FILES) return;
  let cursor: string | undefined;
  do {
    const list = await env.FILES.list({ prefix: `b/${businessId}/`, cursor });
    if (list.objects.length) await env.FILES.delete(list.objects.map((o) => o.key));
    cursor = list.truncated ? list.cursor : undefined;
  } while (cursor);
}

/** Weekly disaster-recovery snapshot (beyond D1 Time Travel's 30 days). Private bucket, never public. */
export async function snapshotBusiness(env: Env, businessId: string): Promise<void> {
  if (!env.FILES) return; // without R2, D1 Time Travel (30 days) is the backup
  const db = env.DB;
  const biz = await db.prepare('SELECT * FROM businesses WHERE id = ?').bind(businessId).first();
  if (!biz) return;
  const [parties, products, txs] = await db.batch([
    db.prepare('SELECT * FROM parties WHERE business_id = ?').bind(businessId),
    db.prepare('SELECT * FROM products WHERE business_id = ?').bind(businessId),
    db.prepare('SELECT * FROM transactions WHERE business_id = ?').bind(businessId),
  ]);
  const day = new Date().toISOString().slice(0, 10);
  await env.FILES!.put(
    `b/${businessId}/snapshots/${day}.json`,
    JSON.stringify({ business: biz, parties: parties.results, products: products.results, transactions: txs.results }),
    { httpMetadata: { contentType: 'application/json' } },
  );
  const list = await env.FILES!.list({ prefix: `b/${businessId}/snapshots/` });
  const keys = list.objects.map((o) => o.key).sort();
  if (keys.length > SNAPSHOT_KEEP) await env.FILES!.delete(keys.slice(0, keys.length - SNAPSHOT_KEEP));
}

export async function runJob(env: Env, job: Job): Promise<void> {
  switch (job.type) {
    case 'delete_business_files':
      return deleteBusinessFiles(env, job.businessId);
    case 'snapshot_business':
      return snapshotBusiness(env, job.businessId);
  }
}

/** Daily maintenance (cron). */
export async function dailyMaintenance(env: Env): Promise<void> {
  const t = Date.now();
  await env.DB.batch([
    env.DB.prepare('DELETE FROM otp_codes WHERE created_at < ?').bind(t - 24 * 3600_000),
    env.DB.prepare('DELETE FROM sessions WHERE (revoked_at IS NOT NULL AND revoked_at < ?) OR expires_at < ?')
      .bind(t - 30 * 24 * 3600_000, t - 30 * 24 * 3600_000),
    env.DB.prepare('DELETE FROM audit_logs WHERE created_at < ?').bind(t - 365 * 24 * 3600_000),
    env.DB.prepare("DELETE FROM support_messages WHERE created_at < ? AND status != 'open'").bind(t - 2 * 365 * 24 * 3600_000),
  ]);

  await sweepReferrals(env);

  // Sundays: snapshot businesses that synced in the last 7 days (only when R2 is available).
  if (env.FILES && new Date(t).getUTCDay() === 0) {
    const active = await env.DB
      .prepare('SELECT id FROM businesses WHERE last_sync_at > ? LIMIT 5000')
      .bind(t - 7 * 24 * 3600_000)
      .all<{ id: string }>();
    for (const b of active.results) {
      if (env.JOBS) await env.JOBS.send({ type: 'snapshot_business', businessId: b.id });
      else await snapshotBusiness(env, b.id);
    }
  }
}
