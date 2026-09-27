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

### Phase 0 — Fix the false DA arrears alarm (live bug) — DONE

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

### Phase 1 — Service timeline — DONE (2026-09-27; open items in Phase 7)

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

Delivered in `shared/.../insights/timeline/`: `PayMatrix` (SSOT; `PayMatrixTest` fails if it drifts from
`scripts/pcdao_factory/output/pay_matrix_7th_cpc.json`), `LevelResolver`, `ServiceTimelineBuilder`,
`ServiceTimeline` models. Also excluded: basic pay that is not a 7th CPC matrix cell (pre-Jun 2017 pay,
arrears-inflated months). A basic shared by two levels with no history to settle it is left unplaced
(level/stage null) rather than guessed.

### Phase 2 — Rebuild existing auditors on the timeline — DONE (2026-09-27; open items in Phase 7)

- **TPTA:** skip months the timeline explains; compute the rate × DA.
- **Missing allowance:** explain drops caused by posting changes.
- **New auditors:** increment on DNI, and MSP.
- **Evidence fields:** extend `Anomaly` with expected, actual and authority.

Gate: a precision run on the corpus. Every remaining finding is genuine (user-confirmed) or removed.

Delivered in `shared/.../insights/`:

- `TimelineAuditor` (an auditor that takes the `ServiceTimeline`; the engine builds it once per run) and
  `PayAuthorities` (SSOT of cited authorities, taken from `scripts/pcdao_factory/output/`).
- `Anomaly` gains `expected`, `actual`, `authority` (all optional). Filled by the TPTA, increment and MSP
  auditors and by `DaArrearsAuditor` (which has no verified authority yet, so it cites none).
- `TptaEntitlementAuditor`: flags absent TPTA for Levels 10-13A from Jul 2017, unless `TptaAbsenceExplainer`
  finds a posting change or a relocation (city class differs before/after the gap). Amount due = lowest
  rate (other-places base) x (1 + DA % read from the payslips), never overstated.
- `MissingAllowanceAuditor`: HRA drop explained by taking quarters; MSP drop explained by Level 14+; TPTA
  removed from it (it was reported twice, by both auditors; now only `TptaEntitlementAuditor` reports it).
- `IncrementAuditor` (new): a January/July increment with no promotion since falls due again 12 months
  later; flags a month where basic pay did not move to the next matrix cell.
- `MspAuditor` (new): flags MSP paid but below the flat 15,500 for Levels 10-13A.
- Both new auditors report under `SALARY_LOSS`, so they reuse the existing severity, prioritisation and
  representation-draft handling.

Gate result: over the corpus the four auditors raise zero findings (`PayAuditCorpusPrecisionTest`). The old
TPTA rule fired on four genuine transition months of this officer (Dec 2019, Apr, May 2022 and Sep 2024
had no TPTA). Recall is covered by synthetic tests only; the corpus officer was paid correctly.

### Validation checkpoint

Run on 3–5 real officers' payslips, on their own devices. Continue to UI only if it finds real errors
without false alarms.

### Phase 3 — Explain every change — DONE (2026-09-27; open items in Phase 7)

Every month-to-month pay-line change gets a reason (e.g. "DA 55→58%, arrears for Jul–Sep").
Gate: coverage (% of changes explained) on the corpus.

Delivered in `shared/.../insights/timeline/`: `ChangeExplanation` (month, field, from, to, reason) and
`PayLineChangeExplainer`, which explains a move in the ten pay-line fields the timeline already models —
Basic Pay (DNI/promotion), DA and its arrears (rate revision, including "DA follows the pay-base rise" on
an increment/promotion month where the rate itself did not change), Transport Allowance (posting change,
relocation, or DA), HRA/licence fee (quarters taken/vacated), and Risk & Hardship/Field allowance (posting
spans). A field with no matching rule comes back with `reason = null` rather than a guess. Both months of
a transition must sit in the trusted `ServiceTimeline` (excluded: `needsReview`, zero or non-matrix basic
pay) or the whole transition is skipped, not guessed from untrustworthy data.

Gate result (`PayLineChangeExplanationCorpusTest`): 91 of 104 tracked month-to-month moves over the corpus
carry a reason (87.5%). The 13 gaps are pinned by month, not just counted, and are all the same field:
licence-fee amount moving while quarters stay occupied (the accommodation/rent bracket is not modeled —
see Phase 7).

### Phase 4 — Pay Audit screen — DONE (2026-09-27; open items in Phase 7)

A dedicated `PayAuditScreen`, entered via a free-visible `PayAuditEntryCard` added to the Insights tab's
primary items (`InsightsBodySections.kt`), gated by `ANOMALY_DETECTION` only at the findings level — the
timeline and finding count stay free, per the phase's own split.

Delivered:

- `EngineResult` (`shared/.../insights/DeterministicIntelligenceEngine.kt`) now exposes the
  `ServiceTimeline` and current-month `List<ChangeExplanation>` it already built internally each run
  (Phase 2's "the engine builds it once per run" made real for a caller) — both `@Transient`, since
  `EngineResult` is declared `@Serializable` but never actually encoded/decoded.
- `PayAuditFindingTypes` (shared, SSOT): the five Pay-Audit-auditor types (`MISSING_ALLOWANCE`,
  `TPTA_ENTITLEMENT`, `ARREARS_AUDIT`, `INCREMENT_MISSED`, `MSP_SHORTFALL`), as distinct from the other
  PRO auditors (DSOP/tax/quarters/debit) that are not part of this screen.
- `partitionPayAuditFindings` (composeApp): the free/premium split, mirroring
  `partitionAdvancedAnomalies` — locked shows count + category labels only, unlocked shows full findings
  including, for the first time anywhere in the app, the `expected`/`actual`/`authority` evidence Phase 2
  attached to `Anomaly` (closing half of the Phase 7 "Evidence is not stored or shown" gap — see below).
- `PayAuditScreen` + `PayAuditFindingsSection`/`PayAuditTimelineSection`/`PayAuditEntryCard` (composeApp):
  findings (gated), "What changed this month" (the current payslip's own `ChangeExplanation`s, reason
  != null only — an unexplained move stays a silent gap, not a "no reason" row), and the full Service
  Timeline (all months, newest first) — the latter two always free. `Screen.PayAudit` added to the nav
  enum and both platforms' detail dispatch (`App.kt` `DetailContent`, iOS `MainViewController.kt`).
- `PayAuditStrings` (new, `ui/theme/`) — `AppStrings.kt` was already at 295/300 lines.

Gate result: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, `iosSimulatorArm64Test`, and
`linkDebugFrameworkIosSimulatorArm64` all green; `ktlintCheck` and the tech-debt/file-size audit clean on
every touched/new file. New tests: `DeterministicIntelligenceEngineTest` (EngineResult exposes a
non-empty timeline and an increment-explained `basicPay` change), `PayAuditFindingsLogicTest` (mirrors
`AdvancedAnomaliesLogicTest`'s locked/unlocked cases, scoped to the five Pay Audit types). Also updated
`AppNavStateTest`'s exhaustive tab-root/detail-screen partition to include `Screen.PayAudit` — the guard
did its job and caught the new case.

### Phase 5 — Letter from proven findings

Only proven findings feed the existing `RepresentationScreen`.

### Phase 6 — Predict

- next increment date and amount
- DSOP room left under the ₹5L cap
- the pay-fixation option calculator, with the Option 2 next-increment date fixed to 12 months after
  re-fixation (verify against the Army Officers Pay Rules 2017; test cases from the official worked
  example)

### Phase 7 — Carry-overs from earlier phases

Everything Phases 0–1 left unproven or unbuilt, kept in one place (CLAUDE.md "fail loud").

- **Real-data validation of the timeline (Phase 1).** The gate ran on one officer's corpus. Never
  exercised on real data: unplaced months from a shared matrix cell (10 vs 10B, 11 vs 12A), FIELD
  posting spans (the corpus has no field allowance), and a Level 12A officer who starts mid-history.
  Fold into the Validation checkpoint: report unplaced months and span counts per officer.
- **TPTA city class for Jun–Sep 2017 (Phase 1).** Those payslips still print the pre-7th CPC TPTA (₹3,712),
  so the class is null. Correct as is; revisit only if a real officer needs those months classified.
- **Officer scope of the matrix (Phase 1).** Only regular-Army Levels 10–18 are ported. MNS and NCC
  matrices are out of scope; decide whether to add them only if such users appear.
- **Timeline has no consumer yet (Phase 1).** Deliberate: Phase 2 wires it into the auditors.
- **Other findings/notes from Phase 0.** None outstanding; all five defects are fixed and tested.

Carried over from Phase 2 (nothing here is claimed done):

- **The app's stored history is too thin for the new explanations.** The engine is fed `LedgerRecordEntity`
  rows, which keep no Risk & Hardship / field allowance, licence fee, arrears, adjustment or `needsReview`
  data. In the app, so: the posting-change and quarters explanations never fire, the arrears/adjustment
  exemptions in the TPTA auditor never fire, and no posting spans exist. The same gap means
  `DaArrearsAuditor` never sees `arrearsDa` in production (pre-existing). Fix: feed the engine full
  `ParsedPayslip` history or extend the ledger table (Room v11 to v12 needs a migration). Blocks the
  Validation checkpoint.
- **Findings are decided at import time.** A TPTA-free month is explained by the months around it, which
  may not be imported yet. Audited with earlier months only, the corpus's Dec 2019, May 2022 and Sep 2024 are
  flagged (pinned in `PayAuditCorpusPrecisionTest`). Needs a re-audit that retracts a finding once later
  payslips explain it, or a rule that holds a finding until the next month exists. Same for an increment
  paid late with arrears the following month.
- **Evidence is not stored or shown.** `expected`/`actual`/`authority` live on `Anomaly`, but
  `FinancialInsightEntity` keeps only the description text and the UI shows nothing more. Phase 4/5.
- **Not user-confirmed.** The gate is zero false findings on one officer. "Genuine" findings need real
  officers (Validation checkpoint).
- **Out of scope for the increment/MSP/TPTA rules:** Level 14+ (different TPTA slab, official-car option,
  no MSP); the first increment after a promotion (date rules INCREMENT_004, six-month rule PROMO_FIX_004);
  leave/suspension months that switch TPTA off (TA_TRANSPORT_002/003, not visible in payslips).
- **Tier — resolved.** `IncrementAuditor`/`MspAuditor` now emit their own types (`INCREMENT_MISSED`,
  `MSP_SHORTFALL`), classified PRO in `AnomalyTierMap` — they no longer piggyback on `SALARY_LOSS`'s FREE
  tier. Wired through the category/title maps, `InsightPrioritizationEngine`, `AnomalySeverityMapper`,
  `AdvancedAnomaliesLogic`'s labels (new `InsightsStrings` entries) and both modules' representation-draft
  trigger lists (`FinancialIntelligenceRepository` and composeApp's `REPRESENTATION_DRAFT_TYPES` — the two
  are hand-kept in sync; `shared` cannot depend on `composeApp` for a single SSOT).
- **Authority check — resolved, and it was wrong.** Verified the MSP letter (No. 1(16)/2017/D(Pay/Services),
  18-09-2017) against public MoD circulars: that letter number is dated 16-11-2017 and covers Extra Work
  Allowance / abolition of Flight Charge Certificate Allowance, not MSP — the pcdao_factory reference had
  copied the flying-allowance citation onto the MSP record. `PayAuthorities.MILITARY_SERVICE_PAY` now cites
  the pay matrix's own source (Army Officers Pay Rules 2017; Handbook pp. 88-93) instead, until a correct
  implementing letter is verified. The `scripts/pcdao_factory/output/` JSON itself is left uncorrected
  (out of scope — it is an authoring reference, not shipped) but carries the same error for
  `flying_allowance`/`special_forces_allowance` if those are ever ported.
- **Other auditors.** `SalaryLossAuditor` (net-pay drop) and the rest were not rebuilt on the timeline; not
  part of Phase 2's list.

Carried over from Phase 3 (nothing here is claimed done):

- **Licence-fee rate is not modeled.** The timeline tracks quarters occupancy as a start/stop toggle only;
  13 of 104 tracked corpus changes are the fee amount moving while quarters stay occupied (a different
  accommodation/rent bracket). Pinned in `PayLineChangeExplanationCorpusTest`, not fixed — no accommodation
  type or rent-bracket data is ported, and none is planned unless a real officer needs it explained.
- **Only ten pay-line fields are explained.** Chosen because they are exactly what the `ServiceTimeline`
  already models (Phases 1-2): Basic Pay, DA, DA/TPTA-DA arrears, MSP, Transport Allowance, HRA, licence
  fee, Risk & Hardship and Field allowance. The other ~30 `Earnings`/`Deductions` fields (income tax, DSOP
  subscription, CEA, NPA, dress/ration/technical allowance, non-DA arrears, all `adj*` corrections) have no
  structural rule to explain a move and are not tracked at all — a changed value in one of them produces no
  `ChangeExplanation` entry, not an unexplained one, so it stays invisible to the coverage gate. Extend the
  tracked set only when a real officer's payslip needs one of these explained.
- **A transition following an untrusted month is skipped, not attempted.** If the immediately preceding
  stored payslip is excluded from the timeline (`needsReview`, zero or non-matrix basic pay — e.g. the
  corpus's Feb-Mar 2022 gap), `PayLineChangeExplainer` returns nothing for that month rather than comparing
  against the last trustworthy month further back. Same underlying data gap as Phase 1's exclusions; not
  reported as "unexplained" because there is no reliable "from" value to explain a move against.
- **`PayLineChangeExplainer` had no consumer — resolved in Phase 4.** `PayAuditScreen`'s "What changed this
  month" section now calls it (via `EngineResult.changeExplanations`) for the currently selected payslip's
  own transition. Not fully wired: it shows only that one month's changes, not every transition across the
  whole displayed timeline (see Phase 4 carry-over below).
- **TPTA arrears not linked to a DA rise (`arrearsTpta`) are not tracked.** Only DA-linked TPTA-DA arrears
  (`arrearsTptaDa`) are explained, mirroring `DaArrearsAuditor`'s scope; a posting-change back-payment of
  base TPTA itself has no rule and is out of scope until a real officer shows one.

Carried over from Phase 4 (nothing here is claimed done):

- **"What changed this month" covers one transition, not the whole timeline.** `PayAuditScreen` calls
  `PayLineChangeExplainer` only for the currently selected payslip vs. its immediate predecessor (via
  `EngineResult.changeExplanations`), matching how anomalies are already computed per-current-payslip. It
  does not show a change row for every month-to-month transition across the full Service Timeline list
  below it — a user has to step through payslips to see each month's changes. No corpus/history-wide
  "explain every transition at once" view exists yet; build one only if a real officer needs it.
- **The screen inherits the same thin-ledger gap as Insights (Phase 2 carry-over).** `PayAuditScreen` reads
  `viewModel.ledgerRecords` (`LedgerRecordEntity`), which still keeps no Risk & Hardship/field allowance,
  licence fee, arrears, or `needsReview` data in the running app. So on-device, posting/quarters
  explanations and the arrears exemptions in `TptaEntitlementAuditor` still won't fire against real stored
  data — the screen is correct on the corpus (full `ParsedPayslip`) but will show a thinner timeline and
  fewer findings than the corpus tests suggest until the ledger schema gap is fixed. Same underlying issue,
  not a new one; still blocks the Validation checkpoint.
- **Evidence display is only half-closed.** `expected`/`actual`/`authority` now render on the Pay Audit
  screen's own finding rows, but the older `AdvancedAnomaliesCard` on the Insights tab (which shows the
  same PRO anomalies, including the Pay Audit ones, in its own "Advanced Anomaly Checks" card) still shows
  `description` text only. Two surfaces for overlapping data with inconsistent detail; not reconciled here
  — left for Phase 5 (representation drafts already read `description`) or a later Insights cleanup.
- **No Compose UI test exercises `PayAuditScreen`/`PayAuditTimelineSection`/`PayAuditFindingsSection`.**
  Matches the existing convention (no Insights-tab composable has a UI test either — only the pure logic
  functions do), so not a new gap, but recorded here per CLAUDE.md's fail-loud rule: only
  `PayAuditFindingsLogicTest` (pure partition function) and the `DeterministicIntelligenceEngine` field
  exposure test were added; the screen itself was verified only by a green compile + `check_tech_debt_limits`,
  not by running it on a device or simulator.
- **The Service Timeline list has no pagination or collapsing.** All months render as a flat list, newest
  first, with no cap — fine for the single de-identified corpus officer's history, untested for a real
  officer with a much longer service record. Deferred until real usage shows it is a problem, consistent
  with "precision over coverage" elsewhere in this plan.

## Deferred / dropped

- **Deferred until users ask:** situational tiles / 3-tier matrix, claim/LTC/TA rules (~300 of the 378), deadline trackers.
- **Dropped:** the "unclaimed ₹" counter, personas, the 18% penal-interest hazard (no verified authority), the prototype HTML.
