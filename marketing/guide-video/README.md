# HisabBondhu user guide video (16:9, Bangla, narrated)

A ~5 min tutorial for YouTube, Facebook and the Play Store promo video, built with
[HyperFrames](https://github.com/heygen-com/hyperframes) (HTML → MP4).

**Covers:** install → language → email-code sign-in → profile → shop setup → home dashboard
(হাতে থাকা টাকা, পাবো, দেবো, আজকের হিসাব) → entry buttons → cash sale → sale with products →
sale on credit (বাকি) with part payment → customer list → customer ledger (টাকা পেলাম / বাকি বিক্রি) →
reminders, WhatsApp/SMS and PDF statement → expenses and purchases (cash / credit, supplier
payment) → stock → reports → correcting a mistake (হিসাব বাতিল) → offline and backup →
settings and contact → call to action.

The left side is the app (real screenshots in `assets/screens/`); the right side shows the step
title, numbered steps that light up as the narrator reaches them, and word-by-word captions.
The store page, email-code sheet, WhatsApp chat, cancel dialog and offline banner are drawn,
because no capture of them exists; their texts are the app's real strings.

## Edit

Everything lives in **`tools/build-guide.mjs`**, the `SCENES` table: narration (`vo`), screen,
title, steps, and the actions (taps, highlights, zooms), each keyed to a word of the narration so
it lands when the narrator says it.

```bash
node tools/build-guide.mjs        # writes index.html, compositions/*.html, subtitles.srt
npx hyperframes check             # lint + layout + contrast + motion
npx hyperframes snapshot --at 20,70
npx hyperframes render -o renders/HisabBondhu-guide-16x9.mp4 -q delivery
```

`HYPERFRAMES_NO_TELEMETRY=1` turns the CLI's usage telemetry off.

## Voice

`assets/vo/sNN.mp3`, one file per scene, made with Google Gemini TTS (`gemini-3.8-flash-tts`,
voice "Achird"). Scene lengths follow the real audio length, so after changing a narration line:

```bash
node tools/build-guide.mjs                                # updates tools/narration.json
GEMINI_API_KEY=... python3 tools/voice-gemini.py s07      # re-voice that scene
node tools/build-guide.mjs                                # re-time with the new length
```

A scene with no voice file is timed from its text length, so the film can be previewed before
the voice exists. To use another voice (for example ElevenLabs "Rahat"), replace the MP3s with
the same names and rebuild.

`subtitles.srt` (generated) can be uploaded to YouTube / Facebook as the caption track.

## Sound

Narration plus soft tap clicks and one bass hit on the end card (Pixabay licence, see
`assets/sfx/CREDITS.md`). No music bed; add one at volume ≈ 0.12 under the voice if wanted.
