#!/usr/bin/env bash
# Installs the debug app + tour, runs it per language and pulls the screenshots.
# Usage (inside a running emulator session): bash android/tools/screenshot-tour.sh <name> "<langs>"
set -uo pipefail
NAME="${1:-phone}"
LANGS="${2:-bn}"
PKG=com.hisabbondhu.app.debug
APK=android/app/build/outputs/apk/debug/app-debug.apk
TEST_APK=android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
OUT="screenshots/$NAME"
mkdir -p "$OUT"

adb wait-for-device
adb install -r -g "$APK"
adb install -r -g "$TEST_APK"

# Clean, realistic status bar for store screenshots
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
  adb shell am instrument -w -r -e class com.hisabbondhu.app.ScreenshotTour -e prefix "$NAME" -e lang "$L" \
    "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/instrument-$L.log"
  adb exec-out run-as "$PKG" tar c -C files screenshots | tar x -C "$OUT" --strip-components=1 || true
done
adb logcat -d -s ScreenshotTour > "$OUT/tour-logcat.txt" || true
ls -la "$OUT"
