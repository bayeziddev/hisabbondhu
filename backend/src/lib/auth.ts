import type { MiddlewareHandler } from 'hono';
import type { AppEnv } from '../env';
import { verifyJwt } from './crypto';
import { fail } from './http';

/** Requires a valid access token and a live (not revoked) session. */
export const requireAuth: MiddlewareHandler<AppEnv> = async (c, next) => {
  const header = c.req.header('authorization') || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : '';
  if (!token) fail(401, 'unauthorized');
  const payload = await verifyJwt(token, c.env.JWT_SECRET);
  if (!payload) fail(401, 'unauthorized');
  const session = await c.env.DB
    .prepare('SELECT revoked_at, expires_at FROM sessions WHERE id = ? AND user_id = ?')
    .bind(payload.sid, payload.sub)
    .first<{ revoked_at: number | null; expires_at: number }>();
  if (!session || session.revoked_at || session.expires_at < Date.now()) fail(401, 'session_expired');
  c.set('userId', payload.sub);
  c.set('sessionId', payload.sid);
  await next();
};

/** Tenant isolation: the user must be a member of the business in the URL. Returns the role. */
export async function requireMember(db: D1Database, businessId: string, userId: string, roles?: string[]): Promise<string> {
  const row = await db
    .prepare('SELECT role FROM business_members WHERE business_id = ? AND user_id = ?')
    .bind(businessId, userId)
    .first<{ role: string }>();
  if (!row) fail(404, 'business_not_found');
  if (roles && !roles.includes(row.role)) fail(403, 'forbidden');
  return row.role;
}

export async function audit(db: D1Database, action: string, opts: { businessId?: string | null; userId?: string | null; detail?: string; ip?: string }) {
  await db
    .prepare('INSERT INTO audit_logs (business_id, user_id, action, detail, ip, created_at) VALUES (?, ?, ?, ?, ?, ?)')
    .bind(opts.businessId ?? null, opts.userId ?? null, action, opts.detail ?? null, opts.ip ?? null, Date.now())
    .run();
}
