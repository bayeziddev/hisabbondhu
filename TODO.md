# হিসাববন্ধু (HisabBondhu) — TODO

> দোকানের হিসাব, সবসময় হাতের কাছে।
> Offline-first Bangla digital ledger for small businesses in Bangladesh.

Legend: `[x]` done in repo · `[ ]` pending · 👤 = needs the owner (you) to do it manually

---

## 0. Planning & brand
- [x] PRD summary + implementation status (`docs/PRODUCT.md`)
- [x] Final name chosen: **হিসাববন্ধু / HisabBondhu**
- [x] Brand guide (colours, typography, voice) — `brand/BRAND.md`
- [x] Logo (SVG) + wordmark + launcher icon (adaptive + monochrome)
- [x] Play Store icon 512×512 + feature graphic 1024×500
- [ ] 👤 Check name on Google Play, domain (`hisabbondhu.com` / `.app`), Facebook/YouTube handles, BD trademark

## 1. Android app (Kotlin + Jetpack Compose)
### Foundation
- [x] Gradle (Kotlin DSL) project, version catalog, R8 minify, desugaring
- [x] Material 3 theme with brand colours, Bengali-friendly typography
- [x] Bangla + English (per-app language, Bengali digits, lakh grouping ৳১,২৫,০০০)
- [x] Money stored as integer **paisa**, quantity as integer milli-units
- [x] Asia/Dhaka day boundaries for "today" reports

### Offline-first data (Room = source of truth)
- [x] Room DB: parties (customer/supplier), products, transactions (append-only)
- [x] Every row has UUID + device id + `syncState` (idempotent sync)
- [x] Corrections by **reversal** transaction, never hard delete
- [x] Balances derived from ledger (customer due, supplier payable, cash, stock, profit)

### Screens
- [x] Splash + language selection
- [x] Business setup wizard (shop name, owner, type, opening cash) — skippable
- [x] Dashboard: today's sale / cash-in / due sale / expense, total receivable & payable, cash balance, low stock, pending sync
- [x] Quick actions: বিক্রি · টাকা পেলাম · খরচ · কেনা (+ টাকা দিলাম, ফেরত, স্টক সমন্বয়, ক্যাশ যোগ/তোলা)
- [x] Transaction entry (cash/credit sale, payment in/out, purchase, expense, returns, stock adjust)
- [x] Customer & supplier list (search, sort by due/recent/name)
- [x] Party profile: balance, history, call, share statement text, PDF statement, reminder date
- [x] Products & stock: list, add/edit, low-stock alert, stock history
- [x] Reports: today / week / month / last month / all; payment-method split; CSV + JSON export
- [x] Settings: business profile, account & sync, language, reminders, export, help
- [x] Legal & support in-app: Contact us, Privacy Policy, Terms, Your Rights, Delete account & data

### Online features (optional login — app fully usable without internet)
- [x] Mobile number + OTP login
- [x] WorkManager sync (push/pull, exponential backoff, runs after reboot, only on network)
- [x] Sync status chip: সংরক্ষিত · sync হচ্ছে · sync সম্পন্ন · আবার চেষ্টা প্রয়োজন
- [x] Token refresh + logout + in-app account deletion
- [x] Daily reminder notification for customers with due date

## 2. Backend (Cloudflare Workers + D1 + R2 + Queues)
- [x] Worker API (TypeScript, Hono) — `/v1/...`
- [x] D1 schema + migrations (multi-tenant `business_id` on every row)
- [x] OTP auth: hashed codes, expiry, attempt + rate limits, review/demo number
- [x] JWT access token (15 min) + rotating refresh token, session revoke
- [x] Sync push (idempotent upsert) / pull (cursor by `server_seq`)
- [x] Account & data deletion (API + website flow), deletion audit log
- [x] Contact form endpoint
- [x] R2 backups, Queue consumer for heavy jobs (falls back inline if Queues unavailable)
- [x] Cron cleanup of expired OTP/sessions
- [x] Unit tests (vitest) + end-to-end tests on local Worker/D1 (28 checks)
- [ ] 👤 Add Cloudflare secrets, then run the **Backend** workflow — it creates D1 / R2 / Queue and deploys automatically (see `docs/SETUP.md`)
- [ ] 👤 Choose SMS gateway for OTP (e.g. BulkSMSBD / SSL Wireless) and add API key

## 3. Website (GitHub Pages) — Google Play required URLs
- [x] Home / landing page
- [x] Privacy Policy (`privacy.html`)
- [x] Terms & Conditions (`terms.html`)
- [x] Data deletion request (`data-deletion.html`) — works for uninstalled users too
- [x] Your Rights (`your-rights.html`)
- [x] Contact Us (`contact.html`)
- [ ] 👤 Enable GitHub Pages: Settings → Pages → Source = **GitHub Actions**
- [ ] 👤 Replace placeholder support email `support@hisabbondhu.com` if you don't own that domain

## 4. CI/CD (GitHub Actions)
- [x] Android: unit tests + debug APK on every push/PR
- [x] Android: signed **release AAB + APK** when signing secrets exist; GitHub Release on `v*` tag
- [x] Backend: typecheck + tests; deploy with Wrangler when Cloudflare secrets exist
- [x] Website: deploy to GitHub Pages
- [x] Signing keystore generated; secret names documented in `docs/SECRETS.md`
- [ ] 👤 Add GitHub secrets (Settings → Secrets and variables → Actions)

## 5. Google Play Console checklist
- [x] Store listing text (Bangla + English) — `docs/play-store/listing.md`
- [x] Data safety form answers — `docs/play-store/data-safety.md`
- [x] Content rating, target audience, ads, app access answers — `docs/play-store/policy-answers.md`
- [x] Release checklist — `docs/play-store/release-checklist.md`
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
