-- 0003 — referral codes, Hisab points and the Pro entitlement points are redeemed for.
-- Additive only: no existing table is rebuilt, so this is safe on a database with live shops.

-- Pro is active while pro_until (epoch ms) is in the future. NULL = free plan.
ALTER TABLE users ADD COLUMN pro_until INTEGER;

-- Every account's own share code (created the first time the referral screen is opened).
CREATE TABLE IF NOT EXISTS referral_codes (
  user_id    TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  code       TEXT NOT NULL UNIQUE,              -- 6 chars, A–Z + 2–9 without look-alikes (no 0/O/1/I)
  created_at INTEGER NOT NULL
);

-- Who invited whom. One row per invited (referee) account.
CREATE TABLE IF NOT EXISTS referrals (
  referee_user_id  TEXT PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
  referrer_user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  code             TEXT NOT NULL,
  status           TEXT NOT NULL DEFAULT 'pending' CHECK (status IN ('pending','qualified','expired','rejected')),
  reason           TEXT,                        -- e.g. same_device, monthly_cap
  created_at       INTEGER NOT NULL,
  decided_at       INTEGER
);
CREATE INDEX IF NOT EXISTS idx_referrals_referrer ON referrals(referrer_user_id, status);
CREATE INDEX IF NOT EXISTS idx_referrals_status ON referrals(status, created_at);

-- An email address can be referred only once, ever. Kept as a salted hash so the rule still holds
-- after the account is deleted (no personal data is kept).
CREATE TABLE IF NOT EXISTS referral_claims (
  email_hash TEXT PRIMARY KEY,
  created_at INTEGER NOT NULL
);

-- Append-only points ledger. Balance = SUM(delta). Award ids are deterministic, so a retry
-- can never pay twice.
CREATE TABLE IF NOT EXISTS points_ledger (
  id         TEXT PRIMARY KEY,
  user_id    TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  delta      INTEGER NOT NULL,
  reason     TEXT NOT NULL CHECK (reason IN ('referral_referrer','referral_welcome','redeem_pro','admin_adjust')),
  ref        TEXT,
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_points_user ON points_ledger(user_id, created_at);
