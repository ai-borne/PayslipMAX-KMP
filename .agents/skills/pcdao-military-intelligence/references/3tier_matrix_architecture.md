# 3-Tier Situational Matrix Architecture & UI Guidelines

This document details the architectural design and mobile constraints of the 3-Tier Situational Matrix
powering the PayslipMax AI military intelligence engine.

---

## 1. Matrix Overview

The 3-tier architecture balances access to 378 canonical military rules without mobile visual clutter.

```
┌────────────────────────────────────────────────────────────────────────┐
│                        3-TIER RESOLVER UX PIPELINE                     │
├────────────────────────────────────────────────────────────────────────┤
│ Tier 1: Smart Auto-Inference (Zero User Effort)                       │
│ - Inspects ParsedPayslip fields (Basic Pay, Level, Field Allowances)   │
│ - Auto-activates matched tiles with [AUTO-DETECTED] badges             │
├────────────────────────────────────────────────────────────────────────┤
│ Tier 2: 5 Categorized Quick-Tap Tabs                                   │
│ - [POSTING] [HOUSING] [CHILDREN (CEA)] [PROMOTION] [FUNDS / LTC]       │
│ - Top 3-5 high-frequency military operational contexts per tab        │
├────────────────────────────────────────────────────────────────────────┤
│ Tier 3: Specialized Military Factors Bottom Sheet                      │
│ - Expandable modal via "+ Add Specialized Factor"                      │
│ - Rare operational & medical factors (MARCOS, Siachen, Gallantry, CTG) │
└────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Tier Breakdown & Tile Mappings

### Tier 1: Smart Auto-Inference (`SituationalAutoInferer.kt`)
- **Rank & Level**: Derived from Basic Pay + MSP comparison with `pay_matrix_7th_cpc.json`.
- **Field Posting**: Detected if `HAFAA`, `CFAA`, or `CMFAA` appears in earnings ledger.
- **Peace Posting**: Detected if `TPTA` appears in earnings.
- **DSOPF Pacing**: Flags high subscription pacing if monthly DSOPF > ₹41,666.

### Tier 2: Categorized Tabs (`SituationalTileMatrix.kt`)
1. **`POSTING`**:
   - `HAFAA Field Area` (₹16,900/mo)
   - `Peace (Pune / Higher UA)` (₹7,200 + DA)
   - `Peace (Other Locations)` (₹3,600 + DA)
   - `Siachen Glacier` (₹42,500 + 25%)
   - `North-East SDA` (10% of Basic Pay)
2. **`HOUSING`**:
   - `HRA (X-Class City)` (30% Basic Pay)
   - `HRA (Y-Class City)` (20% Basic Pay)
   - `Government Married Quarters` (triggers rent deduction check)
3. **`CHILDREN_CEA`**:
   - `1 Child CEA` (₹33,750/yr escalated)
   - `2 Children CEA` (₹67,500/yr escalated)
   - `Hostel Subsidy` (₹1,01,250/child/yr)
4. **`PROMOTION`**:
   - `Promoted in Past 36 Months` (activates `PayFixationCard.kt`)
   - `DNI Cycle 1 January` vs `DNI Cycle 1 July`
5. **`FUNDS_LTC`**:
   - `DSOP > ₹41,666/mo` (monitors ₹5L Sec 10(11) tax shield)
   - `Retirement within 1 Year` (Rule 14 3-month stoppage alarm)
   - `LTC Claim Pending` (30-day forfeiture tracker)

### Tier 3: Specialized Factors Bottom Sheet (`AddFactorBottomSheet.kt`)
- `MARCOS / Special Forces Airborne`: ₹25,000/mo (Army Rule 103).
- `Siachen Glacier RH-MAX`: ₹42,500/mo (MoD Order 1(16)/2017/D(Pay)).
- `Divyang Child (Double CEA)`: ₹67,500/yr (DoPT OM 14028/3/2014-Estt).
- `Divyang Officer (Double TPTA)`: Double transport allowance rate.
- `Gallantry Award`: Tax-exempt monthly allowance (Sec 10(18) ITA).
- `Composite Transfer Grant (CTG)`: 80% of Basic Pay on permanent posting.

---

## 3. Strict Codebase & Line-Count Constraints

To maintain modularity and strict compliance with `.agents/rules/boundaries.md`:

| Constraint | Limit | Implementation Strategy |
|---|---|---|
| **Max File Length** | **< 300 LOC** | Decompose complex UI into sub-composables (`ImpactCountersStrip.kt`, `SituationalTileMatrix.kt`, `PayFixationCard.kt`) |
| **Max Function Length** | **$\le$ 50 LOC** | Extract small helper functions and layout primitives |
| **Architecture** | **MVVM** | UI strictly observes StateFlow from `PcdaoAuditViewModel.kt` |
| **Data Flow** | **SSOT** | `ActiveSituationalContext` is the single source of truth for all calculations |
| **Zero Mock Personas** | **Production** | Month switcher binds strictly to real `ParsedPayslip` vault entries |
