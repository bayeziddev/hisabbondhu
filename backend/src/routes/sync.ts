import { Hono } from 'hono';
import type { AppEnv } from '../env';
import { requireAuth, requireMember } from '../lib/auth';
import { fail, now, readJson } from '../lib/http';
import { decideReferral } from '../lib/referral';
import { partySchema, productSchema, pullSchema, pushSchema, txSchema } from '../lib/validate';
import { BIZ_COLUMNS } from './business';

export const syncRoutes = new Hono<AppEnv>();
syncRoutes.use('*', requireAuth);

/*
 * Each table gets ONE statement per push using json_each(?) — this keeps us far below D1's
 * per-invocation query and bound-parameter limits, even on the Free plan.
 * server_seq = MAX(existing) + position, assigned inside the same write transaction, so every
 * write gets a sequence number larger than anything a client could already have pulled.
 */
const PARTY_UPSERT = `
INSERT INTO parties (business_id, id, type, name, phone, address, note, opening_balance_paisa, reminder_at,
                     created_at, updated_at, deleted, device_id, server_seq)
SELECT ?1, j.value->>'id', j.value->>'type', j.value->>'name', j.value->>'phone', j.value->>'address', j.value->>'note',
       j.value->>'opening_balance_paisa', j.value->>'reminder_at', j.value->>'created_at', j.value->>'updated_at',
       j.value->>'deleted', j.value->>'device_id',
       (SELECT COALESCE(MAX(server_seq), 0) FROM parties WHERE business_id = ?1) + j.key + 1
FROM json_each(?2) AS j WHERE true
ON CONFLICT (business_id, id) DO UPDATE SET
  type = excluded.type, name = excluded.name, phone = excluded.phone, address = excluded.address, note = excluded.note,
  opening_balance_paisa = excluded.opening_balance_paisa, reminder_at = excluded.reminder_at,
  updated_at = excluded.updated_at, deleted = excluded.deleted, device_id = excluded.device_id, server_seq = excluded.server_seq
WHERE excluded.updated_at > parties.updated_at`;

const PRODUCT_UPSERT = `
INSERT INTO products (business_id, id, name, category, sku, barcode, unit, purchase_price_paisa, sale_price_paisa,
                      opening_stock_milli, min_stock_milli, created_at, updated_at, deleted, device_id, server_seq)
SELECT ?1, j.value->>'id', j.value->>'name', j.value->>'category', j.value->>'sku', j.value->>'barcode', j.value->>'unit',
       j.value->>'purchase_price_paisa', j.value->>'sale_price_paisa', j.value->>'opening_stock_milli', j.value->>'min_stock_milli',
       j.value->>'created_at', j.value->>'updated_at', j.value->>'deleted', j.value->>'device_id',
       (SELECT COALESCE(MAX(server_seq), 0) FROM products WHERE business_id = ?1) + j.key + 1
FROM json_each(?2) AS j WHERE true
ON CONFLICT (business_id, id) DO UPDATE SET
  name = excluded.name, category = excluded.category, sku = excluded.sku, barcode = excluded.barcode, unit = excluded.unit,
  purchase_price_paisa = excluded.purchase_price_paisa, sale_price_paisa = excluded.sale_price_paisa,
  opening_stock_milli = excluded.opening_stock_milli, min_stock_milli = excluded.min_stock_milli,
  updated_at = excluded.updated_at, deleted = excluded.deleted, device_id = excluded.device_id, server_seq = excluded.server_seq
WHERE excluded.updated_at > products.updated_at`;

// Transactions are immutable: a retry of the same id is silently ignored (idempotent).
const TX_INSERT = `
INSERT INTO transactions (business_id, id, type, party_id, product_id, qty_milli, rate_paisa, unit_cost_paisa, discount_paisa,
                          total_paisa, paid_paisa, method, category, note, occurred_at, created_at, device_id, reversal_of,
                          received_at, server_seq)
SELECT ?1, j.value->>'id', j.value->>'type', j.value->>'party_id', j.value->>'product_id', j.value->>'qty_milli',
       j.value->>'rate_paisa', j.value->>'unit_cost_paisa', j.value->>'discount_paisa', j.value->>'total_paisa',
       j.value->>'paid_paisa', j.value->>'method', j.value->>'category', j.value->>'note', j.value->>'occurred_at',
       j.value->>'created_at', j.value->>'device_id', j.value->>'reversal_of', ?3,
       (SELECT COALESCE(MAX(server_seq), 0) FROM transactions WHERE business_id = ?1) + j.key + 1
FROM json_each(?2) AS j WHERE true
ON CONFLICT (business_id, id) DO NOTHING`;

type Rejected = { entity: string; id: string; reason: string };

function validateAll<T>(entity: string, rows: unknown[], schema: { safeParse(v: unknown): { success: true; data: T } | { success: false; error: { issues: { message: string }[] } } }, rejected: Rejected[]): T[] {
  const ok: T[] = [];
  for (const raw of rows) {
    const r = schema.safeParse(raw);
    if (r.success) ok.push(r.data);
    else rejected.push({ entity, id: String((raw as { id?: unknown })?.id ?? ''), reason: r.error.issues[0]?.message ?? 'invalid' });
  }
  return ok;
}

syncRoutes.post('/:id/sync/push', async (c) => {
  const businessId = c.req.param('id');
  const userId = c.get('userId');
  const role = await requireMember(c.env.DB, businessId, userId);
  if (role === 'viewer') fail(403, 'forbidden');
  const body = await readJson(c, pushSchema);

  const rejected: Rejected[] = [];
  const parties = validateAll('parties', body.parties, partySchema, rejected)
    .map((p) => ({ ...p, deleted: p.deleted ? 1 : 0 }));
  const products = validateAll('products', body.products, productSchema, rejected)
    .map((p) => ({ ...p, deleted: p.deleted ? 1 : 0 }));
  const txs = validateAll('transactions', body.transactions, txSchema, rejected);

  const db = c.env.DB;
  const t = now();
  const stmts: D1PreparedStatement[] = [];
  if (parties.length) stmts.push(db.prepare(PARTY_UPSERT).bind(businessId, JSON.stringify(parties)));
  if (products.length) stmts.push(db.prepare(PRODUCT_UPSERT).bind(businessId, JSON.stringify(products)));
  if (txs.length) stmts.push(db.prepare(TX_INSERT).bind(businessId, JSON.stringify(txs), t));
  stmts.push(db.prepare('UPDATE businesses SET last_sync_at = ? WHERE id = ?').bind(t, businessId));
  stmts.push(
    db.prepare('INSERT INTO audit_logs (business_id, user_id, action, detail, created_at) VALUES (?, ?, ?, ?, ?)')
      .bind(businessId, userId, 'sync_push',
        JSON.stringify({ device: body.device_id, p: parties.length, pr: products.length, t: txs.length, rej: rejected.length, key: c.req.header('idempotency-key') ?? null }), t),
  );
  await db.batch(stmts); // one atomic transaction
  if (txs.length) c.executionCtx.waitUntil(decideReferral(c.env, userId).catch((e) => console.error('referral', e)));

  return c.json({ ok: true, accepted: { parties: parties.length, products: products.length, transactions: txs.length }, rejected });
});

syncRoutes.post('/:id/sync/pull', async (c) => {
  const businessId = c.req.param('id');
  await requireMember(c.env.DB, businessId, c.get('userId'));
  const { cursors, limit } = await readJson(c, pullSchema);
  const db = c.env.DB;

  const [biz, parties, products, txs] = await db.batch([
    db.prepare(`SELECT ${BIZ_COLUMNS} FROM businesses WHERE id = ?`).bind(businessId),
    db.prepare(`SELECT id, type, name, phone, address, note, opening_balance_paisa, reminder_at, created_at, updated_at,
                deleted, device_id, server_seq FROM parties WHERE business_id = ? AND server_seq > ? ORDER BY server_seq LIMIT ?`)
      .bind(businessId, cursors.parties, limit),
    db.prepare(`SELECT id, name, category, sku, barcode, unit, purchase_price_paisa, sale_price_paisa, opening_stock_milli,
                min_stock_milli, created_at, updated_at, deleted, device_id, server_seq
                FROM products WHERE business_id = ? AND server_seq > ? ORDER BY server_seq LIMIT ?`)
      .bind(businessId, cursors.products, limit),
    db.prepare(`SELECT id, type, party_id, product_id, qty_milli, rate_paisa, unit_cost_paisa, discount_paisa, total_paisa,
                paid_paisa, method, category, note, occurred_at, created_at, device_id, reversal_of, server_seq
                FROM transactions WHERE business_id = ? AND server_seq > ? ORDER BY server_seq LIMIT ?`)
      .bind(businessId, cursors.transactions, limit),
  ]);

  type Seq = { server_seq: number; deleted?: number };
  const strip = <T extends Seq>(rows: T[]) =>
    rows.map(({ server_seq: _s, ...rest }) => ('deleted' in rest ? { ...rest, deleted: !!rest.deleted } : rest));
  const last = (rows: Seq[], fallback: number) => (rows.length ? rows[rows.length - 1].server_seq : fallback);

  const pr = (parties.results ?? []) as Seq[];
  const pd = (products.results ?? []) as Seq[];
  const tx = (txs.results ?? []) as Seq[];

  return c.json({
    business: (biz.results ?? [])[0] ?? null,
    parties: strip(pr),
    products: strip(pd),
    transactions: strip(tx),
    cursors: {
      parties: last(pr, cursors.parties),
      products: last(pd, cursors.products),
      transactions: last(tx, cursors.transactions),
    },
    has_more: pr.length === limit || pd.length === limit || tx.length === limit,
  });
});
