#!/usr/bin/env bash
# ==============================================================================
# run_pcdao_audit_suite.sh — Complete Canonical Math & Regression Suite
# ==============================================================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
WORKSPACE_ROOT="$(cd "${SCRIPT_DIR}/../../../.." && pwd)"

echo "=========================================================="
echo "  PCDA(O) Military Moat — Canonical & Regression Audit    "
echo "=========================================================="
echo "Workspace: ${WORKSPACE_ROOT}"

# 1. Canonical Factory Validation (Python)
echo ""
echo "[1/5] Validating 9 Canonical JSON Data Packs..."
python3 "${WORKSPACE_ROOT}/scripts/pcdao_factory/validate_factory_output.py"

# 2. Scenario Simulation Verification (Python)
echo ""
echo "[2/5] Running Scenario Simulation Test Harness..."
python3 "${WORKSPACE_ROOT}/scripts/pcdao_factory/test_simulation_scenarios.py"

# 3. KMP Shared Module Tests (Kotlin)
echo ""
echo "[3/5] Running KMP Shared Unit Tests (Engines, Models, Rules)..."
(cd "${WORKSPACE_ROOT}" && ./gradlew :shared:testDebugUnitTest --daemon)

# 4. ComposeApp Module Tests (Kotlin)
echo ""
echo "[4/5] Running Compose UI Unit Tests (ViewModel, Screen State)..."
(cd "${WORKSPACE_ROOT}" && ./gradlew :composeApp:testDebugUnitTest --daemon)

# 5. Architecture File Size Guard (<300 LOC per file)
echo ""
echo "[5/5] Checking Architectural File Size Limits (<300 LOC)..."
(cd "${WORKSPACE_ROOT}" && ./gradlew checkFileSizes --daemon)

echo ""
echo "=========================================================="
echo "  All 5 Audit Layers Passed with 100% Deterministic Math! "
echo "=========================================================="
