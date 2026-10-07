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
TEST_CLASS="com.payslipmax.pdfparser.smoke.GuideMinifiedSmokeTest"

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

result=$(adb shell am instrument -w -e class "$TEST_CLASS" "$TEST_ID/androidx.test.runner.AndroidJUnitRunner" 2>&1)
echo "$result"
case "$result" in
    *"OK (1 test)"*) echo "✅ Minified Guide smoke passed on the device." ;;
    *) echo "❌ Minified Guide smoke failed."; exit 1 ;;
esac
