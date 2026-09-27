# Pay Audit — Phase Plan

Status: agreed 2026-09-27. Branch: `feature/pay-audit` (from `release/ios-1.0.0-v6` @ `74723452`).
Replaces the "PayslipMax AI / PCDA(O) Intelligence Engine" 8-phase draft (Antigravity `implementation_plan.md`).

## Vision

"Every rupee of your pay, checked against the rules — offline."

The app rebuilds the officer's service timeline from their stored payslips, then checks every month
against PCDA(O) rules. Zero user input. A finding is shown only when the payslips prove it, and it
cites a verified authority.

## Decisions

| Topic | Decision |
|---|---|
| Pricing | Part of existing Premium, under the existing `FeatureGate.ANOMALY_DETECTION` gate. No new FeatureGate. |
| Scope | Army officers under PCDA(O) only. |
| Rates | No hardcoded DA % timeline. The DA rate applied is derived from each payslip: DA ÷ (Basic + MSP). Only structural rules are coded (1 Jan / 1 Jul effective dates, arrears formula, 50%-DA escalation), plus a short list of one-off events (e.g. the Jan 2020–Jun 2021 DA freeze). The pay matrix changes once per Pay Commission. |
| Architecture | Extend the existing `DeterministicIntelligenceEngine` / `RuleAuditor` framework in `shared/.../insights/`. No parallel `pcdao/` engine. |
| Rules data | `scripts/pcdao_factory/` stays an offline authoring reference. Only the pay matrix is ported into `shared`. The 378-rule JSON does not ship. |
| Naming | "Pay Audit", not "AI". It is deterministic rule logic. |

## Principles

1. Zero input first. A check that needs the user to tap or type something is deferred.
2. Precision over coverage. One false "you are owed ₹X" costs more trust than ten missed findings.
3. Every rupee shown as due is computed from the payslips, never from assumptions.
4. Offline, with no PII leaving the device (unchanged).

## Evidence behind the approach (corpus, one de-identified officer, 2014–2026)

- From the numbers alone, the history reconstructs: Level 11 with DNI in July, promotion to Level 12A
  in Oct 2019, DNI in January afterwards, three R&H posting spans, higher-rate-city TPTA in 2020–21,
  and quarters occupancy (licence fee).
- DA arrears on all ten 7th CPC-era DA rises are explained by structural rules alone. Four of them
  include TPTA-DA arrears merged into `arrearsDa` (pre-2024 payslips).

## Phases

Each phase ends with a green build and 100% passing tests, followed by a Phase Summary per CLAUDE.md.

### Phase 0 — Fix the false DA arrears alarm (live bug)

`DaArrearsAuditor` feeds `SALARY_LOSS`, which is in `REPRESENTATION_DRAFT_TYPES`, so a false mismatch
offers the user a complaint letter. Known defects:

- It always assumes 3 arrears months. Mar 2024 (arrears for Jan–Feb) is reported as "underpaid ₹6,240".
- When the rate did not rise, it assumes a 2% rise.
- It ignores TPTA-DA arrears merged into `arrearsDa` on pre-2024 payslips.
- It applies 7th CPC math to 6th CPC-era payslips.
- It reports over-payments as `SALARY_LOSS`.

Fix:

- Arrears months = the months from the effective date (1 Jan or 1 Jul) up to the month before the payslip.
- No rate rise means no audit.
- Rates are compared as whole percentages.
- Accept DA-only or DA + merged TPTA-DA.
- Skip payslips before 7th CPC pay was credited.
- Flag only under-payment.

Gate: unit tests for each defect, plus a corpus test showing zero false `SALARY_LOSS` from this auditor.

### Phase 1 — Service timeline

Payslip history becomes a month-by-month `ServiceTimeline` containing:

- level and stage
- DNI and promotions
- DA % applied
- posting periods (R&H / field)
- TPTA city class
- quarters occupancy

The pay matrix is ported into `shared` as a Kotlin object (SSOT). Months with `needsReview` or zero
basic pay are excluded.

Gate: on the corpus, the timeline reconstructs the known events listed above.

### Phase 2 — Rebuild existing auditors on the timeline

- **TPTA:** skip months the timeline explains; compute the rate × DA.
- **Missing allowance:** explain drops caused by posting changes.
- **New auditors:** increment on DNI, and MSP.
- **Evidence fields:** extend `Anomaly` with expected, actual and authority.

Gate: a precision run on the corpus. Every remaining finding is genuine (user-confirmed) or removed.

### Validation checkpoint

Run on 3–5 real officers' payslips, on their own devices. Continue to UI only if it finds real errors
without false alarms.

### Phase 3 — Explain every change

Every month-to-month pay-line change gets a reason (e.g. "DA 55→58%, arrears for Jul–Sep").
Gate: coverage (% of changes explained) on the corpus.

### Phase 4 — Pay Audit screen

A timeline and findings inside Insights, under `ANOMALY_DETECTION`. Free tier: timeline and finding
count. Premium: details.

### Phase 5 — Letter from proven findings

Only proven findings feed the existing `RepresentationScreen`.

### Phase 6 — Predict

- next increment date and amount
- DSOP room left under the ₹5L cap
- the pay-fixation option calculator, with the Option 2 next-increment date fixed to 12 months after
  re-fixation (verify against the Army Officers Pay Rules 2017; test cases from the official worked
  example)

## Deferred / dropped

- **Deferred until users ask:** situational tiles / 3-tier matrix, claim/LTC/TA rules (~300 of the 378), deadline trackers.
- **Dropped:** the "unclaimed ₹" counter, personas, the 18% penal-interest hazard (no verified authority), the prototype HTML.
