/** Minimal shape of the Workers Rate Limiting binding (optional). */
export interface RateLimiter {
  limit(options: { key: string }): Promise<{ success: boolean }>;
}

export type Job =
  | { type: 'delete_business_files'; businessId: string }
  | { type: 'snapshot_business'; businessId: string };

export interface Env {
  DB: D1Database;
  FILES: R2Bucket;
  JOBS?: Queue<Job>;
  API_LIMITER?: RateLimiter;

  // vars (wrangler.toml)
  APP_ENV: string;              // "production" | "development"
  ALLOWED_ORIGINS: string;      // comma separated, e.g. https://bayeziddev.github.io
  OTP_DEV_MODE?: string;        // "true" only for local testing — returns the code in the response
  SMS_PROVIDER?: string;        // "bulksmsbd" | "sslwireless" | "webhook" | "log"

  // secrets (wrangler secret put …)
  JWT_SECRET: string;
  OTP_PEPPER: string;
  SMS_API_KEY?: string;
  SMS_SENDER_ID?: string;
  SMS_API_URL?: string;
  REVIEW_PHONE?: string;        // Google Play reviewer test number (no SMS sent)
  REVIEW_OTP?: string;
  SUPPORT_WEBHOOK_URL?: string; // optional: forward contact-form messages (e.g. Slack/Discord webhook)
}

export type Vars = {
  userId: string;
  sessionId: string;
  ip: string;
};

export type AppEnv = { Bindings: Env; Variables: Vars };
