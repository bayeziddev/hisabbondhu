import { Hono } from 'hono';
import { cors } from 'hono/cors';
import { HTTPException } from 'hono/http-exception';
import type { AppEnv, Env, Job } from './env';
import { clientIp } from './lib/http';
import { dailyMaintenance, runJob } from './jobs';
import { accountRoutes } from './routes/account';
import { adminRoutes } from './routes/admin';
import { authRoutes } from './routes/auth';
import { businessRoutes } from './routes/business';
import { publicRoutes } from './routes/public';
import { referralRoutes } from './routes/referral';
import { syncRoutes } from './routes/sync';
import { VERSION } from './version';


const app = new Hono<AppEnv>();

// Security headers for every response.
app.use('*', async (c, next) => {
  await next();
  c.header('X-Content-Type-Options', 'nosniff');
  c.header('Referrer-Policy', 'no-referrer');
  c.header('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');
  c.header('X-Frame-Options', 'DENY');
  c.header('Cache-Control', 'no-store');
});

// CORS only for our website (contact + data-deletion forms). The Android app does not need CORS.
app.use('/v1/*', async (c, next) => {
  const allowed = (c.env.ALLOWED_ORIGINS || '').split(',').map((s) => s.trim()).filter(Boolean);
  return cors({
    origin: (origin) => (allowed.includes(origin) ? origin : null),
    allowMethods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowHeaders: ['Content-Type', 'Authorization', 'Idempotency-Key'],
    maxAge: 86400,
  })(c, next);
});

// Optional platform rate limiter (per IP) — configure [[ratelimits]] in wrangler.toml.
app.use('/v1/*', async (c, next) => {
  c.set('ip', clientIp(c));
  if (c.env.API_LIMITER) {
    const { success } = await c.env.API_LIMITER.limit({ key: c.get('ip') });
    if (!success) return c.json({ error: 'rate_limited', message: 'Too many requests' }, 429);
  }
  return next();
});

app.get('/', (c) => c.json({ name: 'HisabBondhu API', version: VERSION, docs: '/v1/health' }));
app.get('/v1/health', async (c) => {
  const db = await c.env.DB.prepare('SELECT 1 AS ok').first<{ ok: number }>().catch(() => null);
  return c.json({ ok: true, version: VERSION, db: db?.ok === 1, time: Date.now() });
});

app.route('/v1/auth', authRoutes);
app.route('/v1/businesses', businessRoutes);
app.route('/v1/businesses', syncRoutes);
app.route('/v1/account', accountRoutes);
app.route('/v1/public', publicRoutes);
app.route('/v1/referral', referralRoutes);
app.route('/v1/admin', adminRoutes);

app.notFound((c) => c.json({ error: 'not_found' }, 404));
app.onError((err, c) => {
  if (err instanceof HTTPException) return err.getResponse();
  console.error('unhandled', err);
  return c.json({ error: 'server_error' }, 500);
});

export default {
  fetch: app.fetch,
  async scheduled(_event: ScheduledController, env: Env, ctx: ExecutionContext) {
    ctx.waitUntil(dailyMaintenance(env));
  },
  async queue(batch: MessageBatch<Job>, env: Env) {
    for (const msg of batch.messages) {
      try {
        await runJob(env, msg.body);
        msg.ack();
      } catch (e) {
        console.error('job failed', msg.body.type, e);
        msg.retry({ delaySeconds: 60 });
      }
    }
  },
} satisfies ExportedHandler<Env, Job>;

