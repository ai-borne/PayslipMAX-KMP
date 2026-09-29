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

# 6. Interactive Traversal on Pixel 9 (1080x2424)
echo "Executing automated visual UI flow..."

# Step 1: Dashboard
capture_step "01" "dashboard"

# Step 2: Navigate to Insights tab (x=625, y=2250)
adb -s "${DEVICE_ID}" shell input tap 625 2250
sleep 1
adb -s "${DEVICE_ID}" shell input swipe 540 1800 540 1000 300
sleep 1
capture_step "02" "insights_screen"

# Step 3: Open PayslipMax AI card (x=770, y=1900)
adb -s "${DEVICE_ID}" shell input tap 770 1900
sleep 2
capture_step "03" "pcdao_cockpit"

# Step 4: Scroll down to Situational Matrix
adb -s "${DEVICE_ID}" shell input swipe 540 1800 540 600 300
sleep 1
capture_step "04" "situational_matrix"

# Step 5: Open "+ Add Specialized Factor" bottom sheet (x=700, y=1620)
adb -s "${DEVICE_ID}" shell input tap 700 1620
sleep 1
capture_step "05" "specialized_factors_sheet"

# Step 6: Select MARCOS (x=765, y=1030) and tap Apply (x=540, y=2280)
adb -s "${DEVICE_ID}" shell input tap 765 1030
sleep 1
adb -s "${DEVICE_ID}" shell input tap 540 2280
sleep 1

# Step 7: Toggle Peace UA tile (x=400, y=980) to trigger collision with HAFAA
adb -s "${DEVICE_ID}" shell input tap 400 980
sleep 1
adb -s "${DEVICE_ID}" shell input swipe 540 1800 540 800 300
sleep 1
capture_step "06" "collision_hazard_detected"

# Step 8: Tap 1-Tap PCDA(O) Redressal Kit (x=750, y=1780)
adb -s "${DEVICE_ID}" shell input tap 750 1780
sleep 1
capture_step "07" "claim_generator_list"

# Step 9: Open formal letter draft (x=300, y=770)
adb -s "${DEVICE_ID}" shell input tap 300 770
sleep 1
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
