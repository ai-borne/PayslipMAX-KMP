# Tech Debt Register — Nov 2026

Audit date: 2026-11-07, branch `feature/td-summary-card`. Each item was checked against the code, not taken from older
docs. This file is the working guide for clearing the debt: strike an item off (`- [x]`, date, commit) when its phase
closes. Older debt docs (`07_techDebt_10sep2026`, `08_TechDebt_20Sep2026`) are superseded by this one.

**Ranking.** Severity first (user or security impact), then regression risk. Execution order follows the phases
below, which put low-risk work first; the high-risk items come last, each as its own sprint.

## Regression controls (apply to every phase)

1. **Baseline first.** Phase 0 records the green state so any later failure is attributable.
2. **Characterization tests before editing an existing production file.** The test must pass on the old code first.
3. **One item per commit**, each through the pre-commit hook and the pre-push gate (full Android + common, corpus,
   iOS tests, gitleaks).
4. **Parser touches** also need: 139/139 `TokenParseCorpusRegressionTest`, `PhantomFreeCorpusInvariantTest`,
   iOS parity + `iosSimulatorArm64Test` (Native perf).
5. **Release-only risk** (R8, dependencies, JNI): `./gradlew :composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true`
   plus a device smoke test; green debug tests do not prove the release build.
6. A phase closes only with a Phase Summary (debt incurred, how it was resolved, build + tests green).

## Phase 0 — Baseline

- [x] 2026-11-07, commit 40e600c3 (no code changed). Recorded green: `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`
      (exit 0; includes `TokenParseCorpusRegressionTest` 0 failures / 0 skipped, `PhantomFreeCorpusInvariantTest` 0 failures),
      `iosSimulatorArm64Test` (exit 0), `:composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true` (exit 0),
      `check_tech_debt_limits.py --strict` over all tracked `.kt` (1434 files, 0 violations; the earlier "622" counted a
      narrower set).

## Phase 1 — Tests only (no production code changes; zero regression risk)

| # | Item | Severity | Risk | Evidence |
|---|---|---|---|---|
| 7 | Pin parser thresholds with characterization tests: `COLUMN_MERGE_GAP_PT = 85f` (tight 4-column row, < 85pt peaks) and `RawLabelNoiseFilter` 60 chars / 8 words | Medium | None | `engine/ColumnBoundaryDetector.kt:20`, `parser/RawLabelNoiseFilter.kt:17-18`; no direct test today |
| 10 | Direct unit tests for code covered only indirectly: `DaArrearsWindow` (changed in 907b42ac), `WholeWordScanner`, `TokenGridAssigner`, `Part2RowPass`/`PageRestore`, `PreClaimChecks` | Medium | None | symbol-level grep of `*Test` source sets, 2026-11-07 |
| 11 | iOS-native Room migration test (iOS relies on shared migrations only) | Medium | None | `docs/AI_INSIGHTS_PIPELINE.md` §12, Room destructive-fallback entry |

- [x] 7 (2026-11-07) · - [x] 10 (2026-11-07) · - [x] 11 (2026-11-07) — tests only, no production file changed.
  Added: `ColumnBoundaryDetectorTest`, `RawLabelNoiseFilterTest` (item 7); `DaArrearsWindowTest`, `WholeWordScannerTest`,
  `TokenGridAssignerTest`, `Part2RestoreRowsTest` (Part2RowPass), `Part2PageRestoreTest` (PageRestore), `PreClaimChecksTest`
  (item 10); `IosPayslipDatabaseUpgradeTest` + simulator env var in `shared/build.gradle.kts` (item 11). Each threshold/edge test was
  mutation-checked (changed the constant or comparison in production, saw the test fail, reverted).

## Phase 2 — PII in logs (highest severity, very low risk)

| # | Item | Severity | Risk | Evidence |
|---|---|---|---|---|
| 1 | Android extractor prints **every token's text** (names, account, amounts) to `System.out` → logcat, in release, on every import. No `-assumenosideeffects` rule. iOS does not do this. | **High** | Very low (output only; nothing reads the dump — checked `CorpusCaptureTest`, `scripts/`) | `shared/src/androidMain/.../parser/AndroidTokenExtractor.kt:35-43` |

Steps: (a) a JVM test builds a PDF in-test with PDFBox, runs `extractTokenized`, asserts nothing reached `System.out`
(fails on current code); (b) delete the dump; (c) add a guard to `check_tech_debt_limits.py` banning `println` in
`*Main` source sets outside `logging/Logger.kt`.

- [ ] 1

## Phase 3 — Low-risk cleanups (one commit each)

| # | Item | Severity | Risk | Evidence |
|---|---|---|---|---|
| 26 | Hardcoded UI strings (rule 4): chooser title `"Open PDF"`, `contentDescription = "Edit…"`, `"Import Payslip"` → `AppStrings` | Low | Low — keep the exact text, UI tests may find nodes by it | `MainActivity.kt:135`, `LedgerRowComponents.kt:95`, `DashboardUploadArea.kt:41` |
| 3 | `e.printStackTrace()` and two Swift `print("Error …: \(error)")` bypass `Logger`/sanitizer; `Logger.w/e` also `println` in release (fixed strings by design, so low) | Low | Very low | `MainActivity.kt:137`, `iosApp/iosApp/ContentView.swift:76,224`, `logging/Logger.kt:30,42` |
| 19 | Function-wide `@Suppress("DEPRECATION")` on 4 composables hides any future deprecation inside them — narrow to the statement. (The 4 in `AndroidAppIntegrityChecker`/`PayslipApplication` are legitimate SDK-version fallbacks: leave them.) | Low | Low | `App.kt:124`, `GuideTab.kt:25`, `PayslipReplicaDetailScreen.kt:30`, `RepresentationScreen.kt:28` |
| 14 | `PayslipMaxTests` target has `IPHONEOS_DEPLOYMENT_TARGET = 27.0` (app is 18.6) — XCTests cannot run below iOS 27 | Low | Low; verify with an Xcode build | `iosApp/*.xcodeproj/project.pbxproj:321,348` |
| 9 | 7 `!!` in production, all guarded by an immediately preceding check — replace with `mapNotNull`/`filterNotNull`. Do after Phase 1 tests exist. | Low | Low–medium (Pay Audit, Part II) | `FinancialIntelligenceRepository.kt:226-228`, `EntryForm.kt:110`, `OrderShelf.kt:21`, `BankViewModel.kt:209`, `GuideStrings.kt:142` |

- [ ] 26 · - [ ] 3 · - [ ] 19 · - [ ] 14 · - [ ] 9

## Phase 4 — Docs and repo hygiene (no code risk)

| # | Item | Severity | Risk | Evidence |
|---|---|---|---|---|
| 24 | `AI_INSIGHTS_PIPELINE.md` (the parser SSOT) has drifted: 8 "52 fixtures" mentions (corpus is 139), "`PCDA_EARLY_DUAL_COL` has zero fixtures" (corpus starts Mar 2015), "neither store enrolled" (app is live on both) | Medium | None | `docs/AI_INSIGHTS_PIPELINE.md` lines 460, 686, 710, 721-728, 747, 758; §12 |
| 22 | Orphaned `pcdao_ai_prototype.html` (1,705 lines, outside the size audit). **Keep `public/index.html`** — it is the Firebase Hosting root. `__pycache__/`, `dist/` are gitignored (local only). | Low | None | owner approved deletion 2026-11-07 |
| 21 | Merged branches: local `feature/backup-hardening`, `feature/guide-hospital-regularisation`, `feature/orders-bank`; remote `origin/feature/orders-bank`. Keep `origin/feature/part2-orders` (frozen archive, not merged). | Low | None | owner approved local + remote deletion 2026-11-07 |

- [ ] 24 · - [x] 22 (2026-11-07, deleted) · - [x] 21 (2026-11-07, deleted)

## Phase 5+ — High-risk items, each its own planned sprint

| # | Item | Severity | Risk | Controls |
|---|---|---|---|---|
| 17 | Dependencies behind Kotlin 2.3.20: coroutines 1.9.0, Koin 4.0.0, serialization 1.7.3, Room 2.7.2, lifecycle 2.8.3, Firebase BoM 33.7.0; `pdfbox-android` 2.0.27.0 is a long-unmaintained fork (versions not checked against latest releases) | Medium | High (R8, iOS link, Room KSP) | one dependency per phase; minified build + device smoke; `PayslipDatabaseUpgradeTest` for Room |
| 4 | `PayslipViewModel` is a hub: 276 lines + 13 extension files (~1,100), 11 constructor collaborators (5 default to `provide*()` service location), 27 inline FQNs. Newer features already have their own ViewModels (11 others) — follow that pattern. | Medium | High (import, dashboard, billing) | one feature slice at a time behind characterization tests |
| 18 | `-keep class com.google.ai.edge.litertlm.** { *; }` — doc 05 §7 proposed narrowing; never done | Low | High (release-only, device-only Tier 6 crash, invisible to corpus) | only with a release-build Tier 6 device smoke; otherwise record "accepted" |
| 2 | Page-2 earnings spill-over: both extractors scan one table page (`AndroidTokenExtractor.kt:28`, `IosTokenExtractor.kt:31`); doc estimates ~10% of months | High (silent missing pay) | **High** (changes token input for every document; phantom filters tuned to one page) | **Owner decision 2026-11-07: kept last.** A previous attempt failed and parsing works today. Start only when explicitly reopened, with spill fixtures measured from the local corpus first. |

- [ ] 17 · - [ ] 4 · - [ ] 18 · - [ ] 2

## Phase 6 — Carry-over from Phase 1 (found while writing the tests; production changes, so not done in a tests-only phase)

| # | Item | Severity | Risk | Evidence |
|---|---|---|---|---|
| 27 | `DaArrearsWindow.minimumMonths`/`closestMonths` document "null if none", but `candidateMonths` is never empty for a month 1-12 (the smallest candidate is `(m-1)%6`+6 or more). The null branch is unreachable, and `candidateMonths(0)` returns `[5, 11]` instead of rejecting a non-month. Decide: tighten the contract (non-null return, `require(monthNum in 1..12)`) or keep it and fix the KDoc; callers in Pay Audit must be read first. | Low | Low–medium (Pay Audit) | `insights/DaArrearsWindow.kt:19-34`; pinned by `DaArrearsWindowTest` |

- [ ] 27

## Closed / accepted — no work

| # | Item | Why |
|---|---|---|
| 5 | Companion-object closure slots (Gemma installer/engine, review bridge) | Closed, justified allowlist in `scripts/stateful_singleton_guard.py`; Swift interop needs a static entry point |
| 6 | Grammar families outnumber strategy implementations | Deliberately deferred until corpus evidence of era quirks (pipeline doc §12) |
| 8 | No general Native regex-perf guard | Pre-commit flags lookaround/backreference regex in `commonMain`; `TaxParserUtils` lookarounds are gone |
| 12 | Corpus can't model real-world layout noise | Process, not code — add noise variants when real cases appear |
| 13 | No 2015–2021 corpus coverage | Resolved: corpus covers Mar 2015–Apr 2026 (stale doc text tracked under 24) |
| 15 | Release Gemma model is a local file | Accepted: fixed model path for every `bundleRelease` |
| 16 | Unused GCS bucket, `GEMMA_CACHE_KEY`/`HF_TOKEN` secrets | Owner console task, deliberately not scripted |
| 20 | 13 main files at 270–299 lines | Forecast, not debt; split when next touched (surgical-changes rule) |
| 23 | TODO in vendored `iosApp/Vendor/LiteRTLM/swift/Message.swift:89` | Third-party; note when upgrading LiteRT-LM |
| 25 | Git-history PII rewrite, FAQ watcher manual, 37 unverified cards, iPhone walk | Owner decisions, tracked elsewhere |
