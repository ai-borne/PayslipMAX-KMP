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

### Phase 5 — Letter from proven findings — DONE (2026-09-27; open items in Phase 7)

Only proven findings feed the existing `RepresentationScreen`. "Proven" is the vision statement's own
wording (`payslips prove it, and it cites a verified authority`), made a literal instance-level check:
`expected`, `actual` and `authority` must all be non-null on the `Anomaly`. A type in
`REPRESENTATION_DRAFT_TYPES` whose emitted instance lacks any one of those three no longer drafts a
letter for that instance.

Delivered in `shared/.../insights/` and `shared/.../repository/`:

- `Anomaly.isProven()` (`RepresentationDraftTypes.kt`): the single predicate above.
- `FinancialIntelligenceRepository.processPayslipAndRunAnalysis` step 5 now gates draft generation on
  `anomaly.type in REPRESENTATION_DRAFT_TYPES && anomaly.isProven()`, not type membership alone.
- `RepresentationDraftGenerator.generateRepresentationDraft` now takes `expected`, `actual`, `authority`
  as required (non-nullable) parameters and renders them into the letter body (amount due / amount
  credited / shortfall, and the citing authority), closing the Phase 4 carry-over note that the letter
  never used the evidence fields Phase 2 attached to `Anomaly`.

Effect on the five Pay Audit types: `TPTA_ENTITLEMENT`, `INCREMENT_MISSED` and `MSP_SHORTFALL` always
carry full evidence, so they draft exactly as before. `SALARY_LOSS` (a bare heuristic with no evidence
fields at all) and `MISSING_ALLOWANCE` (has `expected`/`actual` but no cited authority yet) no longer
auto-draft a letter — flagged as carry-over below, not silently dropped.

Gate result: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, `linkDebugFrameworkIosSimulatorArm64`,
and `ktlintCheck` all green; tech-debt/file-size audit clean on every touched file. New tests:
`RepresentationDraftGeneratorTest` (letter cites expected/actual/authority) and
`FinancialIntelligenceRepositoryTest.testProcessPayslipDoesNotGenerateDraftForUnprovenSalaryLoss` (a
detected `SALARY_LOSS` anomaly produces zero representation drafts). Existing
`testProcessPayslipGeneratesRepresentationDraftAndInsightsForMissingTPTA` still passes unchanged (TPTA
always proven).

### Phase 6 — Predict — DONE (2026-09-27; open items in Phase 7)

Three predictions, delivered in `shared/.../insights/` and `shared/.../insights/timeline/`:

- **Next increment date and amount** (`NextIncrementPredictor`): zero-input, read off the `ServiceTimeline`
  alone. Anchored on the most recent INCREMENT or PROMOTION event — an increment recurs exactly 12 months
  later (it is already on a 1 Jan/1 Jul cycle date); a promotion's first increment in the new level follows
  6 months later, rounded up to the next cycle date. Returns null when the latest month is untrusted, there
  is no prior event to anchor on, or the officer is already at the top stage of their level.
- **DSOP room left under the ₹5L cap** (`DsopRoomCalculator`): sums `dsopSubscription` across the current
  financial year (1 Apr-31 Mar) from the stored history, deduped by month, against the same ₹500,000 Sec
  10(11) cap `DsopComplianceAuditor` already cites. Room left floors at zero — a subscription above the cap
  still credits to the DSOP fund, it simply stops being tax-exempt.
- **The pay-fixation option calculator** (`PayFixationCalculator`): Option 1 (fixed from date of promotion)
  vs Option 2 (fixed from the officer's next DNI in the lower level), per Rule 10 & 11 of the Army Officers
  Pay Rules 2017 (SRO 12(E), 03 May 2017). Ported from the worked reference already in
  `scripts/pcdao_factory/test_simulation_scenarios.py::calculate_pay_fixation_36mo`, with one correction:
  the reference rounds "N months later" up to the next 1 Jan/1 Jul cycle with a boundary bug (a date that
  falls exactly on a cycle month is rounded a full cycle too late); the new `PayMonth.nextIncrementCycle`
  helper fixes that. The ported worked example (Level 10 stage 8 to Level 11, promoted March, DNI July —
  Option 1 fixes at ₹71,500, Option 2 at ₹73,600) is unaffected by the bug either way, and is
  `PayFixationCalculatorTest`'s gate, alongside a regression test for the boundary fix itself.

A shared `PayMonth.plusMonths`/`nextIncrementCycle` helper (`IncrementCycle.kt`) backs both the predictor
and the calculator — the same "N months later, rounded to the officer's actual increment cycle" rule
appears in three places (`IncrementAuditor`, the predictor, and both calculator options), so it is SSOT,
not three copies.

`EngineResult` gains `incrementPrediction`/`dsopRoom` (both `@Transient`, same reasoning as Phase 4's
`timeline`/`changeExplanations`), computed once per engine run alongside everything else. `PayAuditScreen`
gains two new sections: "What's next" (`PayAuditPredictionsSection.kt`, free, zero-input — the next
increment and DSOP room cards) and the pay-fixation calculator (`PayAuditFixationCalculatorSection.kt`,
free) — the one Phase 6 deliverable that inherently needs a user input, because a future promotion cannot
be read off the timeline. `resolveFixationComparison` (`PayAuditFixationCalculatorLogic.kt`) resolves the
officer's current level/stage and DNI cycle month from the timeline automatically, so the calculator only
ever asks for the level being promoted to and the promotion month/year; it also refuses (returns null) a
target level that is not actually higher than the officer's current one, since Rule 10/11 only applies to
an upward move.

Gate result: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, `iosSimulatorArm64Test` (both
`shared` and `composeApp`), `linkDebugFrameworkIosSimulatorArm64`, `ktlintCheck`, and the tech-debt/file-size
audit all green on every touched/new file. New tests: `NextIncrementPredictorTest`,
`DsopRoomCalculatorTest`, `PayFixationCalculatorTest` (the worked-example gate above, plus the boundary-fix
regression and a top-stage/no-further-increment case), `PayAuditFixationCalculatorLogicTest`, and a
`DeterministicIntelligenceEngineTest` case asserting both new `EngineResult` fields are populated.

### Phase 7 — Carry-overs from earlier phases — DONE (2026-09-27)

Everything Phases 0–6 left unproven or unbuilt, kept in one place (CLAUDE.md "fail loud"). Consolidated
2026-09-27 into a single checklist, cleared one item at a time. `[x]` = done before this phase started
(dated); `[ ]` = open, tackled in list order unless a dependency forces reordering (noted inline).

Closed 2026-09-27 with 10 of 25 items resolved (P7-01–P7-10, each in its own commit, tests/ktlint/tech-debt
audit green on every touched file; full `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` green at
close). Per user decision, the remaining 15 items are not attempted in this phase — they are a different
shape of work (deliberately-deferred no-ops, work blocked on real users, and substantial new
features/architecture, not bug fixes) — and are carried into **Phase 8** below rather than left dangling
inside a phase marked done.

- [x] **P7-01 — Anomaly tier fix** (done 2026-09-27, commit `053938dd`, immediately after Phase 2, before
  Phase 3). `IncrementAuditor`/`MspAuditor` emit their own types (`INCREMENT_MISSED`, `MSP_SHORTFALL`),
  classified PRO in `AnomalyTierMap` — no longer piggyback on `SALARY_LOSS`'s FREE tier. Wired through
  category/title maps, `InsightPrioritizationEngine`, `AnomalySeverityMapper`, `AdvancedAnomaliesLogic`'s
  labels (`InsightsStrings`), and the representation-draft trigger list.
- [x] **P7-02 — MSP authority citation fix** (done 2026-09-27, commit `053938dd`). The MSP letter citation
  (No. 1(16)/2017/D(Pay/Services), 18-09-2017) was wrong — that letter number is dated 16-11-2017 and
  covers Extra Work Allowance / Flight Charge Certificate Allowance, not MSP.
  `PayAuthorities.MILITARY_SERVICE_PAY` now cites the Army Officers Pay Rules 2017 / Handbook pp. 88-93
  instead. `scripts/pcdao_factory/output/` JSON left uncorrected (authoring reference, not shipped).
- [x] **P7-03 — SSOT refactor for representation-draft types** (done 2026-09-27, commit `1fe03855`,
  follow-up to P7-01). `REPRESENTATION_DRAFT_TYPES` is a single `shared` constant, imported directly by
  composeApp — not two hand-synced lists as an earlier draft of this doc implied.
- [x] **P7-04 — Pay-fixation calculator: range-validate promotion year/month** (done 2026-09-27).
  `resolveFixationComparison` now rejects a promotion month that isn't strictly after the officer's latest
  trusted timeline month, or that's more than 30 years ahead (a full commissioned-officer career span) —
  closing the 1800/9999 `toIntOrNull()` gap. Month was already `coerceIn(1, 12)` in the UI; only the year
  was unbounded. New tests: `returnsNullWhenThePromotionMonthIsNotAfterTheLatestTimelineMonth`,
  `returnsNullWhenThePromotionYearIsImplausiblyFarInTheFuture`
  (`PayAuditFixationCalculatorLogicTest.kt`).
- [x] **P7-05 — Next-increment prediction vs. already-overdue** (done 2026-09-27). `NextIncrementPrediction`
  gains `isOverdue` (true when the predicted date is on or before the officer's latest trusted timeline
  month — the same boundary `IncrementAuditor` uses for `INCREMENT_MISSED`). `PayAuditPredictionsSection`
  relabels the card "Overdue since `<date>`" in the error color instead of "Due `<date>`" when true. New
  tests: `NextIncrementPredictorTest.flagsOverdueWhenTheDueDateIsWellBeforeTheLatestPayslip`, plus
  `isOverdue` assertions added to the two existing prediction tests (boundary-exact and genuinely-upcoming
  cases).
- [x] **P7-06 — DsopRoomCalculator dedup ordering** (done 2026-09-27). Now dedupes `listOf(current) +
  history` (current first) by `(year, monthNum)`, so `current`'s figure wins over a stale duplicate in
  history rather than whichever happened to be first in the old `history + current` order. New test:
  `currentTakesPrecedenceOverAStaleDuplicateInHistory`.
- [x] **P7-07 — Calculator's silent DNI-month default** (done 2026-09-27). `resolveFixationComparison` now
  returns `FixationCalculatorResult(comparison, dniMonthAssumed)`; `PayAuditFixationCalculatorSection` shows
  a note ("No increment found in your payslips yet — assuming a January DNI cycle...") in the error color
  above the comparison whenever `dniMonthAssumed` is true. New assertions on `dniMonthAssumed` added to the
  two existing DNI-related tests in `PayAuditFixationCalculatorLogicTest`.
- [x] **P7-08 — Evidence display is only half-closed** (done 2026-09-27). `AdvancedAnomaliesCard`'s
  `AnomalyDetailRow` now also renders `expected`/`actual`/`authority` when present, reusing
  `PayAuditStrings`' evidence labels (SSOT with `PayAuditFindingRow`) so the Insights-tab card and the Pay
  Audit screen show the same detail for the same finding. No new test — matches the existing convention
  that no Insights-tab composable has a UI test (P7-19).
- [x] **P7-09 — Insights-tab CTAs not scoped to "proven"** (done 2026-09-27).
  `candidateRecommendedActions` now requires `it.isProven()` alongside the type check; `anomalyActionTarget`
  now takes the full `Anomaly` and returns `null` for an unproven `REPRESENTATION_DRAFT_TYPES` instance —
  and `toInsightUiModel` drops the action label whenever the target is null, so an unproven finding no
  longer shows a label with nowhere to go. Existing tests exercising an unproven `SALARY_LOSS` (never
  proven in practice per Phase 5) updated to use a proven fixture where the old CTA behavior was the point
  of the test; new tests added for the unproven case in both `SmartInsightsBuilderTest` and
  `RecommendedActionsLogicTest`.
- [x] **P7-10 — `SALARY_LOSS`/`MISSING_ALLOWANCE` no longer auto-draft letters** (decided 2026-09-27,
  user call: drop `SALARY_LOSS`, leave `MISSING_ALLOWANCE` open). `SALARY_LOSS` removed outright from
  `REPRESENTATION_DRAFT_TYPES` — it's a bare net-pay heuristic with no evidence fields, never provable in
  production, so exclusion is now explicit instead of an implicit always-false `isProven()`.
  `MISSING_ALLOWANCE` stays in the set (its HRA/MSP-Level-14 rules are structural and in principle
  citable) but still has no verified `PayAuthorities` entry — sourcing one needs real legal-citation
  research, deliberately not attempted here to avoid repeating the MSP-citation mistake P7-02 caught.
  Updated tests across `FinancialIntelligenceRepositoryTest`, `SmartInsightsBuilderTest`,
  `RecommendedActionsLogicTest`, `GatedNavigationInvariantTest` to use `MISSING_ALLOWANCE` for the
  proven/unproven CTA cases (the type actually exercising the `isProven()` gate now), plus new tests
  confirming `SALARY_LOSS` never gets a CTA even when synthetically "proven".

Not on the Phase 7 checklist: items already fully resolved with no residual gap (Phase 0's five
DA-arrears defects; Phase 1's timeline-has-no-consumer note, closed by Phase 2's wiring).

### Phase 8 — Remaining Phase 7 carry-overs — DONE (2026-09-27)

The 15 items Phase 7 left open, unchanged in substance, IDs kept as `P7-NN` (they're the same items —
renumbering would just break the cross-references already made to them). Each is tagged with why it
wasn't attempted in Phase 7, so this phase doesn't read as a silent backlog dump.

Per user decision at the start of this phase: the 6 deliberately-deferred/blocked-on-real-users items
below are left untouched (no code change — that's the correct behavior for a trigger-condition item, not
a gap), and the 6 new-feature/architecture items are carried into **Phase 9** rather than rushed. Only the
3 small/medium fixes (P7-12, P7-13, P7-20) were in scope for this phase, one commit each:

- [x] **P7-13 — Transition following an untrusted month is skipped, not attempted** (done 2026-09-27,
  commit `9b644dc1`). `PayLineChangeExplainer.explain` no longer takes a single `previous` payslip — it
  takes the full `history` and always walks back to the last trustworthy month in the `ServiceTimeline`
  for both the amount diff and the reasoning context, rather than comparing against (or skipping because
  of) the literal immediately-preceding stored payslip. Corpus gate numbers moved (104→106 tracked
  changes, 93/106 explained, 87.7%) because a previously-skipped transition is now attempted; the 13
  pinned licence-fee gaps are unchanged.
- [x] **P7-20 — Other auditors not rebuilt on the timeline** (done 2026-09-27, commit `e88c6c14`). Scoped
  down from a full per-auditor rebuild (explicit user decision, 6 separate domain rewrites was too much
  for this item): `DeterministicIntelligenceEngine` now skips any plain (non-`TimelineAuditor`) auditor —
  `SalaryLossAuditor`, `DaArrearsAuditor`, `MarriedQuartersRiskAuditor`, `UnexpectedDebitAuditor`,
  `DsopComplianceAuditor`, `TaxProjectionAuditor` — when `current`/`previous` is `needsReview`, closing the
  false-positive-on-untrusted-data gap. Deliberately *not* gated on pay-matrix-cell membership (the
  broader "trusted timeline month" concept `ServiceTimelineBuilder` itself uses): that's a
  timeline-construction detail for level/stage resolution, not a general data-trust signal, and gating on
  it broke unrelated tests using synthetic non-matrix basic pay. The full per-auditor rebuild onto
  `ServiceTimeline` fields (matching TPTA/Increment/MSP in Phase 2) remains open — folded into Phase 9's
  new-feature-shaped work rather than left as a dangling half-open item here.
- [x] **P7-12 — "What changed this month" covers one transition, not the whole timeline** (done
  2026-09-27, commit `1f3b9b91`, after P7-13 so it could reuse the fixed trusted-month logic).
  `PayLineChangeExplainer.explainAll` walks every consecutive pair of stored months. `EngineResult` gains
  `allChangeExplanations` (the full-history list; `changeExplanations` keeps its current-month meaning
  unchanged). `PayAuditScreen` gains an "Every change explained" section below "What changed this month",
  excluding the current month to avoid duplicate `LazyColumn` keys/rows with the section above it. The
  corpus test's manual pairwise loop now calls the same production function (DRY).

Gate result: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, `iosSimulatorArm64Test`, and
`linkDebugFrameworkIosSimulatorArm64` all green; `ktlintCheck` and the tech-debt/file-size audit clean on
every touched file, after each of the three commits individually. No tech debt incurred: each item's fix
was scoped down (not stubbed) where the full version was out of scope, and every scope-down is documented
above and carried forward explicitly rather than silently dropped.

Untouched, carried forward as-is per user decision (unchanged in substance from Phase 7 — see there for
full text): **P7-11** (Service Timeline pagination — trigger: an unusably long real timeline), **P7-21**
(MNS/NCC matrix scope — trigger: an MNS/NCC user), **P7-22** (TPTA city class null Jun–Sep 2017 — trigger:
a real officer needs those months classified), **P7-23** (out-of-scope rules: Level 14+, first
post-promotion increment, leave/suspension TPTA — trigger: a real officer in one of these situations),
**P7-24** (real-data timeline validation — blocked on real users), **P7-25** (the validation checkpoint
itself — blocked on real users and on Phase 9's P7-18).

### Phase 9 — New rule modeling and architecture carry-overs — DONE (2026-09-27)

The 6 items Phase 8 explicitly did not attempt (new rule modeling and multi-file architecture work, not
bug fixes — user decision at the start of Phase 8 was to give each proper design attention rather than
rush them alongside P7-12/13/20). IDs kept as `P7-NN`, same reasoning as Phase 8. Each item landed in its
own commit, `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, `iosSimulatorArm64Test`,
`linkDebugFrameworkIosSimulatorArm64`, and `ktlintCheck` all green after every one. Per user decision at
the start of this phase: P7-15 was skipped outright (no verified data exists to build it on — see below),
the other 5 were attempted and landed, each scoped down from its full ambition where the full version
would have meant either guessing at unverified rates/authorities or a much larger architecture change than
the gap warranted. What each scope-down left open is consolidated into Phase 10, not left dangling here.

**New feature work (new rule modeling, not bug fixes):**

- [x] **P7-14 — TPTA arrears not linked to a DA rise (`arrearsTpta`) are untracked** (done 2026-09-27,
  commit `a958887c`). `PayLineChangeExplainer` now tracks `arrearsTpta` and explains it via the same
  posting-change/relocation edge (`TptaAbsenceExplainer`) that already explains a TPTA drop, distinct from
  `arrearsTptaDa`'s DA-rise-only rule. Corpus gate moved from 93/106 (87.7%) to 95/108 (88.0%).
- [x] **P7-15 — Licence-fee rate (accommodation/rent bracket) not modeled — skipped, not attempted**
  (decided 2026-09-27, user call). No rent-bracket table exists anywhere in `scripts/pcdao_factory/output/`
  — the offline authoring reference this app's rules are ported from simply doesn't have one. Coding a rate
  without a verified source would break the vision statement's own rule ("cites a verified authority").
  Unchanged from Phase 8: still pinned in `PayLineChangeExplanationCorpusTest`, still open, carried to
  Phase 10 unmodified — the trigger condition (someone supplies the real PCDA rent-bracket table) hasn't
  changed since Phase 7 first listed this item.
- [x] **P7-16 — Only ten pay-line fields are explained — scoped to Non-Practicing Allowance** (done
  2026-09-27, commit `2482ce05`). Of the ~30 untracked `Earnings`/`Deductions` fields, only NPA had both a
  citable structural rule (20% of basic pay, capped at ₹2,37,500 — GoI MoD letter dated 28-09-2017) and
  data the timeline already carries (basic-pay INCREMENT/PROMOTION events). It's now tracked, explained
  when a change coincides with one of those events, and left unexplained on onset/cessation since
  medical-corps eligibility isn't modeled anywhere in the app (no guessing). Every other field on the
  original list — CEA, dress/ration/technical allowance, special forces pay, DSOP subscription, income tax,
  `adj*` corrections — needs data this app doesn't parse (dependents, trade qualification, officer type,
  subscriber election) or is inherently variable by design; this is a documented scope boundary, not
  unfinished work, so it is not carried forward. Corpus gate moved from 95/108 (88.0%) to 95/110 (86.4%): a
  real one-month NPA credit/reversal in the corpus (Oct/Nov 2019) has no coinciding pay-base event and is
  correctly left unexplained, pinned alongside the licence-fee gaps.

**Substantial architecture work (each its own multi-file undertaking):**

- [x] **P7-17 — Findings decided at import time; no retraction/re-audit — the "hold" half only** (done
  2026-09-27, commit `1816233d`). The plan named two alternatives; this phase implemented the simpler one.
  `TptaAbsenceExplainer.isPendingFutureData` holds (doesn't flag) a TPTA-absence finding when it isn't
  explained today only because the same-window sample that would confirm or rule out a relocation hasn't
  been imported yet — fixing the corpus's three import-order false positives (Dec 2019, May 2022, Sep
  2024) without needing any stored-finding reconciliation. What's still open: nothing in the app re-runs
  `analyze()` for an already-imported month when a later one arrives, so a truly held finding never
  surfaces once the explaining data does exist — the "re-audit that retracts/surfaces a finding" half of
  this item is unbuilt. Carried to Phase 10.
- [x] **P7-18 — Stored history too thin for the new explanations** (done 2026-09-27, commit `6c8b4e69`).
  Took the Room migration path per user decision (not the "migrate `InsightsState` to read
  `PayslipUiState.payslips` directly" alternative). `LedgerRecordEntity` schema v11→v12: added
  `riskHardshipAllowance`, `fieldAllowance`, `licenseFee`, `furnitureRent`, `arrearsDa`, `arrearsTpta`,
  `arrearsTptaDa`, `adjTpta`, `adjMsp`, `needsReview` — every field a Pay Audit timeline auditor
  (`DaArrearsAuditor`, `TptaEntitlementAuditor`, `MspAuditor`, `MarriedQuartersRiskAuditor`) actually reads,
  traced auditor-by-auditor rather than porting all ~40 `Earnings`/`Deductions` fields speculatively.
  `AutoMigration(11, 12)` with Room-visible defaults on every new column, `PayslipDatabaseUpgradeTest`
  green. `needsReview` now round-trips, so Phase 8's P7-20 gate stops being a no-op on the Insights-tab
  path. `TaxLedgerAggregator`'s `adj*` fields (a tax-summary concern, separate from the Pay Audit auditors)
  were deliberately left out — not this item's gap.
- [x] **P7-19 — No Compose/UI test exercises any Pay Audit composable — scoped to `PayAuditFindingsSection`**
  (done 2026-09-27, commit `ef113b54`). Correction to this item's own premise: it is not the codebase's
  first Compose UI test — ~20 already exist (e.g. `AdvancedAnomaliesCardTest`) using Compose Multiplatform's
  `runComposeUiTest` + Robolectric, an established pattern this test follows, not a new convention. Added
  `PayAuditFindingsSectionTest` (3 cases: unlocked finding renders its description/evidence/authority;
  locked display shows only the count/CTA teaser without leaking a finding's description; empty state
  renders). `PayAuditTimelineSection`, `PayAuditPredictionsSection`, `PayAuditFixationCalculatorSection`,
  and the Phase 5 representation-gating end-to-end path remain untested at the UI layer — same convention
  gap as the rest of the Insights tab, not unique to Pay Audit, carried to Phase 10.

### Phase 10 — Remaining Phase 9 carry-overs

What Phase 9 left open, plus the Phase 8 items still blocked on a trigger condition (unchanged, listed here
only for continuity since Phase 9 didn't touch them). None of these are regressions — each is either a
documented scope-down (see Phase 9 above for why) or a pre-existing trigger-condition item.

- [ ] **P7-15 — Licence-fee rate/accommodation bracket not modeled.** Unchanged since Phase 7. Trigger: a
  verified PCDA rent-bracket table becomes available (from the user or a future authoring-reference pass).
- [ ] **P7-17b — Held TPTA findings never resurface once the explaining data exists.** The "hold" fix
  (Phase 9) stops a false positive from firing, but nothing re-runs `analyze()` for an already-imported
  month when a later payslip arrives, so a genuinely-missing-TPTA month that happened to look ambiguous at
  import time stays silently held forever, not just until resolved. Needs either a re-audit trigger on
  each new import (re-run `analyze()` for the N months around it, reconcile against stored
  `FinancialInsightEntity` rows) or an explicit "pending" UI state so the held finding isn't just invisible.
  Engine-level logic (`isPendingFutureData`) already exists; this is the app-path wiring, and it depends on
  Phase 9's P7-18 ledger fields to matter on the Insights-tab path.
- [ ] **P7-19b — UI test coverage stops at `PayAuditFindingsSection`.** `PayAuditTimelineSection`,
  `PayAuditPredictionsSection`, `PayAuditFixationCalculatorSection`, and the Phase 5
  representation-gating end-to-end path (a proven finding actually reaching `RepresentationScreen`, an
  unproven one not) have no Compose UI test yet.

Untouched, carried forward as-is per user decision at the start of Phase 8 (unchanged in substance — see
Phase 8/Phase 7 for full text): **P7-11** (Service Timeline pagination — trigger: an unusably long real
timeline), **P7-21** (MNS/NCC matrix scope — trigger: an MNS/NCC user), **P7-22** (TPTA city class null
Jun–Sep 2017 — trigger: a real officer needs those months classified), **P7-23** (out-of-scope rules:
Level 14+, first post-promotion increment, leave/suspension TPTA — trigger: a real officer in one of these
situations), **P7-24** (real-data timeline validation — blocked on real users), **P7-25** (the validation
checkpoint itself — blocked on real users and, per Phase 9's P7-18 note, now also worth re-checking that
the Insights-tab needsReview gate behaves as expected once real data exists).

## Deferred / dropped

- **Deferred until users ask:** situational tiles / 3-tier matrix, claim/LTC/TA rules (~300 of the 378), deadline trackers.
- **Dropped:** the "unclaimed ₹" counter, personas, the 18% penal-interest hazard (no verified authority), the prototype HTML.
