/** Minimal shape of the Workers Rate Limiting binding (optional). */
export interface RateLimiter {
  limit(options: { key: string }): Promise<{ success: boolean }>;
}

export type Job =
  | { type: 'delete_business_files'; businessId: string }
  | { type: 'snapshot_business'; businessId: string };

export interface Env {
  DB: D1Database;
  FILES?: R2Bucket;             // optional: R2 needs activation (payment method) in Cloudflare
  JOBS?: Queue<Job>;
  API_LIMITER?: RateLimiter;

  // vars (wrangler.toml)
  APP_ENV: string;              // "production" | "development"
  ALLOWED_ORIGINS: string;      // comma separated, e.g. https://bayeziddev.github.io
  OTP_DEV_MODE?: string;        // "true" only for local testing — returns the code in the response
  EMAIL_PROVIDER?: string;      // "resend" | "log"
  EMAIL_FROM?: string;          // e.g. "HisabBondhu <info@sayadbayezid.com>" (domain verified in Resend)
  SUPPORT_EMAIL?: string;       // reply-to address on emails
  PHONE_OTP_ENABLED?: string;   // "true" to enable SMS phone verification (needs an SMS gateway)
  SMS_PROVIDER?: string;        // "bulksmsbd" | "sslwireless" | "webhook" | "log"
  REFERRAL_REDEEM_ENABLED?: string; // "true" once Pro exists — until then points are only collected
  REFERRAL_MIN_AGE_HOURS?: string;  // invited account age before the inviter is rewarded (default 24)

  // secrets (wrangler secret put …)
  JWT_SECRET: string;
  OTP_PEPPER: string;
  RESEND_API_KEY?: string;
  ADMIN_TOKEN?: string;         // protects /v1/admin/* (leads & support inbox export)
  SMS_API_KEY?: string;
  SMS_SENDER_ID?: string;
  SMS_API_URL?: string;
  REVIEW_EMAIL?: string;        // Google Play reviewer test email (no email is sent)
  REVIEW_OTP?: string;
  SUPPORT_WEBHOOK_URL?: string; // optional: forward contact-form messages (e.g. Slack/Discord webhook)
}

export type Vars = {
  userId: string;
  sessionId: string;
  ip: string;
};

export type AppEnv = { Bindings: Env; Variables: Vars };
