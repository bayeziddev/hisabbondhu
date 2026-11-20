# Google Play Console — everything in one place

Package: `com.hisabbondhu.myapp` · Category: **Business** · Free · Default language: **Bengali (bn-BD)** + English (en-US)

## 1. Graphics (Store presence → Main store listing)
| Play field | Size | File |
|---|---|---|
| App icon | 512 × 512 PNG | `brand/play-store/icon-512.png` |
| Feature graphic | 1024 × 500 PNG | `brand/play-store/feature-graphic-1024x500.png` |
| Phone screenshots (2–8) | 1080 × 1920 | `brand/play-store/screenshots/phone/` (Bangla) · `screenshots/phone-en/` (English listing) |
| 7-inch tablet screenshots | 1080 × 1920 | `brand/play-store/screenshots/tablet-7/` |
| 10-inch tablet screenshots | 1920 × 1080 | `brand/play-store/screenshots/tablet-10/` |
| Chromebook screenshots | 1920 × 1080 | `brand/play-store/screenshots/chromebook/` |
| Android TV banner | 1280 × 720 | `brand/play-store/tv-banner-1280x720.png` (only if you ever publish a TV version) |
| Icon in every size | 48 → 1024 | `brand/play-store/icons/` |

**Not applicable — leave empty:** Wear OS, Android TV screenshots, Android Automotive, Android XR. HisabBondhu is a
phone/tablet app; those sections only appear for apps built for those devices, and screenshots of a UI that does not
exist would break Play's metadata policy.

## 2. Texts → [`listing.md`](listing.md)
App name (30), short description (80), full description (bn + en), contact details.

## 3. Release → [`release-notes.md`](release-notes.md)
Release name `1.0.0 – First release` + release notes (bn-BD / en-US).

## 4. App content
- Data safety → import [`data-safety/hisabbondhu-data-safety.csv`](data-safety/hisabbondhu-data-safety.csv)
- Everything else (privacy policy, app access login, ads, rating, audience, financial features…) → [`policy-answers.md`](policy-answers.md)

## 5. Order of work → [`release-checklist.md`](release-checklist.md)
