import { describe, expect, it } from 'vitest';
import { randomOtp, safeEqual, sha256Hex, signJwt, verifyJwt } from '../src/lib/crypto';
import { maskPhone, normalizeBdPhone } from '../src/lib/phone';
import { contactSchema, partySchema, txSchema } from '../src/lib/validate';

describe('phone', () => {
  it('normalises Bangladeshi numbers', () => {
    expect(normalizeBdPhone('01712345678')).toBe('+8801712345678');
    expect(normalizeBdPhone('+880 1712-345678')).toBe('+8801712345678');
    expect(normalizeBdPhone('০১৭১২৩৪৫৬৭৮')).toBe('+8801712345678');
    expect(normalizeBdPhone('01212345678')).toBeNull();
    expect(normalizeBdPhone(42)).toBeNull();
    expect(maskPhone('+8801712345678')).toBe('+88017•••••678');
  });
});

describe('crypto', () => {
  const secret = 'test-secret-that-is-long-enough-123456';
  it('signs and verifies JWT', async () => {
    const t = await signJwt('user-1', 'sess-1', secret, 60);
    const p = await verifyJwt(t, secret);
    expect(p?.sub).toBe('user-1');
    expect(p?.sid).toBe('sess-1');
  });
  it('rejects tampered, wrong-key and expired tokens', async () => {
    const t = await signJwt('user-1', 'sess-1', secret, 60);
    const [h, p, s] = t.split('.');
    const forged = JSON.parse(Buffer.from(p.replace(/-/g, '+').replace(/_/g, '/'), 'base64').toString());
    forged.sub = 'attacker';
    const fp = Buffer.from(JSON.stringify(forged)).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    expect(await verifyJwt(`${h}.${fp}.${s}`, secret)).toBeNull();
    expect(await verifyJwt(t, 'another-secret-value-000000000000')).toBeNull();
    const old = await signJwt('u', 's', secret, 60, Date.now() - 3600_000);
    expect(await verifyJwt(old, secret)).toBeNull();
    expect(await verifyJwt('garbage', secret)).toBeNull();
  });
  it('makes 6-digit OTPs', () => {
    for (let i = 0; i < 200; i++) expect(randomOtp()).toMatch(/^\d{6}$/);
  });
  it('hashes and compares', async () => {
    const a = await sha256Hex('abc');
    expect(a).toBe('ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad');
    expect(safeEqual(a, a)).toBe(true);
    expect(safeEqual(a, a.replace('b', 'c'))).toBe(false);
    expect(safeEqual('a', 'ab')).toBe(false);
  });
});

const base = {
  id: '3f2b6c1e-1111-4222-8333-944455556666',
  device_id: 'device-0001-abcd',
  occurred_at: 1_790_000_000_000,
  created_at: 1_790_000_000_000,
};

describe('ledger validation', () => {
  it('accepts a credit sale', () => {
    expect(txSchema.safeParse({ ...base, type: 'SALE', total_paisa: 125000, paid_paisa: 75000, party_id: 'party-0001-abcd' }).success).toBe(true);
  });
  it('rejects paid > total and non-positive totals', () => {
    expect(txSchema.safeParse({ ...base, type: 'SALE', total_paisa: 100, paid_paisa: 200 }).success).toBe(false);
    expect(txSchema.safeParse({ ...base, type: 'EXPENSE', total_paisa: 0, paid_paisa: 0 }).success).toBe(false);
  });
  it('requires a party for payments', () => {
    expect(txSchema.safeParse({ ...base, type: 'PAYMENT_IN', total_paisa: 100, paid_paisa: 100 }).success).toBe(false);
  });
  it('accepts reversal rows with negated amounts', () => {
    expect(txSchema.safeParse({
      ...base, type: 'SALE', total_paisa: -125000, paid_paisa: -75000, reversal_of: 'orig-0001-abcd',
    }).success).toBe(true);
  });
  it('accepts stock adjustments with zero money', () => {
    expect(txSchema.safeParse({ ...base, type: 'STOCK_ADJUST', total_paisa: 0, paid_paisa: 0, product_id: 'prod-0001-abcd', qty_milli: -2000 }).success).toBe(true);
  });
  it('rejects unknown types and SQL-ish ids', () => {
    expect(txSchema.safeParse({ ...base, type: 'DROP', total_paisa: 1, paid_paisa: 1 }).success).toBe(false);
    expect(txSchema.safeParse({ ...base, id: "x'; DROP TABLE users; --", type: 'SALE', total_paisa: 1, paid_paisa: 1 }).success).toBe(false);
  });
  it('validates parties', () => {
    expect(partySchema.safeParse({ ...base, type: 'CUSTOMER', name: 'করিম ভাই', updated_at: base.created_at }).success).toBe(true);
    expect(partySchema.safeParse({ ...base, type: 'CUSTOMER', name: '', updated_at: base.created_at }).success).toBe(false);
  });
  it('contact form honeypot + limits', () => {
    expect(contactSchema.safeParse({ name: 'A', subject: 'Hi', message: 'Hello there' }).success).toBe(true);
    expect(contactSchema.safeParse({ name: 'A', subject: 'Hi', message: 'no' }).success).toBe(false);
  });
});
