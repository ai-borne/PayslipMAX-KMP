# PCDA(O) & Military Financial Intelligence Master Codex

> **Single Source of Truth (SSOT)** for canonical Ministry of Defence (MoD) circulars,
> PCDA(O) Pune official orders, and pay regulations governing the PayslipMax AI moat.
>
> *Maintained autonomously by GitHub CI & `scripts/pcdao_factory/pcdao_watchdog.py`.*

---

## 1. Intelligence Summary & Status

- **Codex Version**: 1.0.0
- **Total Ingested Baseline Orders**: 5 Core Authority Packages (378 Canonical Rules)
- **Last Watchdog Sync**: 2026-09-28
- **Primary Authorities**: PCDA(O) Pune, MoD Dept of Military Affairs, DoPT, Controller General of Defence Accounts (CGDA)

---

## 2. Chronological Circular & Knowledge Registry

### [MOD-DA-54-HIKE-2026-09] Grant of Dearness Allowance to Armed Forces Officers at Revised Rate of 54%
- **Order Number**: `1(2)/2026/D(Pay/Services)` | **Date**: 2026-09-29
- **Authority**: Ministry of Defence / PCDA(O) Pune
- **Category**: `ALLOWANCE_REVISION`
- **Source Link**: [https://pcdaopune.gov.in/orders/da_revision_54.pdf](https://pcdaopune.gov.in/orders/da_revision_54.pdf)
- **Key Provisions**:
  - Central DA enhanced from 50% to 54% with retrospective effect.
  - Triggers recalculation across all active military pay calculations.
  - Higher Rate Cities Transport Allowance: ₹7,200 + 54% DA = ₹11,088/mo.
  - Other Cities Transport Allowance: ₹3,600 + 54% DA = ₹5,544/mo.
- **Codebase Impact**: Updates SituationalRuleResolver.kt DA rate engine and 1-tap redressal math.

---

### [MOD-DA-50-ESCALATION-2024] Central DA Reaching 50% Statutory Escalation
- **Order Number**: `1(1)/2024/D(Pay/Services)` | **Date**: 15 March 2024
- **Authority**: Ministry of Personnel, Public Grievances and Pensions (DoPT) & MoD
- **Category**: `ALLOWANCE_REVISION`
- **Key Provisions**:
  - Children Education Allowance (CEA) hiked from ₹2,250/mo to **₹2,812.50/mo (₹33,750/yr)**.
  - Hostel Subsidy hiked from ₹6,750/mo to **₹8,437.50/mo (₹1,01,250/yr)**.
  - Dress Allowance hiked from ₹20,000/yr to **₹25,000/yr**.
  - House Rent Allowance (HRA) escalated to **30% / 20% / 10%** for X/Y/Z class cities.
- **Codebase Impact**: Handled in `SituationalRuleResolver.kt` line-item calculations.

---

### [MOD-TR-230B-HAFAA-TPTA] TPTA Mutual Exclusion with Field Deployments
- **Order Number**: `12630/Tpt.A/Mov C/246/D(Mov)/17` | **Date**: 15 September 2017
- **Authority**: MoD / Travel Regulations Rule TR-230(B)
- **Category**: `RECOVERY_HAZARD`
- **Key Provisions**:
  - Officers serving in Field, Modified Field, or High Altitude areas (HAFAA/CFAA/CMFAA) where government conveyance is provided cannot draw Transport Allowance concurrently.
  - Concurrent claims trigger debit of entire drawn TPTA with **18% penal interest**.
- **Codebase Impact**: Handled in `AllowanceCollisionAuditor.kt` emitting critical hazard alarms.

---

### [ARMY-RULE-10-11-2017] Promotion Pay Fixation & DNI Cycles
- **Order Number**: `Army Officers Pay Rules 2017 (Rule 10 & 11)` | **Date**: 03 May 2017
- **Authority**: Ministry of Defence (Department of Military Affairs)
- **Category**: `PAY_FIXATION`
- **Key Provisions**:
  - Officers promoted have 30 statutory days from Part II Order casualty to elect Option 1 (Promotion Date) or Option 2 (Date of Next Increment - DNI).
  - Qualifying service requires minimum 6 months before 1 January or 1 July increment cycle.
- **Codebase Impact**: Handled in `PayFixationOptimizer.kt` simulating 36-month trajectory.

---

### [MOD-1-16-2017-RH] Risk and Hardship Matrix Implementation
- **Order Number**: `1(16)/2017/D(Pay/Services)` | **Date**: 18 September 2017
- **Authority**: Ministry of Defence
- **Category**: `ALLOWANCE_REVISION`
- **Key Provisions**:
  - Established 9-cell Risk & Hardship Matrix (RH-MAX, R1H1 through R3H3).
  - Siachen Glacier rate set at ₹42,500/mo.
  - Highly Active Field Area (HAFAA) set at ₹16,900/mo.
- **Codebase Impact**: Handled in `risk_hardship_rates.json` and `SituationalRuleResolver.kt`.

---

### [MOD-1-25-2017-TPTA] Transport Allowance Slabs & Rates
- **Order Number**: `1(25)/2017/D(Pay/Services)` | **Date**: 07 July 2017
- **Authority**: Ministry of Defence
- **Category**: `ALLOWANCE_REVISION`
- **Key Provisions**:
  - Higher Rate Cities (20 urban agglomerations): ₹7,200/mo + DA.
  - Other locations: ₹3,600/mo + DA.
  - Divyang officers entitled to double rate (2.0x).
- **Codebase Impact**: Handled in `transport_allowance_rates.json` and `PcdaoAuditViewModel.kt`.
