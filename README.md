<p align="center"><img src="brand/logo-mark.svg" width="96" alt="HisabBondhu logo"></p>

# হিসাববন্ধু · HisabBondhu

**দোকানের হিসাব, সবসময় হাতের কাছে।** — An offline-first Bangla digital ledger (khata) for small businesses in Bangladesh:
sales, dues (baki), payments, expenses, purchases and stock. Works with no internet and syncs automatically when online.

> **এই ফাইলটিই প্রজেক্টের উইকি।** কোন ফাইল কোথায়, কোন endpoint কী করে, কোন টেবিলে কী থাকে — সব এখানে।
> **যেকোনো আপডেটের আগে ও পরে এই ফাইল হালনাগাদ করুন** ([নিয়ম](#rules--যা-কখনো-ভুল-করা-যাবে-না)); API টেবিল আর আসল কোড না মিললে CI ফেল করে।
> পরিকল্পনা ও কাজের ক্রম: **[docs/ROADMAP.md](docs/ROADMAP.md)**

![Feature graphic](brand/play-store/feature-graphic-1024x500.png)

## Contents
1. [Rules — যা কখনো ভুল করা যাবে না](#rules--যা-কখনো-ভুল-করা-যাবে-না)
2. [Repository map](#repository-map)
3. [How offline-first works](#how-offline-first-works)
4. [API reference](#api-reference) · [Error codes](#error-codes) · [Limits](#limits)
5. [Database](#database)
6. [Android app](#android-app)
7. [Website](#website)
8. [Configuration & secrets](#configuration--secrets)
9. [CI/CD workflows](#cicd-workflows)
10. [Testing](#testing)
11. [How to add a feature (checklist)](#how-to-add-a-feature)
12. [Release](#release)

---

## Rules — যা কখনো ভুল করা যাবে না

| Rule | Why |
|---|---|
| Play Store package (applicationId) is **`com.hisabbondhu.myapp`**. Kotlin namespace stays `com.hisabbondhu.app`. Never change either. | The Play Console app is `com.hisabbondhu.myapp`; a different id is a different app. A unit test (`ReferralLinkTest`) fails if it changes. Debug builds are `com.hisabbondhu.myapp.debug`. |
| **Update this README before/with every change**: new file, endpoint, table, screen, setting. | It is the project wiki. |
| Every endpoint must appear in the [API table](#api-reference). Run `node backend/scripts/check-api.mjs`. | CI fails if the server, the app, the website and this table disagree. |
| **Strings:** edit `android/tools/strings_source.py`, then run `python3 android/tools/gen_strings.py`. Never edit `res/values*/strings.xml` by hand. | The XML is generated; hand edits are overwritten. |
| **Website pages:** edit `website/tools/build_pages.py`, then run `python3 website/tools/build_pages.py`. Never edit `website/*.html` by hand. | The pages are generated; hand edits are overwritten. |
| **Database changes:** add a new `backend/migrations/000N_*.sql`; never edit an applied migration. A Room (phone) schema change needs a version bump + migration in `AppDatabase.kt`. | Production D1 already ran 0001–0003; users' phones hold version 1. |
| Money = integer **paisa**, quantity = integer **milli-units**, time = **epoch ms (UTC)**; "today" uses Asia/Dhaka. | No rounding errors; same numbers on every device. |
| Ledger is **append-only**; corrections are reversal entries (`reversal_of`). | Audit trail; sync can never lose an entry. |
| New personal data collected → update privacy policy, terms and Data safety CSV (`docs/play-store/data-safety/build_csv.py`). | Play policy. |
| No secrets in the repo (keys, tokens, keystore). Use GitHub secrets / `wrangler secret`. | See [docs/SECRETS.md](docs/SECRETS.md). |

---

## Repository map

| Path | What |
|---|---|
| [`android/`](android) | Android app — Kotlin, Jetpack Compose (Material 3), Room, WorkManager, DataStore, Retrofit |
| [`backend/`](backend) | API — Cloudflare Workers (Hono, TypeScript) + D1 + R2 + Queues + Cron |
| [`website/`](website) | GitHub Pages site — home, privacy, terms, your rights, data deletion, contact (bilingual, generated) |
| [`docs/`](docs) | [ROADMAP](docs/ROADMAP.md) · [SETUP](docs/SETUP.md) · [SECRETS](docs/SECRETS.md) · [PRODUCT](docs/PRODUCT.md) · [Play Store kit](docs/play-store) |
| [`brand/`](brand) | Logo, icons, Play Store graphics/screenshots, brand guide ([BRAND.md](brand/BRAND.md)), guide screenshots |
| [`marketing/`](marketing) | Ad video (9:16), narrated user-guide video (16:9), YouTube thumbnails — HyperFrames projects |
| [`.github/workflows/`](.github/workflows) | CI/CD — see [workflows](#cicd-workflows) |
| [`TODO.md`](TODO.md) | Original MVP checklist (current plan: ROADMAP) |
| [`CLAUDE.md`](CLAUDE.md) | Short rules for AI coding sessions (same rules as above) |

### `backend/`
| File | What |
|---|---|
| `src/index.ts` | Hono app: security headers, CORS (website only), per-IP rate limit, route mounting, cron + queue handlers |
| `src/env.ts` | `Env` (bindings, vars, secrets) and `Job` types |
| `src/jobs.ts` | Daily cron (OTP/session/audit cleanup, referral sweep, Sunday R2 snapshots) and queue jobs |
| `src/version.ts` | API version string |
| `src/lib/auth.ts` | `requireAuth` (JWT), `requireMember` (business membership/role), `audit()` |
| `src/lib/crypto.ts` | JWT sign/verify (HS256), SHA-256, random tokens/OTP, UUID, constant-time compare |
| `src/lib/email.ts` | Email normalisation + login-code email (Resend) |
| `src/lib/http.ts` | `fail()` error helper, `readJson()` + zod validation, client IP |
| `src/lib/otp.ts` | Create/consume one-time codes, rate limits, Play reviewer fixed code |
| `src/lib/phone.ts` | Bangladeshi mobile number normalisation/masking |
| `src/lib/sms.ts` | SMS gateways (BulkSMSBD, SSL Wireless, webhook) — off until `PHONE_OTP_ENABLED` |
| `src/lib/users.ts` | User row → public JSON (`publicUser`, incl. `pro_until`/`pro_active`) |
| `src/lib/validate.ts` | zod schemas: business, party, product, transaction, push/pull, contact |
| `src/lib/referral.ts` | Referral rules (`REFERRAL`), code generator, points balance, decide/sweep referrals |
| `src/routes/auth.ts` | `/v1/auth/*` — email code login, refresh, logout |
| `src/routes/account.ts` | `/v1/account*` — profile, export, phone verify, sessions, **account deletion** (`deleteUserEverything`) |
| `src/routes/business.ts` | `/v1/businesses` — list, link, update shop profile |
| `src/routes/sync.ts` | `/v1/businesses/:id/sync/push|pull` — offline sync |
| `src/routes/referral.ts` | `/v1/referral*` — code, apply, redeem |
| `src/routes/public.ts` | `/v1/public/contact` — contact form (website + app) |
| `src/routes/admin.ts` | `/v1/admin/*` — leads, support inbox, stats (needs `ADMIN_TOKEN`) |
| `migrations/0001_init.sql` | Core schema (users, sessions, businesses, members, parties, products, transactions, audit, support, deletion log) |
| `migrations/0002_email_accounts.sql` | Accounts by verified email; `otp_codes` by channel/target |
| `migrations/0003_referrals.sql` | `referral_codes`, `referrals`, `referral_claims`, `points_ledger`, `users.pro_until` |
| `scripts/e2e.mjs` | End-to-end test against a local Worker + D1 |
| `scripts/check-api.mjs` | API contract check (server ↔ app ↔ website ↔ this README) |
| `test/lib.test.ts` | Unit tests (vitest) |
| `wrangler.toml` | Worker config, vars, D1/R2/Queue bindings, cron |

### `android/app/src/main/java/com/hisabbondhu/app/`
| Path | What |
|---|---|
| `HisabApp.kt`, `MainActivity.kt` | Application (creates `AppContainer`), single activity |
| `AppContainer.kt` | Manual DI: settings, Room DB, network, repositories, sync scheduler; reads the Play install referrer once |
| `core/` | `Money` (paisa ⇄ ৳, Bengali digits), `Dates` (Asia/Dhaka), `Ids` (UUIDs) |
| `domain/Types.kt` | Enums: `PartyType`, `TxType`, `PayMethod`, `SyncState` |
| `data/local/` | Room: `AppDatabase` (v1), `Entities` (parties, products, transactions), `LedgerDao` |
| `data/prefs/SettingsStore.kt` | DataStore: language, business profile, auth tokens, sync cursors, reminders, referral codes |
| `data/remote/` | `HisabApi` (Retrofit endpoints), `Network` (OkHttp, auth + token refresh, `apiErrorCode()`), `dto/Dtos.kt` |
| `data/repo/` | `AuthRepository`, `LedgerRepository`, `SyncEngine` (push/pull), `ReferralRepository`, `Mappers` |
| `sync/` | `SyncWorker` (WorkManager sync), `SyncScheduler`, `ReminderWorker` (daily due reminders), `ConnectivityObserver` |
| `ui/nav/AppNav.kt` | Navigation graph + `Routes` |
| `ui/screens/…` | Screens (see [routes](#screens-and-routes)) |
| `ui/components/Components.kt` | Shared UI: `AppTopBar`, `SectionCard`, `NavRow`, `AmountField`, `ConfirmDialog`, `SyncChip`… |
| `ui/Labels.kt` | `num()`, `date()`, `money()` helpers (Bangla digits) |
| `ui/theme/` | Colours, typography, theme |
| `util/` | `Share` (WhatsApp/SMS/email/Play links, **`PLAY_PACKAGE`**), `PdfStatement`, `Exporter` (CSV/JSON), `Messages` (reminder texts), `InstallReferrer`, `ReferralCode` |

Other Android folders: `app/src/test/` (JVM unit tests), `app/src/androidTest/` (`ScreenshotTour`, `GuideTour` UI tours + `DemoData`),
`tools/` (`strings_source.py` + `gen_strings.py`, `screenshot-tour.sh`, `guide-tour.sh`), `gradle/libs.versions.toml` (dependency versions).

---

## How offline-first works
```
 Phone (works offline)                              Cloudflare (when online)
 ┌───────────────────────────────┐   HTTPS + JWT   ┌──────────────────────────────┐
 │ Compose UI ⇄ ViewModels        │                 │ Worker (Hono)                │
 │        ⇅ Flow                  │  push PENDING ▶ │  /sync/push  idempotent upsert│
 │ Room (source of truth)         │                 │  /sync/pull  cursor by seq    │
 │  parties · products ·          │ ◀ pull changes  │ D1 (multi-tenant SQL)        │
 │  transactions (append-only)    │                 │ R2 snapshots · Queue jobs    │
 │ WorkManager SyncWorker         │                 │ Cron cleanup                 │
 └───────────────────────────────┘                 └──────────────────────────────┘
```
- Every row has a UUID created on the phone → retries never create duplicates (`ON CONFLICT DO NOTHING`).
- Each push is one atomic D1 batch; each table row gets a `server_seq`, and pull returns rows after the client's cursor.
- Customers/products use last-write-wins on `updated_at`; transactions are immutable.
- Balances, stock and cash are **derived** from the ledger, so data from several phones is always consistent.

---

## API reference
Base URL: `https://hisabbondhu-api.<subdomain>.workers.dev` (Android `BuildConfig.API_BASE_URL`, website `assets/config.js`).
Auth: `Authorization: Bearer <access_token>` (JWT, 15 min) unless marked *none*. Errors are JSON `{ "error": "<code>", "message": "…" }` — see [error codes](#error-codes).

> Rows below are checked by `backend/scripts/check-api.mjs` against `backend/src` — keep the format `` | `METHOD` | `/path` | … ``.

| Method | Path | Auth | Request body | Response | Used by |
|---|---|---|---|---|---|
| `GET` | `/` | none | — | `{name, version, docs}` | manual check |
| `GET` | `/v1/health` | none | — | `{ok, version, db, time}` | deploy smoke test |
| `POST` | `/v1/auth/request-otp` | none | `{email}` | `{ok, expires_in, resend_after, dev_code?}` (dev only) | app, website |
| `POST` | `/v1/auth/verify-otp` | none | `{email, code, device_id?, device_name?}` | `{access_token, refresh_token, expires_in, new_user, user}` | app, website |
| `POST` | `/v1/auth/refresh` | none (refresh token) | `{refresh_token}` | `{access_token, refresh_token, expires_in, user}` — rotates; reuse revokes the session | app (`RefreshApi`) |
| `POST` | `/v1/auth/logout` | none (refresh token) | `{refresh_token}` | `{ok}` | app |
| `GET` | `/v1/account` | Bearer | — | `{user, businesses, devices}` | — (right of access) |
| `GET` | `/v1/account/export` | Bearer | — | JSON file: user, referral, businesses with parties/products/transactions | — (portability) |
| `PUT` | `/v1/account/profile` | Bearer | `{name, phone, marketing_opt_in}` | `{user}` | app |
| `POST` | `/v1/account/phone/request-otp` | Bearer | — | `{ok, dev_code?}` — 503 until `PHONE_OTP_ENABLED` | — (later) |
| `POST` | `/v1/account/phone/verify` | Bearer | `{code}` | `{user}` — 503 until `PHONE_OTP_ENABLED` | — (later) |
| `DELETE` | `/v1/account/sessions/:sid` | Bearer | — | `{ok}` — sign out one device | — |
| `DELETE` | `/v1/account` | Bearer | — | `{ok, deleted_businesses}` — deletes everything | app, website |
| `GET` | `/v1/businesses` | Bearer | — | `{businesses: [business]}` | app |
| `POST` | `/v1/businesses/link` | Bearer | business `{id, name, owner_name, phone, address, business_type, opening_cash_paisa, updated_at}` | business — `201` new, `200` existing | app |
| `PUT` | `/v1/businesses/:id` | Bearer (member) | business (same `id`) | business | app |
| `POST` | `/v1/businesses/:id/sync/push` | Bearer (member, not viewer) + `Idempotency-Key` header | `{device_id, parties[], products[], transactions[]}` (≤500 each) | `{ok, accepted{…}, rejected[{entity,id,reason}]}` | app (`SyncEngine`) |
| `POST` | `/v1/businesses/:id/sync/pull` | Bearer (member) | `{cursors{parties,products,transactions}, limit≤1000}` | `{business, parties, products, transactions, cursors, has_more}` | app (`SyncEngine`) |
| `POST` | `/v1/public/contact` | none | `{name, phone?, email?, subject, message, source, website}` (`website` = honeypot) | `{ok, id}` | app, website |
| `GET` | `/v1/referral` | Bearer | — | referral summary: `{code, share_url, points, pro_until, pro_active, redeem_enabled, can_apply, applied_code, applied_status, stats, rules, history}` | app |
| `POST` | `/v1/referral/apply` | Bearer | `{code}` | referral summary | app |
| `POST` | `/v1/referral/redeem` | Bearer | `{option: "pro_30" \| "pro_365"}` | referral summary — 503 until `REFERRAL_REDEEM_ENABLED` | app |
| `GET` | `/v1/admin/leads` | `Bearer <ADMIN_TOKEN>` | — | `{count, leads}` | owner |
| `GET` | `/v1/admin/support` | `Bearer <ADMIN_TOKEN>` | — | `{count, messages}` | owner |
| `GET` | `/v1/admin/stats` | `Bearer <ADMIN_TOKEN>` | — | counts | owner |

Where each caller lives: app → `android/.../data/remote/HisabApi.kt`; website → `website/tools/build_pages.py` (contact + data-deletion forms).

### Error codes
| HTTP | `error` | Meaning |
|---|---|---|
| 400 | `invalid_json` | Body is not JSON |
| 400 | `code_expired` | Login code expired or already used |
| 401 | `unauthorized` | Missing/invalid token (admin: wrong `ADMIN_TOKEN`) |
| 401 | `session_expired` | Session revoked/expired — log in again |
| 401 | `invalid_refresh` | Malformed refresh token |
| 403 | `forbidden` | Role not allowed (e.g. viewer pushing) |
| 404 | `not_found` · `business_not_found` · `code_not_found` | Unknown route, business you are not a member of, or unknown referral code |
| 409 | `already_applied` | A referral code was already used for this account/email |
| 413 | `payload_too_large` | Body over 2.5 MB |
| 422 | `invalid_input` · `invalid_email` · `invalid_phone` · `invalid_code` · `invalid_option` | Validation failed |
| 422 | `own_code` · `mutual_referral` · `apply_window_closed` · `not_enough_points` | Referral rules |
| 429 | `rate_limited` · `too_many_attempts` | Slow down / too many wrong codes |
| 502 | `email_failed` · `sms_failed` | Provider error |
| 503 | `feature_disabled` · `redeem_disabled` | Switched off by config |

The app maps codes to Bangla/English messages (`authErrorText`, `referralErrorText`). **Never rename an existing code** — older app versions depend on it.

### Limits
| What | Limit |
|---|---|
| Any `/v1/*` request | 120 / minute / IP (`API_LIMITER`) |
| Login code | resend after 60 s; 5 / hour / email; 20 / hour / IP; 5 wrong attempts; valid 5 min (reviewer code skips per-email limits) |
| Access / refresh token | 15 min / 60 days (rotating) |
| Sync | ≤500 rows per table per push; ≤1000 per pull page |
| Contact form | 5 / hour / IP + honeypot |
| Referral | code within 7 days of sign-up; reward after 5 entries + 24 h; 20 rewards / 30 days per inviter |

---

## Database

### Server — Cloudflare D1 (`backend/migrations/`)
| Table | Key | Holds |
|---|---|---|
| `users` | `id` | email (unique), phone, name, marketing opt-in, `pro_until` |
| `otp_codes` | `id` | hashed login/phone codes (deleted after 24 h) |
| `sessions` | `id` | hashed refresh token, device id/name, expiry, revoked |
| `businesses` | `id` | shop profile, `owner_user_id`, `last_sync_at` |
| `business_members` | `(business_id, user_id)` | role: owner / manager / cashier / viewer |
| `parties` | `(business_id, id)` | customers & suppliers, `server_seq` |
| `products` | `(business_id, id)` | products & stock settings, `server_seq` |
| `transactions` | `(business_id, id)` | append-only ledger, `reversal_of`, `received_at`, `server_seq` |
| `audit_logs` | `id` | security/audit events (kept 12 months) |
| `support_messages` | `id` | contact form messages |
| `deletion_log` | `id` | proof of deletion (email hash only) |
| `referral_codes` | `user_id` | each account's share code |
| `referrals` | `referee_user_id` | who invited whom, status pending/qualified/expired/rejected |
| `referral_claims` | `email_hash` | one referral per email, ever (survives deletion) |
| `points_ledger` | `id` | Hisab points (+award / −redeem); balance = SUM(delta) |

Apply locally: `cd backend && npx wrangler d1 migrations apply hisabbondhu --local`. Production: the Backend workflow applies them on `main`.

### Phone — Room (`data/local/`, version 1)
`parties`, `products`, `transactions` (+ `syncState` per row). Settings, tokens and cursors live in DataStore (`SettingsStore`).

---

## Android app

### Screens and routes
| Route (`Routes`) | Screen file |
|---|---|
| onboarding (before login) | `ui/screens/onboarding/` — `LanguageScreen`, `AuthScreen` (email code), `ProfileScreen` (name, mobile, referral code), `BusinessSetupScreen` |
| `home` | `home/HomeScreen` (+ `HomeViewModel`) |
| `parties` · `party/{id}` · `partyEdit?type&id` | `parties/PartiesScreen` · `PartyDetailScreen` · `PartyEditScreen` |
| `products` · `product/{id}` · `productEdit?id` | `products/ProductsScreen` · `ProductDetailScreen` · `ProductEditScreen` |
| `txNew/{type}?partyId&productId` · `tx/{id}` · `transactions` | `tx/TxEntryScreen` (+ `TxEntryViewModel`) · `TxDetailScreen` · `TransactionsScreen` |
| `reports` | `reports/ReportsScreen` |
| `more` | `more/MoreScreen` |
| `business` · `profile` · `account` | `BusinessSetupScreen` (edit) · `ProfileScreen` (edit) · `more/AccountScreen` |
| `referral` | `more/ReferralScreen` |
| `contact` · `deleteData` · `legal/{page}` | `more/ContactScreen` · `DeleteDataScreen` · `LegalScreen` (bundled website pages) |

### Build facts
- `applicationId = "com.hisabbondhu.myapp"`, `namespace = "com.hisabbondhu.app"`, minSdk 24, targetSdk 36, `versionCode` = CI run number.
- `BuildConfig`: `API_BASE_URL`, `WEBSITE_URL`, `SUPPORT_EMAIL`, `SUPPORT_WHATSAPP` (from CI env / defaults in `app/build.gradle.kts`).
- The website's legal pages are copied into the APK at build time (`CopyLegalPagesTask`) so they open offline.
- Main libraries: Compose BOM, Room, WorkManager, DataStore, Retrofit + kotlinx-serialization, OkHttp, Play Install Referrer.

---

## Website
Generated by **`website/tools/build_pages.py`** (all text, both languages, effective date) → `index`, `privacy`, `terms`, `your-rights`,
`data-deletion`, `contact`, `404`. `assets/config.js` holds the API base (replaced by the Pages workflow), support email, WhatsApp and Play link;
`assets/site.js` sends the forms. Published by the **Website** workflow on `main`.

---

## Configuration & secrets
| Where | Name | Purpose |
|---|---|---|
| `backend/wrangler.toml` `[vars]` | `APP_ENV`, `ALLOWED_ORIGINS` | environment, website CORS origin |
| 〃 | `OTP_DEV_MODE` | `"true"` only locally — returns the code in the response |
| 〃 | `EMAIL_PROVIDER`, `EMAIL_FROM`, `SUPPORT_EMAIL` | login-code email |
| 〃 | `PHONE_OTP_ENABLED`, `SMS_PROVIDER` | SMS phone verification (off) |
| 〃 | `REFERRAL_REDEEM_ENABLED` | `"true"` when Pro exists (redeeming points) |
| 〃 | `REFERRAL_MIN_AGE_HOURS` | invited account age before the inviter is rewarded (default 24) |
| Worker secrets | `JWT_SECRET`, `OTP_PEPPER`, `RESEND_API_KEY`, `ADMIN_TOKEN`, `REVIEW_EMAIL`, `REVIEW_OTP`, `SMS_*`, `SUPPORT_WEBHOOK_URL` | see [docs/SECRETS.md](docs/SECRETS.md) |
| GitHub secrets | `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, `ANDROID_KEYSTORE_*`, `ANDROID_KEY_*` | deploy + signing |
| GitHub variables | `API_BASE_URL`, `WEBSITE_URL`, `SUPPORT_EMAIL` | Android build + website |
| Marketing (local only) | `GEMINI_API_KEY` | voice-over generation (`marketing/guide-video/tools/voice-gemini.py`) — never committed |

---

## CI/CD workflows
| Workflow | File | Runs on | Does |
|---|---|---|---|
| Android — build AAB & APK | `android.yml` | push (`main`, `claude/**`), PR | unit tests, debug APK, signed release AAB + APK (with secrets), release on `v*` tag |
| Backend — test & deploy | `backend.yml` | push/PR on `backend/**` | typecheck, unit tests, e2e on local Worker + D1, config check; on `main`: D1 migrations, deploy, smoke test (health + reviewer sign-in) |
| API contract | `api-contract.yml` | push/PR on API, app API, website, README | `backend/scripts/check-api.mjs` |
| Website — GitHub Pages | `pages.yml` | push on `main` (`website/**`) | publishes `website/` |
| Screenshots | `screenshots.yml` | push (`main`, `claude/**`) on `android/**` | emulator tours (phone + tablets, bn/en) → `brand/play-store/raw/` |
| Guide screens | `guide-screens.yml` | push on `claude/**` changing `GuideTour` (or manual) | full feature tour → `brand/guide/raw/` |

---

## Testing
```bash
# backend
cd backend && npm ci
npm run typecheck && npm test                 # TypeScript + vitest
npm run check:api                             # server ↔ app ↔ website ↔ README
# end-to-end (local Worker + D1) — see .github/workflows/backend.yml for the .dev.vars it writes
npx wrangler d1 migrations apply hisabbondhu --local
npx wrangler dev --local --port 8787 &  API=http://127.0.0.1:8787 node scripts/e2e.mjs

# android (needs Google Maven access)
cd android && ./gradlew testDebugUnitTest assembleDebug
```

---

## How to add a feature
Follow the item in [docs/ROADMAP.md](docs/ROADMAP.md) (master list) and its **Definition of Done**. Typical steps:

1. **Database** — new `backend/migrations/000N_name.sql` (additive). Phone schema change → bump Room version + migration.
2. **Backend** — logic in `src/lib/…`, routes in `src/routes/…`, mount in `src/index.ts`; validation with zod; errors with `fail(status, code)`.
3. **Tests** — unit (`test/lib.test.ts`) and e2e checks (`scripts/e2e.mjs`).
4. **README** — add every endpoint to the [API table](#api-reference), new tables to [Database](#database), new files to the maps; run `npm run check:api`.
5. **App** — DTOs (`data/remote/dto/Dtos.kt`), endpoint (`HisabApi.kt`), repository (`data/repo/`), screen (`ui/screens/…`), route (`ui/nav/AppNav.kt`), strings (`tools/strings_source.py` → `gen_strings.py`).
6. **Policy** — new personal data? update `website/tools/build_pages.py` (privacy/terms) and `docs/play-store/data-safety/build_csv.py`.
7. **Roadmap** — mark the item ✅; open a PR describing the change.

---

## Release
1. Merge to `main` → Backend deploys (migrations + Worker), Website publishes, Android builds a signed AAB (`versionCode` = run number).
2. Download the AAB from the Android workflow run (or the GitHub Release on a `v*` tag) → Play Console → testing/production track.
3. If data collection changed: import `docs/play-store/data-safety/hisabbondhu-data-safety.csv` (Data safety → Import from CSV).
4. Release notes: `docs/play-store/release-notes.md`. Full checklist: `docs/play-store/release-checklist.md`.

## Licence
© HisabBondhu. All rights reserved. Third-party components: see `android/app/src/main/assets/licenses/`.
