-- 0002 — accounts are identified by a verified email (login code sent by email).
-- The mobile number becomes a required profile field (SMS verification can be enabled later).
-- Tables are rebuilt because SQLite cannot drop NOT NULL/UNIQUE from users.phone.
-- SAFETY GUARD: rebuilding `users` would cascade-delete shop data through ON DELETE CASCADE.
-- This migration is only for databases that have no shops yet (the phone-login version never went live).
-- If shops exist, the CHECK fails and the whole migration rolls back without touching anything.
CREATE TABLE _guard_0002 (shops INTEGER CHECK (shops = 0));
INSERT INTO _guard_0002 SELECT COUNT(*) FROM businesses;
DROP TABLE _guard_0002;

PRAGMA defer_foreign_keys = true;
CREATE TABLE users_v2 (
  id                TEXT PRIMARY KEY,
  email             TEXT NOT NULL UNIQUE,      -- lower-case; verified with an email OTP at login
  phone             TEXT,                      -- +8801XXXXXXXXX (profile)
  name              TEXT,
  phone_verified_at INTEGER,
  marketing_opt_in  INTEGER NOT NULL DEFAULT 0,
  created_at        INTEGER NOT NULL,
  last_login_at     INTEGER
);
INSERT INTO users_v2 (id, email, phone, created_at, last_login_at)
  SELECT id, 'legacy-' || id || '@invalid.local', phone, created_at, last_login_at FROM users;
DROP TABLE users;
ALTER TABLE users_v2 RENAME TO users;
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);
CREATE INDEX IF NOT EXISTS idx_users_created ON users(created_at);

-- One-time codes are short-lived; old rows are simply discarded.
DROP TABLE IF EXISTS otp_codes;
CREATE TABLE otp_codes (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  channel     TEXT NOT NULL DEFAULT 'email' CHECK (channel IN ('email','sms')),
  target      TEXT NOT NULL,                   -- email address or +880 phone
  code_hash   TEXT NOT NULL,                   -- SHA-256(pepper:target:code), never the code itself
  ip          TEXT,
  attempts    INTEGER NOT NULL DEFAULT 0,
  consumed    INTEGER NOT NULL DEFAULT 0,
  created_at  INTEGER NOT NULL,
  expires_at  INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_otp_target ON otp_codes(target, created_at);
CREATE INDEX IF NOT EXISTS idx_otp_ip ON otp_codes(ip, created_at);

-- Deletion proof keyed by a hash of the email instead of the phone.
CREATE TABLE deletion_log_v2 (
  id           TEXT PRIMARY KEY,
  email_hash   TEXT NOT NULL,
  source       TEXT NOT NULL,
  businesses   INTEGER NOT NULL DEFAULT 0,
  requested_at INTEGER NOT NULL,
  completed_at INTEGER
);
INSERT INTO deletion_log_v2 (id, email_hash, source, businesses, requested_at, completed_at)
  SELECT id, phone_hash, source, businesses, requested_at, completed_at FROM deletion_log;
DROP TABLE deletion_log;
ALTER TABLE deletion_log_v2 RENAME TO deletion_log;
