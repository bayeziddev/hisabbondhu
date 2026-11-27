# HisabBondhu — rules for every change

README.md is the project wiki. Read its "Rules" section first; these are the ones that must never slip:

1. **Before and with every change, update README.md** — repository map (new files), API table (new/changed endpoints),
   Database (new tables/columns), Android screens/routes, configuration. Then run `node backend/scripts/check-api.mjs`
   (also runs in CI as "API contract"); it fails if the server, `HisabApi.kt`, the website and the README API table disagree.
2. **Play Store package is `com.hisabbondhu.myapp`** (namespace stays `com.hisabbondhu.app`). Never change either;
   Play links use `Share.PLAY_PACKAGE`.
3. **Generated files — never edit by hand:**
   - `android/app/src/main/res/values*/strings.xml` ← `android/tools/strings_source.py` + `python3 android/tools/gen_strings.py`
   - `website/*.html` ← `website/tools/build_pages.py` + `python3 website/tools/build_pages.py`
   - `docs/play-store/data-safety/hisabbondhu-data-safety.csv` ← `build_csv.py`
4. **Database:** new `backend/migrations/000N_*.sql`, additive; never edit an applied migration. Room schema change →
   version bump + migration.
5. Money in paisa, quantity in milli-units, time in epoch ms (UTC); ledger is append-only (reversals, no deletes).
6. New personal data → privacy policy + terms (`build_pages.py`) + Data safety (`build_csv.py`).
7. Work in the order of `docs/ROADMAP.md` (master list) and its Definition of Done; mark items ✅ when finished.
8. Tests: `cd backend && npm run typecheck && npm test && npm run check:api`; e2e per `.github/workflows/backend.yml`.
   Android builds in CI (Google Maven may be blocked locally).
9. No secrets in the repo (keystore, API keys, tokens, `.dev.vars`).
