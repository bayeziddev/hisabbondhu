const enc = new TextEncoder();

export function b64url(bytes: Uint8Array): string {
  let s = '';
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

export function b64urlDecode(input: string): Uint8Array {
  const s = input.replace(/-/g, '+').replace(/_/g, '/') + '==='.slice((input.length + 3) % 4);
  const bin = atob(s);
  const out = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) out[i] = bin.charCodeAt(i);
  return out;
}

export function randomToken(bytes = 32): string {
  const buf = new Uint8Array(bytes);
  crypto.getRandomValues(buf);
  return b64url(buf);
}

/** Uniform 6-digit code using rejection sampling (no modulo bias). */
export function randomOtp(): string {
  const buf = new Uint32Array(1);
  const limit = Math.floor(0xffffffff / 1_000_000) * 1_000_000;
  for (;;) {
    crypto.getRandomValues(buf);
    if (buf[0] < limit) return (buf[0] % 1_000_000).toString().padStart(6, '0');
  }
}

export async function sha256Hex(input: string): Promise<string> {
  const digest = await crypto.subtle.digest('SHA-256', enc.encode(input));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/** Constant-time comparison of two strings of equal length. */
export function safeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

export interface JwtPayload {
  sub: string;
  sid: string;
  iat: number;
  exp: number;
  iss: string;
}

const ISSUER = 'hisabbondhu';

async function hmacKey(secret: string): Promise<CryptoKey> {
  return crypto.subtle.importKey('raw', enc.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign', 'verify']);
}

export async function signJwt(sub: string, sid: string, secret: string, ttlSeconds: number, now = Date.now()): Promise<string> {
  const iat = Math.floor(now / 1000);
  const header = b64url(enc.encode(JSON.stringify({ alg: 'HS256', typ: 'JWT' })));
  const payload = b64url(enc.encode(JSON.stringify({ sub, sid, iat, exp: iat + ttlSeconds, iss: ISSUER } satisfies JwtPayload)));
  const data = `${header}.${payload}`;
  const sig = await crypto.subtle.sign('HMAC', await hmacKey(secret), enc.encode(data));
  return `${data}.${b64url(new Uint8Array(sig))}`;
}

export async function verifyJwt(token: string, secret: string, now = Date.now()): Promise<JwtPayload | null> {
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  const [h, p, s] = parts;
  try {
    const header = JSON.parse(new TextDecoder().decode(b64urlDecode(h)));
    if (header.alg !== 'HS256') return null;
    const ok = await crypto.subtle.verify('HMAC', await hmacKey(secret), b64urlDecode(s), enc.encode(`${h}.${p}`));
    if (!ok) return null;
    const payload = JSON.parse(new TextDecoder().decode(b64urlDecode(p))) as JwtPayload;
    if (payload.iss !== ISSUER || typeof payload.sub !== 'string' || typeof payload.sid !== 'string') return null;
    if (typeof payload.exp !== 'number' || payload.exp * 1000 <= now) return null;
    return payload;
  } catch {
    return null;
  }
}

export function uuid(): string {
  return crypto.randomUUID();
}
