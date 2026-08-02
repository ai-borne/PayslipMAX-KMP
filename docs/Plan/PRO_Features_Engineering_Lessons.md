# PRO Features Engineering Retrospective & Development Playbook

## Executive Summary
This document captures key engineering lessons, domain pitfalls, and architectural standards established during the **Tax Optimization Planner** sprint. These lessons serve as a mandatory blueprint for upgrading other PRO features: **DSOP Simulator**, **Retirement Planning**, **Posting Planning**, and **Claim Generator**.

---

## 1. Domain Rule Engine Architecture & Knowledge Bases

| Issue Encountered in Tax Planner | Root Cause | Standard for Other PRO Features |
| :--- | :--- | :--- |
| Stale ₹50,000 Standard Deduction used for New Tax Regime instead of Finance Act 2024 ₹75,000. | Hardcoded deduction values in calculation engine. | **Rule Pack Isolation**: All rates, thresholds, and statutory limits must live in centralized, versioned Knowledge Bases (e.g., `PensionRuleKnowledgeBase`, `DsopRuleKnowledgeBase`). |
| UI footer displayed `FY 2025-26` while title resolved `April 2026` to `FY 2026-27`. | Inconsistent FY/AY string derivation across UI components. | **Single Source Resolver**: Create a single resolver class per domain (e.g., `TaxYearResolver`, `RetirementDateResolver`) to enforce SSOT for temporal rules. |

---

## 2. Calculation Precision & Dynamic Benchmarks

| Issue Encountered in Tax Planner | Root Cause | Standard for Other PRO Features |
| :--- | :--- | :--- |
| Hardcoded `8.2%` "Optimal Effective Rate" displayed for a ₹36L+ officer (where true minimum is ~21.5%). | Static benchmark rate fallback in narrative engine. | **Zero Static Benchmarks**: Benchmarks must be dynamically derived per user profile. E.g., DSOP Simulator optimal growth must evaluate individual rank, pay level, and voluntary contribution headroom. |
| NPS Opportunity showed `₹15,000` savings without checking active winner regime context. | Isolated opportunity calculation ignoring regime switch impact. | **Holistic Opportunity Analysis**: Opportunity cards must compute net tax/corpus impact *relative to the active best baseline*, not in isolation. |

---

## 3. UI/UX & Compose Layout Rules

| Issue Encountered in Tax Planner | Root Cause | Standard for Other PRO Features |
| :--- | :--- | :--- |
| Badge chip `Preliminary estimate · 1 of 12` distorted vertically into a single letter column. | Jetpack Compose `Row` title `Text` missing `Modifier.weight(1f, fill = false)`. | **Flex Container Hygiene**: In horizontal `Row` headers containing badges or action icons, always apply `Modifier.weight(1f, fill = false)` to the primary label `Text` to prevent badge compression. |
| YTD TDS and Remaining Tax text labels collided horizontally. | Side-by-side single-line text layout in constrained width. | **Vertical Stacking for Financial Pairs**: Financial metrics (YTD vs Remaining, Current vs Projected) must be stacked vertically inside `Column` layouts on mobile viewports. |

---

## 4. Data Transparency & Projection Confidence

| Pattern Standardized | Implementation Detail | Target Application |
| :--- | :--- | :--- |
| **Confidence Badges** | Render data completeness indicators: `🟢 High Confidence (12/12 mos)`, `🟡 Moderate Confidence (6-11 mos)`, `🔴 Preliminary Estimate (1-5 mos)`. | **Retirement Planning & DSOP Simulator**: Indicate whether pension/corpus estimates are based on full service history or partial payslips. |
| **Spike / Anomaly Alerts** | Trigger visual warning cards when remaining period deductions jump significantly (e.g., TDS runway jump warning). | **Claim Generator & Posting Planning**: Alert officers when field allowance drops or recovery risks emerge after posting transfers. |

---

## 5. Architectural Cleanliness & Quality Assurance

1. **Strict 300-Line Limit**: No source file may exceed 300 lines of code. Split monolithic screens/engines into focused sub-components (`TaxPlannerResultBuilder`, `TaxTdsRunwayProgressCard`, `TaxNarrativeBenchmarkCard`).
2. **Test-Driven Development (TDD)**: Every calculation engine fix must begin with unit test coverage (`DualRegimeEngineAuditTest`, `WealthOptimizationEngineAuditTest`, `TaxYearResolverTest`). 100% of unit tests must pass before git commits.
3. **Unified Result Builders**: UI screens must consume a single read-only schema (e.g., `TaxPlannerResult`) produced by an orchestrator builder (`TaxPlannerResultBuilder`), eliminating ad-hoc UI-layer math.

---

## 6. Implementation Checklist for Upcoming PRO Features

- [ ] **DSOP Simulator**:
  - [ ] Implement `DsopRuleKnowledgeBase` for statutory interest rates (7.1%).
  - [ ] Add dynamic compounding projections based on actual closing balance and voluntary increments.
  - [ ] Add data confidence badge based on parsed payslip ledger history.
- [ ] **Retirement Planning**:
  - [ ] Create `RetirementYearResolver` for qualifying service and age calculations.
  - [ ] Implement dynamic pension, gratuity, commutation (50%), and leave encashment calculations.
  - [ ] Ensure Compose `Row` title texts carry `Modifier.weight(1f, fill = false)`.
- [ ] **Claim Generator**:
  - [ ] Isolate allowance dispute rules in `RepresentationRuleEngine`.
  - [ ] Enforce strict 300-line limits across draft template builders.
- [ ] **Posting Planning**:
  - [ ] Map field area / high altitude / peace area allowance transition logic.
  - [ ] Calculate net tax impact of field allowance exemptions under Sec 10(14).
