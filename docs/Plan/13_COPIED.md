# Orders Bank: what was copied from the frozen branch, and the baseline

Source of every copy: `origin/feature/part2-orders` at `22554101` (tag `archive/part2-orders-2026-10-30`). Nothing is merged; each file below was
copied by path or ported by hand, then tested on this branch. The list grows every phase (plan 13a, Phase 0 step 3).

## Baseline (feature/orders-bank, unchanged tree, 2026-10-30)

| Check | Result |
|---|---|
| `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` | BUILD SUCCESSFUL |
| shared, debug / release unit tests | 1188 / 1188, 0 failed; release has 1 skipped |
| composeApp, debug / release unit tests | 1079 / 1053, 0 failed, 0 skipped |
| iOS simulator tests, shared / composeApp | 1142 / 669, 0 failed, 0 skipped |
| Corpus (`TokenParseCorpusRegressionTest`, 139 fixtures in `index.json`) | pass |
| `assembleRelease -PallowPlaceholderGemmaModel=true`, `check_r8_guide.py` | pass; 21 models kept, bundle byte-identical |
| Python: `scripts/tests` / rule-card tools | 30 / 265 pass |
| Release merged manifest | 11 permissions, 1 declared, 0 features, 3 exported components |

Pre-existing skip (main, not this work): `GemmaModelPathsAndroidTest`, one test, release variant only.

## Phase 0

| File here | From the old branch | How |
|---|---|---|
| `shared/.../crypto/DecryptionFailedException.kt`, `PcdaMark.kt`, `androidMain/.../AndroidDecryptFailure.kt`, `iosMain/.../IosDecryptFailure.kt` | same paths, commit `915e62a9` | verbatim |
| both `CryptoHelper.kt` (android, ios) | same paths | the old diff applied (uses `PcdaMark`, retypes failures) |
| `shared/.../repository/PortableBackup.kt` (`BackupDamagedException`) | `part2/data/Part2Backup.kt` | one class, moved to the repository package |
| `PayslipBackupService.kt` | same path | a 4-line catch; the rest of that file's old diff is Part II backup and is not copied |
| `composeApp/.../ui/screens/RestoreFailureStatus.kt` | `BackupGuard.kt` (`restoreFailedStatus`) | rewritten for main: no Part II report type |
| `ui/theme/BackupRestoreStrings.kt` and its test | same paths | verbatim |
| tests: `DecryptFailureTypeTest`, `PcdaMarkTest`, `AndroidDecryptFailureTest`, `IosDecryptFailureTest`, `DecryptFailureSourceTest`, `PayslipBackupWrongPasswordTest` | same paths | verbatim, except the source test points at `RestoreFailureStatus.kt` and the wrong-password test drops one import |
| `RestoreStatusTest` | same path | rewritten: main restores `Result<Unit>`, so the "rows left out" cases are not copied |
| `iosApp/iosApp/PrivacyInfo.xcprivacy` (disk-space entry E174.1), `scripts/tests/test_privacy_manifest.py` | commit `6c8877e6` | entry re-typed, test verbatim |
| `BaseModelDownloadBanner.kt` (`FlowRow`), `BaseModelDownloadBannerLayoutUiTest` | commit `95f5c4a3` | 3-line change, test verbatim |
| `scripts/check_merged_manifest.py` and its tests | same paths | verbatim |
| `scripts/merged_manifest_baseline.json` | same path | regenerated from main's release manifest; each entry keeps the reason reviewed on the old branch |
| `scripts/run_flag_on_tests.py` and its tests, `Part2FlagExpectation.kt` | same paths | verbatim except two comment lines and the value (false) |
| `Part2AvailabilityTest` | same path | rewritten: only `isPart2Enabled`, the flag pin and the catalog pin |
| `shared/schemas/.../15.json` | same path | byte-identical (sha256 `108917b0…b9b7`) |
| `part2/data/Part2Entities.kt` | same path | `Part2OrderEntity`, `Part2PageEntity` unchanged; `Part2DraftEntity` replaced by `Part2EntryEntity` |

Not copied in Phase 0 on purpose: `Part2Dao.kt` (no caller until Phases 3 and 4; it arrives with its first use).

## Phase 1 (content tools, the columns table, about, related, bundle section, validators)

| File here | From the old branch | How |
|---|---|---|
| `rule_cards/tools/part2_codes.py`, `part2_codes_check.py`, `part2_nav.py` | same paths | verbatim |
| `rule_cards/tools/test_part2_codes.py`, `test_part2_file_sizes.py` | same paths | verbatim, except `columns.json` is added to `ONE_ROW_PER_LINE` |
| `rule_cards/tools/test_part2_nav.py` | same path | verbatim minus one test (`test_back_from_leave_lists_every_area_regrant_the_leave_templates_can_link`): it reads the 63 slip templates, which this branch does not have |
| `rule_cards/part2/codes.json`, `nav.json` | same paths | byte-identical |
| `rule_cards/tools/fixtures/part2_columns_oracle.json` | the 63 templates `part2/templates/*.json` | needs only (from, to, data 1-4 per code), exported once; no text |
| `part2_columns*.py`, `test_part2_columns*.py`, `part2/README.md` | new on this branch | not copied |
| `rule_cards/part2/related.json` | links of the 15 leave templates in `part2/templates/*.json` | exported once and deduplicated by target (281 links, manual wins over practice); `part2_related_check.py` guards it. The nav `also` links stay in `nav.json` (one source, not copied) |
| `rule_cards/part2/about.json` | new | 30 pages written on this branch from the ODP and PCDA handbooks; sources in a local sheet only |
| `shared/.../guide/model/Part2LookupModels.kt` | same path | copied, trimmed (no rule or template fields), gained `columns`, `about`, `related`; limits gained `caption` and `about_part` |
| `shared/.../guide/data/GuidePart2LookupValidator.kt`, `GuideValidationHelpers.kt`, `model/GuideChangeModels.kt` | same paths | verbatim; `GuideBundleValidator` and `GuidePreClaimValidator` now use the helpers (as on the old branch); the change-log models moved out of `GuideModels.kt` (273 lines, pin 285) |
| `shared/.../GuidePart2LookupValidatorTest.kt`, `SyntheticGuidePart2.kt`, `SyntheticGuideBundle.withPart2()` | same paths | test verbatim; fixture trimmed of templates and given the casualty data |
| `GuideSecurityContractTest.kt` | same path | the old part2 exemption from the `"from"` check (a column row's `from` is the manual's From column) |
| `GuidePart2CasualtyValidator.kt`, `Part2CasualtyModels.kt`, `GuidePart2CasualtyValidatorTest.kt`, `Part2CasualtyContract.kt` | new on this branch | not copied |

## Phase 2 (Learn about Part II)

Characterization first: the lookup domain and its tests were copied and trimmed only as far as the Phase 1 models require (this branch has
no `templates`, `rules` or draft types), then run green on the new data before any behaviour change: `Part2LookupIndexTest` 19 tests,
and `GuideNavPart2Test`, `Part2LookupViewModelTest`, the real-bundle contracts (303 serials, 321 codes, 10 ribbons) 47 tests in all.
Not run green unchanged (not verified, by design): the old `Part2LookupUiTest`. Its screens (a case level, draft buttons, vault and due
rows) are replaced, so the UI tests were written new against the new screens, red first.

| File here | From the old branch | How |
|---|---|---|
| `shared/.../part2/domain/Part2LookupIndex.kt` | same path | copied, trimmed (no draft options, templates or printed-as); then changed: every ribbon is a tile, a ribbon is one flat list under its case names, a page gained columns, the plain-English page and the companions; `knowsCase` removed |
| `shared/.../part2/domain/Part2LookupContent.kt` | same path | copied, trimmed; then changed (new content types, no case or span types) |
| `shared/.../part2/domain/Part2LookupIndexTest.kt` (commonTest) | same path | copied, rewritten for `SyntheticGuidePart2` and the new behaviour |
| `composeApp/.../ui/screens/guide/Part2Destination.kt` | same path | copied, cut to `Home`, `Ribbon`, `Code` (no `Case`, `Locked`, `Draft`, `Slip`, `Hrms`, `Order`, `Due`) |
| `composeApp/.../ui/screens/guide/part2/Part2LookupViewModel.kt` | same path | copied; `case(id)` removed |
| `.../part2/Part2Availability.kt` | same path (`Part2Entry`, `part2Entry`, `part2FactoryOrNull`) | the Phase 0 file gained the entry decision and the factory; no draft entry |
| `.../part2/Part2HomeScreen.kt`, `Part2RibbonScreen.kt`, `Part2CodeScreen.kt`, `Part2Rows.kt`, `Part2SearchField.kt` | same paths | copied and rewritten: no drafts, vault or due rows, no case screen; `Part2CaseScreen` is not copied |
| `.../part2/Part2CodeSections.kt` | new | the columns, plain-English and "Usually published with" sections |
| `composeApp/.../ui/screens/guide/GuideTabPart2.kt` | same path | rewritten: `Part2Host(entry, model)`, the entry row and the level switch; no drafts, scanner, printer or reminders |
| `composeApp/.../di/Part2Module.kt` | same path | rewritten: one `single` (the Learn model) |
| `composeApp/.../ui/theme/LearnStrings.kt` | `Part2Strings.kt` | new object, only the search, list and page strings Learn needs; nothing else of `Part2Strings` is copied |
| `GuideLoadStates.kt` | extracted from `GuideTab.kt` | `GuideLoading` and `GuideLoadFailed` moved (no change) to keep `GuideTab.kt` under its pin |
| tests: `GuideNavPart2Test`, `Part2LookupViewModelTest`, `Part2BrowseContract`, `Part2LookupContract`, `Part2EntryTest` | same paths | copied, rewritten for the flat ribbon and `SyntheticGuidePart2`; `Part2LookupContract.assertRulesMatchTheRegistry` and `Part2BrowseContract.assertRejoinalIs...` dropped (no rules or templates here) |
| tests: `Part2UiBase`, `Part2LookupUiTest` | same paths | rewritten for Learn; `Part2LookupRestoreUiTest`, `Part2LookupRealBundleUiTest` not copied (their cases are inside the new tests) |
| tests: `Part2CasualtyPageUiTest`, `Part2LearnA11yUiTest`, `Part2LearnRealBundleUiTest`, `Part2LearnSecurityContractTest`, `Part2WiringTest`, `LearnContrastTest` | new | not copied |
| `GuideHomeScreen.kt` | main | edited by hand: the Learn row moved below the areas under its own heading (owner, 2026-10-30, option A) |
