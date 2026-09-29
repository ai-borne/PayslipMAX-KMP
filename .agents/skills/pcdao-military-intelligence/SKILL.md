---
name: pcdao-military-intelligence
description: >-
  Run, verify, audit, benchmark, or expand the PCDA(O) Military Financial Intelligence Engine
  and PayslipMax AI mobile cockpit. Use whenever testing canonical PCDA rules, pay fixation
  calculations (Rule 10/11), allowance collision hazard auditing (TPTA vs HAFAA), on-device
  Pixel 9 / ADB verification, or adding new military entitlement rules and MoD circulars.
---

# PCDA(O) Military Financial Intelligence Engine & PayslipMax AI

This skill provides automated runbooks, validation scripts, and engineering guidelines for the
core defensible moat of PayslipMax: the **PCDA(O) Military Financial Intelligence Engine**.

---

## 1. Fast-Path On-Device ADB Verification (Pixel 9)

To verify the engine on a connected Android device (Google Pixel 9) with automated UI traversal
and screenshot evidence, run:

```bash
bash .agents/skills/pcdao-military-intelligence/scripts/verify_device.sh [optional-output-dir]
```

### What this script automates:
1. **Device Pre-flight**: Detects connected ADB device, checks screen wake state, and unblocks keyguard.
2. **Safe Sideload**: Compiles and installs `:composeApp:installDebug` targeting `--user current` to prevent multi-profile Play Store collision.
3. **App Launch**: Starts `MainActivity` with clean task state.
4. **Interactive Traversal**:
   - Navigates to **Insights** tab and opens **PayslipMax AI** (5th tool card).
   - Toggles **Situational Matrix** tiles (triggers mutual exclusion hazard: TPTA + HAFAA).
   - Opens the **"+ Add Specialized Factor"** bottom sheet.
   - Triggers the **1-Tap PCDA(O) Redressal Kit** and loads the formal Pune representation draft.
5. **Visual Proof Artifacts**: Saves timestamped PNG screenshots of every state to `optional-output-dir` (or active artifact directory).

---

## 2. Fast-Path Canonical & Regression Test Suite

To verify the mathematical fidelity of all 378 canonical rules, 9 data packs, and Kotlin domain engines, run:

```bash
bash .agents/skills/pcdao-military-intelligence/scripts/run_pcdao_audit_suite.sh
```

### This script validates:
- **Canonical Data Factory**: `python3 scripts/pcdao_factory/validate_factory_output.py` (cell-by-cell verification of 7th CPC levels 10–18, 40 stages, IOR 2.67, Siachen ₹42.5k, HAFAA ₹16.9k).
- **Scenario Simulator**: `python3 scripts/pcdao_factory/test_simulation_scenarios.py` (Option 1 vs Option 2 36-month pay fixation +₹62,800 delta, collision auditor, DSOPF tax cap).
- **KMP Shared Unit Tests**: `./gradlew :shared:testDebugUnitTest`
- **Compose UI Unit Tests**: `./gradlew :composeApp:testDebugUnitTest`
- **Codebase Limits**: `./gradlew checkFileSizes` (enforcing <300 LOC per file, $\le$50 LOC per function).

---

## 3. Moat Expansion Playbook: Adding New Military Rules

When a new MoD Circular, DA Revision, or 8th CPC order is released:

### Step 1: Update Canonical Data Factory
1. Edit or add raw rules in `scripts/pcdao_factory/output/`.
2. Sync the updated JSON into Compose resources:
   `composeApp/src/commonMain/composeResources/files/pcdao/`
3. Add cell/rate assertions in `scripts/pcdao_factory/validate_factory_output.py`.

### Step 2: Update Python Simulation Scenario
1. Add a test case in `scripts/pcdao_factory/test_simulation_scenarios.py` with expected rupee deltas.
2. Run `python3 scripts/pcdao_factory/test_simulation_scenarios.py` to confirm deterministic outcome.

### Step 3: Implement in Kotlin KMP Engine
1. Update strongly-typed domain model in `shared/src/commonMain/kotlin/com/payslipmax/pcdao/model/`.
2. Implement deterministic math in `shared/src/commonMain/kotlin/com/payslipmax/pcdao/engine/` or `reconciliation/`.
3. If the rule involves mutual exclusion (e.g. new flying allowance restriction), add hazard logic to `AllowanceCollisionAuditor.kt`.
4. If it's a claimable entitlement, add MoD circular citation to `RedressalLetterGenerator.kt`.
5. Strictly adhere to boundaries: **No file > 300 LOC, no function > 50 LOC**.

### Step 4: Wire to UI & Verify
1. If the rule is an operational factor, add it to `SituationalContextModels.kt` and `AddFactorBottomSheet.kt`.
2. Run `bash .agents/skills/pcdao-military-intelligence/scripts/verify_device.sh` to capture visual evidence on Pixel 9.

---

## Detailed References

- [Canonical Rules Cheatsheet](./references/canonical_rules_cheatsheet.md): Index of 378 rules, 5 action types, and PCDA Pune task desks.
- [3-Tier Matrix Architecture](./references/3tier_matrix_architecture.md): Smart Auto-Inference, 5 Quick-Tap Tabs, and Specialized Factors Sheet.
