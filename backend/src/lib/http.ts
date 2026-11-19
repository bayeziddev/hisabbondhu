import type { Context } from 'hono';
import { HTTPException } from 'hono/http-exception';
import type { z } from 'zod';

/** Error with a stable machine-readable code the app maps to a translated message. */
export function fail(status: 400 | 401 | 403 | 404 | 409 | 413 | 422 | 429 | 500 | 502 | 503, error: string, message?: string): never {
  throw new HTTPException(status, {
    res: new Response(JSON.stringify({ error, message: message ?? error }), {
      status,
      headers: { 'content-type': 'application/json; charset=utf-8' },
    }),
  });
}

export async function readJson<S extends z.ZodTypeAny>(c: Context, schema: S): Promise<z.output<S>> {
  const len = Number(c.req.header('content-length') || '0');
  if (len > 2_500_000) fail(413, 'payload_too_large');
  let body: unknown;
  try {
    body = await c.req.json();
  } catch {
    fail(400, 'invalid_json');
  }
  const parsed = schema.safeParse(body);
  if (!parsed.success) {
    const first = parsed.error.issues[0];
    fail(422, 'invalid_input', `${first?.path.join('.') || 'body'}: ${first?.message ?? 'invalid'}`);
  }
  return parsed.data;
}

export function clientIp(c: Context): string {
  return c.req.header('cf-connecting-ip') || c.req.header('x-forwarded-for')?.split(',')[0]?.trim() || 'unknown';
}

export const now = () => Date.now();
