#!/usr/bin/env bash
# Runs the full user-guide tour (Bangla) and pulls its screenshots into screenshots/guide.
# Usage (inside a running emulator session): bash android/tools/guide-tour.sh
set -uo pipefail
PKG=com.hisabbondhu.myapp.debug
APK=android/app/build/outputs/apk/debug/app-debug.apk
TEST_APK=android/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
OUT="screenshots/guide"
mkdir -p "$OUT"

adb wait-for-device
adb install -r -g "$APK"
adb install -r -g "$TEST_APK"

# No on-screen keyboard and no system error dialogs in the screenshots.
for ime in $(adb shell ime list -s); do adb shell ime disable "$ime" >/dev/null 2>&1 || true; done
adb shell settings put global hide_error_dialogs 1 >/dev/null 2>&1 || true

# Clean, realistic status bar
adb shell settings put global sysui_demo_allowed 1 || true
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" >/dev/null 2>&1 || true; }
demo enter
demo clock -e hhmm 0930
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4
demo network -e mobile show -e datatype none -e level 4
demo notifications -e visible false

adb shell pm clear "$PKG" >/dev/null
adb logcat -c || true
timeout 1800 adb shell am instrument -w -r -e class com.hisabbondhu.app.GuideTour \
  "$PKG.test/androidx.test.runner.AndroidJUnitRunner" > "$OUT/instrument.log" 2>&1
echo "instrument exit: $?"; tail -5 "$OUT/instrument.log"
adb exec-out run-as "$PKG" tar c -C files screenshots | tar x -C "$OUT" --strip-components=1 || true
adb logcat -d -s GuideTour:V TestRunner:V AndroidRuntime:E > "$OUT/logcat.txt" || true
grep -E "step|shot|not found|no field|failed" "$OUT/logcat.txt" | head -120 || true
ls -la "$OUT"
