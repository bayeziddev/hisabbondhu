# HisabBondhu ad video (9:16, Bangla)

A ~76 s vertical ad for Reels / TikTok / Shorts / Facebook & Instagram Stories ads, built with
[HyperFrames](https://github.com/heygen-com/hyperframes) (HTML → MP4).

**Story:** hook ("কাগজের খাতায় বাকির হিসাব?") → app intro → step-by-step tutorial
(install → language → email code sign-in → profile → shop setup → dashboard → sale →
baki khata → WhatsApp reminder → stock → reports → offline & backup) → call to action.

Everything shown inside the phone is a real screenshot of the app (`assets/screens/`,
captured on the emulator). The store page, email-code sheet and WhatsApp chat are drawn,
because no capture of them exists; the reminder text is the app's real message.

## Edit

All scenes, on-screen text, captions and timings live in **`tools/build.mjs`**, the `SCENES`
table. Change a line and regenerate:

```bash
node tools/build.mjs                 # writes index.html + compositions/*.html
npx hyperframes check                # lint + layout + contrast + motion
npx hyperframes snapshot --at 5,30   # look at frames before rendering
npx hyperframes render -o renders/HisabBondhu-ad-9x16.mp4 -q delivery
```

`HYPERFRAMES_NO_TELEMETRY=1` turns the CLI's usage telemetry off.

## Layout

1080×1920, 30 fps. Words, the phone and the captions stay inside the area every short-video
app leaves clear (y 288–1248, x 120–888, narrowing to 780 below y 840). The brand line under
the captions is decoration and may be covered by the app's own buttons.

## Sound

Sound effects only (Pixabay licence, see `assets/sfx/CREDITS.md`, free for commercial use):
taps, whooshes, pops and one riser into the end card. There is **no music bed**. Add a track
from the ad platform's licensed library (Meta / TikTok ads manager) or drop an MP3 into
`assets/` and add an `<audio>` to `index.html` at volume ≈ 0.3.

## Assets

- `assets/screens/*.png` – real app screenshots (Bangla)
- `assets/fonts/` – Hind Siliguri (SIL Open Font License)
- `assets/vendor/gsap.min.js` – GSAP 3.15 (local, renders need no network)
