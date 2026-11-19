// End-to-end test against a running Worker (local `wrangler dev` with OTP_DEV_MODE=true).
// Usage: API=http://127.0.0.1:8787 node scripts/e2e.mjs
const API = process.env.API || 'http://127.0.0.1:8787';
const phone = '01712' + String(Math.floor(Math.random() * 1e6)).padStart(6, '0');
let failures = 0;

async function call(method, path, body, token) {
  const res = await fetch(API + path, {
    method,
    headers: { 'content-type': 'application/json', ...(token ? { authorization: `Bearer ${token}` } : {}) },
    body: body ? JSON.stringify(body) : undefined,
  });
  let json = null;
  try { json = await res.json(); } catch {}
  return { status: res.status, json };
}
function check(name, cond, extra) {
  console.log(`${cond ? '✓' : '✗'} ${name}` + (cond ? '' : ` ${JSON.stringify(extra)}`));
  if (!cond) failures++;
}
const uuid = () => crypto.randomUUID();
const t0 = Date.now();

const health = await call('GET', '/v1/health');
check('health', health.status === 200 && health.json.db === true, health);

const bad = await call('POST', '/v1/auth/request-otp', { phone: '12345' });
check('invalid phone rejected', bad.status === 422 && bad.json.error === 'invalid_phone', bad);

const otp = await call('POST', '/v1/auth/request-otp', { phone });
check('request otp', otp.status === 200 && /^\d{6}$/.test(otp.json.dev_code), otp);
const again = await call('POST', '/v1/auth/request-otp', { phone });
check('resend throttled', again.status === 429, again);

const wrong = await call('POST', '/v1/auth/verify-otp', { phone, code: otp.json.dev_code === '000000' ? '111111' : '000000', device_id: 'dev-e2e-0001' });
check('wrong code rejected', wrong.status === 400 && wrong.json.error === 'invalid_code', wrong);

const ver = await call('POST', '/v1/auth/verify-otp', { phone, code: otp.json.dev_code, device_id: 'dev-e2e-0001', device_name: 'e2e' });
check('verify otp', ver.status === 200 && ver.json.access_token && ver.json.refresh_token, ver);
const reuse = await call('POST', '/v1/auth/verify-otp', { phone, code: otp.json.dev_code });
check('otp single-use', reuse.status === 400, reuse);
let token = ver.json.access_token;
let refresh = ver.json.refresh_token;

check('auth required', (await call('GET', '/v1/account')).status === 401);

const bizId = uuid();
const link = await call('POST', '/v1/businesses/link', { id: bizId, name: 'করিম স্টোর', updated_at: t0 }, token);
check('link creates business', link.status === 201 && link.json.id === bizId, link);
const link2 = await call('POST', '/v1/businesses/link', { id: uuid(), name: 'other', updated_at: t0 }, token);
check('link returns existing business', link2.status === 200 && link2.json.id === bizId, link2);

const partyId = uuid();
const txId = uuid();
const party = { id: partyId, type: 'CUSTOMER', name: 'করিম ভাই', phone: '01811111111', opening_balance_paisa: 0, created_at: t0, updated_at: t0, deleted: false, device_id: 'dev-e2e-0001' };
const sale = { id: txId, type: 'SALE', party_id: partyId, total_paisa: 125000, paid_paisa: 75000, method: 'CASH', occurred_at: t0, created_at: t0, device_id: 'dev-e2e-0001' };
const bogus = { id: uuid(), type: 'SALE', total_paisa: 100, paid_paisa: 999, occurred_at: t0, created_at: t0, device_id: 'dev-e2e-0001' };
const push = await call('POST', `/v1/businesses/${bizId}/sync/push`, { device_id: 'dev-e2e-0001', parties: [party], transactions: [sale, bogus] }, token);
check('push accepted + invalid row rejected', push.status === 200 && push.json.accepted.transactions === 1 && push.json.rejected.length === 1, push);

const pushAgain = await call('POST', `/v1/businesses/${bizId}/sync/push`, { device_id: 'dev-e2e-0001', transactions: [sale] }, token);
check('push is idempotent', pushAgain.status === 200, pushAgain);

// Older edit must not overwrite newer (last-write-wins)
await call('POST', `/v1/businesses/${bizId}/sync/push`, { device_id: 'dev-e2e-0002', parties: [{ ...party, name: 'Newer', updated_at: t0 + 10 }] }, token);
await call('POST', `/v1/businesses/${bizId}/sync/push`, { device_id: 'dev-e2e-0003', parties: [{ ...party, name: 'Older', updated_at: t0 + 5 }] }, token);

const pull = await call('POST', `/v1/businesses/${bizId}/sync/pull`, { cursors: {}, limit: 500 }, token);
check('pull returns data', pull.status === 200 && pull.json.transactions.length === 1 && pull.json.parties.length === 1, pull);
check('no duplicate transaction', pull.json.transactions.filter((t) => t.id === txId).length === 1, pull.json.transactions);
check('last-write-wins', pull.json.parties[0]?.name === 'Newer', pull.json.parties);
check('booleans mapped', pull.json.parties[0]?.deleted === false, pull.json.parties[0]);
check('business profile in pull', pull.json.business?.name === 'করিম স্টোর', pull.json.business);

const pull2 = await call('POST', `/v1/businesses/${bizId}/sync/pull`, { cursors: pull.json.cursors }, token);
check('cursor pull returns nothing new', pull2.json.transactions.length === 0 && pull2.json.parties.length === 0, pull2.json);

// Tenant isolation: a second user must not see business 1
const phone2 = '01912' + String(Math.floor(Math.random() * 1e6)).padStart(6, '0');
const o2 = await call('POST', '/v1/auth/request-otp', { phone: phone2 });
const v2 = await call('POST', '/v1/auth/verify-otp', { phone: phone2, code: o2.json.dev_code });
const spy = await call('POST', `/v1/businesses/${bizId}/sync/pull`, { cursors: {} }, v2.json.access_token);
check('tenant isolation (pull)', spy.status === 404, spy);
const spyPush = await call('POST', `/v1/businesses/${bizId}/sync/push`, { device_id: 'evil-device-01', parties: [] }, v2.json.access_token);
check('tenant isolation (push)', spyPush.status === 404, spyPush);
const hijack = await call('POST', '/v1/businesses/link', { id: bizId, name: 'hijack', updated_at: t0 }, v2.json.access_token);
check('cannot claim another business id', hijack.status === 201 && hijack.json.id !== bizId, hijack);

// Refresh rotation + reuse detection
const r1 = await call('POST', '/v1/auth/refresh', { refresh_token: refresh });
check('refresh works', r1.status === 200 && r1.json.refresh_token !== refresh, r1);
const replay = await call('POST', '/v1/auth/refresh', { refresh_token: refresh });
check('old refresh token rejected', replay.status === 401, replay);
const afterReuse = await call('GET', '/v1/account', null, r1.json.access_token);
check('session revoked after reuse', afterReuse.status === 401, afterReuse);

// Fresh login for deletion test
await new Promise((r) => setTimeout(r, 10));
const contact = await call('POST', '/v1/public/contact', { name: 'Test', subject: 'Hello', message: 'This is a test message', source: 'e2e' });
check('contact form', contact.status === 200 && contact.json.ok, contact);

const v2token = v2.json.access_token;
const exp = await call('GET', '/v1/account/export', null, v2token);
check('export works', exp.status === 200 && exp.json.format === 'hisabbondhu-export', exp);
const del = await call('DELETE', '/v1/account', null, v2token);
check('delete account', del.status === 200 && del.json.ok, del);
const afterDel = await call('GET', '/v1/account', null, v2token);
check('token dead after deletion', afterDel.status === 401, afterDel);

console.log(failures ? `\n${failures} check(s) failed` : '\nAll e2e checks passed');
process.exit(failures ? 1 : 0);
