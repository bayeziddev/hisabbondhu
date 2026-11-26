#!/usr/bin/env python3
"""Make the narration with Google Gemini TTS.

    GEMINI_API_KEY=... python3 tools/voice-gemini.py            # only scenes without a voice file
    GEMINI_API_KEY=... python3 tools/voice-gemini.py s07 s12    # redo these scenes

Reads tools/narration.json (written by build-guide.mjs), writes assets/vo/<id>.mp3, then
run `node tools/build-guide.mjs` again so the scenes take the new lengths.
The key is read from the environment only; never put it in a file in this repository.
"""
import base64, json, os, re, subprocess, sys, tempfile, time, urllib.error, urllib.request

MODEL = os.environ.get("TTS_MODEL", "gemini-3.8-flash-tts")
VOICE = os.environ.get("TTS_VOICE", "Achird")  # friendly male voice
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
KEY = os.environ.get("GEMINI_API_KEY") or sys.exit("set GEMINI_API_KEY")


def tts(text: str) -> bytes:
    # Plain text only: style prompts in front of the text get read aloud by this model.
    body = {"contents": [{"parts": [{"text": text}]}],
            "generationConfig": {"responseModalities": ["AUDIO"],
                                 "speechConfig": {"voiceConfig": {"prebuiltVoiceConfig": {"voiceName": VOICE}}}}}
    req = urllib.request.Request(
        f"https://generativelanguage.googleapis.com/v1beta/models/{MODEL}:generateContent",
        data=json.dumps(body).encode(), headers={"Content-Type": "application/json", "x-goog-api-key": KEY})
    for attempt in range(6):
        try:
            r = json.load(urllib.request.urlopen(req, timeout=180))
            return base64.b64decode(r["candidates"][0]["content"]["parts"][0]["inlineData"]["data"])
        except urllib.error.HTTPError as e:
            if e.code in (429, 500, 503) and attempt < 5:
                time.sleep(12 * (attempt + 1))
                continue
            sys.exit(f"HTTP {e.code}: {e.read().decode()[:300]}")


def main():
    scenes = json.load(open(os.path.join(ROOT, "tools", "narration.json"), encoding="utf-8"))
    only = set(sys.argv[1:])
    for sc in scenes:
        out = os.path.join(ROOT, "assets", "vo", f"{sc['id']}.mp3")
        if only and sc["id"] not in only:
            continue
        if not only and os.path.exists(out):
            continue
        text = re.sub(r"\[[^\]]*\]\s*", "", sc["vo"]).strip()
        audio = tts(text)
        with tempfile.NamedTemporaryFile(suffix=".wav" if audio[:4] == b"RIFF" else ".pcm", delete=False) as f:
            f.write(audio)
        src = ["-i", f.name] if audio[:4] == b"RIFF" else ["-f", "s16le", "-ar", "24000", "-ac", "1", "-i", f.name]
        # trim the silence at both ends, then even out the loudness
        subprocess.run(["ffmpeg", "-v", "error", "-y", *src, "-af",
                        "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.05,areverse,"
                        "silenceremove=start_periods=1:start_threshold=-45dB:start_silence=0.12,areverse,"
                        "loudnorm=I=-16:TP=-1.5:LRA=11", "-ar", "44100", "-ac", "1", "-b:a", "128k", out], check=True)
        os.unlink(f.name)
        print("wrote", os.path.relpath(out, ROOT))


main()
