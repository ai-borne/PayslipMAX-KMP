# Leave Bank — Phase Plan

Status: agreed 2026-10-21; Phases 1–2 done, Phase 3 next. Branch: `feature/leave-bank` (off `main` @ `21437c8b`).
This file is the plan; phase status is tracked in the table below.

## Context

PCDA(O) December statements (Dec 2022 onward) carry a "Leave Accumulation Details" page: one row per
Part II order, giving annual leave accrued (ACCLVE) or encashed with LTC (LVELTC), with a running total.
Officers can encash up to 300 days at retirement, so this balance is worth ~₹20–25 lakh. Today the app
ignores the page, and Retirement Calculators assumes 300 days. Goal: parse the page once, in shared
code, and show it in three places: a free table on the payslip, a Premium Leave bank card in Retirement
Calculators that pre-fills the leave days, and a Premium Smart Insight when the balance is at the cap.

Facts established before planning:
- Page present in 4 corpus fixtures: `12_dec_2022/2023/2024/2025`. Absent in the other 135.
- Two layouts. 2022 has `SrNo`, `dd.mm.yyyy` dates, order numbers like `0446/2021`, and a trailing
  "Leave" column. 2023+ has `dd/mm/yyyy` and no SrNo. On LVELTC rows the first number may be blank or `0`.
- PDFBox (Android) and macOS PDFKit (iOS) produce identical row-per-line text for the page (checked on
  the real Dec 2022 and Dec 2025 PDFs). One parser over `TokenizedPayslip.fullText` works on both.
- Rows differ between years (Dec 2022 lists 3 LVELTC rows, Dec 2023 lists 0, Dec 2024/25 list 2).
  The latest statement is the only source we trust; years are never merged.
- PCDA totals are shown as printed and never recalculated.
- `CorpusFixtures.diff` compares only officer, summary, earnings, deductions and tax, so adding a field
  to `ParsedPayslip` cannot break the 139/139 gate.
- `ParsedPayslip` is stored as encrypted JSON (`EncryptedPayslipEntity`, strict `Json`). A new field
  with a `null` default decodes from old rows, so no Room migration and no schema bump.

## Owner decisions (locked 2026-10-21)

| Topic | Decision |
|---|---|
| Paywall | Table + balance free (payslip view). Insights Premium under existing `FeatureGate.RETIREMENT_CALCULATORS`. No new gate. |
| Payslip teaser | Shows the rupee value. Tap → Retirement Calculators (Premium) or the upgrade sheet (free). |
| Calculator | Leave days pre-filled `min(balance, 300)`, editable, labelled with the source statement. Falls back to 300. |
| Over cap | Show the printed figure (301) + "1 over cap". Encashment uses 300. |
| Rupee basis | Current Basic + DA (+NPA), via `RetirementCalculatorEngine.leaveEncashment`. |
| Table | Date · Type · Days · Total. Order no./date on row tap. |
| Smart Insight | At/over-cap warning ships in v1. |
| LTC | "X of 60 days used" only. No copy about the effect on retirement encashment. |
| Out of v1 | Cross-year diff of vanished rows, gap-year flags, re-parse prompt for old imports. |
| Kill switch | `LaunchFlags.LEAVE_BANK_ENABLED`. Hides all three surfaces; the parser always runs. |
| Model | Nullable `leaveAccumulation` on `ParsedPayslip`. |
| Release | Next minor release, both platforms. |

## Architecture

```
fullText ──► LeaveAccumulationExtractor (shared/parser, pure)  ──► ParsedPayslip.leaveAccumulation
                                                                          │ (encrypted JSON, backups)
payslips ──► LeaveBankCalculator.summarize (shared/insights, SSOT) ──► LeaveBankSummary
                                                                          │
PayslipViewModel.leaveBankSummary: StateFlow (one derivation) ──► Payslip view │ Ret. Calculators │ Smart Insight
```

- **Model** (`shared/.../domain/LeaveAccumulation.kt`): `@Serializable LeaveAccumulation(entries)` and
  `LeaveEntry(casDate: String /*ISO*/, casCode: String /*as printed*/, days: Int, totalDays: Int?, orderNo: String, orderDate: String /*ISO*/)`.
  For ACCLVE, `days` = Accu AL and `totalDays` = Total Accu AL. For LVELTC, `days` = days encashed and `totalDays` = null.
- **Cas codes SSOT**: `LeaveCasCode` object (`ACCRUED = "ACCLVE"`, `LTC_ENCASHED = "LVELTC"`). Any other code is kept as printed, never dropped.
- **Extractor** (`shared/.../parser/LeaveAccumulationExtractor.kt`): line-based, `indexOf`/`split` only, no lookaround or backreference regex.
  - Find "ACCUMULATION DETAILS" (case-insensitive), then the column-header line containing "Cas Date".
  - Parse rows: optional leading SrNo, a date token, numbers, an all-caps code, order no., order date, optional trailing column.
  - Stop at the first non-row line.
  - **Fail closed**: any malformed row inside the section → `null`. Never return a partial table.
  - Bounds against hostile input: ≤ 200 rows, numbers ≤ 4 digits, valid calendar dates.
- **Pipeline hook**: in `SharedParsingPipeline.parse`, after `assembleAndValidate`:
  `finalParsed.copy(leaveAccumulation = runCatching { LeaveAccumulationExtractor.extract(tokenized.fullText) }.getOrNull())`.
  It reads the raw `fullText`, not the cleaned text. It never touches earnings, deductions, `needsReview`, Gemma or SchemaValidator.
- **Summary SSOT** (`shared/.../insights/LeaveBankCalculator.kt`):
  - `summarize(payslips): LeaveBankSummary?` takes the latest payslip that *carries* leave data (not hardcoded to December), and the latest payslip overall for pay.
  - Output: `balanceDays` (printed), `asOfDate` (latest ACCLVE `casDate`), `sourceMonth/Year`, `daysOverCap`, `encashableDays`, `ltcDaysUsed`, `ltcCapDays`, `valueAtCurrentPay`.
  - Add `LTC_ENCASH_MAX_DAYS = 60` next to `LEAVE_MAX_DAYS` in `tax/PensionRuleKnowledgeBase.kt`.
- **ViewModel**: `PayslipViewModel.leaveBankSummary = uiState.map { summarize(it.payslips) }.distinctUntilChanged().stateIn(WhileSubscribed(UI_STOP_TIMEOUT_MS))`.
  Derived once, not copied into the 4+ `payslips = …` update sites.
- **Kill switch**: `LaunchFlags.LEAVE_BANK_ENABLED = false` plus `isLeaveBankEnabled()` (pattern: `GuideAvailability.kt`).
  Flipped to `true` in Phase 6, in its own commit.
- **Strings**: new `ui/theme/LeaveBankStrings.kt`. **Colors**: `MaterialTheme.colorScheme`/`CardTint`/`AppDimensions` tokens only.

Reused, not rebuilt: `RetirementCalculatorEngine.leaveEncashment`, `PensionRuleKnowledgeBase.LEAVE_MAX_DAYS`,
`ExpandableDetailSection`, `FlatBorderedCard`/`CardTint`, `PayslipUpgradeSheet`, `gateForScreen`,
`NavBridge.navigateToDetail` (precedent: Pay Audit → GuideCard, E7), `rememberHasAccess`, `ParserUtilsIosPerfTest` pattern,
`CorpusFixtures` loaders, `reparseAllPayslips` (owner re-parses manually; no prompt).

## Rules applied to every phase

- **Build green at phase end, or no next phase.** Gate: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`
  + `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` + `./gradlew iosSimulatorArm64Test`.
- **TDD**: tests are written first in each phase. 100% pass, none skipped. Corpus stays 139/139.
- **300 LOC**: `python3 scripts/check_tech_debt_limits.py --strict <touched .kt>` every phase.
  A file may exceed 300 only with an SRP comment saying why it isn't split. None are expected.
  `PayslipReplicaScreen.kt` (284) is split before it grows (Phase 4).
- **Strings and colors**: no literals in composables. Every string goes in `LeaveBankStrings`; every color is a theme token.
- **Security**: untrusted PDF text is bounded and parsed with linear algorithms only. Leave values never go into `Logger`,
  `CrashReporter` or exceptions (`TelemetrySanitizer` untouched). Data stays in the encrypted blob. gitleaks runs via the hooks.
  Each phase ends with a short OWASP/PII review of its diff.
- **R8 every phase**: `./gradlew :composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true` green.
  From Phase 1, also grep `composeApp/build/outputs/mapping/release/mapping.txt` for the `LeaveAccumulation`/`LeaveEntry`
  `$$serializer` classes to prove they survive. Add a keep rule only if one is missing, and keep it narrow.
  Phase 6 proves it on a release APK on the device.
- **ADB (Pixel 9)**: phases 3–6 start with a nudge to connect the Pixel 9 and confirm `adb devices`.
  The `verify` skill recipe applies: back up before any uninstall, `-i com.android.vending` for release installs,
  uiautomator dump for taps. During device walks before Phase 6, `LEAVE_BANK_ENABLED` is flipped locally,
  and `git diff` must show it reverted before the commit.
- **Commits**: one commit per phase, made after the owner accepts that phase's summary. Push/PR only in Phase 6, on request.
- **Phase Summary** at each end, with (1) tech debt incurred, (2) exact steps taken to resolve it, and
  (3) build/test/R8 confirmation with commands and counts. The next phase starts only after acceptance.

## Phases

| Phase | Status | Commit |
|---|---|---|
| 0. Branch, plan doc, baseline | done (signed off 2026-10-21) | `71101f32` |
| 1. Model + extractor + pipeline hook | done (signed off 2026-10-21) | `43325a1f` |
| 2. Summary SSOT + test fixture | done (2026-10-21) | `4fb36624` |
| 2a. Pre-Phase-3 follow-up: leave/DA/NPA formula, Day-1 labels | done (2026-10-21) | this commit |
| 3. ViewModel + kill switch + Leave bank card | not started | — |
| 4. Payslip view section + teaser | not started | — |
| 5. Smart Insight | not started | — |
| 6. Release hardening, iOS, flip, PR | not started | — |

### Phase 0: Branch, plan doc, baseline
- Create `feature/leave-bank` off `main`. Add `docs/Plan/11_LeaveBank_PhasePlan.md` (this plan).
- Baseline, recorded in the summary: full `check` green, corpus 139/139, `iosSimulatorArm64Test` green,
  `minifyReleaseWithR8` green, plus current line counts of every file to be touched.
- Exit: doc committed; baseline numbers captured.
- Baseline recorded 2026-10-21 on `main` @ `21437c8b`:
  - `check -x iosX64Test -x iosSimulatorArm64Test`: green.
    - shared debug 1145/1145, release 1145 with 1 skip (pre-existing: `GemmaModelPathsAndroidTest` is debug-only, `assumeTrue(isDebugBuild())`).
    - composeApp debug 1028/1028, release 1002/1002.
    - Corpus: `TokenParseCorpusRegressionTest` and `IosTokenParseCorpusRegressionTest` pass.
  - `iosSimulatorArm64Test`: shared 1100/1100, composeApp 627/627. `linkDebugFrameworkIosSimulatorArm64`: green.
  - `minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true`: green.
  - Line counts: `PayslipReplicaScreen.kt` 284, `PayslipViewModel.kt` 279, `SharedParsingPipeline.kt` 224, `Model.kt` 181,
    `RetirementCalculatorsScreen.kt` 152, `SmartInsightsBuilder.kt` 140, `InsightsState.kt` 108,
    `RetirementCalculatorsComponents.kt` 75, `PayslipReplicaDetailScreen.kt` 70, `PensionRuleKnowledgeBase.kt` 46,
    `LaunchFlags.kt` 40, CLAUDE.md 158.

### Phase 1: Model + extractor + pipeline hook (shared)
Tests first:
- `commonTest/.../parser/LeaveAccumulationExtractorTest.kt`, synthetic text, no real data:
  - Each layout: 2023+ and 2022 (SrNo, `dd.mm.yyyy`, `0446/2021`, trailing column).
  - LVELTC with a blank first number and with `0`.
  - Mixed-case heading; CRLF line endings; noise lines before the section.
  - Section absent → null. Malformed row → null. Unknown cas code kept as printed. Row cap enforced.
- `androidUnitTest/.../parser/LeaveAccumulationCorpusTest.kt` (via `CorpusFixtures`):
  - Dec 2022 → 15 rows / balance 222 / LTC 30.
  - Dec 2023 → 13 / 252 / 0.
  - Dec 2024 → 16 / 282 / 20.
  - Dec 2025 → 17 / 301 / 20.
  - All other 135 → `null`. This is the false-positive guard.
- `iosTest/.../parser/LeaveAccumulationIosPerfTest.kt`, copying the `ParserUtilsIosPerfTest` bounds and linearity check.
- Serialization test: a payslip with leave data round-trips through `toEncryptedEntity`, and old JSON without the
  field still decodes. `applyCorrections` keeps the field.

Then implement the model, `LeaveCasCode`, the extractor and the pipeline hook (Architecture above).
Docs: add a "post-Tier-7 side extractor" section and a changelog entry to `docs/AI_INSIGHTS_PIPELINE.md`.
Exit: all of the above green, corpus 139/139, R8 + mapping grep green.

### Phase 2: Summary SSOT (shared) + test fixture
- First, add `shared-test-fixtures/.../testing/SyntheticLeaveAccumulation.kt`. It builds payslips with and without
  leave data, at or below/over the cap.
- Tests first, `commonTest/.../insights/LeaveBankCalculatorTest.kt`:
  - Picks the latest leave-bearing statement and the latest pay, even when they are different payslips.
  - Balance 301 → over-cap 1, encashable 300. Balance 222 → 0 / 222.
  - `asOfDate` is the latest ACCLVE `casDate`, not the statement month.
  - LTC is summed from LVELTC rows only.
  - Value equals `leaveEncashment(basic, da, encashable, npa)`.
  - No leave data → null. Ties and out-of-order input are handled.
- Implement `LeaveBankCalculator` + `LeaveBankSummary`, and add `LTC_ENCASH_MAX_DAYS`.
- Exit: green gate + R8.
- Done 2026-10-21. Rules the plan left open, now pinned by `LeaveBankCalculatorTest`:
  - "Latest" orders by `(year, monthNum)`, then `file`, so same-month duplicates resolve the same way in any input order.
  - Balance = printed `totalDays` of the latest ACCLVE row by `casDate`; same-date ties go to the later `orderDate`, then printed order.
  - If the latest leave-bearing statement has no ACCLVE row, the summary is `null`. It never falls back to an older year.
  - `LeaveAccumulationCorpusTest` now reads balance and LTC through `summarize` (its private balance helper was a second copy of the rule).

### Phase 3: ViewModel + kill switch + Retirement Calculators Leave bank card (Premium surface)
Nudge: connect the Pixel 9.
- Tests first:
  - ViewModel test: `leaveBankSummary` emits from payslips and updates after import or delete (existing ViewModel test harness + `FakePayslipDao`).
  - Pure UI-model test `LeaveBankUiModelTest`: labels, "N over cap", "X of 60", as-of formatting, rupee formatting
    with existing formatters. This is the logic-file pattern from `PayAuditFindingsLogic.kt`.
  - `isLeaveBankEnabled()` test.
- Implement:
  - `LaunchFlags.LEAVE_BANK_ENABLED = false`, `ui/screens/LeaveBankAvailability.kt`, `LeaveBankStrings.kt`.
  - `LeaveBankUiModel.kt` (logic), `LeaveAccumulationTable.kt` (shared table composable with tap-to-expand rows,
    using `rememberSaveable`), `LeaveBankCard.kt`.
  - In `RetirementCalculatorsScreen.kt`: the card goes after `RetCalcInputsSection`. `leaveDays` starts at
    `summary?.encashableDays ?: LEAVE_MAX_DAYS`, with `remember` keyed on `(payslip, summary)`.
  - In `RetirementCalculatorsComponents.kt`: optional source-label text under the leave field.
  - `PayslipViewModel.kt` is at 279 lines (Phase 0 baseline). The `leaveBankSummary` StateFlow adds about 4 lines.
    If it would pass 290, the derivation moves to a small `LeaveBankStateHolder` the ViewModel owns, instead of growing the file.
- Device (Pixel 9, debug, flag flipped locally):
  - Re-parse all; Retirement Calculators shows 301 / 1 over cap / LTC 20 of 60 / value; the field is pre-filled 300.
  - Table expands, and a row tap shows order no./date. Light + dark.
  - Developer override "Force Free" → screen stays locked as today.
  - Process death (`adb shell am kill` while backgrounded) → state restores.
- Exit: green gate + R8 + device screenshots attached to the summary; flag reverted in the diff.
- Owner decisions 2026-10-21 (pre-Phase-3 follow-up, one commit on `feature/leave-bank` before Phase 3 starts). All done, see the status table:
  - Leave encashment = (Basic + NPA + DA on Basic + NPA) / 30 × days, in both the calculator and `LeaveBankSummary`. DA on
    Basic + NPA = printed DA × (Basic + NPA) / (Basic + MSP + NPA), using the payslip's own DA rate, with no rate table.
  - DA/DR rate = printed DA / (Basic + MSP + NPA) (was DA / Basic, which gave 64.2% instead of 58% for Dec 2025). One helper in
    `RetirementCalculatorEngine` is used by every caller.
  - Pension and gratuity: NPA added in the builder. Verified: Handbook ch. 31 para C(i)–(ii), p. 279, says reckonable emoluments are
    Pay + MSP + NPA, if any, for pension, plus DA for gratuity.
  - Day-1 corpus card: move the remaining 4 row titles and the 1 subtitle to `AppStringsPremium`, wording unchanged.
  - Leave-days hint: "(Basic + NPA + DA on them) ÷ 30 × days. MSP and DA on MSP are excluded per PCDA."
  - If the latest leave statement has no accrual row, Leave Bank shows nothing (confirmed; no fallback to an older year).
  - The same-month tie-break on `file` stays as determinism only: storage keys payslips by `dateStr` with REPLACE, so duplicates can't exist.
- Background, raised in the Phase 2 review (pre-existing code, not changed in Phase 2):
  - `RetirementPlannerResultBuilder` calls `leaveEncashment(basic, da, leaveDays)` without NPA, but `LeaveBankSummary.valueAtCurrentPay`
    includes NPA (owner decision). For an officer drawing NPA, the card and the Day-1 corpus "Leave Encashment" row would differ.
    Authority (researched 2026-10-21, owner call pending): PCDA(O) Handbook on Pay & Allowances 2023, ch. 29 para 1 (p. 269):
    "Leave Encashment payment will be equal to Basic Pay + NPA, if any, + DA thereon on the date of retirement/release";
    formula [Basic + DA + NPA (if any)] / 30 × days, max 300. Ch. 13 para 18 (p. 103): NPA "will be treated as Pay for the purpose of
    computation of Dearness Allowance, Pension … including calculation of retirement benefits". Both cite GoI MoD letter
    No. 4(10)/2017/D(Med) dt 28-09-2017. The FAQ (Dec 2025) has no retirement-encashment entry; its LTC entry uses the same
    formula ("NPA only in the case of medical officers"; "MSP is not included").
    Related finding: "DA thereon" is DA on Basic + NPA, but the payslip's DA is on Basic + MSP + NPA (handbook ch. 15, p. 130;
    corpus DA = 58.000% × (Basic + MSP) in Dec 2025). Passing the printed DA counts DA on MSP: about ₹89,900 too high at 300 days
    (58% × ₹15,500 × 10). This affects both the calculator and `LeaveBankSummary`.
  - Done ahead of Phase 3 (2026-10-21): the "Leave Encashment (300 Days)" literal in `RetirementDay1CorpusCard.kt` became
    `AppStringsPremium.retDay1LeaveEncashmentLabel(days)`. Days come from `RetirementPlannerResult.leaveEncashmentDays`, set by the new
    `RetirementCalculatorEngine.encashableLeaveDays` (0..300), which `leaveEncashment` and `LeaveBankCalculator` now also use.

### Phase 4: Payslip view section (free) + teaser
Nudge: connect the Pixel 9.
- Pre-step (300-LOC rule): move `ReplicaSections` from `PayslipReplicaScreen.kt` (284 lines) into
  `PayslipReplicaSections.kt` unchanged. Before/after device screenshots of a non-December payslip prove nothing moved.
- Tests first:
  - The section shows only when `payslip.leaveAccumulation != null && isLeaveBankEnabled()`, as a pure visibility function.
  - The teaser routes to `Screen.RetirementCalculators` when `hasAccess(RETIREMENT_CALCULATORS)`, otherwise to the upgrade sheet.
- Implement `LeaveAccumulationSection.kt`:
  - `ExpandableDetailSection`, collapsed by default. Subtitle: "As of 31 Dec 2024 · 301 days".
  - It shows *that* payslip's own table, reusing `LeaveAccumulationTable`.
  - Teaser row: "Your leave bank today ≈ ₹X". Today's value comes from the summary; the table comes from this statement.
- Wire it after `LedgerSection`. Thread `onLeaveBankClick` through `PayslipReplicaDetailScreen` → `NavBridge.navigateToDetail`,
  following the E7 precedent. Reuse `PayslipUpgradeSheet` for the free path.
- Device:
  - Dec 2025 shows the section; Jul 2025 does not; Dec 2022 shows the 2022 layout correctly.
  - The teaser opens Retirement Calculators. Back returns to the payslip (system back + edge swipe).
  - With "Force Free", the teaser opens the upgrade sheet. Tab re-tap from the nested screen behaves like the other details.
- Exit: green gate + R8 + screenshots; flag reverted.

### Phase 5: Smart Insight (Premium)
Nudge: connect the Pixel 9.
- Tests first, `SmartInsightsBuilderTest`:
  - The card appears when `balanceDays >= 300`, at the cap included, because new accrual lapses there. It stays absent below 300.
  - It appears on the first-statement path too.
  - `gate == RETIREMENT_CALCULATORS`, and the target is Retirement Calculators.
  - The existing "no CTA twice" dedup hides the Retirement-calculator Recommended Action while the card shows.
  - Flag off → no card.
- Implement:
  - `InsightsState.leaveBank: LeaveBankSummary?`, fed from `viewModel.leaveBankSummary`.
  - A `leaveCapCard()` in `SmartInsightsBuilder.kt` (140 lines; if it nears the limit, split into `LeaveInsightCards.kt`).
  - Copy in `LeaveBankStrings` that states only the lapse fact.
- Device: the insight shows for 301; it is locked under "Force Free" and opens Retirement Calculators under Pro.
- Exit: green gate + R8 + screenshots; flag reverted.

### Phase 6: Release hardening, iOS, flip, PR
Nudge: connect the Pixel 9.
- Flip `LEAVE_BANK_ENABLED = true` in its own commit.
- Android release on the Pixel 9:
  - `assembleRelease -PallowPlaceholderGemmaModel=true`. Back up first, then install with `-i com.android.vending`.
  - Re-parse all, then walk all three surfaces. This proves R8 kept the serializers and the UI. Then `check_r8_guide.py` (unchanged).
- iOS:
  - `linkDebugFrameworkIosSimulatorArm64` + full `iosSimulatorArm64Test` (includes the perf test).
  - An iPhone walk checklist goes to the owner, covering native back from Retirement Calculators to the payslip and a PDFKit-parsed Dec payslip.
    iOS UI can't be driven from here.
- Docs: plan status → done; changelog entry in `AI_INSIGHTS_PIPELINE.md`; one bullet in CLAUDE.md "Feature areas" pointing to doc 11 (stays ≤200 lines).
- Pre-push hook (full gate). PR to `main` only when the owner asks.

## Risks and controls

| Risk | Control |
|---|---|
| False match on a non-leave page | Heading + "Cas Date" header + fail-closed rows; the 135-fixture null guard. |
| Kotlin/Native regex stall | No regex in the hot path; iOS perf test. |
| R8 strips new serializers | Mapping grep each phase; release APK walk in Phase 6. |
| Old imports show nothing | Accepted by the owner (re-parse manually). |
| Downgrade (old app reads new JSON with strict `Json`) | Not possible via the stores; accepted, noted in doc 11. |
| PCDA adds a new cas code | Kept as printed; Type shows the raw code; logged as a count only. |
| Copy implies a rule we haven't settled (LTC vs retirement) | Copy limited to "X of 60 used" and the cap fact. |

## Verification (end-to-end)

1. `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` (corpus 139/139 + new suites).
2. `./gradlew iosSimulatorArm64Test` and `:composeApp:linkDebugFrameworkIosSimulatorArm64`.
3. `./gradlew :composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true` + mapping grep.
4. Optional real-PDF JVM check: `PlatformPdfParserTest` with `-Dpayslip.localCorpus` on `Pay Slip Elements`.
5. Pixel 9: debug walk in each of phases 3–5; release APK walk in Phase 6.
6. Owner iPhone walk in Phase 6.
