#!/usr/bin/env bash
# Installs the debug app + tour, runs it per language and pulls the screenshots.
# Usage (inside a running emulator session): bash android/tools/screenshot-tour.sh <name> "<langs>"
set -uo pipefail
NAME="${1:-phone}"
LANGS="${2:-bn}"
PKG=com.hisabbondhu.myapp.debug
APK=android/app/build/outputs/apk/debug/app-debug.apk
TEST_APK=android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
OUT="screenshots/$NAME"
mkdir -p "$OUT"

adb wait-for-device
adb install -r -g "$APK"
adb install -r -g "$TEST_APK"

# No on-screen keyboard in screenshots (text is entered through accessibility).
for ime in $(adb shell ime list -s); do adb shell ime disable "$ime" >/dev/null 2>&1 || true; done

# Clean, realistic status bar
adb shell settings put global sysui_demo_allowed 1 || true
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null 2>&1 || true; }
demo enter
demo clock -e hhmm 0930
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4
demo network -e mobile show -e datatype none -e level 4
demo notifications -e visible false

for L in $LANGS; do
  adb shell pm clear "$PKG" >/dev/null
  adb logcat -c || true
  timeout 900 adb shell am instrument -w -r -e class com.hisabbondhu.app.ScreenshotTour -e prefix "$NAME" -e lang "$L" \
    "$PKG.test/androidx.test.runner.AndroidJUnitRunner" > "$OUT/instrument-$L.log" 2>&1
  echo "instrument exit: $? ($NAME/$L)"; tail -5 "$OUT/instrument-$L.log"
  adb exec-out run-as "$PKG" tar c -C files screenshots | tar x -C "$OUT" --strip-components=1 || true
  adb logcat -d -s ScreenshotTour:V TestRunner:V AndroidRuntime:E > "$OUT/logcat-$L.txt" || true
  grep -E "step|shot|not found|failed" "$OUT/logcat-$L.txt" | head -60 || true
done
ls -la "$OUT"
