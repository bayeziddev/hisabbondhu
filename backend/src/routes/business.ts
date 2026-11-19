import { Hono } from 'hono';
import type { AppEnv } from '../env';
import { audit, requireAuth, requireMember } from '../lib/auth';
import { uuid } from '../lib/crypto';
import { clientIp, fail, now, readJson } from '../lib/http';
import { businessSchema } from '../lib/validate';

export const businessRoutes = new Hono<AppEnv>();
businessRoutes.use('*', requireAuth);

type BizRow = {
  id: string; name: string; owner_name: string; phone: string; address: string;
  business_type: string; opening_cash_paisa: number; updated_at: number;
};

export const BIZ_COLUMNS = 'id, name, owner_name, phone, address, business_type, opening_cash_paisa, updated_at';

businessRoutes.get('/', async (c) => {
  const rows = await c.env.DB
    .prepare(`SELECT b.${BIZ_COLUMNS.split(', ').join(', b.')}, m.role FROM businesses b
              JOIN business_members m ON m.business_id = b.id WHERE m.user_id = ? ORDER BY b.created_at`)
    .bind(c.get('userId'))
    .all<BizRow & { role: string }>();
  return c.json({ businesses: rows.results });
});

/**
 * Called right after login. If the account already has a business, return it (the phone then merges
 * its offline data into that business). Otherwise create one using the phone's local business id.
 */
businessRoutes.post('/link', async (c) => {
  const input = await readJson(c, businessSchema);
  const userId = c.get('userId');
  const db = c.env.DB;

  const existing = await db
    .prepare(`SELECT b.${BIZ_COLUMNS.split(', ').join(', b.')} FROM businesses b
              JOIN business_members m ON m.business_id = b.id
              WHERE m.user_id = ? ORDER BY (m.role = 'owner') DESC, b.created_at LIMIT 1`)
    .bind(userId)
    .first<BizRow>();
  if (existing) return c.json(existing);

  const t = now();
  // Never trust a client-chosen id that already exists (could belong to another account).
  const taken = await db.prepare('SELECT 1 FROM businesses WHERE id = ?').bind(input.id).first();
  const id = taken ? uuid() : input.id;
  await db.batch([
    db.prepare(`INSERT INTO businesses (id, owner_user_id, name, owner_name, phone, address, business_type, opening_cash_paisa, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`)
      .bind(id, userId, input.name, input.owner_name, input.phone, input.address, input.business_type, input.opening_cash_paisa, t, input.updated_at || t),
    db.prepare(`INSERT INTO business_members (business_id, user_id, role, created_at) VALUES (?, ?, 'owner', ?)`)
      .bind(id, userId, t),
  ]);
  await audit(db, 'business_created', { businessId: id, userId, ip: clientIp(c) });
  const row = await db.prepare(`SELECT ${BIZ_COLUMNS} FROM businesses WHERE id = ?`).bind(id).first<BizRow>();
  return c.json(row, 201);
});

businessRoutes.put('/:id', async (c) => {
  const businessId = c.req.param('id');
  await requireMember(c.env.DB, businessId, c.get('userId'), ['owner', 'manager']);
  const input = await readJson(c, businessSchema);
  if (input.id !== businessId) fail(422, 'invalid_input', 'id mismatch');
  // Last-write-wins on updated_at for the profile.
  await c.env.DB
    .prepare(`UPDATE businesses SET name = ?, owner_name = ?, phone = ?, address = ?, business_type = ?,
              opening_cash_paisa = ?, updated_at = ? WHERE id = ? AND updated_at < ?`)
    .bind(input.name, input.owner_name, input.phone, input.address, input.business_type,
      input.opening_cash_paisa, input.updated_at, businessId, input.updated_at)
    .run();
  const row = await c.env.DB.prepare(`SELECT ${BIZ_COLUMNS} FROM businesses WHERE id = ?`).bind(businessId).first<BizRow>();
  return c.json(row);
});
