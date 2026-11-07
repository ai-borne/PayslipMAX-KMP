#!/bin/sh
# R8 Check 2 for the Claim Guide (docs/Plan/rule_cards/16_guide_phase_plan.md): installs the R8-minified
# `minifiedTest` app on the attached Android device and runs :guideSmokeTest (a self-instrumenting test APK in
# its own process) against it.
#
# The app is installed with Play as its installer (-i com.android.vending), or the release integrity guard blocks a
# sideload. This script never uninstalls: an uninstall wipes the payslips on the device. If a build with another
# signature (for example the Play build) is installed, it stops and says so; back up, uninstall from every user
# profile yourself, and rerun.
set -u
cd "$(dirname "$0")/.." || exit 1

APP_ID="in.aiborne.payslipmax"
APP_APK="composeApp/build/outputs/apk/minifiedTest/composeApp-minifiedTest.apk"
TEST_APK="guideSmokeTest/build/outputs/apk/minifiedTest/guideSmokeTest-minifiedTest.apk"
TEST_ID="com.payslipmax.pdfparser.smoke"
TEST_CLASSES="com.payslipmax.pdfparser.smoke.GuideMinifiedSmokeTest,com.payslipmax.pdfparser.smoke.BackupMinifiedSmokeTest"
GOLDEN="shared/src/commonTest/resources/golden-backups/jvm-orders-bank-v5.pcda"
GOLDEN_ON_DEVICE="/sdcard/Download/p9smoke-v5.pcda"

./gradlew :composeApp:assembleMinifiedTest :guideSmokeTest:assembleMinifiedTest -PallowPlaceholderGemmaModel=true -q 2>&1 || exit 1

install_out=$(adb install -r -i com.android.vending --user current "$APP_APK" 2>&1)
if [ $? -ne 0 ]; then
    echo "$install_out"
    case "$install_out" in
        *INSTALL_FAILED_UPDATE_INCOMPATIBLE*)
            echo "❌ A build with another signature is installed. Back up (.pcda), uninstall $APP_ID from every user profile, rerun." ;;
        *) echo "❌ Could not install the minifiedTest app." ;;
    esac
    exit 1
fi
adb install -r --user current "$TEST_APK" >/dev/null 2>&1 || { echo "❌ Could not install the smoke test APK."; exit 1; }

# The backup leg restores the committed v5 golden (Plan 14a, P9): push it to Downloads for the file picker, take the counts it must preview
# from the manifest, and delete it afterwards. The restore is never committed, so the device's own data is untouched.
counts=$(python3 - <<'PY'
import json
m = {g["file"]: g for g in json.load(open("shared/src/commonTest/resources/golden-backups/manifest.json"))}["jvm-orders-bank-v5.pcda"]
print(f'-e payslips {m["payslips"]} -e entries {m["part2Entries"]} -e orders {m["part2Orders"]} -e pages {m["part2Pages"]}')
PY
)
[ -n "$counts" ] || { echo "❌ Could not read the golden's counts from the manifest."; exit 1; }
adb push "$GOLDEN" "$GOLDEN_ON_DEVICE" >/dev/null || { echo "❌ Could not push the golden to the device."; exit 1; }

# shellcheck disable=SC2086
result=$(adb shell am instrument -w -e class "$TEST_CLASSES" $counts "$TEST_ID/androidx.test.runner.AndroidJUnitRunner" 2>&1)
adb shell rm -f "$GOLDEN_ON_DEVICE"
echo "$result"
case "$result" in
    *"OK (4 tests)"*) echo "✅ Minified Guide, pre-claim and Backup & Restore smoke passed on the device." ;;
    *) echo "❌ Minified smoke failed."; exit 1 ;;
esac
