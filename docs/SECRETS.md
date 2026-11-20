# GitHub Secrets & Variables

Add these in **GitHub → repository → Settings → Secrets and variables → Actions**.
Values are **never** committed to the repository. (The generated values were handed to the owner privately.)

## 1. Android signing (builds the signed release AAB + APK)

| Secret name | What it is |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | The upload keystore file (`hisabbondhu-upload.jks`) encoded as one base64 line |
| `ANDROID_KEYSTORE_PASSWORD` | Keystore (store) password |
| `ANDROID_KEY_ALIAS` | Key alias — `hisabbondhu-upload` |
| `ANDROID_KEY_PASSWORD` | Key password (same as the store password — PKCS12 keystores use one password) |

**Keep a safe backup** of `hisabbondhu-upload.jks` and its password (e.g. Google Drive + a password manager).
With **Play App Signing** (default for new apps) this is only your *upload* key — if it is ever lost,
you can ask Google Play support to reset it. Without the keystore you cannot publish updates until it is reset.

Make your own keystore instead (optional):
```bash
keytool -genkeypair -v -keystore hisabbondhu-upload.jks -storetype PKCS12 -alias hisabbondhu-upload \
  -keyalg RSA -keysize 4096 -validity 10950 -dname "CN=HisabBondhu, O=HisabBondhu, L=Dhaka, C=BD"
base64 -w0 hisabbondhu-upload.jks   # macOS: base64 -i hisabbondhu-upload.jks
```

## 2. Cloudflare backend (deploys the API automatically)

| Secret name | Required | What it is / where to get it |
|---|---|---|
| `CLOUDFLARE_API_TOKEN` | ✅ | Cloudflare dashboard → My Profile → API Tokens → Create Token → **Edit Cloudflare Workers** template, then add **Account · D1 · Edit** and **Account · Queues · Edit** (R2 edit is included in the template as *Workers R2 Storage*) |
| `CLOUDFLARE_ACCOUNT_ID` | ✅ | Cloudflare dashboard → Workers & Pages → right sidebar “Account ID” |
| `JWT_SECRET` | ✅ | Random 48+ byte string — signs login tokens (`openssl rand -base64 48`) |
| `OTP_PEPPER` | ✅ | Random 32+ byte hex — hashes OTP codes (`openssl rand -hex 32`) |
| `RESEND_API_KEY` | ✅ | resend.com → API Keys → Create (permission: Sending access). Login codes are emailed from `info@sayadbayezid.com` |
| `ADMIN_TOKEN` | ✅ | Random 32+ char string — protects `/v1/admin/leads` (download your shop leads as CSV) |
| `REVIEW_EMAIL` | ✅ for Play review | Test email for Google reviewers, e.g. `play.review@sayadbayezid.com` (no email is sent to it) |
| `REVIEW_OTP` | ✅ for Play review | Fixed 6-digit code for `REVIEW_EMAIL` |
| `SMS_API_KEY`, `SMS_SENDER_ID`, `SMS_API_URL` | later | Only when you turn on SMS phone verification (`PHONE_OTP_ENABLED=true`) |
| `SUPPORT_WEBHOOK_URL` | optional | Slack/Discord webhook to get contact-form messages instantly |

> Changing `JWT_SECRET` logs everyone out. Changing `OTP_PEPPER` only invalidates codes sent in the last 5 minutes.

## 3. Repository variables (not secret)

**Settings → Secrets and variables → Actions → Variables tab**

| Variable | Example | Used by |
|---|---|---|
| `API_BASE_URL` | `https://hisabbondhu-api.<your-subdomain>.workers.dev/` | Android build + website forms |
| `WEBSITE_URL` | `https://bayeziddev.github.io/hisabbondhu/` | Android build (legal links) |
| `SUPPORT_EMAIL` | `support@sayadbayezid.com` (already the default) | Android build + website |
| `ALLOWED_ORIGINS` | `https://bayeziddev.github.io` | API CORS for website forms |
| `SMS_PROVIDER` | `bulksmsbd` or `sslwireless` (later) | API |
| `PHONE_OTP_ENABLED` | `false` now, `true` when SMS is paid for | API |
| `SUPPORT_WHATSAPP` | `8801519601517` | Android build (Contact screen) |
