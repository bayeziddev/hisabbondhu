import { z } from 'zod';

const id = z.string().min(8).max(64).regex(/^[A-Za-z0-9-]+$/);
const ms = z.number().int().min(0).max(4102444800000); // up to year 2100
const money = z.number().int().min(-1_000_000_000_000).max(1_000_000_000_000);
const shortText = (max: number) => z.string().max(max).nullish().transform((v) => (v == null || v === '' ? null : v));

export const businessSchema = z.object({
  id,
  name: z.string().max(120).default(''),
  owner_name: z.string().max(120).default(''),
  phone: z.string().max(30).default(''),
  address: z.string().max(300).default(''),
  business_type: z.string().max(40).default('general_store'),
  opening_cash_paisa: money.default(0),
  updated_at: ms,
});
export type BusinessInput = z.infer<typeof businessSchema>;

export const partySchema = z.object({
  id,
  type: z.enum(['CUSTOMER', 'SUPPLIER']),
  name: z.string().min(1).max(120),
  phone: shortText(30),
  address: shortText(300),
  note: shortText(1000),
  opening_balance_paisa: money.default(0),
  reminder_at: ms.nullish().transform((v) => v ?? null),
  created_at: ms,
  updated_at: ms,
  deleted: z.boolean().default(false),
  device_id: id,
});
export type PartyInput = z.infer<typeof partySchema>;

export const productSchema = z.object({
  id,
  name: z.string().min(1).max(160),
  category: shortText(80),
  sku: shortText(60),
  barcode: shortText(60),
  unit: z.string().min(1).max(20).default('pcs'),
  purchase_price_paisa: money.default(0),
  sale_price_paisa: money.default(0),
  opening_stock_milli: money.default(0),
  min_stock_milli: money.default(0),
  created_at: ms,
  updated_at: ms,
  deleted: z.boolean().default(false),
  device_id: id,
});
export type ProductInput = z.infer<typeof productSchema>;

export const TX_TYPES = [
  'SALE', 'PAYMENT_IN', 'PAYMENT_OUT', 'PURCHASE', 'EXPENSE',
  'SALE_RETURN', 'PURCHASE_RETURN', 'STOCK_ADJUST', 'CASH_IN', 'CASH_OUT',
] as const;

export const txSchema = z.object({
  id,
  type: z.enum(TX_TYPES),
  party_id: id.nullish().transform((v) => v ?? null),
  product_id: id.nullish().transform((v) => v ?? null),
  qty_milli: money.default(0),
  rate_paisa: money.default(0),
  unit_cost_paisa: money.default(0),
  discount_paisa: money.default(0),
  total_paisa: money,
  paid_paisa: money,
  method: z.enum(['CASH', 'BKASH', 'NAGAD', 'ROCKET', 'BANK', 'CARD', 'OTHER']).default('CASH'),
  category: shortText(40),
  note: shortText(500),
  occurred_at: ms,
  created_at: ms,
  device_id: id,
  reversal_of: id.nullish().transform((v) => v ?? null),
}).superRefine((t, ctx) => {
  // Ledger invariants (same rules as the app). Reversals carry negated amounts.
  const sign = t.reversal_of ? -1 : 1;
  const total = t.total_paisa * sign;
  const paid = t.paid_paisa * sign;
  if (t.type !== 'STOCK_ADJUST') {
    if (total <= 0) ctx.addIssue({ code: 'custom', message: 'total must be positive' });
    if (paid < 0 || paid > total) ctx.addIssue({ code: 'custom', message: 'paid must be between 0 and total' });
  }
  if ((t.type === 'PAYMENT_IN' || t.type === 'PAYMENT_OUT') && !t.party_id) {
    ctx.addIssue({ code: 'custom', message: 'payments need a party' });
  }
});
export type TxInput = z.infer<typeof txSchema>;

export const pushSchema = z.object({
  device_id: id,
  parties: z.array(z.unknown()).max(500).default([]),
  products: z.array(z.unknown()).max(500).default([]),
  transactions: z.array(z.unknown()).max(500).default([]),
});

export const pullSchema = z.object({
  cursors: z.object({
    parties: z.number().int().min(0).default(0),
    products: z.number().int().min(0).default(0),
    transactions: z.number().int().min(0).default(0),
  }).default({}),
  limit: z.number().int().min(1).max(1000).default(500),
});

export const contactSchema = z.object({
  name: z.string().trim().min(1).max(100),
  phone: z.string().trim().max(30).nullish(),
  email: z.string().trim().max(120).email().nullish().or(z.literal('')),
  subject: z.string().trim().min(1).max(120),
  message: z.string().trim().min(5).max(4000),
  source: z.string().max(20).optional(),
  website: z.string().max(0).optional(), // honeypot: must stay empty
});
