import { Hono } from 'hono';
import type { AppEnv } from '../env';
import { safeEqual, sha256Hex } from '../lib/crypto';
import { fail } from '../lib/http';

/**
 * Owner-only exports: shop leads and the support inbox.
 * Authorization: Bearer <ADMIN_TOKEN secret>. If ADMIN_TOKEN is not set the routes do not exist.
 *   curl -H "Authorization: Bearer $ADMIN_TOKEN" "https://<api>/v1/admin/leads?format=csv" -o leads.csv
 */
export const adminRoutes = new Hono<AppEnv>();

adminRoutes.use('*', async (c, next) => {
  const token = c.env.ADMIN_TOKEN;
  if (!token || token.length < 24) fail(404, 'not_found');
  const header = c.req.header('authorization') || '';
  const given = header.startsWith('Bearer ') ? header.slice(7) : '';
  // Hash both sides so the comparison is constant-time regardless of length.
  if (!safeEqual(await sha256Hex(given), await sha256Hex(token))) fail(401, 'unauthorized');
  await next();
});

function toCsv(rows: Record<string, unknown>[]): string {
  if (!rows.length) return '';
  const cols = Object.keys(rows[0]);
  const esc = (v: unknown) => {
    let s = v == null ? '' : String(v);
    if (/^[=+\-@]/.test(s)) s = `'${s}`; // spreadsheet formula injection guard
    return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
  };
  return '﻿' + [cols.join(','), ...rows.map((r) => cols.map((k) => esc(r[k])).join(','))].join('\n') + '\n';
}

const iso = (ms: unknown) => (typeof ms === 'number' ? new Date(ms).toISOString() : '');

adminRoutes.get('/leads', async (c) => {
  const rows = await c.env.DB.prepare(`
    SELECT u.name, u.email, u.phone, u.marketing_opt_in, u.created_at, u.last_login_at,
           b.name AS shop_name, b.business_type, b.address AS shop_address, b.last_sync_at,
           (SELECT COUNT(*) FROM transactions t WHERE t.business_id = b.id) AS transactions
    FROM users u
    LEFT JOIN businesses b ON b.owner_user_id = u.id
    ORDER BY u.created_at DESC LIMIT 10000`).all<Record<string, unknown>>();
  const list = rows.results.map((r) => ({
    ...r,
    marketing_opt_in: r.marketing_opt_in ? 'yes' : 'no',
    created_at: iso(r.created_at),
    last_login_at: iso(r.last_login_at),
    last_sync_at: iso(r.last_sync_at),
  }));
  if (c.req.query('format') === 'csv') {
    return c.body(toCsv(list), 200, {
      'content-type': 'text/csv; charset=utf-8',
      'content-disposition': 'attachment; filename="hisabbondhu-leads.csv"',
    });
  }
  return c.json({ count: list.length, leads: list });
});

adminRoutes.get('/support', async (c) => {
  const rows = await c.env.DB
    .prepare('SELECT id, name, phone, email, subject, message, source, status, created_at FROM support_messages ORDER BY created_at DESC LIMIT 1000')
    .all<Record<string, unknown>>();
  const list = rows.results.map((r) => ({ ...r, created_at: iso(r.created_at) }));
  if (c.req.query('format') === 'csv') {
    return c.body(toCsv(list), 200, { 'content-type': 'text/csv; charset=utf-8' });
  }
  return c.json({ count: list.length, messages: list });
});

adminRoutes.get('/stats', async (c) => {
  const t = Date.now();
  const s = await c.env.DB.prepare(`SELECT
      (SELECT COUNT(*) FROM users) AS users,
      (SELECT COUNT(*) FROM users WHERE created_at > ?) AS users_7d,
      (SELECT COUNT(*) FROM businesses WHERE last_sync_at > ?) AS active_shops_7d,
      (SELECT COUNT(*) FROM transactions) AS transactions,
      (SELECT COUNT(*) FROM support_messages WHERE status = 'new') AS new_messages`)
    .bind(t - 7 * 86400_000, t - 7 * 86400_000).first();
  return c.json(s);
});
