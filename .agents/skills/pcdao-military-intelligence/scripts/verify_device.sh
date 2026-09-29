#!/usr/bin/env bash
# ==============================================================================
# verify_device.sh — Automated On-Device Visual Verification for PayslipMax AI
# Targets: Connected Google Pixel 9 (or auto-detected ADB device)
# ==============================================================================

set -euo pipefail

# 1. Resolve workspace root & output directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"
OUTPUT_DIR="${1:-${WORKSPACE_ROOT}/build/device_verification/$(date +%Y%m%d_%H%M%S)}"
mkdir -p "${OUTPUT_DIR}"

echo "=========================================================="
echo "  PayslipMax AI — Automated Pixel 9 Device Verification   "
echo "=========================================================="
echo "Workspace:  ${WORKSPACE_ROOT}"
echo "Output Dir: ${OUTPUT_DIR}"

# 2. Pre-flight ADB checks
command -v adb >/dev/null 2>&1 || { echo "ERROR: adb command not found on PATH"; exit 1; }

DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l | tr -d ' ')
if [ "${DEVICE_COUNT}" -eq 0 ]; then
    echo "ERROR: No authorized ADB device connected."
    exit 1
fi

DEVICE_ID="${DEVICE_SERIAL:-$(adb devices | grep -v "List" | grep "device$" | head -n 1 | awk '{print $1}')}"
DEVICE_MODEL=$(adb -s "${DEVICE_ID}" shell getprop ro.product.model | tr -d '\r')
echo "Target Device: ${DEVICE_MODEL} (${DEVICE_ID})"

# 3. Wake screen and clear lock if needed
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

# 4. Sideload fresh debug build targeting current user
echo "Building and installing :composeApp:installDebug..."
(cd "${WORKSPACE_ROOT}" && ./gradlew :composeApp:installDebug --daemon)

# 5. Launch app cleanly
echo "Launching PayslipMax MainActivity..."
adb -s "${DEVICE_ID}" shell am force-stop in.aiborne.payslipmax
adb -s "${DEVICE_ID}" shell am start -n in.aiborne.payslipmax/com.payslipmax.pdfparser.MainActivity
sleep 2

# Helper: Capture screencap
capture_step() {
    local step_num="$1"
    local step_name="$2"
    local dest="${OUTPUT_DIR}/${step_num}_${step_name}.png"
    echo "  [Capture] ${step_num}: ${step_name} -> ${dest}"
    adb -s "${DEVICE_ID}" exec-out screencap -p > "${dest}"
}

# 6. Interactive Traversal on Pixel 9 (Dynamic UIAutomator Selectors)
echo "Executing automated visual UI flow using device_automator.py..."
automator() {
    python3 "${SCRIPT_DIR}/device_automator.py" --device "${DEVICE_ID}" "$@"
}

# Step 1: Dashboard
capture_step "01" "dashboard"

# Step 2: Navigate to Insights tab (Dynamic text/desc selector)
echo "Navigating to Insights tab..."
automator tap --text "Insights" || automator tap --desc "Insights"
sleep 1
capture_step "02" "insights_screen"

# Step 3: Open PayslipMax AI card (Dynamic testTag/res-id selector with auto-scroll)
echo "Opening PayslipMax AI Cockpit..."
automator tap --res-id "pcdao_card" --scrolls 4 || automator tap --contains "PayslipMax AI" --scrolls 4
sleep 2
capture_step "03" "pcdao_cockpit"

# Step 4: Scroll down to Situational Matrix if needed
echo "Inspecting Situational Matrix..."
automator wait-for --res-id "posting_peace_tile" --timeout 5 || automator swipe --direction down
capture_step "04" "situational_matrix"

# Step 5: Open "+ Add Specialized Factor" bottom sheet (Dynamic testTag)
echo "Opening Specialized Factors sheet..."
automator tap --res-id "add_factor_button" --scrolls 2 || automator tap --contains "Specialized Factor" --scrolls 2
sleep 1
capture_step "05" "specialized_factors_sheet"

# Step 6: Select MARCOS factor and tap Apply
echo "Selecting MARCOS Special Forces factor..."
automator tap --contains "MARCOS" --scrolls 1 || automator tap --contains "Special Forces" --scrolls 1
automator tap --contains "Apply Factors" --scrolls 1 || automator tap --text "Apply Factors" --scrolls 1
sleep 1

# Step 7: Toggle Peace UA tile to trigger collision with HAFAA (Dynamic testTag)
echo "Toggling Peace Station to trigger TPTA vs HAFAA collision..."
automator tap --res-id "posting_peace_tile" --scrolls 2 || automator tap --contains "Peace (Pune" --scrolls 2
sleep 1
capture_step "06" "collision_hazard_detected"

# Step 8: Tap 1-Tap PCDA(O) Redressal Kit (Dynamic testTag)
echo "Triggering 1-Tap PCDA(O) Redressal Kit..."
automator tap --res-id "redressal_kit_button" --scrolls 3 || automator tap --contains "Redressal Kit" --scrolls 3
sleep 1
capture_step "07" "redressal_modal_open"

# Step 9: Verify formal representation letter draft preview
echo "Verifying formal letter preview..."
automator wait-for --contains "Representation" --timeout 5 || sleep 1
capture_step "08" "official_representation_draft"

# Clean return to cockpit
adb -s "${DEVICE_ID}" shell input keyevent 4
sleep 1
adb -s "${DEVICE_ID}" shell input keyevent 4
sleep 1

echo "=========================================================="
echo "  Verification Complete! All 8 screenshots captured:     "
echo "  Location: ${OUTPUT_DIR}"
echo "=========================================================="
ls -la "${OUTPUT_DIR}"
