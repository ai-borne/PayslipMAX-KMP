#!/usr/bin/env bash
# ==============================================================================
# verify_device.sh — Automated On-Device Hardware Verification for PayslipMax AI
# Targets: Connected Google Pixel 9 (or auto-detected ADB device)
# Phase 6: Release Sign-Off Traversal (Onboarding, Discovery, Matrix Affordance,
# Hazard Demystification, and Military Redressal Kit Protocol).
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
OUTPUT_DIR="${1:-${WORKSPACE_ROOT}/build/device_verification/onboarding_release}"
mkdir -p "${OUTPUT_DIR}"

echo "=========================================================="
echo "  PayslipMax AI — Automated Pixel 9 Device Verification   "
echo "  Phase 6: Onboarding, Discovery & Release Sign-Off       "
echo "=========================================================="
echo "Workspace:  ${WORKSPACE_ROOT}"
echo "Output Dir: ${OUTPUT_DIR}"

# 1. Pre-flight ADB checks
command -v adb >/dev/null 2>&1 || { echo "ERROR: adb command not found on PATH"; exit 1; }

DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')
if [ "${DEVICE_COUNT}" -eq 0 ]; then
    echo "ERROR: No authorized ADB device connected."
    exit 1
fi

DEVICE_ID="${DEVICE_SERIAL:-$(adb devices | grep -v "List" | grep "device$" | head -n 1 | awk '{print $1}')}"
DEVICE_MODEL=$(adb -s "${DEVICE_ID}" shell getprop ro.product.model | tr -d '\r')
ANDROID_VER=$(adb -s "${DEVICE_ID}" shell getprop ro.build.version.release | tr -d '\r')
SCREEN_RES=$(adb -s "${DEVICE_ID}" shell wm size | tr -d '\r')
echo "Target Device: ${DEVICE_MODEL} (${DEVICE_ID})"
echo "Android:       ${ANDROID_VER}"
echo "Screen:        ${SCREEN_RES}"

# 2. Wake screen and clear lock
ensure_device_awake() {
    local wakefulness
    wakefulness=$(adb -s "${DEVICE_ID}" shell dumpsys power | grep "mWakefulness=" | head -n 1 | tr -d '\r')
    if [[ "${wakefulness}" != *"Awake"* ]]; then
        echo "Waking up device display..."
        adb -s "${DEVICE_ID}" shell input keyevent 26
        adb -s "${DEVICE_ID}" shell input keyevent 82
        sleep 1
    fi
}
ensure_device_awake

# Animation scale controls
set_animation_scales() {
    local scale="$1"
    adb -s "${DEVICE_ID}" shell settings put global window_animation_scale "${scale}" 2>/dev/null || true
    adb -s "${DEVICE_ID}" shell settings put global transition_animation_scale "${scale}" 2>/dev/null || true
    adb -s "${DEVICE_ID}" shell settings put global animator_duration_scale "${scale}" 2>/dev/null || true
}
set_animation_scales 0.5

# 3. Sideload fresh debug build targeting current user
echo "Building and installing :composeApp:installDebug..."
(cd "${WORKSPACE_ROOT}" && ./gradlew :composeApp:installDebug --daemon)

# 4. Reset onboarding preference to test first-time orientation sheet
echo "Resetting has_seen_pcdao_audit_intro preference..."
adb -s "${DEVICE_ID}" shell 'run-as in.aiborne.payslipmax sed -i "/has_seen_pcdao_audit_intro/d" shared_prefs/payslipmax_onboarding_prefs.xml' 2>/dev/null || true

# 5. Launch app cleanly
echo "Launching PayslipMax MainActivity..."
adb -s "${DEVICE_ID}" shell am force-stop in.aiborne.payslipmax
sleep 1
adb -s "${DEVICE_ID}" shell am start -n in.aiborne.payslipmax/com.payslipmax.pdfparser.MainActivity
sleep 3

capture_step() {
    local step_num="$1"
    local step_name="$2"
    local dest="${OUTPUT_DIR}/${step_num}_${step_name}.png"
    echo "  [Capture] ${step_num}: ${step_name} -> ${dest}"
    adb -s "${DEVICE_ID}" exec-out screencap -p > "${dest}"
}

automator() {
    python3 "${SCRIPT_DIR}/device_automator.py" --device "${DEVICE_ID}" "$@"
}

echo ""
echo "----------------------------------------------------------"
echo "  SCENARIO 01: App Launch & Primary Dashboard Discovery   "
echo "----------------------------------------------------------"
automator wait-for --res-id "dashboard_audit_banner_card" --timeout 10
automator wait-for --contains "statements analyzed" --timeout 5
capture_step "01" "dashboard_discovery"

echo ""
echo "----------------------------------------------------------"
echo "  SCENARIO 02 & 03: Enter Cockpit & First-Time Onboarding "
echo "----------------------------------------------------------"
echo "Tapping Dashboard Audit CTA..."
automator tap --res-id "dashboard_audit_cta"
sleep 2

echo "Verifying Slide 1: Automated IRLA Statutory Audit..."
automator wait-for --contains "IRLA Statutory Audit" --timeout 5
capture_step "02" "onboarding_slide_1_audit"

echo "Advancing to Slide 2: Situational Matrix..."
automator tap --text "Next"
sleep 1
automator wait-for --contains "Situational Matrix" --timeout 5
capture_step "03" "onboarding_slide_2_matrix"

echo "Advancing to Slide 3: Redressal Kit..."
automator tap --text "Next"
sleep 1
automator wait-for --contains "Redressal Kit" --timeout 5
capture_step "04" "onboarding_slide_3_redressal"

echo "Dismissing Onboarding Sheet with 'Enter Cockpit'..."
automator tap --text "Enter Cockpit"
sleep 2
automator wait-for --contains "PayslipMax AI" --timeout 5
capture_step "05" "pcdao_cockpit_initial"

echo ""
echo "----------------------------------------------------------"
echo "  SCENARIO 04: Replay Guide Top-Bar Action                "
echo "----------------------------------------------------------"
echo "Tapping Guide button on top bar..."
automator tap --res-id "pcdao_guide_button"
sleep 1
automator wait-for --contains "IRLA Statutory Audit" --timeout 5
capture_step "06" "guide_replayed"

echo "Dismissing replayed guide via Skip..."
automator tap --text "Skip"
sleep 1
capture_step "07" "cockpit_after_guide"

echo ""
echo "----------------------------------------------------------"
echo "  SCENARIO 05: Situational Matrix Tab Affordance          "
echo "----------------------------------------------------------"
echo "Verifying scroll cue affordance is visible..."
automator wait-for --res-id "matrix_scroll_cue" --timeout 5
capture_step "08" "matrix_tabs_scroll_cue_visible"

echo "Tapping scroll cue to scroll category tabs to the end..."
for i in {1..6}; do
    automator tap --res-id "matrix_scroll_cue" --scrolls 0 --optional || true
    sleep 0.5
done
automator wait-for --contains "Funds & Release" --timeout 5
capture_step "09" "matrix_tabs_scrolled_to_end_cue_hidden"

echo ""
echo "----------------------------------------------------------"
echo "  SCENARIO 06: Hazard Demystification Dialog              "
echo "----------------------------------------------------------"
echo "Tapping Recovery Hazards KPI card..."
automator tap --res-id "hazards_kpi_card" --scrolls 2
sleep 1
automator wait-for --text "Understanding Recovery Hazards" --timeout 5
automator wait-for --contains "TR-230(B)" --timeout 5
capture_step "10" "hazard_explainer_dialog"

echo "Dismissing hazard dialog via Understood..."
automator tap --text "Understood"
sleep 1
capture_step "11" "cockpit_hazard_dismissed"

echo ""
echo "----------------------------------------------------------"
echo "  SCENARIO 07: 1-Tap PCDA(O) Redressal Kit Verification   "
echo "----------------------------------------------------------"
echo "Scrolling down to Redressal Kit button..."
automator tap --res-id "redressal_kit_button" --scrolls 4
sleep 2

echo "Verifying formal military correspondence & inferred rank..."
automator wait-for --contains "CONFIDENTIAL & OFFICIAL MILITARY CORRESPONDENCE" --timeout 5
automator wait-for --contains "Lt Colonel" --timeout 5
automator wait-for --contains "Yours faithfully" --timeout 5
capture_step "12" "redressal_kit_dialog"

echo "Dismissing Redressal Kit dialog..."
automator tap --desc "Close" || adb -s "${DEVICE_ID}" shell input keyevent 4
sleep 1
capture_step "13" "release_verified_final"

# Restore animation scales to default
echo "Restoring animation scales to 1.0x..."
set_animation_scales 1.0

echo ""
echo "=========================================================="
echo "  Phase 6 Hardware Verification Complete! Visual Proof:   "
echo "  Directory: ${OUTPUT_DIR}"
echo "=========================================================="
ls -la "${OUTPUT_DIR}"
