# HisabBondhu — product summary & implementation status

Source: the owner's Final PRD (হিসাববন্ধু — বাংলাদেশি ক্ষুদ্র ব্যবসার জন্য বাংলা ডিজিটাল হিসাব অ্যাপ).

**Vision:** an offline-first Bangla digital ledger for small businesses in Bangladesh. Entries are written without
internet and synced automatically later. A transaction should take 10–15 seconds; one entry updates the cash, ledger,
stock and reports together.

**First-release focus (from the PRD):** fast sale entry · customer & supplier ledger · offline mode · clear statement
sharing · accurate cash & due calculation.

## MVP feature status
| PRD section | Status | Notes |
|---|---|---|
| 5.1 Mobile login (OTP) | ✅ | Optional login; hashed OTP, rate limits, refresh rotation, device sessions |
| 5.2 Business setup wizard | ✅ | Skippable; shop, owner, mobile, address, type, opening cash |
| 5.3 Home dashboard | ✅ | Today sale / cash-in / credit / expense / profit, receivable, payable, cash, low stock, sync status |
| 5.4 Customer ledger | ✅ | Search, sort (due/recent/name), opening due, reminder date, statement share, PDF |
| 5.5 Supplier ledger | ✅ | Same as customers with payable direction |
| 5.6 Transaction types | ✅ | Sale (cash/credit), payment in/out, purchase, expense, sale/purchase return, stock adjust, cash in/out; opening balances on party & business |
| 5.7 Quick flows | ✅ | One screen per type, colour-coded, cash/credit toggle, “add another” |
| 5.8 Automatic calculation | ✅ | Integer paisa; balances derived from append-only ledger |
| 5.9 Transaction message | ✅ | Bangla/English text via share sheet, WhatsApp, SMS; PDF statement |
| 5.10 Reminder | ✅ (basic) | Per-customer date + daily notification; bulk/auto reminders later (paid) |
| 5.11 Basic stock | ✅ | Units, prices, min-stock alert, history; barcode stored (scanner = phase 2) |
| 5.12 Reports & export | ✅ | Periods, payment-method split, expense breakdown; CSV + JSON |
| 8 Offline-first & sync | ✅ | Room source of truth, WorkManager, idempotent push, cursor pull, LWW for master data, reversals |
| 10 Cloudflare backend | ✅ | Workers + D1 + R2 + Queues + cron, tenant isolation, audit log |
| 11 API | ✅ | `/v1/auth/*`, `/v1/businesses/*`, `/v1/businesses/{id}/sync/push|pull`, `/v1/account`, `/v1/public/contact` |
| Legal & Play requirements | ✅ | Privacy, Terms, Your rights, Data deletion (in-app + web), Contact |

## Deviations from the PRD (decisions made while building)
- **Login is optional** instead of required — the app is usable instantly offline, and Play reviewers need no test account.
- **Manual dependency injection** instead of Hilt — fewer build-time moving parts, faster cold start on low-end phones.
- **Transactions have one optional product line** (cart/POS with many lines is phase 2, as the PRD suggests).
- **Profit is an estimate**: sales − purchase cost of items sold − expenses.

## Next phases (from the PRD roadmap)
Phase 2: PDF invoice, barcode scanner, staff accounts & roles, reminder queue, thermal printer, multi-device realtime.
Phase 3: branches, POS, bKash/Nagad reconciliation, Bangla QR, supplier orders, web admin. Phase 4: iOS, web dashboard.
