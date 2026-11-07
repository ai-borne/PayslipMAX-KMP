# Part II Orders Bank — Execution Plan, part A: rules and Phases 0 to 2

Status: draft for owner sign-off, 2026-10-30. Nothing is built. Builds on `13_OrdersBank_PhasePlan.md` (scope, architecture, cross-cutting
checks); part B (`13b_OrdersBank_Execution_P3-P6.md`) holds Phases 3 to 6 and the traceability table. Phases run strictly in order, one
at a time. The Phase Summary at the end of each is the only way to the next one.

## 1. The rules, and how this project enforces each

| Rule (owner's brief) | Enforced here by | Proof in each Phase Summary |
|---|---|---|
| 1. Every phase ends with a successful build; no next phase on a failing build | The gate in section 3, run in full at phase end | Commands, exit codes, test counts |
| 2. TDD; 100% of tests pass | Tests written first and shown red for the right reason; unit, integration and platform tests listed per phase; no `@Ignore`, no new skip | Counts before and after; any pre-existing skip named |
| 3. MVVM, SOLID, DRY, SSOT; 300 lines | Module map in plan 13; `checkFileSizes` and `check_tech_debt_limits.py --strict` hard-fail a `.kt` over 300; composables at most 50 | Line count of every touched file |
| 4. No hardcoded strings or colours | All copy in `OrdersBankStrings.kt` and `LearnStrings.kt`; colours as tokens in `Theme.kt`; the audit script checks `ui/` | Literal grep of the diff: clean |
| 5. Security | Threat checklist (plan 13, item 6), the `security-review` skill on each code phase's diff, gitleaks in the hooks, the manifest check | Findings and fixes, or "no findings" |

Conflicts between your brief and the project, and which wins:
1. **The 300-line exception.** The brief allows a longer file when SRP needs it, with a comment. Our gates fail any `.kt` over 300 with no
   exemption, so the stricter rule wins: no Kotlin file uses the exception. A non-Kotlin file (Python, JSON, Swift) may exceed 300 only with
   a header comment saying why it cannot be split; none is planned.
2. **"String resources".** This is a Kotlin Multiplatform app with one copy SSOT per platform-neutral object (`AppStrings.kt` pattern), not
   Android XML. The new string objects are that SSOT. The one platform-mandated copy is iOS `NSCameraUsageDescription`, checked against it by a test.
3. **"100% pass".** The shared debug suite has one known pre-existing skip on the old branch. Phase 0 records the real baseline of this
   branch; a phase may not add a skip, and the summary names any pre-existing one.

## 2. The loop every phase follows

1. Read every file the phase touches and its nearest pattern (named in the phase).
2. Write the tests (RED). Run them and show that they fail for the missing behaviour, not for a compile error.
3. Implement the smallest code that passes (GREEN). No speculative code.
4. Break each guarded rule on purpose (caps, status derivation, migration, validation, privacy), see its test fail, restore the file and check
   its sha256. A rule no test can fail is a gap; fix the test.
5. Run the gate (section 3). 6. Run the `security-review` skill on the diff and fix findings. 7. Sweep for tech debt (unused code, duplicate
   logic, a literal, a file near 300); fix all of it in this phase. 8. Write the Phase Summary and stop. 9. After the owner accepts, one commit
   (hooks on, no bypass) with the `Co-Authored-By` trailer. Nothing is pushed without the owner's word.

Phase Summary (the brief's three items plus the project's proofs):
```
Phase N — <name>                                         Commit: <sha after acceptance>
1. Tech debt incurred:   each item, or "none"
2. Resolved by:          exact steps, file:line, for every item above; nothing deferred
3. Build and tests:      gate commands + exit status; tests per module (debug / release / iOS), before to after; corpus 139/139
   plus: deliberate breaks and the test that caught each; line count of every touched file, largest first;
   string/colour literal grep; security-review result; R8 and manifest output; owner items for the next phase
```

## 3. The gate (phase end; every command must exit 0)

1. `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` (debug and release unit tests, ktlint, `checkFileSizes`, corpus).
2. `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64`, then `./gradlew iosSimulatorArm64Test`; XCTest alone, never beside Gradle.
3. `python3 docs/Plan/rule_cards/tools/refresh.py --check --allow-no-sources` (check only: a plain `refresh.py` rewrites date stamps in four
   generated files, which is churn outside the phase; run it, and commit its output, only in a phase that changes the Guide content).
4. `python3 scripts/check_tech_debt_limits.py --strict <touched .kt>`, `python3 -m unittest discover -s scripts/tests`,
   `python3 -m unittest discover -s docs/Plan/rule_cards/tools`, `check_schema_immutability.py`.
5. `./gradlew :composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true`; in phases marked R8+ also `assembleRelease`,
   `scripts/check_r8_guide.py` and `scripts/check_merged_manifest.py`.
6. With the flag on: `python3 scripts/run_flag_on_tests.py` (from Phase 0, until the flag is flipped in the repo).

Pins that must not grow (lines on main today): `App.kt` 297, `AppStrings.kt` 288, `GuideTab.kt` 257, `GuideModels.kt` 285. New code goes in new
files or the edit is net zero by extracting something first.

## Phase 0: Branch, baseline, general fixes, storage proof (size: small)

Objective: a green branch from main with four fixes main benefits from, a dark-launch flag, and a proven migration to schema 16.
Read first: `PayslipDatabase.kt`, `PayslipDatabaseUpgradeTest`, `shared/schemas/…/14.json`, `CryptoHelper` (both), `PrivacyInfo.xcprivacy`,
`BaseModelDownloadBanner.kt`, `LaunchFlags.kt`, the old branch's `Part2Entities.kt`, `scripts/run_flag_on_tests.py`, `ci.yml`.

Steps:
1. Confirm `main` equals `origin/main` and is clean; create `feature/orders-bank`; commit plans 13, 13a, 13b as the first commit.
2. Tag the old branch `archive/part2-orders-2026-10-30` locally. Done and pushed on 2026-10-30 (with `main`).
3. Baseline: run the gate unchanged; record test counts per module, any pre-existing skip, corpus 139/139, line counts of every file the
   phase touches. Write them into the Phase 0 summary and `docs/Plan/13_COPIED.md` (the provenance list; it grows every phase).
4. General fix A, restore error: tests first (the old `PayslipBackupWrongPasswordTest` and `AndroidDecryptFailureTest` ideas, rewritten for
   main); `DecryptionFailedException` in both `CryptoHelper`s; the fixed restore text through `AppStrings`.
5. General fix B, privacy manifest: `test_privacy_manifest.py` first (fails on the undeclared disk-space use), then the declaration.
6. General fix C, banner: `BaseModelDownloadBannerLayoutUiTest` first (308 dp at 2x font fails), then the `FlowRow`.
7. General fix D, manifest guard: copy `check_merged_manifest.py` and its tests; regenerate the baseline from main's release merged manifest
   (`assembleRelease` with the placeholder model); every entry gets a reason.
8. Dark launch: `LaunchFlags.PART2_ENABLED = false`; `Part2FlagExpectation` in `shared-test-fixtures`; copy `run_flag_on_tests.py` with its tests
   and the `part2-flag-on-ci` job; a test pins that flag off leaves the paywall catalog and the Guide home exactly as main has them.
9. Storage proof, tests first: extend `PayslipDatabaseUpgradeTest` with 14 to 16 and old 15 to 16 (rows in `part2_drafts` and
   `part2_orders`; afterwards orders are byte-identical and drafts are gone). Then: copy old `15.json` byte-identical (sha checked);
   `Part2OrderEntity`, `Part2PageEntity` as on the old branch; new `Part2EntryEntity` (id plus ciphertext); `Part2Dao` for the three tables;
   `AutoMigration(14, 15)`, `AutoMigration(15, 16, spec = DropPart2Drafts)` with `@DeleteTable`; generate `16.json`.

Files (budget): `Part2Entities.kt` about 45, `Part2Dao.kt` about 110, `PayslipDatabase.kt` main's length plus about 12 (check it stays under 300),
`Part2FlagExpectation.kt` about 10, four fix files each touched minimally.
Tests: unit and integration as above; Robolectric not needed. iOS: `iosSimulatorArm64Test` runs the Room schema on Native.
Security: the restore error never echoes library text; no new data surface; the privacy manifest is strictly additive.
Gate: sections 3 items 1 to 6; R8+ (Room entities change).
Debt watch: `PayslipDatabase.kt` size; a baseline entry without a reason; a copied fixture with a real identifier (scan the diff).
Exit: gate green; with the flag off the app is main's app (the pin test).
Owner items: the schema-16 decision (deletes `part2_drafts`, so the Pixel's synthetic drafts are lost).

## Phase 1: ODP content (size: large)

Objective: `columns.json` (303 rows), the about and related data, and a bundle section the app validates on load.
Read first: `codes.json`, `nav.json`, `tools/part2_codes.py`, `part2_nav.py`, `bundle.py`, `compile.py`, `refresh.py`, `GuideBundleParser`,
`GuideBundleValidator`, `GuideModels.kt`, `GuideValidationHelpers.kt`, old `Part2LookupModels.kt` and `GuidePart2LookupValidator.kt`.

Steps:
1. Copy the content tools, `codes.json`, `nav.json` and their tests (`test_part2_codes.py`, `test_part2_nav.py`, `test_part2_file_sizes.py`);
   they must be green unchanged on the new branch before anything else moves.
2. Tests first, `test_part2_columns.py`: one row per serial and its codes equal `codes.json`; each of From, To, Data 1 to 4 is
   `required`, `optional` or `none`; a `none` column carries no caption; captions are plain text and bounded; the 63 casualties built on the
   old branch match their independent oracle (`tools/fixtures/part2_columns_oracle.json`, 63 rows of needs only, exported once from the old
   templates); a deliberately broken row (wrong code, unknown need, angle brackets, missing serial) is refused.
3. Extract Annexure III from the local PDF page images into `columns.json`; run the checker; the owner spot-checks about 25 rows drawn by a
   seeded random pick, and the draw and result go in DECISIONS. Local sources stay outside git (the `part2_codes.py` convention).
4. `about.json`: plain English for about 30 casualties the owner picks, written from the manual and read once by the owner. Manual data
   for the other casualties is built at run time from `codes.json` (one SSOT, nothing duplicated). `related.json`: from the old templates'
   links and nav `also`, each tagged `manual` with page or `practice`, for the casualties that have data only.
5. Kotlin, tests first: `GuidePart2CasualtyValidatorTest` (each rule above refused again on load; an unknown version; a bundle without the new
   keys still loads); then `Part2CasualtyModels.kt` and `GuidePart2CasualtyValidator.kt`; the `part2` field on `GuideBundle` (net zero in
   `GuideModels.kt`: move one model out first if needed).
6. Wire `bundle.py`, `compile.py`, `refresh.py`; regenerate the bundle; `refresh.py --check` clean; the app bundle size noted against `MAX_CHARS`.

Files (budget): `Part2LookupModels.kt` 70, `Part2CasualtyModels.kt` about 90, validators about 190 and 130, `part2_columns.py` about 220 (split by concern),
`columns.json` and `about.json` are data (JSON over 300 lines is allowed only with a header note in the sibling README; sizes are measured and
recorded).
Tests: Python unit (tools), Kotlin unit, integration (real bundle through the real parser: 303 casualties, 321 codes, 10 ribbons, columns for all).
Security: content is plain text with angle brackets refused; no markup, links or scripts ship in the bundle; caps on every field.
Gate: R8+ (new `@Serializable`): `check_r8_guide.py` shows the new models kept and the bundle shipped byte-identical.
Debt watch: duplication between `about` text and `codes.json` (forbidden by design); the local extraction helper must not reach CI.
Exit: gate green; the owner's spot-check recorded with its result.
Owner items: the 25-row spot check; the ~30 casualties; one read of their plain-English text.

## Phase 2: Learn about Part II, free (size: medium)

Objective: the wiki on both platforms behind the flag: home, ribbons, casualty page, search.
Read first: old `Part2Destination.kt`, `GuideTabPart2.kt`, `Part2HomeScreen`, `Part2RibbonScreen`, `Part2CodeScreen`, `Part2LookupViewModel`,
`Part2LookupIndex`, `Part2LookupContent`; main's `GuideNavState.kt`, `GuideNavStateSaver.kt`, `GuideTab.kt`, `GuideTabRoute.kt`,
`GuideHomeScreen.kt`, `AppModule.kt`, `Koin.kt`; `Theme.kt`.

Steps:
1. Copy, as characterization before any edit, the lookup domain and its tests (`Part2LookupIndexTest`, `Part2LookupContract`, `Part2BrowseContract`,
   `Part2LookupViewModelTest`, `Part2LookupUiTest`, `GuideNavPart2Test`, `Part2CaseScreen` is not copied); run them green.
2. Tests first for the changes: the ribbon is one flat list with the case names as headings (no case level); the casualty page shows name,
   group meaning, chapter, page, columns with captions, about (or manual data), "Usually published with" with its manual or practice label;
   search by code, serial and name over all 303; Back goes up one level; the saver keeps ids only and `retainKnown` drops unknown ids; flag
   off removes the entry row; the lock screen drops the search text.
3. Implement: `Part2Destination` as a sealed sub-interface (`Home`, `Ribbon`, `Code`), `LearnStrings.kt`, theme tokens, `Part2Module` loaded only
   while the flag is on, the one entry row on the Guide home (the Insights Part II ribbon; the old Dashboard Due card is not copied).
   "Add to Orders Bank" is shown disabled until Phase 3.
4. Touch points edited by hand, each net zero or smaller than the pin: `GuideNavState`, `GuideNavStateSaver`, `GuideTabRoute`, `GuideHomeScreen`,
   `GuideTab`, `AppModule`, `Koin`.

Files (budget): `Part2HomeScreen` 61, `Part2RibbonScreen` about 60, `Part2CodeScreen` about 90, `LearnStrings.kt` about 120, view model 53; composables at most 50 lines.
Tests: unit, Compose UI (real shipped bundle), Koin graph test, `iosTest` timing bound for search (the old walk of about 930 searches took 794 ms on
Native against a 1,500 ms budget; the bound is re-measured here), accessibility (control names, 2x font on 360 dp), contrast in both themes.
Security: search text and the query are never saved or logged; ids only in state.
Gate: sections 3 items 1 to 6 (R8 minify only, no new serialized models).
Debt watch: `GuideTab.kt` and `GuideHomeScreen.kt` wiring growth; a second copy of a string.
Exit: gate green; Pixel walk on synthetic state (light and dark, process death, 2x font).
Owner items: confirm the entry row's place on the Guide home; read the casualty page on the phone.
