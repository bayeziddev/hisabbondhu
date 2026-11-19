<p align="center"><img src="brand/logo-mark.svg" width="96" alt="HisabBondhu logo"></p>

# হিসাববন্ধু · HisabBondhu

**দোকানের হিসাব, সবসময় হাতের কাছে।** — An offline-first Bangla digital ledger (khata) for small businesses in Bangladesh:
sales, dues (baki), payments, expenses, purchases and stock. Works with no internet and syncs automatically when online.

![Feature graphic](brand/play-store/feature-graphic-1024x500.png)

## Repository
| Folder | What |
|---|---|
| [`android/`](android) | Android app — Kotlin, Jetpack Compose (Material 3), Room, WorkManager, DataStore, Retrofit |
| [`backend/`](backend) | API — Cloudflare Workers (Hono, TypeScript) + D1 + R2 + Queues + Cron |
| [`website/`](website) | GitHub Pages site — Privacy Policy, Terms, Your rights, Data deletion, Contact (bilingual) |
| [`brand/`](brand) | Logo, icons, Play Store graphics, brand guide |
| [`docs/`](docs) | [Setup guide](docs/SETUP.md) · [Secrets](docs/SECRETS.md) · [Play Store kit](docs/play-store) · [Product status](docs/PRODUCT.md) |
| [`TODO.md`](TODO.md) | Roadmap checklist |

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
- Every row has a UUID created on the phone → retries never create duplicates.
- Money is integer **paisa**; balances, stock and cash are **derived** from the ledger, so merged data from several phones is always consistent.
- Mistakes are fixed with a **reversal** entry (never edited/deleted) → full audit trail.
- Customers/products use last-write-wins on `updatedAt`.

## Build & release
CI builds everything on every push — see **Actions**:
- **Android** → unit tests, debug APK, and **signed release AAB + APK** when signing secrets are set.
- **Backend** → typecheck, unit tests, end-to-end tests on a local Worker + D1, then deploy on `main`.
- **Website** → GitHub Pages on `main`.

Start with **[docs/SETUP.md](docs/SETUP.md)**.

## Licence
© HisabBondhu. All rights reserved. Third-party components: see `android/app/src/main/assets/licenses/`.
