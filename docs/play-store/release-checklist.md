# Release checklist — first Play Store launch

## Before you start
- [ ] Google Play developer account (one-time US$25). New **personal** accounts must run a **closed test with
      at least 12 testers for 14 continuous days** before applying for production access.
- [ ] Check the name: search “হিসাববন্ধু / HisabBondhu” on Play, check domain & Facebook page availability.
- [ ] Support email you own set as `SUPPORT_EMAIL` variable (website + app rebuilt).
- [ ] Website live on GitHub Pages (privacy + data-deletion URLs open on a phone).
- [ ] Backend deployed and `API_BASE_URL` variable set (optional for an offline-only first release).

## Build
- [ ] Signing secrets added → run **Android — build AAB & APK** → download artifact.
- [ ] Install the **release APK** on 2–3 phones (one low-end, Android 8–10). Test: airplane-mode entry, reminder
      notification, PDF share, WhatsApp message, Bangla ↔ English switch, login + sync, logout, delete data.

## Play Console
1. **Create app** → name, default language Bengali (bn-BD), App, Free, accept declarations.
2. **App integrity → Play App Signing**: keep “Google-managed key” (default). Your keystore becomes the upload key.
3. **App content**: fill every item using `policy-answers.md` and `data-safety.md`.
4. **Store listing**: texts from `listing.md`, icon + feature graphic from `brand/play-store/`, 4–8 phone screenshots.
5. **Store settings**: Category *Business*, contact email, website.
6. **Testing → Internal testing**: create release, upload the `.aab`, release notes (bn + en), add your testers' emails.
7. **Testing → Closed testing**: same AAB, 12+ testers, keep it running 14 days, collect feedback.
8. **Production**: apply for access → create release → roll out (start with 20% staged rollout).
9. Upload `mapping-<version>.txt` under **App bundle explorer → Downloads → ReTrace mapping** for crash de-obfuscation.

## Every update
- [ ] Push to `main` (or tag `vX.Y.Z`) → new AAB with a higher versionCode automatically.
- [ ] Upload AAB to internal testing → test → promote to production.

## Release notes template
```
<bn-BD>
• নতুন: …
• উন্নতি: …
• সমস্যার সমাধান: …
</bn-BD>
<en-US>
• New: …
• Improved: …
• Fixed: …
</en-US>
```
