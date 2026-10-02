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
**P7-24** / **P7-25** (real-data validation — partially done 2026-10-01, n = 1; see the Phase 11 list below).

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

### Phase 10 — Remaining Phase 9 carry-overs — DONE (2026-09-27)

What Phase 9 left open, plus the Phase 8 items still blocked on a trigger condition (unchanged, listed here
only for continuity since Phase 9 didn't touch them). None of these are regressions — each is either a
documented scope-down (see Phase 9 above for why) or a pre-existing trigger-condition item.

Per user decision at the start of this phase: P7-15 was researched, not attempted (see below); P7-17b was
scoped to the simpler of its two named alternatives (explicit pending UI state, not a re-audit trigger);
P7-19b was taken in full (all four listed gaps). Each landed in its own commit; `./gradlew check -x
iosX64Test -x iosSimulatorArm64Test`, `linkDebugFrameworkIosSimulatorArm64`, and `ktlintCheck` all green
after every one.

- [x] **P7-15 — Licence-fee rate/accommodation bracket not modeled — researched online, dropped, not
  modeled** (decided 2026-09-27, user call: "check online? if not, we may drop it"). A real, citable
  circular exists — PCDA(WC) Circular No. E/II/161/R&A/Misc, dated 13.09.2022, citing MoD letter No.
  1(1)/2014-D(Q&C) dated 01.07.2022, effective 01.07.2020, revising flat-rate licence fee for Service
  Officers & Defence Civilians by plinth-area slab (Substandard/Unclassified accommodation charged at 75%
  of the Standard/Classified rate for the same plinth area) — but the actual plinth-area-to-rupee slab
  table is embedded as an image/PDF annexure ("Annexure II (a) to (c)") that couldn't be extracted as
  verifiable text/data. Modeling only the one rate found (the top slab, ₹5,860/month) would misclassify
  every officer not in that slab — exactly the "cites a verified authority" bar the vision statement sets,
  not met by a partial table. Moved to **Deferred/dropped** below rather than left as an open Phase 10
  item; unlike the trigger-condition items carried forward year over year, this one has an identified
  concrete source (the annexure PDF) that would resolve it if the user or a future pass can supply its
  contents.
- [x] **P7-17b — Held TPTA findings never resurface once the explaining data exists — scoped to the
  explicit pending UI state** (done 2026-09-27, commit `911b1623`). Of the two alternatives P7-17b named,
  the user picked the simpler one: `Anomaly` gains `isPending`; `TptaEntitlementAuditor` now returns the
  held finding marked `isPending = true` instead of `emptyList()`, so it is shown (with a "waiting for a
  later payslip" note) rather than invisible. A pending finding has no `expected`/`actual`/`authority`, so
  `isProven()` still returns false — it never drafts a representation letter, matching the vision
  statement's own bar. The full re-audit-trigger alternative (re-running `analyze()` for nearby months on
  each new import, reconciling against stored `FinancialInsightEntity` rows) remains unbuilt — not carried
  forward as an open item since the user's own call was to take the simpler path, not defer the harder one.
  `PayAuditCorpusPrecisionTest`'s held-months assertion updated from `isEmpty()` to asserting exactly the 3
  held months (Dec 2019, May 2022, Sep 2024) are returned with `isPending = true` and `isProven() == false`.
- [x] **P7-19b — UI test coverage stops at `PayAuditFindingsSection` — closed in full** (done 2026-09-27,
  commit `be433aef`). All four listed gaps covered: `PayAuditTimelineSectionTest` (changes/all-changes/
  service-timeline, 5 cases), `PayAuditPredictionsSectionTest` (next-increment incl. overdue, DSOP room, 4
  cases), `PayAuditFixationCalculatorSectionTest` (empty state, and a resolved comparison off the card's
  own default state — no dropdown/text-field interaction needed since the defaults already produce a valid
  comparison against the fixture timeline, 2 cases), and `RepresentationDraftListTest` (2 cases) for the
  Phase 5 representation-gating path's UI half — `FinancialIntelligenceRepositoryTest` already proved the
  generation gate (proven → draft row inserted; unproven → none); this proves an inserted draft renders as
  a card and an empty list renders the empty state, not nothing. `RepresentationDraftList` made `internal`
  (was `private`) so the test can call it directly with plain data, matching every other tested Pay Audit
  composable's convention instead of requiring a `PayslipViewModel`.

**Tech debt found and fixed in this phase** (commit `87c17e66`, same day): P7-17b's pending note was added
only to `PayAuditFindingRow` (Pay Audit screen), missing P7-08's own SSOT claim that
`AdvancedAnomaliesCard`'s `AnomalyDetailRow` (Insights tab) renders the same evidence for the same finding.
A held TPTA finding looked confirmed on the Insights tab while showing pending on the Pay Audit screen —
caught during this phase's own review, fixed by adding the identical conditional block to
`AnomalyDetailRow`, with a new `AdvancedAnomaliesCardTest` case.

### Phase 11 — Continuity carry-overs (no new items opened this phase)

Phase 10 closed all three of its own items (two done, one researched-and-dropped) — nothing from Phase 10
itself is left open. What remains is the same set of pre-existing trigger-condition items Phase 8 and Phase
10 each carried forward unchanged, since neither phase's scope touched them. Listed here only for
continuity, per CLAUDE.md's "fail loud" rule — not a sign of unfinished Phase 10 work.

- **P7-11** — Service Timeline pagination. Trigger: an unusably long real timeline.
- **P7-21** — MNS/NCC pay-matrix scope. Trigger: an MNS/NCC user.
- **P7-22** — TPTA city class null for Jun–Sep 2017. Trigger: a real officer needs those months classified.
- **P7-23** — Out-of-scope rules (Level 14+, first post-promotion increment, leave/suspension TPTA).
  Trigger: a real officer in one of these situations.
- **P7-24** — Real-data timeline validation. **PARTIALLY DONE (2026-10-01, n = 1 real officer).** The
  developer's own payslips (Jan 2024 – Aug 2026, Level 12A) were walked on a Pixel 9 minified release build;
  the officer confirmed the timeline facts (level, stage 7→8 in Jan 2026, DA steps, R&H and quarters changes).
  Remaining trigger: other officers / levels / postings.
- **P7-25** — The validation checkpoint itself. **PARTIALLY DONE (n = 1).** Zero false findings on the real
  months; the only findings are four months of "Verified … match exactly" DA/TPTA-DA arrears rows (the
  officer marked them genuine). Two defects found and fixed test-first: a Pay Audit crash with Premium
  unlocked (duplicate LazyColumn key when two findings share type+month, `8ce8e766`) and a mislabelled
  Nov 2024 TPTA change (`90f0c47e`). Per-change explanation coverage cannot be read from the UI (only
  explained rows are listed); the corpus figure (87.5%) stands. Remaining trigger: 3–5 other officers; also
  worth re-checking that the Insights-tab `needsReview` gate behaves as expected once more real data exists.
- **UX redesign (consolidation plan Phase 2, 2026-10-01).** The Pay Audit screen now opens on a verdict card
  (correct / issue with pay line and amount / waiting), has an on-screen month picker limited to months with a
  payslip, and three tabs (This month, History, Plan ahead), driven by `PayAuditViewModel`. An arrears
  under-payment (`SALARY_LOSS`, `arrearsDa`) is shown as an issue so the verdict cannot read clean over it; the
  "pay lines" wording no longer claims every line was audited.

- **Entry points (consolidation plan Phase 3, 2026-10-01).** Ported from `pay_audit_1.0` and rewritten for the
  timeline/evidence model: a free-visible Dashboard banner, an "Audit this month" card on the payslip detail
  screen (opens Pay Audit on that month through the selected-payslip state into `setInputs(requestedMonth)`), and
  a one-time orientation sheet behind `OnboardingManager.shouldShowPayAuditIntro()`. No tiles, no "unclaimed ₹".
  Checked on the Pixel 9 (release build): the banner, the CTA (opened the viewed month, Jun 2026) and the one-time sheet work. Open: the orientation cannot be re-opened after dismissal.

- **Release readiness (consolidation plan Phase 4, 2026-10-01).** Full Android gate, the four corpus tests (re-run
  forced, no assertion changed), iOS tests (`--rerun-tasks`) and the iOS link are green. The minified release was
  smoke-tested on the Pixel 9 (banner, "Audit this month" opening the viewed month, verdict, History, Plan ahead,
  verified-arrears cards with "Why?", the orientation sheet staying dismissed after an upgrade and relaunch; no
  fatal/R8 errors in logcat). The iOS app builds, installs and launches on the simulator; its UI is not driven
  (see the verify skill) and stays a manual check on a real iPhone. The `pay_audit_1.0` archive note is
  `10_PayAudit_1.0_Archive.md`. Still unverified on any device: the locked (free) state, Issue/Waiting verdicts and
  "Draft letter" (no real month produces them; Compose/ViewModel tests only).

- **Plain wording (consolidation plan Phase 6, 2026-10-02).** Finding descriptions (all auditors), change reasons
  (`PayLineChangeExplainer`), the representation-letter amounts and the History DA-step title now read in plain
  words with Indian-grouped rupees, month names and "58% to 60%". One wording source: `PayAuditWording`
  (`shared/.../insights/`); composeApp `formatCurrency` delegates to it (rounds; no negative zero). No rule or
  engine logic changed and the corpus tests keep the same findings and coverage. Deliberately kept: compact arrows
  for amount ranges in the change rows. Old stored insight rows keep their old wording until Phase 7's re-audit on
  import rewrites them. Not yet seen on a device.

Apart from P7-24/P7-25 (partially done above), none of these has a trigger that has fired yet. This phase does no code work unless one does.

> Single plan: this file is the only plan. The consolidation work (2026-10-01 onwards) is the section below; the engine phases above are its history. Phase numbers below ("Phase 6 = plain wording") are the consolidation phases, not the engine phases above.

## Consolidation on `feature/pay-audit`: validate, then port (Phases 0-8)

### Context

There are two competing Pay Audit branches, both cut from `main` @ `74723452`:

- **`feature/pay_audit_1.0`** (current, 39 commits): a separate `pcdao/` engine with situation tiles, an "Unclaimed ₹" counter and 18 % penal hazards. The audit on 2026-10-01 found confirmed false rupee figures:
  - ₹22,13,800 of fake back-dues;
  - ₹3,37,200 of "HRA unclaimed";
  - DA computed on Basic only;
  - pre-7th Pay Commission payslips audited with 7th CPC rules.
- **`feature/pay-audit`** (35 commits, Phases 0–10 done): extends the existing `DeterministicIntelligenceEngine` / `RuleAuditor`.
  - A `ServiceTimeline` is rebuilt from the payslips.
  - `PayLineChangeExplainer` gives a reason for 87.5 % of month-to-month changes.
  - `PayAuditCorpusPrecisionTest` finds 0 false findings on the corpus.
  - Representation letters are drafted only from proven findings.
  - It predicts the next increment, DSOP room and pay fixation.
  - Its own plan (`docs/Plan/09_PayAudit_PhasePlan.md`) explicitly dropped the tiles, the unclaimed counter and the 18 % penalty.

**Decision (user, 2026-10-01):** `feature/pay-audit` is the base. Pieces of `pay_audit_1.0` are ported only if they pass that branch's precision gate.

Its only open items (Phase 11) are waiting on real-user data, most importantly **P7-25, the validation checkpoint**. The Pixel 9 holds 20 real months (Jan 2025 – Aug 2026), so that checkpoint can partly start now.

**Outcome:** one Pay Audit engine, validated on real payslips, reachable from the app's main screens, released through a minified build without regressions.

### Standards for every phase (exit checklist)

- **TDD.** Tests are written first and shown failing on the old code (red first). Unit and integration tests both.
- **Green build.** `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` plus `iosSimulatorArm64Test` and `linkDebugFrameworkIosSimulatorArm64` when commonMain changed. Zero ktlint violations.
- **Size.** No file over 300 lines (`check_tech_debt_limits.py --strict`).
- **Architecture.** MVVM, DRY, SOLID and SSOT. Extend the existing SSOT objects (`PayAuthorities`, `PayAuditFindingTypes`, `PayMatrix`, `REPRESENTATION_DRAFT_TYPES`); never shadow them. No circular dependencies.
- **Singletons.** Stateless constant and pure-function `object`s may stay. Anything with collaborators or that tests need to substitute is an injected class, applied to new and touched code only. `scripts/stateful_singleton_guard.py` fails on a new `object` holding mutable state; the allowlisted existing ones are inventoried in the DI Phase 2 record below.
- **Resources.** Copy goes in `PayAuditStrings` / `AppStrings` (`AppStrings.kt` is at 295/300 lines, so new copy goes in the feature file). Colors go in `Theme.kt`.
- **Security.**
  - No PII in findings, logs or fixtures.
  - Real payslips are used only through the opt-in local-corpus test and are never committed.
  - Any Room change gets a migration and its upgrade test.
  - gitleaks passes.
  - Any new regex hot path in commonMain gets an `iosTest` timing test.
- **Regression safeguards.**
  - `PayAuditCorpusPrecisionTest`, `ServiceTimelineCorpusTest`, `PayLineChangeExplanationCorpusTest` and `TokenParseCorpusRegressionTest` must stay green **without loosening any assertion**. Any intended assertion change is listed in the handoff with its reason.
  - Parser files are out of scope.
  - Hooks are never bypassed.
- **Handoff.** Each phase ends with a summary: tech debt incurred, how it was fully resolved in the same phase, and build and test proof.

---

### Phase 0: branch consolidation and baseline (no feature code)

1. **Preserve the current branch.** `feature/pay_audit_1.0` is 3 commits ahead of `origin` (`f4864755`, `63686cda`, `8b722d75`).
   - Push them. The pre-push hook runs the full Android, iOS and gitleaks suite and must pass, never bypassed.
   - Tag the tip `archive/pay-audit-pcdao-1.0` and push the tag.
   - Leave the branch on the remote, unmerged and undeleted, so it can be revived or cherry-picked later.
   - The tag is annotated with the reason it was superseded (false findings; replaced by `feature/pay-audit`). No new commit is made on the archived branch.
2. Check out `feature/pay-audit`. Check whether `95f844df` (removal of the machine-specific `org.gradle.java.home`) is needed there. If it is, cherry-pick it alone.
3. Run the full gate (Android `check`, iOS tests, iOS link, ktlint) and record the result.
4. **R8 baseline.**
   - Build the minified release.
   - On the Pixel: back up first (see the pixel-adb gotchas memory), then install. This migrates schema v11 → v12.
   - Confirm the Pay Audit screen, timeline and findings render in the release build.
   - `proguard-rules.pro` on that branch has no pcdao-specific rules; confirm no keep rule is needed.
5. Update memory: base branch decision, and that `pay_audit_1.0` is archived.

**Exit:** green gate, a working release build on the Pixel, and baseline screenshots recorded.

### Phase 1: real-data validation, first part of P7-25 (developer's own payslips)

1. On the Pixel, record what Pay Audit shows for all 20 real months:
   - every finding (type, month, expected, actual, authority, pending or not);
   - change-explanation coverage;
   - the timeline (level, stage, DNI, postings).
2. Load three peace-posting months (Mar, Sep and Dec 2024) and repeat.
3. You mark each finding **genuine** or **false**, and each unexplained change as acceptable or as a gap.
4. Every false finding or wrong timeline fact becomes a TDD fix:
   - write a failing synthetic test that encodes why it is wrong;
   - fix it in the auditor or timeline;
   - keep the corpus precision tests green.
   - Real payslips are never committed. If a regression fixture is needed, it goes through `CorpusScrubber` first.
5. Explanation gaps on real data: model one only if a verified authority exists (the same bar as P7-15). Otherwise list it as a known gap.

**Exit:** zero false findings on the developer's real months, and the coverage figure recorded. P7-24 and P7-25 are updated in `09_PayAudit_PhasePlan.md` as partially done (n = 1 real officer).

### Phase 2: UX redesign of the Pay Audit screen (design first, then build)

**UX problems found in `feature/pay-audit`'s code:**

| # | Problem | Where |
|---|---|---|
| U1 | No answer at the top. The screen opens on a "Findings" header, and a clean month shows a single line of text. | `PayAuditScreen.PayAuditBody` |
| U2 | No month picker on the screen. The month comes silently from the global selection. | same |
| U3 | **A change row doesn't name the pay line.** It shows "10/2024 — ₹3600 → ₹0" without saying TPTA. | `PayAuditTimelineSection.PayAuditChangeRow` |
| U4 | Six sections in one scroll. "Every change explained" (about 100 rows) sits above the predictions, so they're buried. | `PayAuditBody` order |
| U5 | Raw formatting: "8/2025", ₹ without digit grouping, and unexplained terms (Stage, DNI, TPTA, MSP). | rows, `PayAuditStrings` |
| U6 | A finding has no next step: no "what this means", and no "Draft letter" link to the Representation screen. | `PayAuditFindingRow` |
| U7 | The engine runs inside a Composable (`rememberPayAuditEngineResult`) with no dedicated ViewModel, which violates MVVM. | `PayAuditScreen` |

**Steps:**

0. **Primary acceptance criterion (user, 2026-10-01): a clear verdict.** On opening, the user must be able to tell within one glance, without scrolling, whether the selected month's pay is correct, has an issue (with the pay line and ₹ amount), or is waiting for a later payslip. U3–U7 are fixed too, but the verdict card is the gate.
1. **Check it on the device first.** Install the `feature/pay-audit` debug build on the Pixel, walk the screen with you, and record your specific pain points (screenshots). This list is the acceptance criteria.
2. **Clickable HTML prototype (confirmed).** Published as a private artifact you can open on your phone. It shows all verdict states (clean / issue / pending / locked) using realistic numbers from the corpus, with no PII. No Compose work starts until you approve it.
3. **Proposed layout** (to be validated by the prototype):
   - **Header:** a month picker, ported from 1.0's `AuditMonthSelector` and aligned with Insights.
   - **Verdict card:**
     - clean: "Aug 2026: 12 pay lines checked, all correct";
     - issue: "1 issue: TPTA ₹5,508 short";
     - waiting: "1 waiting for next payslip".
     - Plus a one-line history summary: "20 months audited · 0 issues".
   - **Three tabs:**
     - *This month:* findings as evidence cards (pay line, expected / credited / difference, a "Why?" expander with the authority, and a "Draft letter" action for proven findings), then "What changed" with the pay line named and the reason in plain words.
     - *History:* the timeline grouped into spans (postings, level/stage, DA steps) instead of one row per month. Issue months are marked, and "every change" is collapsed by year.
     - *Plan ahead:* next increment, DSOP room, pay-fixation calculator.
   - **Info (ⓘ) glossary sheet**, ported from 1.0's onboarding sheet and rewritten: DNI, Stage, TPTA, MSP, "pending".
   - **Formatting:** amounts use the existing `formatCurrency` (`InsightsComponents.kt`, Indian digit grouping); month names come from `PayslipPatternConfig.monthNames`.
   - **Locked (free) state:** the verdict, the "What changed" tab and History stay free. Findings show the count and pay-line names, and the unlock CTA explains its value.
4. **MVVM.** A new `PayAuditViewModel`, injected through Koin, owns the selected month, the engine result, the tab and the derived UI state. Composables only render. `rememberPayAuditEngineResult` is removed.
5. **Tests first:**
   - ViewModel tests for the verdict states (clean / issue / pending / locked) and month switching.
   - Compose UI tests: the change row names the pay line, the verdict copy, tab content, and the Draft-letter action appearing only for proven findings.
   - Existing `PayAudit*SectionTest`s are adapted. Each changed assertion is listed in the handoff.
6. Files stay under 300 lines; one composable per section file. Copy goes in `PayAuditStrings` and colors in `Theme.kt`.

**Exit:** a design you approved, implemented; green gate; a Pixel walkthrough showing that every recorded pain point is resolved.

### Phase 3: entry points ported from `pay_audit_1.0`

These are the only parts of 1.0 that fit the zero-input, precision-first principles:

- **Dashboard discovery banner.** Port `DashboardAuditBannerCard` and its test from 1.0, pointing at `Screen.PayAudit`.
- **"Audit this month" CTA on the payslip detail screen.** Port `ReplicaAuditActionCard` and its tests. It opens Pay Audit with that month pre-selected; the selection is passed through the existing nav/state, not a new global.
- **First-time orientation sheet.** Port `PcdaoOnboardingSheet` together with the `OnboardingManager`/`OnboardingStorage` flag (Android and iOS actuals plus their contract tests). Rewrite the copy for the timeline/evidence model: no tiles, no "unclaimed ₹".
- Copy goes in `PayAuditStrings`. Premium gating stays at the findings level only, unchanged (`ANOMALY_DETECTION`).
- Tests are ported and adapted first, and must fail until the components are wired.

**Exit:** green gate, Compose UI tests for all three entry points, and the Pixel shows each entry reaching the correct month.

### Phase 4: release readiness

1. Run all regression safeguards (above) plus the full pre-push suite.
2. Build the minified release and smoke-test it on the Pixel: entry points, timeline, findings (locked and unlocked), a draft letter from a proven finding, and the predictions.
3. iOS: link, run the iOS tests, and smoke-test on the simulator through the existing Xcode path.
4. Docs:
   - close Phase 11 items in `09_PayAudit_PhasePlan.md` as appropriate;
   - add a `pay_audit_1.0` archive note (why it was dropped, what was ported);
   - correct the corpus fixture count in `CLAUDE.md` (52 → 139).
5. Open a PR from `feature/pay-audit` to `main`, but only when you ask.

**Exit:** green gate, release build verified on both platforms, docs current.

---

### Final phase: carry-overs from Phase 0 (fail-loud, consolidated 2026-10-01)

Phase 0 steps 1, 2, 3, 5 are done. Step 4 (R8 baseline) is only partly done:

- Done: the minified release APK was built from `feature/pay-audit` @ `76f91c67`. It installed on the Pixel after an uninstall (signature mismatch) and launches into onboarding.
- **Done (2026-10-01):** the user restored the `.pcda` backup on the Pixel. The Pay Audit screen (findings, what changed, every change explained, what's next, pay-fixation calculator, service timeline) rendered in the minified release with real data, and logcat showed no `ClassNotFound`/`NoSuchMethod`/fatal errors. So no R8 keep rule is needed.
- **Done:** baseline screenshots saved in the session scratchpad (`baseline_1..4_*.png`). They show real financial data, so they are never committed.
- **Not verified:** the Aug 2026 audit shows "No findings" with no unlock/lock state visible on this build. The locked-vs-unlocked findings display needs a check in Phase 2/4.
- **Pending:** note whether the Gemma background download (started automatically on first launch) should be cancelled or left to finish.
- Note: `assembleRelease` ran `uploadCrashlyticsMappingFileRelease`, i.e. the build uploaded an R8 mapping file to Crashlytics. Confirm this is acceptable for local builds.
- Note: the iOS test and link run took only 30 s, so Gradle may have reused cached results. The Phase 4 gate should force a re-run (`--rerun-tasks` on `iosSimulatorArm64Test`).
- Environment gotcha: an IDE-started Gradle daemon breaks AGP builds (missing `jlink`/`jmod`). Run `./gradlew --stop` immediately before each build from the shell.

Phase 0's exit is met except for the notes above (Gemma download, Crashlytics upload, iOS cache re-run, locked-state check).

#### Phase 1 carry-overs (2026-10-01)

- Done: 33 months (Jan 2024 – Aug 2026) recorded on the Pixel; 4 months show 2 "Verified … match exactly" arrears rows; user marked them genuine, all timeline facts correct, Nov 2024 TPTA label a gap (fixed, `90f0c47e`). Crash on the unlocked screen fixed (`8ce8e766`).
- **Not fixed (user marked genuine, but note):** the old Insights engine's "Quarters Rent Recovery Risk ₹60,000–80,000" alert is not a Pay Audit finding; user says it is correct for them.
- **Gap, not modeled:** change-explanation coverage cannot be read from the UI (only explained rows are shown); corpus figure 87.5% stands. No unexplained real change was visible.
- **Not verified on the device:** the two fixes need a new build on the Pixel. Installing a local build needs an uninstall (signature mismatch) = wipes data; not done. Re-verify at Phase 2/4 after a `.pcda` backup.
- Mar 2024 / Sep 2024 / Dec 2024 recorded as part of the 33 months; Mar 2024 could not be re-read on the Insights tab (dropdown selection failed), its crash was seen in the logcat only.
- The Pay Audit screen has no month picker and the Insights month dropdown is a calendar (Jan 2024 – Aug 2026) that includes months with no payslip: both worth covering in the Phase 2 UX work.

#### Phase 2 carry-overs (2026-10-01, fail-loud)

Phase 2 is implemented, tested and walked through on the Pixel (design approved by the user the same day). Exit is met except the device-unverified items below.

- **Pixel walkthrough: done 2026-10-01 (release build, `adb install -r` over the existing local release, same signing cert and versionCode 16, so no uninstall; the user exported a `.pcda` backup first).** Verified on the device with real data (screenshots stay in the session scratchpad, never committed):
  - Pay Audit opens on "This month"; the verdict card is visible without scrolling ("no issues found on N pay lines", "32 months audited · 0 issues").
  - Apr 2026 (two verified arrears rows) renders unlocked without the Phase 1 duplicate-key crash, shown as "Verified · not an issue" cards.
  - Change rows name the pay line with grouped rupees; the Nov 2024 TPTA change now reads "TPTA follows DA: 50%→53%" (Phase 1 fix confirmed).
  - Month steppers, the month picker (months without a payslip greyed out), the ⓘ glossary, the History spans and the Plan ahead tab all work.
  - Found and fixed: History said "Effective Apr 2026" for a DA step (the payslip month, not the effective date); now "First paid on the Apr 2026 payslip" (commit `6d8dd367`, rebuilt, reinstalled with `install -r` and re-checked on the device).
  - ~~**Not verified on the device:** the locked (free-tier) state, the Issue and Waiting verdicts, the "Why?" expander and "Draft letter"~~ **Verified on the Pixel 2026-10-02 in Phase 8 (synthetic seed).** Still unobserved: whether the Gemma download or Play dialog appeared (not seen).
  - **Device gaps found:** the second verified card (TPTA DA arrears) is titled "DA arrears" like the first because the engine uses `field = "arrearsDa"` for both (the description disambiguates); the month picker's "no payslip" label wraps to two lines and makes those rows taller.
- **Resolved (2026-10-01, user raised both): false "all correct".** An arrears under-payment (`SALARY_LOSS`, field `arrearsDa`, with expected/actual) now counts as a Pay Audit issue in the display classification (`classifyPayAuditFindings`); the bare net-pay `SALARY_LOSS` heuristic still does not. It is unproven (no authority), so no letter is offered. Corpus probe: 14 verified, 0 shortfalls (n=1 officer). Watch for partial arrears paid across several months reading as a shortfall; the fix, if it happens, is to hold it as waiting. Engine and corpus tests untouched.
- **Resolved: "N pay lines checked" overclaim.** Reworded to "Aug 2026: no issues found on 12 pay lines" (N is still the lines on the payslip, which the wording now says).
- **U5 partly done.** Screen formatting is fixed (Indian grouping, "Aug 2026", glossary). Not fixed: finding `description` strings from the auditors (`shared`, e.g. "arrears of ₹9870") and change `reason` strings from `PayLineChangeExplainer` (e.g. "arrears for 1/2026-3/2026", "58%->60%") are still raw. Both live in `shared` and are asserted by corpus/unit tests; a wording change there is its own phase.
- **DI deviation.** `PayAuditScreen` gets `PayAuditViewModel` from Koin (`koinInject`); the Insights entry card (`PayAuditEntryHost`) uses `remember { PayAuditViewModel() }` because existing Insights UI tests run without Koin and the constructor has no collaborators. Move it to Koin when those tests start Koin.
- **ViewModel lifetime.** `PayAuditViewModel` is a plain class tied to composition (`dispose()` on leave), so the selected tab/month reset on rotation or re-entry. Acceptable now (reopens on the app-wide selected month); revisit if users complain.
- **Insights month dropdown** is still a calendar that includes months with no payslip; only Pay Audit got the payslip-aware picker.
- **Process note:** `PayAuditHistoryLogicTest` was written before `PayAuditHistoryLogic` but never run against a stub (compile-red only); the ViewModel and format tests were run red (17/18 and all failing) first.
- **Not verified:** iOS simulator smoke test of the new sheets (Phase 4); no screenshot comparison against the HTML prototype.

#### Phase 3 carry-overs (2026-10-01, fail-loud)

Phase 3 is implemented and tested (tests written first and shown red). Exit is **not fully met**: the Pixel check is owed.

- **Pixel check done 2026-10-01 (release build, `install -r`, data kept, user OK'd after backup):** Dashboard banner showed "Check your 32 payslips…" and opened Pay Audit on Aug 2026; the orientation sheet appeared on that first open, "Got it" dismissed it, and it did not return on the next open; "Audit this month" on the June 2026 payslip opened Pay Audit on Jun 2026 (not the dashboard's Aug). ~~Still unchecked on the device: the sheet after an app restart (persistence) and locked/Issue/Waiting verdicts.~~ (Persistence verified in Phase 4; locked/Issue/Waiting verified in Phase 8.)
- ~~Not done: Pixel check that each entry reaches the correct month.~~ (superseded by the line above; the paragraph below describes what the unit tests cover.) Needs a new release build on the Pixel, so a `.pcda` backup and the user's OK first (same-cert `install -r` keeps data). Covered only by Compose/ViewModel tests: `PayslipReplicaAuditEntryTest` (CTA selects the viewed payslip, navigates to `Screen.PayAudit`; `PayAuditViewModel.setInputs(requestedMonth)` lands on it), `DashboardAuditEntryTest`, `PayAuditEntryPointCardsTest`, `PayAuditOrientationSheetTest`.
- **Phase 2 asks skipped as already resolved.** Both Phase 2 risks (arrears under-payment shown as an issue; "no issues found on N pay lines" wording) were closed in `9f29a56f` / `6d8dd367`; the Phase 3 prompt was stale on this. No engine change was made in Phase 3.
- **Month hand-off is the existing app-wide selected payslip, not a new global.** "Audit this month" calls `PayslipViewModel.selectPayslip(payslip)` and `PayAuditScreen` already turns the selected payslip into `setInputs(requestedMonth)` on first open. Side effect: after the CTA the Dashboard also shows that month. Not changed because the plan forbids a new global and iOS navigates natively (no argument channel).
- **Orientation sheet is a single bottom sheet, not 1.0's three-slide dialog.** Three short points plus a "Got it" button; the pager, skip/back/next and the indicator were not ported (no tiles to explain). It cannot be re-opened after dismissal; the existing ⓘ glossary covers the terms. Add a "How it works" row to the glossary sheet only if users ask.
- **Orientation flag is a plain boolean per install** (`has_seen_pay_audit_intro`: SharedPreferences / NSUserDefaults), like the coachmark flag. It is not part of the `.pcda` backup, so a restore on a new install shows it once more.
- **Banner copy deliberately makes no claim:** "Check your N payslips against the rules" (the timeline excludes `needsReview` months, so "N payslips checked" would overclaim, the same trap as Phase 2). The banner is free-visible and says nothing about findings; premium gating is unchanged at the findings level.
- **Sheet UI tests use the semantics click action**, not a pointer click: Robolectric does not deliver pointer clicks into `ModalBottomSheet` content (also true with a tall-screen qualifier).
- **`PayAuditScreen` gets `OnboardingManager` by `remember { OnboardingManager() }`**, matching `DashboardScreen`'s default, not Koin.
- **iOS:** the new flag's `actual` compiles, links and passes `iosSimulatorArm64Test`; the iOS UI (banner, CTA through `bridge.navigateToDetail`, sheet) is not smoke-tested (Phase 4).

#### Phase 4 carry-overs (2026-10-01, fail-loud)

Phase 4 is done except the items below. Gate: Android `check` green; the four corpus tests re-run with `--rerun` and green, no assertion changed; `iosSimulatorArm64Test --rerun-tasks` and `linkDebugFrameworkIosSimulatorArm64 --rerun-tasks` green. Minified release (versionCode 16) installed on the Pixel with `install -r` (data kept, after the user's `.pcda` backup and OK).

- **Pixel verified:** banner; "Audit this month" on Jul 2026 opened Jul 2026 (not the dashboard's Aug); verdict, History, Plan ahead (increment, DSOP room, fixation calculator); Apr 2026 verified-arrears cards and "Why?"; the orientation sheet did not return after the upgrade and relaunch (persistence); no FATAL/ClassNotFound/NoSuchMethod in logcat.
- ~~**Not verified on any device:** locked (free) state, Issue and Waiting verdicts, "Draft letter" from a proven finding; the first-ever orientation sheet on this build.~~ **All five verified on the Pixel 2026-10-02 in Phase 8** (debug build, "Force Free", synthetic seed, fresh install). iOS is still unverified (see Phase 8 carry-overs).
- **iOS UI not smoke-tested.** The app builds with xcodebuild, installs and launches on the iPhone 17 simulator, but banner / CTA / sheet could not be driven (no seedable payslip data, no automation per the verify skill). Manual check on a real iPhone before release. Note: the Xcode "Upload symbols to Crashlytics" run-script phase runs on every build, including Debug simulator builds.
- **Crashlytics mapping upload:** the local release was built with `-x uploadCrashlyticsMappingFileRelease` because a local build at the same versionCode as the Play build could overwrite its R8 mapping. A normal `assembleRelease` still uploads; decide whether local builds should skip it.
- **Corpus count:** `index.json` lists 139 (the plan's number was right; the first guess of 140 counted `apr_14`, which is on disk but not indexed, so it is not in the gate). CLAUDE.md, the `TokenParseCorpusRegressionTest` KDoc and four counts in `AI_INSIGHTS_PIPELINE.md` (52 -> 139) are fixed. Not touched: `AI_INSIGHTS_PIPELINE.md` line about `PayslipCorpusRegressionTest` (a deleted legacy test) and the historical "52/52" anecdote in CLAUDE.md (about an old bug).
- **Gemma download:** not observed during the smoke test; whether to cancel or let it finish is still undecided.
- **Not done by design:** no PR opened, nothing pushed.

### Phases 5-8: post-release-readiness items (user decisions 2026-10-01)

Decisions: no PR/push until the very end; Crashlytics mapping upload only for the production release build; Gemma download is production-only (ignore on debug); P7-24/25 validated later with other officers in closed testing.

- **Phase 5 (DONE, commits 5154435d, f85d9786):** TPTA-DA arrears own pay line (arrearsTptaDa); month picker "no payslip" label removed; month/tab survive app recreation (PayAuditSavedState + rememberSaveable); glossary "How Pay Audit works" reopens the orientation; Insights dropdown already payslip-only (pinned by test). ~~Device-unverified: the recreation behaviour~~ **verified on the Pixel 2026-10-02 (Phase 8, "don't keep activities").**
- **Phase 6 (DONE, commit 88007e19): plain wording (#6).** New SSOT `PayAuditWording` (`shared/.../insights/`: grouped rupees via `TaxLedgerAggregator.formatIndianCurrency`, "58% to 60%", month names, "July to August 2018"). All ten auditors' descriptions, `PayLineChangeExplainer` reasons, the representation-letter amounts ("Rs. 1,23,100") and the History "DA 58% to 60%" title use it. No engine/rule logic changed; the four corpus tests have the same findings and coverage. Details and the changed-assertion list are in the Phase 6 hand-off and the carry-overs below.
- **Phase 7 (DONE, 2026-10-02): re-audit on import (#12).** User chose "re-run on import". `FinancialIntelligenceRepository.processPayslipAndRunAnalysis` now audits the imported month and then re-audits every stored month in the 6 months before it (`TptaAbsenceExplainer.RELOCATION_WINDOW_MONTHS`, now public to the module: the window the explainer itself uses, so it covers every month a new payslip can change). Per month, stored `FinancialInsightEntity` rows are reconciled: rows no longer produced are deleted (a held TPTA row whose gap a later payslip explains), the rest are rewritten in the current wording keeping `createdAt` and `isArchived`; a surfaced finding keeps the held row's id (`sha256(month-type-field)`), so it is one row. Drafts: one per month and dispute type (a re-audit or re-import never stacks a duplicate; this also fixed the existing duplicate-on-re-import bug), never for a held finding (it is not proven), and never deleted by a re-audit. No schema change, no DAO change, no auditor rule change. Tests: `FinancialIntelligenceReauditTest` (9; 5 red on the old code with real assertion failures, 4 are guards). Decisions (user, 2026-10-02): scope = the 6-month window; old drafts are left alone.
- **Phase 8 (DONE, 2026-10-02): debug synthetic seed + device checks (#4, #5).** Commits `875b915d` (seed), `7038a5c7` and `86beec4f` (two wording fixes the device walk found). User decisions (2026-10-02): hook = debug-only ContentProvider filling an empty commonMain `DeveloperToolsRegistry`; marker = `ParsedPayslip.file` prefix `debug-seed:` (inside the encrypted JSON, so no schema change) plus refuse-on-collision; seed allowed alongside real data only when no target month exists.
  - **Code** (all in `composeApp/src/androidDebug`, manifest in `src/debug`): `SeedStep` (7 buttons), `SyntheticSeedPayslips` (invented numbers, placeholder officer, months 2018-2023 in blocks years apart), `DebugSeeder` (writes like an import: stored payslip then `processPayslipAndRunAnalysis`; never overwrites a month holding any payslip, ledger row, insight or draft; "Remove seed data" deletes only marked months and the ledger rows, insights and drafts they spawned), `DebugSeedViewModel`, `DebugSeedSection`, `DebugSeedProvider`. Production code touched: `DeveloperToolsRegistry` (new, empty in release) plus one `Render()` line in `SettingsScreen`.
  - **Release proof** (final release APK, built from the committed code): 0 dex classes matching `DebugSeed*`/`SyntheticSeed*` (35 in the debug APK as the control), 0 raw `debugseed`/`debug-seed:`/`Seed Officer` byte hits (248 in debug), no `DebugSeedProvider` in the manifest, 0 hits in R8 `mapping.txt`.
  - **Tests:** 25 new, written first and shown red with real assertion failures (20 of 21 seed tests on stubs; 3 of 3 fix tests on the old code). `DebugSeederTest` (14: every scenario's verdict through the real `FinancialIntelligenceRepository` and `PayAuditViewModel`, all scenarios together giving exactly the four intended findings, free-tier count only, collision, orphan insight, double seed, prerequisite, remove leaves a real month untouched), `DebugSeedSectionTest` (4), `DebugSeedProviderTest` (1), `DeveloperToolsRegistryTest` (2), plus 1 letter-name and 3 verdict-copy tests. **No existing assertion was changed or loosened.**
  - **Defects the device walk found, fixed in the same phase:** (1) the debug manifest at `src/androidDebug/` was not merged (caught by reading the merged manifest, so the provider would never have run); moved to `src/debug/`. (2) A letter for a missed increment was subjected "Non-Admissibility of INCREMENT_MISSED" (raw code): now "Basic Pay (annual increment)", MSP likewise. (3) The Waiting card said "Arrears are not credited yet" for a held TPTA gap: it now says a missing pay line may be a posting change or relocation. Arrears keep their wording.
  - **Device (Pixel 9, debug build, release uninstalled in both profiles with the user's OK, `.pcda` backup confirmed by the user first; screenshots in the session scratchpad only):** Settings > Developer shows the seed section; refusal on a second press; first-run orientation sheet; Issue (Mar 2023 TPTA ₹4,212, Why? with authority, Draft letter lists exactly one letter per proven finding), Waiting (Mar 2018), resolved (Mar 2019: Correct, change row "Posting change or relocation"), surfaced (Mar 2020), missed increment (Jul 2022 ₹2,600); locked state under Force Free (verdict free, amount and evidence locked); month and History tab restored after recreation with "don't keep activities"; held-then-resolved and held-then-surfaced watched live by seeding the held block, viewing Waiting, pressing the follow-up and viewing again; "Remove seed data" removed 34 months. The Pixel then got the final release build (`install -r -i com.android.vending`, versionCode 16) and is empty until the user restores the backup.
  - **Gate:** ktlint; `check -x iosX64Test -x iosSimulatorArm64Test` (composeApp debug 544 tests, release 525); the four corpus tests with `--rerun` (13 tests, fresh XML, 0 failures); `iosSimulatorArm64Test --rerun-tasks` (123 suites, 681 tests, 0 failures); `linkDebugFrameworkIosSimulatorArm64 --rerun-tasks`; release build.

#### Phase 6 carry-overs (2026-10-01, fail-loud)

- **Stale premise:** the goal's "arrears for 1/2026-3/2026" was already "Jan-Mar 2026" in the code (month names via `PayslipPatternConfig`); the real raw parts were "58%->60%", enum names ("HIGHER->OTHER"), "admissible", ungrouped rupees and "N/YYYY" in `IncrementAuditor`.
- **Closed in the Phase 6 follow-up (user decisions 2026-10-02):** the two formatters are merged: composeApp `formatCurrency` delegates to `PayAuditWording.rupees` (rounds, no negative zero; amounts on screen may now differ by 1 rupee from the old truncation). The DSOP 18-month line is reworded (no "Sec 10(11)", same figures).
- **Kept on purpose (user decision):** arrows in composeApp `PayAuditFormat.formatChange` ("₹5,400 → ₹5,508"); only percentages use words.
- **Deferred to Phase 7 (user decision):** old stored insight rows keep their old wording until re-audit; no startup re-audit pass.
- ~~**Device:** wording not yet seen on the Pixel.~~ **Seen on the Pixel 2026-10-02 (Phase 8):** grouped rupees ("₹4,212"), "17% Dearness Allowance", "Posting change or relocation (Transport Allowance)". Phase 8 also found and fixed two wording gaps Phase 6 missed (see Phase 8 record).

#### Phase 7 carry-overs (2026-10-02, fail-loud)

- ~~**Not verified on any device.**~~ **Android: verified on the Pixel 2026-10-02 in Phase 8** (a held TPTA month resolves after a later import; a held month surfaces once with exactly one letter). Phase 6 wording also seen. **iOS: not verified** (see Phase 8 carry-overs).
- ~~**A draft the officer deleted comes back** on the next re-audit or re-import while the finding is still proven.~~ **Fixed 2026-10-02 (commit `2f2a503c`):** new `dismissed_drafts` table (Room v13, AutoMigration 12->13), cleared when the finding stops applying.
- ~~**A draft whose finding later becomes held or resolved stays**~~ **Reversed 2026-10-02 (user decision, commit `6de299b3`):** a letter whose finding no longer applies is removed on the re-audit; letters of still-proven findings (and the officer's edits) and of types the audit never drafts are kept.
- ~~**Backfill imports only re-audit earlier months.**~~ **Fixed 2026-10-02 (commit `6de299b3`):** an import re-audits the months within 6 on both sides, so a later month turning from proven to held is picked up (and its letter removed).
- **Letters for earlier months are signed with the imported payslip's officer** (the stored ledger records are de-identified). Fine while a device holds one officer's payslips.
- **Old wording** is rewritten only for months inside a later import's window; older rows keep it (user decision, no startup pass).
- **Re-audit rewrites all of a month's rows, not just TPTA**, so a re-audited month can also gain or lose a non-TPTA row if the engine's view of it changed with the new history. The four corpus tests are unchanged.
- **iOS gate timing:** `iosSimulatorArm64Test --rerun-tasks` took 45 s this time (29 tasks executed, fresh results, 123 suites 0 failures, the new test 9/9), not the ~40 min seen before; the earlier figure is not a constant.
- ~~**Cost:** up to 7 engine runs per import; not measured on a device.~~ **Measured on the Pixel 2026-10-02:** see "Follow-up decisions" below (negligible).

#### Phase 8 carry-overs (2026-10-02, fail-loud)

- **iOS UI is unverified.** The simulator cannot be driven here, and the seed is Android-debug only, so even on a real iPhone the locked/Issue/Waiting/Draft-letter/orientation/wording/re-audit states cannot be produced without real data that triggers them. Asked of the user: wait for a real iPhone, or accept iOS as covered by the shared logic and `iosSimulatorArm64Test` only.
- **"One row" on the device was not inspected.** Held-then-surfaced was seen as one Waiting-to-Issue change and exactly one letter; that the stored insight row keeps the held row's id is proven only by `FinancialIntelligenceReauditTest` and `DebugSeederTest`, because the Insights screen was not drilled to that month.
- **The user's real data is not on the Pixel.** The device was wiped for the debug install (Owner and the "Praju" profile; the Praju copy is not reinstalled). The user restores the `.pcda` backup and, if wanted, reinstalls the app in Praju from Play. The final release build was installed but not launched, to avoid starting the Gemma download over mobile data.
- **Seeded payslips have no PDF row** (`payslip_pdfs`). Opening a seeded month's PDF and "re-parse all" were not exercised; re-parse only walks stored PDFs, so it skips them.
- **Synthetic limits.** DA is a flat 17% and the officer is a placeholder; the legacy Insights "Quarters Rent Recovery Risk" alert fires on the seed (no HRA or licence fee), which is expected and not a Pay Audit finding.
- **Observed, unchanged:** returning from the letters screen reopens Pay Audit on the app-wide selected month, not the month last viewed (recreation restore is Phase 5's scope; re-entry starts fresh by design).
- ~~**Registry is mutable global state.** `DeveloperToolsRegistry` is an `object` filled at startup by the debug provider; accepted as a debug extension point.~~ **Removed in DI clean-up Phase 2 (2026-10-02):** replaced by Koin multibinding; see the DI Phase 2 record below.
- **Seeder duplicates the import write order** (insert payslip, then audit) because `PayslipRepository` has no "save a parsed payslip" method; adding one only for debug use would put debug-driven API in production code.
- **Cost of the re-audit on import** (up to 7 engine runs per import) is still unmeasured on a device; the 19-month seed block imported without a visible stall but was not timed.

#### Follow-up decisions after Phase 8 (user, 2026-10-02)

- **Done:** backfill re-audit both sides and stale-letter removal (`6de299b3`); deleted letters stay deleted via the `dismissed_drafts` table, Room v13 (`2f2a503c`); Crashlytics mapping uploads only for `bundleRelease`, a local `assembleRelease` skips it with no `-x` flag (`816c8ec7`, observed "SKIPPED" in a real build).
- **Import cost, measured on the Pixel (real 32-month history, minified release, temporary timing patch never committed):** the engine over all 32 real months took 9 ms in total (7 ms for the first, cold run; about 0 ms per month after), so a worst-case import re-auditing 13 months is on the order of 100 ms and a typical 7-audit import a few ms. Whole imports of Aug 2026 and Jan 2026 took 23 ms and 13 ms, with a ledger of only 1-2 months (see the next point). Not a concern.
- **Found while measuring:** a `.pcda` restore brings the payslips back but not the ledger, stored insights or letters (they are not in the backup), and the ledger refills only as payslips are imported. Pay Audit itself reads the payslips, so its screen is unaffected, but stored insight rows and letters are not restored. Not changed; decide whether the backup should carry them.
- **Gemma download: let it continue** (decision recorded; no code change).
- **Planned, not started: DI clean-up (user: "architecturally correct, long term").** Remaining bypasses of Koin: `PayAuditEntryHost` uses `remember { PayAuditViewModel() }`; `PayAuditScreen` uses `remember { OnboardingManager() }`; `DashboardScreen` and `App` default `OnboardingManager()`. Steps, each its own green commit: (1) register `single { OnboardingManager() }` in `appModule`; (2) `PayAuditEntryHost` and `PayAuditScreen` take their ViewModel/manager as parameters defaulting to `koinInject()`; (3) `DashboardScreen`/`App` likewise; (4) every UI test that renders them passes explicit fakes or starts Koin with a test module (Robolectric tests already get Koin from `PayslipApplication`, which is the first thing to confirm, since the Phase 2 note says those tests run without Koin); (5) verify iOS starts Koin before the first composition (`MainViewController`) and re-run `iosSimulatorArm64Test` and the link. Risk: iOS UI cannot be driven here, so a mistaken Koin ordering on iOS would only show on a device.
- **DI clean-up, Phase 1 of 6 (DONE 2026-10-02, commit `b5f739ec`, then the follow-ups below).** `OnboardingManager` is a Koin `single`; `PayAuditEntryHost`, `PayAuditScreen`, `DashboardScreen` and `App` take their ViewModel/manager through `koinInject()`. `appKoinModules(platformModules)` is the one module list Android and iOS start Koin from. iOS ordering confirmed by reading: `createNavHost` is the only Swift entry, calls `ensureKoin()` first, and is the only place a `ComposeUIViewController` is built. Tests (red first): `AppKoinModulesTest` (commonTest, also green on Kotlin/Native), `AppModuleGraphTest` (`Module.verify()`), `KoinInjectedDependenciesUiTest`; Robolectric tests already get a real Koin from `PayslipApplication`, so the Phase 2 note about "tests run without Koin" did not apply. No existing assertion changed. Gate: composeApp debug 555 / release 536, shared release 735 (1 pre-existing assumption-skip), iOS 689 + 374, five corpus classes re-run with fresh XML, release build.
- **Found by that work: the tech-debt audit under-measured composables.** A default parameter with balanced braces (`= remember { X() }`, `= {}`) ended the measured "function" on the parameter line, so the 50-line composable limit never applied to those bodies. Fixed by reusing the paren-aware `_body_span` in `check_file_limits` (regression test `scripts/tests/test_check_tech_debt_limits.py`, run in CI; two of its four cases fail on the old script). The corrected audit exposed five over-limit composables, all split with no behaviour change: `DashboardScreen`, `LedgerSection`, `RepresentationScreen`, `PremiumUpgradeBottomSheet`, `PayslipReplicaScreen` (which also had a raw `80.dp`, now `AppDimensions.BannerClearance`). `PayslipUpgradeSheet` is the shared wiring of the upgrade sheet to a `PayslipViewModel`; ~~`InsightsScreen`, `PremiumFeaturesScreen` and `SettingsScreen` still inline the same wiring and can adopt it when next touched.~~ All three adopted it in DI clean-up Phase 2 (2026-10-02): `PayslipUpgradeSheet` takes an optional `onPrivacyClick` (Settings keeps the policy in-app), test `PayslipUpgradeSheetUiTest`.
- **Koin bypasses that remain by decision (recorded for the singleton inventory in DI Phase 6).** `PayslipViewModel`'s default arguments build `RatingPromptManager()`, `GemmaModelStorageManager()`, `provideGemmaInstallTelemetry()`, `provideAppIntegrityChecker()` and `provideBillingManager()` themselves. They are stateless factories or platform providers, not the stateful singletons the plan targets; `AppModuleGraphTest` lists them as `extraTypes`, which is the evidence. Revisit only if one needs a test double.
- **DI clean-up, Phase 2 of 6 (DONE 2026-10-02): the developer-tools registry is Koin multibinding, and a guard stops new stateful singletons.**
  - **Code.** `DeveloperToolsSection` (interface, commonMain) and `DeveloperToolsSections()`, which renders `getKoin().getAll<DeveloperToolsSection>()`; Settings calls it in place of `DeveloperToolsRegistry.Render()`. Build-variant DI: `src/androidDebug/.../di/VariantModules.kt` binds `DebugSeeder`, `DebugSeedViewModel` and `DebugSeedToolsSection` (as a `DeveloperToolsSection`); `src/androidRelease/.../di/VariantModules.kt` is `emptyList()`; `PayslipApplication` starts Koin with `appKoinModules(variantModules)`. iOS is unchanged (no variant modules). Deleted: `DeveloperToolsRegistry`, `DebugSeedProvider`, `src/debug/AndroidManifest.xml`, and the two tests of the registry and provider.
  - **Guard.** `scripts/stateful_singleton_guard.py` flags a top-level or companion `object` that holds a `var`, `lateinit var` or mutable collection/holder (`mutableMapOf`, `MutableStateFlow`, `Atomic*`, ...). It tracks brace depth, so a `var` inside a function or a nested class is not flagged. `check_tech_debt_limits.py` runs it on every file it is given (pre-commit, `--strict`) and, in the full scan, over every production source set of `shared` and `composeApp`. Enforcement in CI is the unit test `RepositoryStateTest`, which fails on any unlisted singleton and on any allowlist entry that no longer matches one (a stale entry could excuse a future singleton that reuses the name). The CI audit step was non-strict (warnings only); it now runs `--strict` (2026-10-02, the tree passes with 348 files), so every audit rule fails CI.
  - **Singleton inventory (allowlisted, not migrated here).** All are platform-interop bridges or process-wide holders read by a static platform entry point: Android `ContextHolder`, `CryptoHelper`, `AndroidGemmaBaseModelInstaller.Companion` (Play Asset Delivery confirmation handler), `LiteRtEngineStore`, `ReviewActivityBridge`; iOS `CryptoHelper` (Keychain key cache), `IosGemmaBaseModelInstaller.Companion`, `GemmaEngine.Companion`, `IosCrashReporter.Companion`, `IosGemmaInstallTelemetry.Companion` (Swift sets closures through them). The earlier scan counted about 14; the guard finds these 10 (plus the registry, now gone). `Logger` holds a collaborator (`CrashReporter`) through `by lazy` with no `var`, which the guard cannot tell apart from a harmless cached value; it is listed here and not detected mechanically. **Decision (2026-10-02):** `Logger` is accepted as is (a logging facade with no mutable state; its `by lazy` is an immutable cached lookup). The guard stays a line/regex check with a closed allowlist; it is not extended into a parser, and Swift is out of scope. The 10 stay allowlisted because each is a boundary the platform calls before Koin exists or through a static entry point. Migrate one only for a concrete need, as its own device-verified phase, Android first (`ReviewActivityBridge`, `LiteRtEngineStore`, the installer's confirmation handler), never `CryptoHelper` or the iOS bridges without a device.
  - **Tests (red first).** `scripts/tests/test_stateful_singleton_guard.py` (14 cases, run in CI): 8 failed on a stub that reported nothing (16 failures counting subtests); the other 6 are "must not flag" cases (constants, nested class, ordinary class, anonymous object, a clean tree) that any stub passes, so they are guards. `DebugVariantModulesTest` (debug: exactly the seed section; without variant modules none), `ReleaseVariantModulesTest` (release: no modules, no section), `SettingsDeveloperToolsUiTest` (Settings renders every section Koin provides, and none when there are none), `DeveloperToolsSectionKoinTest` (commonTest, so it also runs on Kotlin/Native), `DebugSeedWiringUiTest` (the real debug modules resolved through the production Settings screen; added after review found nothing resolved the seed's own bindings, and shown to fail with `NoDefinitionFoundException` when a binding is removed). In the red run the two tests that exercise the new behaviour failed on assertions; the "none provided" cases and the `getAll` mechanism test passed on the old code because an empty registry and Koin's own behaviour satisfy them, so they are guards, not red evidence. **No existing assertion was changed.** Removed with the code they covered: `DeveloperToolsRegistryTest` (2) and `DebugSeedProviderTest` (1).
  - **Gate.** ktlint; `check -x iosX64Test -x iosSimulatorArm64Test` (composeApp debug 559, release 540; shared release 735 with the one pre-existing assumption-skip, unchanged code); the corpus classes re-run with fresh XML (11 classes, 0 failures); `iosSimulatorArm64Test --rerun-tasks` (shared 689, composeApp 376, 0 failures, 0 skipped); `linkDebugFrameworkIosSimulatorArm64`; `check_tech_debt_limits.py --strict` (348 files); the script tests. The first `check` run hit the known IDE-daemon "jmod" error; every result above is from the rerun after `./gradlew --stop`.
  - **Release proof (final release APK).** 0 dex hits for `DebugSeed`/`SyntheticSeed`/`debugseed`/`debug-seed:`/`Seed Officer` (control: a real app literal is found); 0 hits in R8 `mapping.txt`; the manifest has no seed provider (four providers, none ours); `usage.txt` lists only removed synthetic members (`deserialize`/`serialize`/`copy`/`componentN`/unused accessors) of `PortableBackup`, the entities and `DeveloperToolsSection` (its `Content()` call disappears because the release graph has no implementer, which is the intent); `proguard-rules.pro` untouched; `koin-test` is not on the release runtime classpath.
- **Backup clean-up, Phase 3 of 7 (DONE 2026-10-02, commit `1c5fbade`): `PayslipBackupService` owns export and restore, and REPLACE is atomic and a true copy.** Restore decodes, decrypts and re-encrypts every payslip before touching the database, then writes in one Room `@Transaction` (`PayslipDao.replaceWithBackup` / `mergeBackup`); `clearAllUserData()` is the one list of user and derived tables. Found on the way: Room's `clearAll()` empties the payslip table only, so the old REPLACE left the previous device's PDFs behind (the fake DAO hid it).
- **Backup clean-up, Phase 4 of 7 (DONE 2026-10-02): backup v3 carries letters, deleted-letter records and corrections.** `PortableBackup` (`CURRENT_VERSION = 3`) gains `drafts`, `dismissedDrafts` and `corrections`, all defaulting to empty so v1 and v2 files decode. Corrections are decrypted with the device key and re-encrypted with the backup password on export, and the reverse on restore, so an archive does not depend on one device's key; an undecryptable correction is skipped on export (logged, no PII) and a corrupt one in a backup fails the restore before any write. Restore decodes with `ignoreUnknownKeys`. The writes are one `BackupRows` value passed to the existing transaction. The ledger and insights are never stored (Phase 5 rebuilds them). Tests (`PayslipBackupServiceV3Test`, commonTest so also on Kotlin/Native), red first: 8 of 9 failed on real assertions; the v2 case passed on old code (a guard). The v1 legacy-key case was added after review and is also a guard. Entitlement still never travels (the existing entitlement tests are unchanged and green). Also done: a restore refuses a backup with `version > 3` (`UnsupportedBackupVersionException`, "update the app"), and `GoldenBackups` commits real v1, v2 and v3 archives (hex, synthetic data) that `PayslipBackupGoldenTest` restores on the JVM and Native. Open items are in Phase 7, "From Phase 4" of the clean-up plan.
- **Backup clean-up, Phase 5 of 7 (DONE 2026-10-02): the ledger and insights are rebuilt from payslips.** `FinancialIntelligenceRepository.rebuildAuditHistory(payslips)` upserts every ledger row, then audits each month once in date order against the full history (mappers moved to `LedgerRecordMapping.kt`). `PayslipBackupService.restore` calls it after the transaction (corrected payslips from `PayslipRepository.getAllPayslips()`); a rebuild failure is logged and does not fail the restore. `repairAuditHistoryIfIncomplete` runs once per ViewModel after the first payslip emission and only when a payslip has no ledger row; it never throws. Imports (`importPayslip`, `onFilePicked`) now audit the payslip with its stored corrections applied. Tests, red first (10 of 13 shared tests and 3 of 4 ViewModel tests failed on real assertions; the rest are guards), all in commonTest so they also run on Kotlin/Native. Mutation-checked: auditing without the full history fails 2 tests. Not covered: the per-month officer signature is passed to the generator but the letter text uses placeholders, so it is unobservable (clean-up plan Phase 7).
- **Backup clean-up, Phase 6 of 7 (DONE on device 2026-10-02, with one gap): Pixel verification of the release build.** Minified release (versionCode 16, HEAD `dfcaf747`) installed over the Play-installer copy with `install -r` (certificate SHA-256 compared first and identical; data kept) after the user's `.pcda` backup and OK. (1) *Startup repair:* the database cannot be read on a release build (not debuggable), so the proof is the UI: before the first launch the ledger held 2 months; afterwards Pay Audit read "32 months audited · 0 issues" and the dashboard "32 payslips", Pay Audit unchanged. (2) *Restore of the existing v2 file on the minified build* (`PayslipMax-backup (2).pcda`, 32 payslips, REPLACE): 32 payslips and "32 months audited" afterwards, i.e. the restore rebuilt the ledger. (3) *v3 export then REPLACE restore:* done by the user on a 4-payslip state (they had imported 4 payslips, exported, restored): 4 payslips came back. This proves the minified v3 encode and decode pair; it does **not** prove the 32-month case, and the ledger-month count of that state was not read. An early reading of "4 payslips" looked like data loss and was the user's own test state, not a bug. Logcat across all steps: no FATAL, `SerializationException`, `ClassNotFoundException` or `NoSuchMethodError`. No repository code changed in this phase; the open items are in Phase 7, "From Phase 6" of the clean-up plan.
- **Backup clean-up, Phase 7 of 7 (DONE 2026-10-02): carry-overs closed.** (1) *Ledger refresh:* `refreshAuditHistory()` rebuilds the audit history from the corrected payslips after a saved correction and after a re-parse (a failure is logged and never fails the save); `PayslipViewModelAuditRefreshTest` (commonTest, also on Native), two of three red first on real assertions. (2) *Native Room rollback:* `PayslipBackupServiceRoomIosTest` runs the production schema in an in-memory Room database on iOS; REPLACE empties every table and a failing last write rolls the whole restore back (mutation-checked by removing `@Transaction`). (3) *Device:* v3 export and Replace restore with the 32 months worked on the minified release; the debug build on the Pixel showed the Koin-provided seed section, seeded 3 synthetic months (35 payslips) and removed them (32), with no FATAL; the release build was then reinstalled and the backup restored (32 payslips, 32 months audited). Decisions: letters keep their placeholders (no officer name); no push or PR for now. Gate: ktlint, `check`, corpus classes re-run, iOS 723 + 384 with link, release build, strict audit. Parked: singleton migrations, localising the "newer version" message.


---

### Entry points revised (2026-10-02, user decision)

Pay Audit is a Premium feature, and the home screen must not carry Premium content. This supersedes the Phase 3 entry points:

- **Removed:** the Dashboard banner (`DashboardAuditBannerCard`) and the payslip detail "Audit this month" card (`ReplicaAuditActionCard`), with their strings, parameters and tests. `PayAuditEntryCard`/`PayAuditEntryHost` (the Insights teaser card) were removed earlier the same day.
- **Single entry:** a "Pay Audit" ribbon, first in Insights → Premium Report → Premium Financial Tools, shown to Premium users only. It carries a findings badge (`payAuditBadgeLabel`, hidden on a clean month) fed by `rememberPayAuditFindingsCount`, the same `PayAuditViewModel` pipeline as the screen.
- **Free users** see Pay Audit only as the "Pay Audit & Anomaly Detection" line in the locked Premium hub and in Settings → Premium Features.
- **Tests:** `DashboardHasNoPayAuditEntryTest`, `PayslipReplicaHasNoPayAuditEntryTest`, `PremiumToolsSectionBadgeTest`, `PayAuditRibbonBadgeTest`.

### Not ported from `pay_audit_1.0` (and why)

| Piece | Why not |
|---|---|
| Situation tiles, mission presets, "Unclaimed ₹" counter, 18 % penal hazards, collision auditors | Already dropped in 09's plan as needing user input or lacking a verified authority; the audit confirmed false figures. |
| CEA multi-month check by financial year | Needs the number of children (user input). A missing claim isn't a proven underpayment. Revisit as a reminder, not a finding, if users ask. |
| Forfeiture/deadline tracker | Deferred in 09's plan until users ask. |
| DSOP ₹5 lakh check | Already in `feature/pay-audit` (`DsopRoomCalculator`, `DsopComplianceAuditor`). |
| CI "intelligence watchdog" workflow | Not needed for the app. Would need a separate network and security review. |

### After validation (moat work, not scheduled; gated on Phase 1 results and 3–5 real officers)

- Re-check earlier months automatically when a new payslip arrives: held (pending) findings get resolved, or a later arrears credit closes a gap. This is the harder P7-17b option.
- Claim lifecycle and a "₹ recovered" figure, closed when a later payslip shows the arrears.
- An evidence-pack PDF attached to the representation letter.

### Critical files (on `feature/pay-audit`)

- **Engine:** `shared/src/commonMain/.../insights/` (`DeterministicIntelligenceEngine`, the auditors, `PayAuthorities`, `PayAuditFindingTypes`, `RepresentationDraftTypes`) and `insights/timeline/`.
- **UI:** `composeApp/.../ui/screens/PayAudit*.kt`, `DashboardScreen.kt`, `PayslipReplicaDetailScreen.kt`, `ui/theme/PayAuditStrings.kt`, `App.kt`, iOS `MainViewController.kt`.
- **Onboarding:** `shared/.../onboarding/` (port from 1.0).
- **Tests:** `shared/src/androidUnitTest/.../insights/*CorpusTest.kt`, composeApp `PayAudit*Test.kt`.

### Verification

1. Each phase: the full Gradle gate, iOS tests and link, ktlint, and the size audit.
2. The four corpus tests stay green with no assertion loosened.
3. The minified release on the Pixel at Phases 0, 2, 3 and 4. iOS simulator at Phase 4.
4. Phase 1: every finding on 23 real months reviewed by you, with zero false findings.

## Deferred / dropped

- **Deferred until users ask:** situational tiles / 3-tier matrix, claim/LTC/TA rules (~300 of the 378), deadline trackers.
- **Dropped:** the "unclaimed ₹" counter, personas, the 18% penal-interest hazard (no verified authority), the prototype HTML.
- **Dropped (Phase 10):** the licence-fee/accommodation rent-bracket rule (P7-15) — a real citing circular
  exists (PCDA(WC) Circular No. E/II/161/R&A/Misc, 13.09.2022) but its plinth-area rate table is embedded
  as an image annexure, not extractable text; modeling only the one rate found would misclassify most
  officers. Revisit if the annexure's contents (or an equivalent verified table) become available.
