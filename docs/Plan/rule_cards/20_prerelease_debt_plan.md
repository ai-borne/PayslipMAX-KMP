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

### P11. Deferred items from earlier phases (consolidated; run last, needs a device)
- **From P1 (not done, no Pixel was attached on 2026-10-10):** the Pixel smoke (`scripts/run_guide_minified_smoke.sh`) and
  the targeted walk: a note-only search finds the note; leave Search and come back (query kept) and the results return.
  D9 requires this for P1, so P1 is **not fully verified** until this runs.
- **From P2 (not done, no Pixel attached on 2026-10-10):** regression smoke only (the device cannot force a failure):
  the Pay Audit "See the rule" link still opens its card. The recovery itself is covered by
  `GuideOpenCardTest.aFindingLinkRecoversWhenTheEarlierLoadFailed...`; only the on-device smoke is outstanding.
- **From P3 (not done, no Pixel attached on 2026-10-10):** smoke `scripts/run_guide_minified_smoke.sh`, tab re-tap, and
  the Pay Audit link opening its card. Everything else in P3 is verified (see the P3 result below).
- Add here, with the phase name, any later phase whose device check could not run.
- Exit: every item run on a device and recorded in `19_carry_over.md`, or the owner explicitly waives it.

## P3 result (2026-10-10)

Done: characterization run green before the edit; red = `GuideSecurityContractTest` scan (1 failed); `isGuidePreviewBuild`
(common, android, ios) and the three `GUIDE_PREVIEW` build settings deleted; `isGuideEnabled()` = `LaunchFlags.GUIDE_ENABLED`
(`GuideAvailabilityTest` pins it); `buildConfig = true` and `minifiedTest` kept; guard comments rewritten as kill-switch ones.
Gate: tech-debt audit and ktlint green; `check` green (includes `GuideReleaseLaunchTest`); `iosSimulatorArm64Test` +
iOS link green; `assembleRelease` 69,626,176 bytes (identical to baseline); `check_r8_guide.py` on release: 12 models kept,
bundle byte-identical. Behaviour change: with the switch off, debug also hides the Guide. Not done: the Pixel items (P11).

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
