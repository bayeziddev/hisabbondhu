-- HisabBondhu — Cloudflare D1 schema v1
-- Multi-tenant: every business-owned row carries business_id and is part of its primary key.
-- Money = INTEGER paisa, quantity = INTEGER milli-units, time = INTEGER epoch milliseconds (UTC).

CREATE TABLE IF NOT EXISTS users (
  id            TEXT PRIMARY KEY,
  phone         TEXT NOT NULL UNIQUE,          -- +8801XXXXXXXXX
  created_at    INTEGER NOT NULL,
  last_login_at INTEGER
);

CREATE TABLE IF NOT EXISTS otp_codes (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  phone       TEXT NOT NULL,
  code_hash   TEXT NOT NULL,                   -- SHA-256(pepper:phone:code), never the code itself
  ip          TEXT,
  attempts    INTEGER NOT NULL DEFAULT 0,
  consumed    INTEGER NOT NULL DEFAULT 0,
  created_at  INTEGER NOT NULL,
  expires_at  INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_otp_phone ON otp_codes(phone, created_at);
CREATE INDEX IF NOT EXISTS idx_otp_ip ON otp_codes(ip, created_at);

CREATE TABLE IF NOT EXISTS sessions (
  id            TEXT PRIMARY KEY,
  user_id       TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  refresh_hash  TEXT NOT NULL,                 -- SHA-256 of the rotating refresh secret
  device_id     TEXT,
  device_name   TEXT,
  created_at    INTEGER NOT NULL,
  last_used_at  INTEGER NOT NULL,
  expires_at    INTEGER NOT NULL,
  revoked_at    INTEGER
);
CREATE INDEX IF NOT EXISTS idx_sessions_user ON sessions(user_id);

CREATE TABLE IF NOT EXISTS businesses (
  id                 TEXT PRIMARY KEY,
  owner_user_id      TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  name               TEXT NOT NULL DEFAULT '',
  owner_name         TEXT NOT NULL DEFAULT '',
  phone              TEXT NOT NULL DEFAULT '',
  address            TEXT NOT NULL DEFAULT '',
  business_type      TEXT NOT NULL DEFAULT 'general_store',
  opening_cash_paisa INTEGER NOT NULL DEFAULT 0,
  created_at         INTEGER NOT NULL,
  updated_at         INTEGER NOT NULL,
  last_sync_at       INTEGER
);
CREATE INDEX IF NOT EXISTS idx_business_owner ON businesses(owner_user_id);

CREATE TABLE IF NOT EXISTS business_members (
  business_id TEXT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  user_id     TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role        TEXT NOT NULL DEFAULT 'owner' CHECK (role IN ('owner','manager','cashier','viewer')),
  created_at  INTEGER NOT NULL,
  PRIMARY KEY (business_id, user_id)
);
CREATE INDEX IF NOT EXISTS idx_members_user ON business_members(user_id);

CREATE TABLE IF NOT EXISTS parties (
  business_id           TEXT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  id                    TEXT NOT NULL,
  type                  TEXT NOT NULL CHECK (type IN ('CUSTOMER','SUPPLIER')),
  name                  TEXT NOT NULL,
  phone                 TEXT,
  address               TEXT,
  note                  TEXT,
  opening_balance_paisa INTEGER NOT NULL DEFAULT 0,
  reminder_at           INTEGER,
  created_at            INTEGER NOT NULL,
  updated_at            INTEGER NOT NULL,
  deleted               INTEGER NOT NULL DEFAULT 0,
  device_id             TEXT NOT NULL,
  server_seq            INTEGER NOT NULL,
  PRIMARY KEY (business_id, id)
);
CREATE INDEX IF NOT EXISTS idx_parties_seq ON parties(business_id, server_seq);

CREATE TABLE IF NOT EXISTS products (
  business_id          TEXT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  id                   TEXT NOT NULL,
  name                 TEXT NOT NULL,
  category             TEXT,
  sku                  TEXT,
  barcode              TEXT,
  unit                 TEXT NOT NULL DEFAULT 'pcs',
  purchase_price_paisa INTEGER NOT NULL DEFAULT 0,
  sale_price_paisa     INTEGER NOT NULL DEFAULT 0,
  opening_stock_milli  INTEGER NOT NULL DEFAULT 0,
  min_stock_milli      INTEGER NOT NULL DEFAULT 0,
  created_at           INTEGER NOT NULL,
  updated_at           INTEGER NOT NULL,
  deleted              INTEGER NOT NULL DEFAULT 0,
  device_id            TEXT NOT NULL,
  server_seq           INTEGER NOT NULL,
  PRIMARY KEY (business_id, id)
);
CREATE INDEX IF NOT EXISTS idx_products_seq ON products(business_id, server_seq);

-- Append-only ledger. Corrections are new rows with reversal_of set (never UPDATE/DELETE).
CREATE TABLE IF NOT EXISTS transactions (
  business_id     TEXT NOT NULL REFERENCES businesses(id) ON DELETE CASCADE,
  id              TEXT NOT NULL,
  type            TEXT NOT NULL CHECK (type IN ('SALE','PAYMENT_IN','PAYMENT_OUT','PURCHASE','EXPENSE',
                                                 'SALE_RETURN','PURCHASE_RETURN','STOCK_ADJUST','CASH_IN','CASH_OUT')),
  party_id        TEXT,
  product_id      TEXT,
  qty_milli       INTEGER NOT NULL DEFAULT 0,
  rate_paisa      INTEGER NOT NULL DEFAULT 0,
  unit_cost_paisa INTEGER NOT NULL DEFAULT 0,
  discount_paisa  INTEGER NOT NULL DEFAULT 0,
  total_paisa     INTEGER NOT NULL,
  paid_paisa      INTEGER NOT NULL,
  method          TEXT NOT NULL DEFAULT 'CASH',
  category        TEXT,
  note            TEXT,
  occurred_at     INTEGER NOT NULL,
  created_at      INTEGER NOT NULL,
  device_id       TEXT NOT NULL,
  reversal_of     TEXT,
  received_at     INTEGER NOT NULL,
  server_seq      INTEGER NOT NULL,
  PRIMARY KEY (business_id, id)
);
CREATE INDEX IF NOT EXISTS idx_tx_seq ON transactions(business_id, server_seq);
CREATE INDEX IF NOT EXISTS idx_tx_occurred ON transactions(business_id, occurred_at);

CREATE TABLE IF NOT EXISTS audit_logs (
  id          INTEGER PRIMARY KEY AUTOINCREMENT,
  business_id TEXT,
  user_id     TEXT,
  action      TEXT NOT NULL,
  detail      TEXT,
  ip          TEXT,
  created_at  INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_audit_business ON audit_logs(business_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);

CREATE TABLE IF NOT EXISTS support_messages (
  id         TEXT PRIMARY KEY,
  name       TEXT NOT NULL,
  phone      TEXT,
  email      TEXT,
  subject    TEXT NOT NULL,
  message    TEXT NOT NULL,
  source     TEXT,
  ip         TEXT,
  status     TEXT NOT NULL DEFAULT 'new',
  created_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_support_ip ON support_messages(ip, created_at);

-- Proof that a deletion happened, without keeping personal data (only a salted hash).
CREATE TABLE IF NOT EXISTS deletion_log (
  id           TEXT PRIMARY KEY,
  phone_hash   TEXT NOT NULL,
  source       TEXT NOT NULL,
  businesses   INTEGER NOT NULL DEFAULT 0,
  requested_at INTEGER NOT NULL,
  completed_at INTEGER
);
