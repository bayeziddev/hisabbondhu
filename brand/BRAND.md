# হিসাববন্ধু · HisabBondhu — Brand guide

**Tagline:** দোকানের হিসাব, সবসময় হাতের কাছে · *Your shop's accounts, always at hand*
**Promise:** Your shop's daily sales, dues, cash, expenses, products and stock — simple and in one place.
**Personality:** simple · trustworthy · fast · Bangla-first · helpful · clear · small-business friendly

## Logo
A white ledger (khata) with a mint binding strip and green ruled lines, finished by a blue “verified” tick badge —
*accurate accounts you can trust*. It is original artwork and does not imitate any existing ledger app.

| File | Use |
|---|---|
| `logo-mark.svg` / `logo-mark-1024.png` | App-style rounded mark (website, social, documents) |
| `logo-mark-square.svg` → `play-store/icon-512.png` | Google Play icon (full-bleed; Play applies rounding) |
| `logo-foreground.svg` | Adaptive-icon foreground (inside the 66dp safe circle) |
| `logo-horizontal.svg` / `.png` | Mark + wordmark lockup |
| `play-store/feature-graphic-1024x500.png` | Play feature graphic (source: `tools/feature-graphic.html`) |

Android uses vector versions in `android/app/src/main/res/drawable/` (adaptive, monochrome/themed, notification icon).
Regenerate PNGs: `node brand/tools/render.js`.

**Do:** keep clear space of ¼ mark width; use on green or white. **Don't:** recolour the tick red, stretch, add shadows/outlines, or put on busy photos.

## Colours
| Token | Hex | Use |
|---|---|---|
| Primary green | `#087F5B` | Brand, primary buttons, sale |
| Deep green | `#065F46` | Gradients, dark accents |
| Secondary blue | `#1971C2` | Tick badge, “payment received”, links |
| Success | `#2B8A3E` | Positive money, profit |
| Warning | `#F08C00` | Low stock, payable |
| Danger | `#C92A2A` | Dues, expenses, destructive actions |
| Background | `#F8F9FA` | App/web background |
| Text | `#212529` | Body text |

Dark theme equivalents live in `android/.../ui/theme/Color.kt` and `website/assets/site.css`.

## Typography
**Hind Siliguri** (SIL Open Font License 1.1) for Bangla and Latin — Regular 400, Medium 500, SemiBold 600, Bold 700.
Bundled in the app (`res/font/`) and loaded from Google Fonts on the website. Licence: `brand/fonts/OFL.txt`.

## Voice
- Write how shopkeepers speak: “পাবো / দেবো”, “বাকি”, “টাকা পেলাম” — not accounting jargon.
- Short, polite, reassuring (“ফোনে নিরাপদে সংরক্ষিত”). Never shame customers in reminder messages.
- Numbers in Bengali digits with lakh grouping in Bangla UI: ৳১,২৫,০০০.
