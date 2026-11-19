# হিসাববন্ধু (HisabBondhu) — TODO

> দোকানের হিসাব, সবসময় হাতের কাছে।
> Offline-first Bangla digital ledger for small businesses in Bangladesh.

Legend: `[x]` done in repo · `[ ]` pending · 👤 = needs the owner (you) to do it manually

---

## 0. Planning & brand
- [ ] PRD saved in repo (`docs/PRD.md`)
- [ ] Final name chosen: **হিসাববন্ধু / HisabBondhu**
- [ ] Brand guide (colours, typography, voice) — `brand/BRAND.md`
- [ ] Logo (SVG) + wordmark + launcher icon (adaptive + monochrome)
- [ ] Play Store icon 512×512 + feature graphic 1024×500
- [ ] 👤 Check name on Google Play, domain (`hisabbondhu.com` / `.app`), Facebook/YouTube handles, BD trademark

## 1. Android app (Kotlin + Jetpack Compose)
### Foundation
- [ ] Gradle (Kotlin DSL) project, version catalog, R8 minify, desugaring
- [ ] Material 3 theme with brand colours, Bengali-friendly typography
- [ ] Bangla + English (per-app language, Bengali digits, lakh grouping ৳১,২৫,০০০)
- [ ] Money stored as integer **paisa**, quantity as integer milli-units
- [ ] Asia/Dhaka day boundaries for "today" reports

### Offline-first data (Room = source of truth)
- [ ] Room DB: parties (customer/supplier), products, transactions (append-only)
- [ ] Every row has UUID + device id + `syncState` (idempotent sync)
- [ ] Corrections by **reversal** transaction, never hard delete
- [ ] Balances derived from ledger (customer due, supplier payable, cash, stock, profit)

### Screens
- [ ] Splash + language selection
- [ ] Business setup wizard (shop name, owner, type, opening cash) — skippable
- [ ] Dashboard: today's sale / cash-in / due sale / expense, total receivable & payable, cash balance, low stock, pending sync
- [ ] Quick actions: বিক্রি · টাকা পেলাম · খরচ · কেনা (+ টাকা দিলাম, ফেরত, স্টক সমন্বয়, ক্যাশ যোগ/তোলা)
- [ ] Transaction entry (cash/credit sale, payment in/out, purchase, expense, returns, stock adjust)
- [ ] Customer & supplier list (search, sort by due/recent/name)
- [ ] Party profile: balance, history, call, share statement text, PDF statement, reminder date
- [ ] Products & stock: list, add/edit, low-stock alert, stock history
- [ ] Reports: today / week / month / last month / all; payment-method split; CSV + JSON export
- [ ] Settings: business profile, account & sync, language, reminders, export, help
- [ ] Legal & support in-app: Contact us, Privacy Policy, Terms, Your Rights, Delete account & data

### Online features (optional login — app fully usable without internet)
- [ ] Mobile number + OTP login
- [ ] WorkManager sync (push/pull, exponential backoff, runs after reboot, only on network)
- [ ] Sync status chip: সংরক্ষিত · sync হচ্ছে · sync সম্পন্ন · আবার চেষ্টা প্রয়োজন
- [ ] Token refresh + logout + in-app account deletion
- [ ] Daily reminder notification for customers with due date

## 2. Backend (Cloudflare Workers + D1 + R2 + Queues)
- [ ] Worker API (TypeScript, Hono) — `/v1/...`
- [ ] D1 schema + migrations (multi-tenant `business_id` on every row)
- [ ] OTP auth: hashed codes, expiry, attempt + rate limits, review/demo number
- [ ] JWT access token (15 min) + rotating refresh token, session revoke
- [ ] Sync push (idempotent upsert) / pull (cursor by `server_seq`)
- [ ] Account & data deletion (API + website flow), deletion audit log
- [ ] Contact form endpoint
- [ ] R2 backups, Queue consumer for heavy jobs (falls back inline if Queues unavailable)
- [ ] Cron cleanup of expired OTP/sessions
- [ ] Unit tests (vitest)
- [ ] 👤 Create Cloudflare D1 / R2 / Queue + set Worker secrets (see `docs/SETUP.md`)
- [ ] 👤 Choose SMS gateway for OTP (e.g. BulkSMSBD / SSL Wireless) and add API key

## 3. Website (GitHub Pages) — Google Play required URLs
- [ ] Home / landing page
- [ ] Privacy Policy (`privacy.html`)
- [ ] Terms & Conditions (`terms.html`)
- [ ] Data deletion request (`data-deletion.html`) — works for uninstalled users too
- [ ] Your Rights (`your-rights.html`)
- [ ] Contact Us (`contact.html`)
- [ ] 👤 Enable GitHub Pages: Settings → Pages → Source = **GitHub Actions**
- [ ] 👤 Replace placeholder support email `support@hisabbondhu.com` if you don't own that domain

## 4. CI/CD (GitHub Actions)
- [ ] Android: unit tests + debug APK on every push/PR
- [ ] Android: signed **release AAB + APK** when signing secrets exist; GitHub Release on `v*` tag
- [ ] Backend: typecheck + tests; deploy with Wrangler when Cloudflare secrets exist
- [ ] Website: deploy to GitHub Pages
- [ ] Signing keystore generated; secret names documented in `docs/SECRETS.md`
- [ ] 👤 Add GitHub secrets (Settings → Secrets and variables → Actions)

## 5. Google Play Console checklist
- [ ] Store listing text (Bangla + English) — `docs/play-store/listing.md`
- [ ] Data safety form answers — `docs/play-store/data-safety.md`
- [ ] Content rating, target audience, ads, app access answers — `docs/play-store/policy-answers.md`
- [ ] Release checklist — `docs/play-store/release-checklist.md`
- [ ] 👤 Create Play Console app, enable **Play App Signing**, upload AAB to Internal testing
- [ ] 👤 Closed testing with 12+ testers for 14 days (required for new personal developer accounts)
- [ ] 👤 Phone screenshots (min 2, 1080×1920) taken from the built app

## 6. Phase 2+ (after MVP)
- [ ] PDF invoice, barcode scanner, thermal printer
- [ ] Staff accounts & roles (Owner / Manager / Cashier / Viewer)
- [ ] Multi-branch, POS cart, bKash/Nagad reconciliation, Bangla QR
- [ ] Smart reports (best sellers, dead stock, reorder suggestion)
- [ ] Durable Objects realtime multi-cashier sync
- [ ] Web dashboard, iOS app
