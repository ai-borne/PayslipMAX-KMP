# Pre-release tech-debt plan (branch `chore/guide-prerelease-debt`)

Source: the audit of 2026-10-10 (`android-1.3.0-vc18`..`d6424c11`). Goal: a debt-free `main` before the Guide reaches
production. This branch also counts as maintenance phase **M9** (carry-over). Release prep (version bump, versionCode,
store notes) and the iOS walkthrough are **out of scope** (owner, separate release step).

## Owner decisions (2026-10-10)

| # | Topic | Decision |
|---|---|---|
| D1 | Dark launch | Keep `LaunchFlags.GUIDE_ENABLED` as the one kill switch; remove `isGuidePreviewBuild`, `BuildConfig.GUIDE_PREVIEW` and the debug-preview path. |
| D2 | Notes held after Search | Search state `WhileSubscribed` (same grace as the notes model); the query keeps its own `StateFlow`. |
| D3 | Internal bundle fields | `status`, `topic`, `domain` leave the bundle and `GuideCard`; the validator rejects them like `from`/`open`. |
| D4 | R8 gate | `check_r8_guide.py` checks the **release** build; `minifiedTest` stays only for the device smoke. |
| D5 | Pre-1.3.0 dead code | In scope, own phase. |
| D6 | Release prep | Separate, later. |
| D7 | Flaky Gemma test | Time-boxed hunt (60 min), then a defensive fix with the cause logged as unknown. |
| D8 | Debt register | `19_carry_over.md` in the repo becomes the single source of truth for open debt. |
| D9 | Device checks | Pixel smoke + targeted walk on behaviour phases (P1, P2, P3, P5, P6, P7) only. |
| D10 | Git | One commit per phase after a green gate and the owner's OK; stop after each phase; PR only when the owner says. |
| D11 | Old branches | Last phase: list, owner confirms, I delete only branches proven merged into `main` (local and remote). |
| D12 | Scratch tools | `todo_pay.py`, `pa.py`, `packet.py` move to `tools/archive/`. |

## Rules every phase follows

- **TDD:** write the failing test first and record the red run, then make it green. Deletion-only work is guarded by a
  test that fails if the thing comes back, or by existing characterization tests run before and after.
- **SSOT / DRY:** one constant per rule; extend existing canonical objects (`GuideStrings`, `AppStrings`, `Theme.kt`,
  `ThemeColors.kt`, `shared-test-fixtures`), never shadow them. **MVVM:** rules live in `shared` domain or a ViewModel;
  composables only draw state and forward events.
- Fix every stale comment **in a file the phase touches**; P9 sweeps the rest. Surgical: no other refactors.
- **Gate (every phase, all must pass):** `check_tech_debt_limits.py --strict` on changed `.kt`; `./gradlew ktlintCheck`;
  `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`. Add when relevant: `iosSimulatorArm64Test` +
  `:composeApp:linkDebugFrameworkIosSimulatorArm64` (any `commonMain`/`iosMain` change);
  `:composeApp:assembleRelease -PallowPlaceholderGemmaModel=true` + `scripts/check_r8_guide.py` (models, Koin, Gradle or
  bundle change); `refresh.py --check --allow-no-sources` (tools or bundle change); Pixel smoke
  `scripts/run_guide_minified_smoke.sh` + the named walk (D9). Gradle runs one at a time, JDK 21.
- **Phase handoff:** debt incurred, how it was resolved, gate results with test counts, then stop.

## Phases

### P0. Baseline and debt register (docs only)
- Record the baseline: `check` green, iOS test count, release APK size, bundle SHA-256, every card's `rev`
  (saved to the scratchpad for P5).
- Create `19_carry_over.md`: move the M9 list from `~/.claude/plans/make-a-phase-wise-shiny-mochi.md` and the audit
  findings into one table (id, item, owner/phase, status). Later phases close rows there and nowhere else.
- Gate: `refresh.py --check`, `./gradlew check` (baseline).

### P1. Notes are not held after Search closes (audit #3)
- Red: in `GuideSearchNotesViewModelTest`, collect `state` while unlocked, cancel the collector, advance past the
  grace period, and assert the notes repository has **no** active subscriber. Add a subscriber counter to
  `FakeGuideNotesRepository` (in `shared-test-fixtures`). Keep the existing test that the query survives opening a card.
- Green: `GuideSearchViewModel.state` uses `SharingStarted.WhileSubscribed(...)`.
- SSOT: one `UI_STOP_TIMEOUT_MS = 5_000L` in composeApp, used by `GuideNotesViewModel`, `GuideSearchViewModel` and the three
  literal `5000`s in `PayslipViewModel`.
- Tests that read `state.value` without collecting are switched to collect in `backgroundScope`.
- Pixel: a note-only search finds the note; leave Search and come back (query kept), and the results return.

### P2. Pay Audit "See the rule" recovers after a failed load (audit #4)
- Red: in `GuideOpenCardTest`, the repository fails once and then succeeds; `openCard(id)` after `Failed` must end
  `Ready` with the pending card set (today it stays `Failed`). `FakeGuideRepository` gains a scripted result
  sequence if it lacks one.
- Green: `GuideViewModel.openCard` calls `retry()` when the state is `Failed`, else `load()`. That rule stays in
  the ViewModel, not in `GuideCardHost`.
- Pixel: regression smoke only (the device cannot force a failure); the Pay Audit link still opens its card.

### P3. Remove the debug-preview path, keep the kill switch (D1, audit #6)
- Before: run `GuideReleaseLaunchTest`, `AppNavStateSaverTest`, `PremiumFeaturesCatalogTest`, `AppBottomBarTest`
  and `AppKoinModulesTest` (characterization).
- Red: a test that `isGuideEnabled()` equals `LaunchFlags.GUIDE_ENABLED` in every source set (it fails to compile
  until the expect/actual goes, which is the red), plus a scan in `GuideSecurityContractTest` that
  `GUIDE_PREVIEW`/`isGuidePreviewBuild` never return.
- Green:
  - Delete `isGuidePreviewBuild` (common and both platform versions) and the three `GUIDE_PREVIEW` build settings.
  - Keep `buildConfig = true` (`AndroidAppIntegrityCheckerProvider` reads `BuildConfig.DEBUG`).
  - `isGuideEnabled()` = `LaunchFlags.GUIDE_ENABLED`.
  - The `minifiedTest` build type stays for the smoke.
  - Every guard (nullable `guideNavState`, `appNavStateSaver(guideEnabled)`, `rememberOpenGuideCard(enabled)`,
    `isAdvertised`, `showGuide`) now reads the flag only.
  - Rewrite the guards' comments as "kill switch" ones (App.kt, AppNavStateSaver, AppModule, GuideAvailability,
    GuideCardRoute, PayAuditScreen, LaunchFlags, build.gradle.kts, PremiumFeaturesCatalog).
- Note the change: with the switch off, debug also hides the Guide (no preview build any more).
- Gate adds iOS tests + link, `assembleRelease` + R8 check. Pixel: smoke, tab re-tap, Pay Audit link.

### P4. The R8 gate checks the shipping build (D4, audit #7)
- Red: in `scripts/tests/test_check_r8_guide.py`, the defaults must be the release mapping and APK.
- Green:
  - The script's defaults and docstring now point at release.
  - CI drops `assembleMinifiedTest` and runs the check after `assembleRelease`.
  - pre-push does the same, and the smoke script (when a device is attached) builds `minifiedTest` itself.
  - Fix the stale `proguard-rules.pro` §10 comment, the `GuideModels.kt` "keep rule" comment and CLAUDE.md lines 124/131.
- Gate: `assembleRelease` + `check_r8_guide.py` (no `--mapping` argument) green; the Python tests pass.

### P5. Internal fields leave the bundle (D3, audit #8)
- Red (Python): `test_bundle` says a shipped card has no `status`, `topic` or `domain`. The travel/pay counts are
  re-pinned on `rulebook.json`, not on the bundle.
- Red (Kotlin): in `GuideBundleValidatorTest` / `GuideBundleParserTest`, a card carrying `status`, `topic` or
  `domain` is `INVALID`.
- Green:
  - `bundle.py` `CARD_KEYS` without the three fields.
  - `INTERNAL_CARD_FIELDS` += the three.
  - Remove the fields from `GuideCard` and `SyntheticGuideBundle`.
  - Regenerate with `refresh.py`.
- Regression control: a script compares every card's `rev` with the P0 baseline and finds **no change** (notes'
  "updated since" depends on it). Bundle text is otherwise identical apart from the removed keys. Record the new size.
- Gate adds iOS tests, `assembleRelease` + R8 check, `refresh.py --check`. Pixel: smoke, and a noted card shows no
  "updated since your note".

### P6. Guide dead code and SSOT (audit #9, #10, #11)
- Red tests first:
  - `GuideCardFull` exposes `key`/`attach`/`watch` (compile-red).
  - A shared limit test: the Report an Issue and Guide suggestion dialogs use the same cap.
  - `ThemeColorsTest` gets a duplicate-literal scan that fails today.
- Green:
  - Delete `GuideSuggestion.canSend` and its tests; the dialog's own enabled rule stays.
  - Delete `GuideMaintenanceStrings.openCardHint`.
  - Delete `CardTemplate.body`/`GuideCardBody`; `GuideCardFull` holds the three lists (screen, share and contract
    call sites updated).
  - Move `GuideStaleness.epochDays` to a `shared-test-fixtures` clock helper.
  - One `SUPPORT_MESSAGE_MAX_CHARS` in shared domain, used by `GuideSuggestion` and `ReportIssueDialog`.
  - `ThemeColors` aliases (`Blue100 = LightColorPrimaryContainer`, etc.).
  - `guideSmokeTest` dependencies go into `libs.versions.toml`.
  - Fix the `GuideIndex` "402" and `GuideBundleParser` "252 KB" comments.
- Pixel: a card's sections, Share, Suggest a correction dialog (discard), Report an Issue dialog.

### P7. Pre-1.3.0 dead code (D5)
- Red: an `androidUnitTest` scan that every `const val`/`val` in `AppStrings*` and `AppDimensions` is referenced
  outside its own file; it fails listing today's unused items. A scan that no screen uses a literal glyph (`"✓"`,
  `"✔"`, `"➔"`) in `Text(`.
- Green:
  - Delete `PremiumSettingsCard`, the 9 strings and the 4 dimensions.
  - Add `AppStrings.checkMark` and use it at both sites.
  - Verify `PortableBackup.backupVersion` and the RevenueCat delegate overrides are real before touching them;
    leave them if they are.
  - Fix the `Products.storekit` description ("Cloud Backup").
- Pixel: Settings and the Premium screen render (checkmarks shown).

### P8. Flaky `GemmaInstallTelemetryIntegrationTest` (D7)
**Status (2026-10-11): already fixed on `main` (`18e58901`, #19), per the owner. No work on this branch.**
- Run `:composeApp:testDebugUnitTest --rerun` in a loop of 10 and record the class run before each failure. Fix the
  leaking test (give its ViewModels a test scope, or cancel them in tearDown).
- After 60 minutes without a culprit: give the class its own Main dispatcher in `setUp`/`tearDown` and record that
  the cause is still unknown in `19_carry_over.md`.
- Exit: 5 consecutive green `--rerun` runs.

### P9. Remaining stale comments and docs, scratch tools (audit P3/P4, D12)
- Sweep: grep the stale-wording list again (`dark launch`, `until E9`, `Guide is off`, `402`, `phase E3`) until empty.
- CLAUDE.md: schema v14, Guide notes encrypted at rest, backup v4, R8 gate wording.
- HANDOFF top block: rewritten short, linking to `19_carry_over.md`; the 36 vs 37 unverified count fixed; paste
  prompt updated. Phase plan M8/M9 rows.
- `tools/archive/` for the three scripts (imports checked); README and HANDOFF tables updated.
- `refresh.py`: tool-test output is captured, so a passing run no longer prints `FAILED at step "b"`. Red first: a
  test asserting the passing run's output does not contain `FAILED`.
- Close the register rows this branch fixed.

### P10. Final gate and branch housekeeping (D10, D11)
- Full pre-push equivalent (Android + common, tools, release + R8, iOS tests, native XCTest, schema immutability,
  gitleaks); Pixel smoke plus one walk of the Guide (search with a note, a card, Pay Audit link, dark and light).
- List every local and remote branch with "fully merged into `main`?" (`git branch --merged`,
  `git merge-base --is-ancestor`). The owner confirms; I delete only the merged ones.
- Push and open the PR only when the owner says.

### P11. Deferred device checks (done 2026-10-11, Pixel 9)
Ran every deferred item from P1-P7 on the device (results in the P11 result below and in `19_carry_over.md` A17). One item
waived by the owner: the Pay Audit "See the rule" link (no finding exists on the device).

### P12. Open items for the owner (final consolidated phase; nothing here is code work this branch can finish)
- **B19 (owner):** five branches not proven merged into `main`: `docs/rule-cards-dataset`, `docs/rule-cards-nav`, `feature/insightsgemma`,
  `feature/pay_audit_1.0` (39 unique commits), `test/baseline-909f2ef`. Decide per branch: squash-merged (delete) or abandoned (delete) or keep.
- **Push and PR (D10):** the branch is not pushed. Push and open the PR only when the owner says. The 7 approved remote branch deletions are done (pre-push gate green, 307 s).
- **B16 (owner):** iOS UI walkthrough, 13-row checklist in `16_guide_phase_plan.md`. Includes a look at the Settings checkmark badge and
  the Premium screen on iOS (P7 changed `✔` to `✓`).
- **B4 (owner):** confirm that "Fail loud" is what was meant by "Rule 12". **B5 (owner):** send the regenerated expert review PDF.
- **B18 (owner, D6):** release prep (version bump, versionCode, store notes). Mirror the "Pay Audit" wording in the store listing if it still says "Cloud Backup".
- **Waived (owner, 2026-10-11):** on-device tap of the Pay Audit "See the rule" link.
- **Merge `main` into this branch before the PR** (`18e58901`, P8's fix, is on `main` only).

## P3 result (2026-10-10)

Done: characterization run green before the edit; red = `GuideSecurityContractTest` scan (1 failed); `isGuidePreviewBuild`
(common, android, ios) and the three `GUIDE_PREVIEW` build settings deleted; `isGuideEnabled()` = `LaunchFlags.GUIDE_ENABLED`
(`GuideAvailabilityTest` pins it); `buildConfig = true` and `minifiedTest` kept; guard comments rewritten as kill-switch ones.
Gate: tech-debt audit and ktlint green; `check` green (includes `GuideReleaseLaunchTest`); `iosSimulatorArm64Test` +
iOS link green; `assembleRelease` 69,626,176 bytes (identical to baseline); `check_r8_guide.py` on release: 12 models kept,
bundle byte-identical. Behaviour change: with the switch off, debug also hides the Guide. Not done: the Pixel items (P11).

## P4 result (2026-10-10)

Done: red = `DefaultsTest` in `test_check_r8_guide.py` (1 failed, 1 error); `check_r8_guide.py` defaults, docstring and
failure hint now target the release mapping and APK; CI drops `assembleMinifiedTest` and runs the check after
`assembleRelease`; pre-push builds only `assembleRelease` before the check (its device smoke already builds
`minifiedTest` via `run_guide_minified_smoke.sh`); `proguard-rules.pro` §10, the `GuideModels.kt` doc comment (it
claimed a keep rule exists) and CLAUDE.md (R8 gate, "neither hook runs it") corrected. Gate: Python tests 10/10;
`assembleRelease` 69,626,176 bytes (identical to baseline); `check_r8_guide.py` with no arguments: 12 models kept,
bundle byte-identical; ktlint and tech-debt audit green. Not done: the Pixel items (P11). The CI YAML edit could not be
parsed locally (no PyYAML); it is a two-line change to one step.

## P5 result (2026-10-10)

Done: red = `test_curator_bookkeeping_never_ships` (Python) and `curatorBookkeepingFieldsAreNeverAccepted` (Kotlin), both
failed first; `bundle.py` `CARD_KEYS` without `status`/`topic`/`domain`; `INTERNAL_CARD_FIELDS` += the three; fields removed
from `GuideCard` and `SyntheticGuideBundle`; travel/pay counts re-pinned on `rulebook.json`; the shipped-contract test no
longer needs `topic` (RP-088 is found by id `RB-RP-088`); bundle regenerated. Regression control: the `rev` of all 404 cards
equals the P0 baseline, cards are identical to the old bundle once the three keys are dropped, and the only other top-level
change is `generated` (2026-10-09 to 2026-10-10, which `refresh.py` stamps). Bundle 261,409 to 240,327 bytes (SHA-256
`6bdddb1d719265b196b4308277102293f64a55c16f3222d26b73525c5fb06116`); release APK 69,626,176 to 69,624,204 bytes.
Gate: tech-debt audit, ktlint, `check` (shared 1026, composeApp 895 Android unit tests, 0 failed, 0 skipped),
`iosSimulatorArm64Test` (981 + 559, 0 failed) and iOS link, `assembleRelease` + `check_r8_guide.py` (12 models kept,
bundle byte-identical), `refresh.py --check`, Python tests: all green. Also closed register row A7 (P4 had left it open).
Not done: the Pixel items (P11).

## P6 result (2026-10-10)

Done: red = compile failures on `GuideTestClock`, `SUPPORT_MESSAGE_MAX_CHARS` and `GuideCardFull.key`, plus
`ThemeColorsSourceTest` (the old `ThemeColors.kt` held 5 repeated colour literals, checked against `HEAD`).
`GuideSuggestion.canSend` (and its test), `GuideMaintenanceStrings.openCardHint`, `GuideCardBody` and `CardTemplate.body`
deleted (`GuideCardFull` now holds `key`/`attach`/`watch`; screen, share, search index and contract test updated);
`epochDays` moved to `shared-test-fixtures` `GuideTestClock`; one `SUPPORT_MESSAGE_MAX_CHARS` (shared `domain`) replaces
`GuideSuggestion.MAX_TEXT_LENGTH` and the private cap in `ReportIssueDialog`; `ThemeColors` aliases (`Blue100`, `Blue900`,
`Gray100`, `Gray400`, `White`); `guideSmokeTest` dependencies in `libs.versions.toml`; the "402" (`GuideIndex`) and "252 KB"
(`GuideBundleParser`) comments fixed, and a stale "placeholder bullets are split off" comment in `GuideCardSections`.
Gate: tech-debt audit, ktlint, `check` (shared 1024, composeApp 896 Android unit tests, 0 failed, 0 skipped; the 2 fewer
shared tests are the deleted `canSend`/`body` tests), `iosSimulatorArm64Test` (979 + 559, 0 failed) and iOS link,
`assembleRelease` 69,624,204 bytes (identical to P5), `check_r8_guide.py` (12 models kept, bundle byte-identical),
`refresh.py --check` OK. Not done: the Pixel items (P11). Left on purpose: "402 cards" in test comments
(`GuideSearchIndexTest`, `GuideLoaderIosPerfTest`), which belong to the P9 stale-wording sweep.

## P7 result (2026-10-11)

Done: red = `UiResourceUsageSourceTest` (3 failed: unused strings, unused dimensions, literal glyphs). The scan found **37 unused
string constants and 1 unused getter** (plan guessed 9) plus the 4 dimensions; all deleted (`AppStrings` 11, `AppStringsPremium`
26 + `premiumCatalogSubtitleDisplay` and its private `launchNeutralCatalogSubtitle`), as were `DialogElevation`, `DialogMaxHeight`,
`SpacingExtraLarge`, `IconSizeLarge`. `PremiumSettingsCard` (the unused card; `PremiumSettingsCardContentRow` is live and stays)
and its border-colour helper deleted. `AppStrings.checkMark` ("✓") now used at both sites; the Settings badge changes from
`✔` to `✓`. A constant used only by a live getter in its own file counts as used (rule in the test). Checked and left alone:
`PortableBackup.backupVersion` is a serialized field (real); no RevenueCat delegate overrides exist in the code today, so there
was nothing to remove. Copy fix: "Cloud Backup" was false (backups are local files), so `Products.storekit` and
`settingsPremiumPlanUpgradeSubtitlePrefix` now say "Pay Audit" (the owner should mirror the store listing text if it still
says Cloud Backup). Gate: tech-debt audit, ktlint, `check` (shared 1024, composeApp 899 Android unit tests, 0 failed, 0 skipped),
`iosSimulatorArm64Test` (979 + 559, 0 failed) and iOS link green. `assembleRelease` + R8 check not run (no models, Koin or
Gradle change). Not done: the Pixel items (P11). Tooling note: Gradle needed `-Dorg.gradle.java.home` (JDK 21) because an IDE
extension JRE without `jmod` was picked up.

## P9 result (2026-10-11)

Done: stale wording swept (`dark launch`, `until E9`, `Guide is off`, `phase E3`) in 8 files; CLAUDE.md now says schema v14, notes in the
encrypted `GuideNoteEntity`, backup v4 and the kill switch; HANDOFF top block rewritten short and linked to `19_carry_over.md`; unverified
count is 37 everywhere (the bundle says 37); phase plan M8/M9 rows fixed; the three scratch tools moved to `tools/archive/` (each puts
`tools/` on `sys.path`, each run once from the new place; README, HANDOFF, HANDOFF_PROMPT paths updated); `refresh.py`: the false
`FAILED at step "b"` came from a test of the failure path printing through the default `say`; it now captures and asserts it, and
`test_a_passing_tool_test_run_never_prints_failed` is red-then-green (2 failures before). Gate: tech-debt audit, ktlint, `check`
(both variants), `refresh.py --check --allow-no-sources` (146 tool tests): green. The compile.py run rewrote only the `generated` stamp in
`rulebook.json`; reverted.

## P8 note
Owner said P8 was already fixed in earlier commits (`18e58901`, #19, on `main`). Nothing was done here; B1 closed on that word.

## P10 result and a P7 regression (2026-10-11)

The pre-push hook (it also runs on a branch delete) **failed at the native iOS XCTest step**: `AppStrings.appProtectedShieldDesc` did not exist.
Cause: **P7 deleted `appProtectedTitle` and `appProtectedShieldDesc`, which only Swift (`iOSApp.swift`, `PayslipMaxProtectedOverlayTests`) reads**;
the Kotlin-only scan in `UiResourceUsageSourceTest` could not see them, and `iosSimulatorArm64Test` plus the framework link do not compile the app's Swift.
The P7 claim "gate green" was true for its own gate list but that list did not include the native iOS build. Fixed: both constants restored;
`UiResourceUsageSourceTest` now counts Swift uses as uses and has `everyStringTheSwiftAppReadsStillExists` (red before the fix, green after).
Lesson for the gate: any `AppStrings` deletion needs the native XCTest run (`scripts/git-pre-push.sh` step 5).

The 7 approved branch deletions then ran through a green pre-push gate (307 s: Android + common, tools, release + R8, iOS tests, native XCTest, gitleaks).

## P11 result (2026-10-11, Pixel 9, `minifiedTest` build with real data)
`scripts/run_guide_minified_smoke.sh` (builds `minifiedTest` itself: P4 path proven): OK (1 test). Walk: card sections render; Share opens the
sheet; Suggest a correction typed then cancelled; Settings > Report an Issue dialog; Settings badge shows the checkmark; Premium screen shows
"✓ Included"; a note-only search ("zebrafish") found the card marked "In your note"; opening the card and going back kept the query; leaving
Search for Settings for over 10 s and returning brought the results back; the Guide tab re-tap returns to the root; the noted card showed no
"updated since your note"; the throwaway note was deleted. Not run (waived by the owner): Pay Audit link (0 issues in all 33 audited months).

## Risks and controls
- **Release/R8:** P3, P5 and P6 change models or reachability → `assembleRelease` + check each time.
- **iOS:** no new Native hot path. `WhileSubscribed` and the model changes are covered by `iosSimulatorArm64Test`;
  the iOS UI still needs the owner's walkthrough (not part of this branch).
- **Notes:** P5's `rev` diff against the baseline guards the "updated since your note" marker.
- **Room/backup:** unchanged (no schema bump).
- **Navigation/state:** P1 keeps the query across a card push and a tab switch (process death clears it, as decided);
  P3 keeps `appNavStateSaver` restoring the Guide tab while the switch is on (`GuideReleaseLaunchTest`).

## P0 baseline (recorded 2026-10-10, HEAD `d6424c11`, JDK 21)

| Measure | Value |
|---|---|
| `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` | green; `shared` 1025 + `composeApp` 890 Android unit tests, 0 failed, 0 skipped |
| `iosSimulatorArm64Test` | green; `shared` 980 + `composeApp` 555, 0 failed, 0 skipped |
| `refresh.py --check --allow-no-sources` | `refresh OK`, 144 tool tests (but prints a false `FAILED at step "b"`, register row A15) |
| Release APK (`assembleRelease`, placeholder Gemma) | 69,626,176 bytes |
| Guide bundle | 261,409 bytes, SHA-256 `e6595aa84f4c1efb52b2d25c23a378e4b92f21bc637a1f955c79c5455d390f98`, 404 cards |
| Per-card `rev` | 404 values saved to the scratchpad (`rev_baseline.json`); re-derivable from `git show d6424c11:composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json` for the P5 comparison |

Open debt is tracked only in `19_carry_over.md` (D8).
