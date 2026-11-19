import { Hono } from 'hono';
import type { AppEnv } from '../env';
import { uuid } from '../lib/crypto';
import { clientIp, fail, now, readJson } from '../lib/http';
import { contactSchema } from '../lib/validate';

export const publicRoutes = new Hono<AppEnv>();

/** Contact form (website + app). Stored in D1; optionally forwarded to a webhook. */
publicRoutes.post('/contact', async (c) => {
  const input = await readJson(c, contactSchema);
  if (input.website) return c.json({ ok: true }); // honeypot hit — pretend success
  const ip = clientIp(c);
  const t = now();
  const recent = await c.env.DB
    .prepare('SELECT COUNT(*) AS n FROM support_messages WHERE ip = ? AND created_at > ?')
    .bind(ip, t - 3600_000)
    .first<{ n: number }>();
  if (recent && recent.n >= 5) fail(429, 'rate_limited');

  const id = uuid();
  await c.env.DB
    .prepare(`INSERT INTO support_messages (id, name, phone, email, subject, message, source, ip, created_at)
              VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)`)
    .bind(id, input.name, input.phone || null, input.email || null, input.subject, input.message, input.source ?? 'web', ip, t)
    .run();

  if (c.env.SUPPORT_WEBHOOK_URL) {
    const text = `New HisabBondhu message (${input.subject}) from ${input.name}: ${input.message.slice(0, 500)}`;
    c.executionCtx.waitUntil(
      fetch(c.env.SUPPORT_WEBHOOK_URL, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ text, content: text }),
      }).catch(() => undefined),
    );
  }
  return c.json({ ok: true, id });
});
