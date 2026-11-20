# Setup guide — from code to Google Play

Time needed: about 1–2 hours (plus Google's review time).

## Step 1 — Signed AAB + APK from GitHub Actions
1. Add the 4 Android secrets from [`SECRETS.md`](SECRETS.md#1-android-signing-builds-the-signed-release-aab--apk).
2. GitHub → **Actions → “Android — build AAB & APK” → Run workflow** (or push any change under `android/`).
3. Open the finished run → **Artifacts** → download `HisabBondhu-<version>`. It contains:
   - `HisabBondhu-<version>-release.aab` → upload to Google Play
   - `HisabBondhu-<version>-release.apk` → install directly on phones for testing
   - `HisabBondhu-<version>-debug.apk` and `mapping-<version>.txt` (upload the mapping file to Play for readable crash reports)
4. To publish a GitHub Release with files attached: `git tag v1.0.0 && git push origin v1.0.0`.

`versionCode` = the workflow run number, so every build can be uploaded to Play without editing code.

## Step 2 — Cloudflare backend (online backup & sync)
Required: every shop signs in once (email code). After that the app works offline and syncs when online.

1. Create a free Cloudflare account → note the **Account ID**.
2. Create an **API token** (permissions listed in [`SECRETS.md`](SECRETS.md#2-cloudflare-backend-deploys-the-api-automatically)).
3. Add secrets `CLOUDFLARE_API_TOKEN`, `CLOUDFLARE_ACCOUNT_ID`, `JWT_SECRET`, `OTP_PEPPER` (+ SMS secrets).
4. GitHub → **Actions → “Backend — test & deploy (Cloudflare)” → Run workflow**.
   The workflow automatically: creates the D1 database (Asia-Pacific), R2 bucket and queue, applies migrations,
   deploys the Worker, uploads secrets and runs a health check.
5. Copy the API URL from the run summary (`https://hisabbondhu-api.<subdomain>.workers.dev`) and save it as
   repository **variable** `API_BASE_URL` (with a trailing `/`). Re-run the Android workflow so the app uses it.
6. Optional custom domain: Cloudflare → Workers → hisabbondhu-api → Settings → Domains & Routes → add `api.yourdomain.com`.

> **Queues** need a plan that includes them. If not available, the workflow deploys without the queue and the
> Worker does background jobs inline — nothing else changes.

### Email login codes with Resend (free tier)
1. Sign up at resend.com → **Domains → Add domain** → `sayadbayezid.com` → add the shown DNS records (SPF/DKIM) at your domain DNS → wait for **Verified**.
2. **API Keys → Create API key** (Sending access) → save as GitHub secret `RESEND_API_KEY`.
3. Codes are sent from `HisabBondhu <info@sayadbayezid.com>`; replies go to `support@sayadbayezid.com` (change in `backend/wrangler.toml`).
4. Add `REVIEW_EMAIL` + `REVIEW_OTP` secrets so Google reviewers can sign in without a real inbox.

### SMS phone verification (later, when you start earning)
Already built. Get a BulkSMSBD or SSL Wireless API key + sender ID, add `SMS_API_KEY`/`SMS_SENDER_ID`, set variables
`SMS_PROVIDER=bulksmsbd` and `PHONE_OTP_ENABLED=true`, re-run the Backend workflow.

### Your leads (every shop that signs up)
```bash
curl -H "Authorization: Bearer $ADMIN_TOKEN" "https://<your-api>.workers.dev/v1/admin/leads?format=csv" -o leads.csv
```
Columns: name, email, phone, marketing opt-in, shop name/type/address, sign-up & last-active dates, number of entries.
Only send promotions to rows with `marketing_opt_in = yes`.

## Step 3 — Website (Privacy Policy, Terms, Data deletion, Contact)
1. GitHub → **Settings → Pages → Build and deployment → Source: GitHub Actions**.
2. Merge this branch into `main` (Pages deploys from `main`), or run **“Website — GitHub Pages”** manually from `main`.
3. Your URLs (needed by Play Console):
   - Privacy Policy: `https://bayeziddev.github.io/hisabbondhu/privacy.html`
   - Data deletion: `https://bayeziddev.github.io/hisabbondhu/data-deletion.html`
   - Terms: `…/terms.html` · Your rights: `…/your-rights.html` · Contact: `…/contact.html`
4. Support contact is set to `support@sayadbayezid.com` and WhatsApp `+8801519601517` everywhere (app, website, policies).

## Step 4 — Google Play Console
Follow [`play-store/release-checklist.md`](play-store/release-checklist.md). Copy-paste texts are in
[`play-store/listing.md`](play-store/listing.md), form answers in [`play-store/data-safety.md`](play-store/data-safety.md)
and [`play-store/policy-answers.md`](play-store/policy-answers.md). Graphics are in [`../brand/play-store/`](../brand/play-store).

## Local development
```bash
# Android (needs Android Studio / SDK)
cd android && ./gradlew assembleDebug testDebugUnitTest

# Backend
cd backend && npm ci
cp .dev.vars.example .dev.vars          # local secrets
npm run db:migrate:local && npm run dev  # http://127.0.0.1:8787
npm test && npm run e2e                   # unit + end-to-end tests

# Brand images & website pages
node brand/tools/render.js
python3 website/tools/build_pages.py
python3 android/tools/gen_strings.py      # after editing android/tools/strings_source.py
```
