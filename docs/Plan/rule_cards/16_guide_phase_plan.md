# Claim Guide, Phase E plan (E1 to E9)

Status: E0 (this plan) done 2026-10-07. **E1 done 2026-10-07** (branch `feature/guide-e1-bundle`). **E2 done 2026-10-07**
(branch `feature/guide-e2-tiles`, off E1). **E3 done 2026-10-07** (branch `feature/guide-e3-feed`, off E2). **E4 done 2026-10-07** (same branch). **E5 done 2026-10-07** (merged to `main`, branch deleted). **E6 done 2026-10-08** (merged to `main`, branch deleted). **E7 done 2026-10-08** (merged and pushed). **E8 done 2026-10-08** (merged and pushed 2026-10-09). **E9 done 2026-10-09** (branch `feature/guide-e9-release`, committed, not merged or pushed).
Branching (decided 2026-10-07, solo developer): `main` holds E0 to E4; each later phase is one branch off `main`,
merged (fast-forward) when its gate is green and deleted, so at most one phase branch is ever open.
Items left open by a finished phase are listed under "EP Pending items" at the end, never dropped.
Inputs: Gold dataset (402 cards: 220 travel, 182 pay), `nav.json` (9 areas, 44 cases), `facets.json`,
the approved clickable preview, and `CLAUDE.md`. One branch per phase off `main`
(`feature/guide-eN-<name>`), one PR per phase.

## Rules for every phase (CLAUDE.md 0-5)
1. **One phase at a time.** A phase ends only with all of these green:
   `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, `iosSimulatorArm64Test`,
   `:composeApp:linkDebugFrameworkIosSimulatorArm64`, `ktlintCheck`,
   `python3 scripts/check_tech_debt_limits.py --strict`, and the R8 gate below. A red build blocks the next phase.
2. **TDD.** Unit and integration tests are written first or alongside. 100% pass, none skipped.
3. **MVVM, SOLID, DRY, SSOT.** 300 lines per file at most. A file may exceed it only when splitting
   would break the Single Responsibility Principle, and then a header comment says why. Composable-length check applies.
4. **No hard-coded strings or colours.** App chrome copy goes in `ui/theme/GuideStrings.kt` (the project's
   string convention, sibling of `AppStrings.kt`, which is at 295 lines). Colours are tokens in `Theme.kt`
   (sibling theme file only if Theme.kt would pass 300). Card text, area, case and facet labels are
   **content from the bundle**, not UI copy (owner decision below).
5. **Security in every phase.** No new outbound path. Telemetry only via `TelemetrySanitizer`, and never
   card text, search queries or pin ids. Bundle input validated before use.
6. **Phase Handoff Protocol.** Tech debt incurred, the exact steps that resolved it in the same phase, and
   confirmation that the build and all tests pass. Then stop for owner approval.

## Owner decisions
| Topic | Decision (date) |
|---|---|
| Rulebook updates | Bundled only, with a "Rates as of" chip and a staleness nudge. No remote channel. (10-06) |
| RP-088 HBA 8.5% | Ships with the "Unverified point" chip until the owner confirms the current rate from a primary letter. (10-06) |
| 36 open-point cards | All ship with the chip. The `open` text never ships. (10-06) |
| RP-073 TA Allowance | Known gap: no card, no search alias. (10-06) |
| Guide navigation | Own stack inside the Guide tab. One `Screen.GuideCard` detail only for the Pay Audit link. (10-07) |
| Guide tab re-tap | Goes back to Guide Home. Switching tabs keeps your place. (10-07) |
| Search query | Kept in memory only, so it is gone after the app is killed. (10-07) |
| Nav and facet labels | Taken from the bundle. `GuideStrings.kt` holds app chrome only. (10-07) |
| Pins | Device only, not in backup (format stays v3). (10-07) |
| Rates as of | `2026-01` (the DA 60% step) in `tools/config.py` `RATES_AS_OF`. (10-07) |
| No official source | Exactly the cards with an empty cite (31). `compile.py` fails a GUIDANCE card that has a cite; GUIDANCE was removed from the 5 cited cards (RP-032, 037, 048, 054, 075). (10-07) |
| CI emulator job | No. The `minifiedTest` smoke runs on the Pixel at phase exits and in pre-push when a device is attached. (10-07) |
| Free vs paid | Premium with a free preview (tiles, titles, one-line answer, title search free; detail, cite, personalisation, pins, share, Pay Audit link Premium). Paywall on only after open points on main rate cards are cleared. (10-07) |

Standing rules: own words only. CITE shows a primary authority only. Conflicts: TR 2014 beats the FAQ, and a
newer FAQ beats an older handbook. Legal interpretation is asked, never guessed.

## Regression-risk controls (all phases)
- **Dark launch.** `LaunchFlags.GUIDE_ENABLED` is false in release until E9. The tab, the Pay Audit link and
  every entry point read it. With the flag off the app behaves exactly as today, and a test asserts 4 tabs.
- **Additive only.** New code goes in new files (`shared/.../guide/`, `composeApp/.../ui/screens/guide/`).
  Existing files get the smallest change, each named in its phase: `App.kt`, `AppBottomBar.kt`,
  `MainViewController.kt` (iOS), `SubscriptionManager.kt` (one enum value), `PayAuditFindingsSection.kt`,
  `AppModule.kt` (one include), `Theme.kt`, `proguard-rules.pro`, `ci.yml`, `git-pre-push.sh`.
- **Characterization tests first.** Before an existing file is edited, tests pin its current behaviour and
  pass on `main`: `AppNavStateSaver` round-trip and truncation, the 4-tab bar, the `NavBridge` lock rule,
  every `FeatureGate` under each `DevOverride`, and Pay Audit findings rendering.
- **Baselines.** Recorded at E1 start and compared at every phase end: test count (may only grow), corpus
  139/139, release APK size and method count (only the bundle size may be added), and Pixel cold start.
  Guide data loads lazily on first open, never at launch.
- **Untouched by design:** parser pipeline, Room schema, billing, backup format.

## R8 and release-build safety
Facts today: release is minified and resource-shrunk; `proguard-rules.pro` has no serialization rules;
`minifyReleaseWithR8` runs in CI only and proves just that R8 *compiles*; no test exercises minified behaviour.
- **No reflection.** Plain `@Serializable` data classes made of strings and lists. No polymorphic or sealed
  serialized types, no `Class.forName`, Koin DSL only.
- **No Guide keep rule (since E2).** Once the UI calls the repository, reachability plus kotlinx-serialization's
  bundled rules keep every model and serializer; Check 1 proves it. The E1 narrow rule was removed (EP item 3).
- **Check 1 (CI and pre-push), new `scripts/check_r8_guide.py`.** After `assembleMinifiedTest` (since E2; release
  keeps the Guide dark, so R8 rightly drops it there), it reads
  the R8 mapping and fails unless every guide model and its serializer survive. It also confirms the
  bundle file is in the shrunk output, byte-identical to the source (optimized resource shrinking).
  `git-pre-push.sh` gains the `minifyReleaseWithR8` step, which neither hook runs today.
- **Check 2, minified runtime smoke.** A `minifiedTest` build type (release R8 config, debug signing) and
  one black-box UiAutomator test in the self-instrumenting `:guideSmokeTest` module (`scripts/run_guide_minified_smoke.sh`): load and validate the bundle, then open Home, a case and a card. Run on the Pixel
  at E2, E5, E8 and E9 exit, and in pre-push when a device is attached. A CI emulator job is added only if the owner wants it.
- **Fail loudly in production.** The validator runs on every load. A failure shows an error with retry,
  never a blank tab, and reports only an error code through `TelemetrySanitizer`.
- **iOS counterpart.** There is no R8, but compose resources must be in the app bundle: an iOS simulator test
  reads the real bundle through `Res.readBytes`.

## Navigation and state management
Why: `Screen` is an enum without arguments, `AppNavStateSaver` saves names only, and on iOS each detail is a
native view controller pushed through `NavBridge.navigateToDetail(Screen)`. The Guide needs ids, so it keeps
its own stack.
- `GuideNavState` holds a stack of `GuideDestination`: Home, Area(areaId), Case(caseId, facet?),
  Card(cardId), Search. It changes only through `push`, `pop`, `popToHome`, `upTo` (breadcrumb) and `selectFacet` (since E3).
- It survives process death through a `listSaver` of plain ids (the `PayAuditSavedState` pattern). Restore
  checks every id against the bundle and cuts the stack at the first unknown entry (the `AppNavStateSaver`
  rule). A restore that runs before the bundle loads is validated when loading ends. No crash, no blank screen.
- It is drawn inline in the Guide tab on both platforms, so the bottom bar stays. Shared navigation only
  gains `Screen.Guide` (tab root) and `Screen.GuideCard` (detail).
- Back: on Android a `BackHandler` pops the Guide stack before leaving the tab. On iOS there is no edge-swipe
  inside a tab, so every level below Home shows an on-screen back header, and the breadcrumb navigates up.
- Re-tapping the active Guide tab calls `popToHome`. This is a callback on `AppBottomBar`, not a navigation rewrite.
- Pay Audit link: `Screen.GuideCard` is pushed as a normal detail (a native view controller on iOS; back
  returns to the finding). The card id is a pending target in the app-scoped `GuideViewModel`, so `Screen`,
  `NavBridge` and the saver carry no arguments. A restored `GuideCard` with no target pops itself. The
  existing lock rule in `NavBridge` still applies.
- State holders: an app-scoped `GuideViewModel` owns the loaded bundle (`StateFlow`, loaded once) and derives
  each screen's state. `GuideSearchViewModel` keeps the query in memory only. The facet lives in `Case`, so
  it survives restore. Expanded details use `rememberSaveable` keyed by card id. Scroll position is kept per stack level
  in `GuideNavState` and its saver (since E3), because the tab's content leaves composition on a tab switch.
- While the app is locked no Guide content is composed (existing rule). Unlocking returns to the saved stack.

## SSOT, scalability and clean architecture
- **Data chain:** `authoring/*.txt` -> `compile.py` -> `rulebook.json` -> new `tools/bundle.py` -> one
  bundle file. Generated, never hand-edited. A test fails if `rulebook.json` changes without re-bundling.
- **Bundle contents:** drops `from`, `open`, `skipped`, `coverage`, `topics`, `uncovered`, `pay_topics_open`;
  adds `unverified` (true when `open` is not empty) and `rates_as_of` (set in `tools/config.py`, month confirmed by the owner).
- **One code path for both platforms:** all logic lives in `shared` commonMain `com.payslipmax.pdfparser.guide`,
  layered as `model/` (serializable types), `data/` (parser, validator, repository) and `domain/` (search,
  facet rule, `CardTemplate` placeholder filler, figure resolver, link map). Only clipboard, share and pin
  storage go through platform code. The UI calls the repository interface and domain functions only.
- **Bundle location:** compose resources in composeApp (`Res.readBytes` works on both platforms); the shared
  parser takes a `String`. The E1 spike confirms this; the fallback is an expect/actual reader in shared.
- **Schema evolution:** the parser checks the bundle `version` and rejects a newer major with an error state.
  There is one named `GuideJson` configuration (`ignoreUnknownKeys`). Chips and facets parse to typed values
  with an Unknown fallback the UI ignores. Area, case and facet ids stay strings, never Kotlin enums, so new
  areas or chapters are data-only changes.
- **Trust chips, one source each:** Rates as of = `RATES` chip + `rates_as_of`; Unverified point =
  `unverified`; No official source = empty `cite` (`GuideCard.hasNoOfficialSource`, fixed in E1); Amended = `AMENDED` chip.
- **Facet chips** show only when a case has more than 7 cards and more than one facet (a pure function).
- **Opening a card:** one entry point, `GuideViewModel.openCard(cardId)`, used by Pay Audit now and by other screens later.
- **DI:** a separate `guideModule` (Koin DSL); `AppModule.kt` only includes it.
- **Test fakes:** `FakeGuideRepository` and a small synthetic bundle go in `shared-test-fixtures`. Logic and UI
  tests use them; only E1 contract tests read the real 402 cards, so a rate edit never breaks unit tests.
- **Storage:** `GuidePinsStorage` interface with Android and iOS implementations and a fake (the `OnboardingStorage` pattern).
- **Performance:** parsing and index building run on an injected dispatcher (the `PayAuditViewModel`
  pattern); lists are lazy and keyed by card id. No regex lookaround or backreference in commonMain, and
  every hot path gets an iosTest timing check.
- **Not done, for simplicity:** no separate Gradle module for the Guide.

---

## E1 Bundle, models, loader and safety nets (nothing visible)
**Goal:** valid Guide data loads offline on both platforms, with baselines and the R8 gate in place.
**Files:** `tools/bundle.py`, `tools/test_bundle.py`, `tools/config.py`; the bundle file;
`shared/.../guide/model/GuideModels.kt`, `data/GuideJson.kt`, `data/GuideBundleParser.kt`,
`data/GuideBundleValidator.kt`, `GuideRepository.kt` plus a lazy implementation in `data/`;
`di/GuideModule.kt`; `FakeGuideRepository` and a synthetic bundle in `shared-test-fixtures`;
`LaunchFlags.GUIDE_ENABLED`; `AppModule.kt`; `proguard-rules.pro`; `scripts/check_r8_guide.py` and its
test; `ci.yml`; `git-pre-push.sh`.
**Tests first:** parser round-trip; an unknown field is ignored, an unknown chip maps to Unknown, a newer major version
gives an error state; validator (no orphan or dangling ids, no `from`/`open`, limits, no raw `{` outside
personal cards); bundle matches compiled data (402/220/182 cards, 9 areas/44 cases, facets, 36 unverified); stale-bundle
guard; repository loads once, lazily; `check_r8_guide.py` against sample mapping files; iosTest
`GuideLoaderIosPerfTest` (parse within a stated time budget).
**Exit:** all gates green, including both R8 checks; baselines recorded in the PR.
**Tech-debt checkpoint:** the GUIDANCE chip (36 cards) and empty `cite` (31 cards) are reconciled with the owner into one rule.
**Security:** the bundle is read-only app data; the parser rejects malformed or oversized input.

**E1 result (2026-10-07).** Done as planned, with these recorded choices:
- Bundle location: compose resource `composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json`
  (251,569 bytes). The spike passed on both platforms (Robolectric reads it through Android assets; the iOS
  simulator reads it through `Res.readBytes` and parses and validates it in about 0.3 s against a 1.5 s budget),
  so the shared expect/actual fallback was not needed.
- `GuideModule.kt` sits in `composeApp/.../di/` beside `AppModule.kt`, because the bundle reader (`Res`) is generated
  there; the repository, parser and validator are in `shared`.
- The parser reads `version` before the typed decode and rejects card-level `from`/`open` on the raw JSON, so a
  newer major or a leaked reviewer field is an error state, never a silent ignore. Error codes: `READ_FAILED`,
  `TOO_LARGE`, `MALFORMED`, `UNSUPPORTED_VERSION`, `INVALID`.
- `bundle.py` ships a whitelist of card fields, so a new internal field never ships by default.
- R8 Check 1 runs after `assembleRelease`, not `minifyReleaseWithR8`: the bundle is an asset and only lands in the
  APK. CI and pre-push both run it, together with the rule-card tool tests (stale-bundle guard).
- Without the keep rule R8 removes every guide model, because no release code calls the repository until E2.
- Baselines (base commit `454021dd`, release APK unsigned, placeholder Gemma): tests shared JVM 770 (debug) / 770
  (release, 1 skipped as before), composeApp JVM 571 / 552, iOS shared 723, iOS composeApp 391; corpus suite green;
  APK 69,443,385 bytes, 55,254 dex method ids. After E1: APK content +70,166 bytes compressed (bundle 63,361,
  dex 6,963), +100 method ids (the serializers the E1 keep rule pins; see EP).

## E2 Guide tab shell, Home and Area tiles (flag on in debug only)
**Goal:** a fifth "Guide" tab with 9 area tiles; an area shows case tiles with counts and a rule-number subtitle.
**Files:** `App.kt` (`Screen.Guide`, `isTabRoot`), `AppBottomBar.kt` (fifth item and re-tap callback),
`GuideStrings.kt`, `Theme.kt` tokens, `ui/screens/guide/GuideNavState.kt`, `GuideNavStateSaver.kt`,
`GuideBackHeader.kt`, `GuideViewModel.kt`, `GuideUiState.kt`, `GuideHomeScreen.kt`, `GuideAreaScreen.kt`,
`GuideTile.kt`; the `minifiedTest` build type and the instrumented smoke test.
**Tests:** characterization tests (saver, 4-tab bar, lock rule) pass on `main` first; `GuideNavState`
push, pop and popToHome; saver round-trip, unknown and corrupt ids cut off, restore before the bundle loads;
switching tabs keeps the stack; re-tap goes to Home; Android back pops the Guide stack first; ViewModel
states (loading, loaded, error with retry); tile counts match the data; flag off means 4 tabs and no Guide route restored.
**Exit:** gates green; Pixel run in debug and `minifiedTest`; labels checked on a small screen; baselines in range.
**Tech-debt checkpoint:** feed and card are placeholder routes, removed in E3.

**E2 result (2026-10-07).** Done, with these recorded choices:
- Guide visibility is one compile-time value, `isGuideEnabled()` = `LaunchFlags.GUIDE_ENABLED` or a debug build
  (`BuildConfig.GUIDE_PREVIEW` on Android, true for debug and `minifiedTest`; `isDebugBuild()` on iOS). `App` hoists
  `GuideNavState` only when it is on; it is null otherwise, which hides the tab, blocks the route and lets R8 drop all
  Guide code from release. `guideModule` is included in `appModule` only when it is on. A saved `Guide` tab restores
  to Home when the Guide is off.
- `GuideBackHeader.kt` was not created: the existing `ScreenBackHeader` is already the back-plus-title SSOT; the
  breadcrumb arrives with E3. Case, card and search are `GuidePlaceholderScreen` (one file, removed in E3).
- Also touched (smallest change each): `AppOnboardingOverlay.kt` and `MainViewController.kt` (pass the state; exhaustive
  `when`), `AppModule.kt`, `composeApp/build.gradle.kts`, `settings.gradle.kts`, `libs.versions.toml`, root build file.
  `App.kt` reached 298 lines, so `AppNavStateSaver` moved unchanged to `AppNavStateSaver.kt` (now 287), and
  `MainScaffold`/`ScreenContent` were split (`MainBottomBar`, `TabRootContent`) to stay under 50 lines.
- Check 2 lives in its own self-instrumenting module: an in-app androidTest shares the app's R8-renamed Kotlin
  stdlib and crashed before running. It reads the UI copy through a generated `SmokeStrings` (from `GuideStrings.kt`).
- Small screen: real area and case titles and the five tab labels pass at 360dp (Robolectric native graphics) and on
  the Pixel at 360dp. At 320dp "Dashboard" wraps to two lines (EP item 7).
- Baselines (Pixel 9). Cold start, `am start -W` WaitTime median of 10: Play 1.3.0 with real data 3,100 ms (EP 1);
  same empty state E1 release 193 ms, E2 release 190 ms, E2 `minifiedTest` with the Guide 188 ms. Release APK
  vs E1: content -5,897 bytes, 55,354 -> 55,269 method ids (base 55,254: the bundle plus about 0.9 KB of app-shell
  dex). Tests: shared JVM 800 / 800 (1 skipped as before), composeApp JVM 612 / 591, iOS shared 753, iOS composeApp 416.

## E3 Feed, facet chips and card screen
**Goal:** case tile, then a feed with breadcrumb and facet chips, then the full card.
**Files:** `GuideFeedScreen.kt`, `GuideFacetChips.kt`, `GuideCardScreen.kt`, `GuideCardSections.kt`,
`GuideCardHeader.kt`; `shared/.../guide/domain/GuideFeedLogic.kt` and `CardTemplate.kt`.
**Tests:** facet rule at its edges (7 vs 8 cards, a single facet); filtering keeps order; the chosen facet and the
scroll position survive restore and tab switches; the breadcrumb goes up; every card shows its required
sections, cite in monospace and details collapsed; placeholder bullets go to the hidden "your figure" slot
and are never shown raw; the "also relevant here" links resolve (ltc-rules, 6 cards).
**Exit:** gates green; Pixel walk-through against the approved preview; accessibility labels on tiles and chips.
**Tech-debt checkpoint:** E2 placeholders removed.

**E3 result (2026-10-07).** Done, with these recorded choices:
- Domain in `shared/.../guide/domain/`: `GuideFeedLogic.kt` (feed order, facet rule `FACET_CHIPS_ABOVE_CARDS = 7`,
  filter, and `effectiveFacet`, which ignores a restored facet the feed cannot show, so it never goes empty) and
  `CardTemplate.kt` (placeholder bullets split into `figureTemplates`, the hidden "your figure" slot E6 fills). Added
  `GuideIndex.kt`: id lookups built once per load, so no screen scans the 402 cards.
- Validator: a placeholder is allowed only in a bullet (title, answer, cite and details are rejected even on a
  personal card), because only a bullet can move to the hidden slot. The E1 test that allowed one in `details` now
  puts it in a bullet. The shipped bundle already complies (placeholders only in the key bullets of T181 and T254).
- Breadcrumb: `GuideCrumbs.kt` (pure) and `GuideBreadcrumb.kt` (also beyond the listed files) sit above the existing
  `ScreenBackHeader`, which stays the back SSOT. A card leads back to the feed it was opened from (an "also relevant
  here" card to that feed, not its home); a card with no feed below it leads to its home case. New
  `GuideNavState.upTo` pops to a level on the stack, or builds Home plus the path when it is not there. Links keep
  the 48dp touch target.
- Scroll: kept per stack level in `GuideNavState` and saved as `scroll|level|index|offset` entries, not with
  `rememberSaveable`, because the tab's content leaves composition on a tab switch. A popped level's scroll is
  dropped, and a new facet starts at the top. The area list uses the same helper. Expanded details use
  `rememberSaveable` keyed by card id (kept on restore and while scrolling; collapsed again after a tab switch).
- Card in E3: facet, answer box, Key points, Attach, Watch out, Authority (monospace with a side rule), Details
  collapsed, and the preview's closing note ("Guidance from published rules, not a sanction..."). By plan, these are
  later: trust chips and the unverified warning line (E5), the "your figure" line (E6), and copy cite, share, pin (E8).
  The Watch out heading uses the new `GuideColors.watchOut()` token in `Theme.kt` (the preview's light and dark amber).
- Search keeps `GuidePlaceholderScreen` until E4 (nothing opens Search before E4). The smoke's last step now opens the
  first case's first card and waits for "Key points".
- Gates (2026-10-07): `check` (both variants, corpus included), `ktlintCheck`, tech-debt audit, `iosSimulatorArm64Test`,
  `linkDebugFrameworkIosSimulatorArm64`, `assembleRelease` and `assembleMinifiedTest` plus `check_r8_guide.py` (5
  models and serializers kept, bundle byte-identical), rule-card tool tests: all green. New iosTest
  `everyFeedAndCardBuildsWithinBudgetOnNative` (index, 44 feeds, 402 cards): 63 ms against 1.5 s.
- Baselines. Tests: shared JVM 811 / 811 (1 skipped as before), composeApp JVM 642 / 619, iOS shared 764, iOS
  composeApp 435 (all grew from E2). Release still has no Guide class in its mapping; method ids 55,269 (E2, rebuilt
  like for like) -> 55,273. The +4 are Compose library methods (`FlowRow` alignment, `luminance`, `FilterChip`
  defaults, synthetic lambdas) that R8 optimises differently because Guide code is reachable until the
  `GUIDE_PREVIEW` constant is folded; the same effect E2 recorded as app-shell dex. Cold start not re-measured (no
  device; E3 adds nothing at launch, the Guide still loads on first open).
- Pixel 9: the updated smoke and the walk-through against the preview passed (EP 9, closed).

## E4 Search (secondary)
**Goal:** a search icon on Home that finds words and rule numbers ("177", "177B", "Rule 114").
**Files:** `shared/.../guide/domain/GuideSearchIndex.kt`, `GuideRuleNumberParser.kt` (plain string scans),
`ui/screens/guide/GuideSearchScreen.kt`, `GuideSearchViewModel.kt`.
**Tests:** "177" does not match "1770"; case and punctuation are ignored; empty and one-letter queries; a title match outranks
a details match; the query is never stored, saved or logged (gone after simulated process death); opening a result pushes
the card and back returns to the results; iosTest timing over all 402 cards and a fixed query set.
**Exit:** gates green, including the iOS timing test. **Tech-debt checkpoint:** ranking weights in one constant block.

**E4 result (2026-10-07).** Done, with these recorded choices:
- Domain in `shared/.../guide/domain/`: `GuideRuleNumberParser.kt` (word scan, rule numbers, number matching; no regex) and
  `GuideSearchIndex.kt` (the index and `GuideSearchRanking`, the one block of weights: rule number 100, title 60, answer 30,
  bullets 20, details 10, minimum query 2 characters). A card scores the sum of its best field per query word; equal scores
  keep bundle order. Every word must match (AND). Letter words match the start of a word ("allow" finds "allowance"); number
  words match whole numbers. Cite and case-line text are not searched as prose, only for rule numbers.
- Rule numbers are read only after "Rule" or "Rules" in a cite (and in the rule line of the card's home case, so the 31
  cards with no cite are still found by their case's rule). Lists and ranges work ("Rules 94, 95 and 100 to 101"; "88 to 91"
  gives 89 and 90, spans over 50 are not expanded); dates and letter numbers are never read as rules. "177" finds 177 and
  177A to 177Z (one family) but never 1770; "177B" finds only 177B; a leading "Rule" changes nothing. A single digit alone is
  one character and is not searched; "Rule 2" works.
- `GuideSearchViewModel` is an app-scoped Koin single beside `GuideViewModel`. The query lives only in its memory (not in
  `GuideNavStateSaver`, which writes just `search`; the class has no `CrashReporter`; capped at 100 characters). Each visit from
  Home clears it; coming back from a card keeps the query, the results and the scroll place (the Search level's scroll is
  kept in `GuideNavState` like the others). The index is built lazily on the first search (`Ready.searchIndex`), never on load.
- UI: a search icon in the Home header, `GuideSearchScreen.kt` (back header, auto-focused field only on a fresh search, IME
  Search key, clear button, polite live-region count, results as the existing card rows with the home case as the pill). Back
  from a card or the screen pops one level on both platforms. Strings in `GuideStrings.kt`.
- Debt removed: `GuidePlaceholderScreen.kt` and `GuideStrings.comingNext` deleted (EP 8); `searchTitle` stays as the real
  screen title. The minified smoke gained a search step (tab re-tap to Home, search a card's own title, open it from the results).
- Gates (2026-10-07): `check -x iosX64Test -x iosSimulatorArm64Test` (both variants, corpus, lint) and `ktlintCheck`,
  tech-debt audit, `iosSimulatorArm64Test`, `linkDebugFrameworkIosSimulatorArm64`, `assembleRelease` and `assembleMinifiedTest`
  plus `check_r8_guide.py` (5 models and serializers kept, bundle byte-identical), the 21 rule-card tool tests: green.
  Pixel 9: `run_guide_minified_smoke.sh` passed; a visual check of search on the Pixel showed the field focused with the search
  key, "Rule 177" giving 54 results.
- iOS timing (debug simulator): index 22 ms; 38 realistic queries (nine whole queries plus two phrases typed letter by
  letter, a search per keystroke) 112 ms, about 3 ms each; budgets 1.5 s each. A first version replayed all 700 correctness
  searches inside the timed block and took 2.5 s; that is a workload for correctness, so it now runs untimed on iOS, while
  the timed test uses the realistic set.
- Baselines. Tests: shared JVM 833 / 833 (1 skipped as before), composeApp JVM 658 / 635, iOS shared 786, iOS composeApp 445 (all grew from E3). Release has no Guide
  class in its mapping; method ids 55,273 (E3) -> 55,295 (library classes such as `KeyboardActions` and `LiveRegionMode`, which
  R8 keeps while the `GUIDE_PREVIEW` constant is not folded; the same effect E2 and E3 recorded); release APK 69,539,777
  bytes. Cold start not re-measured: E4 adds nothing at launch (the search objects are created when the Guide first opens).
- Not done by design: highlighting the matched words, recent searches (the query is memory only by owner decision), fuzzy
  or typo matching, searching cite text.

## E5 Trust chips, Premium gating and staleness nudge
**Goal:** the four chips wherever a card appears, and the Guide behind the existing Premium entitlement.
**Files:** `GuideTrustChips.kt`, `FeatureGate.CLAIM_GUIDE` (the `hasAccess` logic is untouched),
`rememberHasAccess`, the existing `PremiumUpgradeSheet*`, a staleness threshold constant.
**Owner decision (2026-10-07), Premium with a free preview.** (Paywall rule and 9-month nudge confirmed the same day; see EP 13, 14.)
Free: all area and case tiles, card titles, the one-line answer, search over titles and rule numbers.
Premium: key points, attach, watch out, cite, details, "your figure", pins, copy cite, share as claim note,
and the Pay Audit link. The gate is one `FeatureGate` value, so the split can be loosened or tightened later
without rework. Free-launch mode keeps everything open, as today. The paywall is turned on only after the
owner has cleared the open points on the main rate cards (including RP-088), because charging for cards
flagged "Unverified point" is a trust risk.
**Tests:** all existing gates pinned first; each surface with the gate on, off and FORCE_FREE; a locked UI
state holds only title and one-line answer (key points, cite and details are absent from the state, not just
hidden); search results for locked users show titles only; the nudge appears only past the threshold (injected clock).
**Exit:** gates green; `minifiedTest` smoke on the Pixel. **Tech-debt checkpoint:** none carried.

**E5 result (2026-10-07).** Done, with these recorded choices:
- **Gate:** one new value, `FeatureGate.CLAIM_GUIDE`; `SubscriptionManager.hasAccess` is unchanged (the existing loops over every gate, with
  FORCE_PRO, FORCE_FREE, free-launch and release, now cover it, plus one explicit test). The owner's "paywall on only after the open
  points are cleared" is a separate switch, `LaunchFlags.GUIDE_PAYWALL_ENABLED = false` (pinned by `GuidePaywallFlagTest`).
  `guideUnlocked(hasAccess, devOverride, paywallEnabled)` in `GuideAccess.kt` is the one decision: unlocked when the user has
  access, or the paywall is off (except QA's `FORCE_FREE`, so the locked screens can be seen; the override is inert in production).
  Free-launch mode keeps everything open as today. `GuideTabRoute` wires it to the existing `PayslipUpgradeSheet`; `GuideTab` takes a
  `GuideAccess(isUnlocked, onUnlock)`, so the Guide never touches billing.
- **Locked state holds only the free half.** `GuideCardContent` carries title, one-line answer, facet and trust; the paid half is
  `GuideCardContent.full: GuideCardFull?` (key points, attach, watch-out, cite, details), null when locked, so no screen can draw it by
  mistake. The locked card shows `GuideLockedPanel` (what Premium adds and an "Unlock with Premium" button) in its place.
- **Search (EP 10 closed).** `GuideSearchScope { FULL, PREVIEW }`: `PREVIEW` reads titles and rule numbers only (not the answer, key points,
  attach, watch-out or details), the same ranking. `GuideSearchViewModel` starts locked and `GuideTab` sets it from `access`, so a
  screen that forgets can only search less. A rule-number search still matches the cite and the case's rule line for free users (the
  owner's "rule numbers" scope); the cite text itself is not shown.
- **Trust chips (one source each) on every card row (feed, search) and the card screen:** `GuideTrust.of(card, ratesAsOf)` = Rates as of
  (RATES chip plus the bundle's `rates_as_of`, shown as "Rates as of Jan 2026"), Amended, Unverified point (`unverified`), No
  official source (empty cite). Chips are flags and a date, free for everyone. An unverified card also shows the line "This point is
  still being checked...". All copy is in `GuideStrings`; the warning chip uses the existing `GuideColors.watchOut()` token.
- **Staleness nudge.** `GuideStaleness.isStale(ratesAsOf, nowMillis)`; `STALE_AFTER_MONTHS = 9` (DA is revised twice a year, so
  by nine months a revision has been announced). The clock is injected into `GuideViewModel` (`nowMillis`, default
  `currentTimeMillis`). The nudge shows on a rate card only, as "These rates are from Jan 2026 and may have changed since. Check
  the latest order before you claim."; it never states a new rate. With `rates_as_of` 2026-01 it shows from Oct 2026. Threshold is EP 13.
- **Catalog.** The exhaustive `featureMeta` has a `CLAIM_GUIDE` row, but `premiumFeatureCatalog()` lists it (Premium screen, hub
  bullets) only when `isAdvertised`: the Guide exists in the build and its paywall is on. Dark or free, the Guide is not for sale, so
  Premium does not promise it.
- Search hint copy now reads "Search by topic, or by a rule number...", true for free and paid users.
- Tests added: shared 17 (trust, staleness, search scope) + flag and gate tests; composeApp: access table, locked vs unlocked card state
  and UI, chips on rows and cards, nudge before and after the threshold (injected clock), locked search on screen, three real-app
  tests (QA `FORCE_FREE` locks the card and Unlock opens the upgrade sheet; the debug default is open; flipping the override
  unlocks the open card), the real 402-card contract (chip counts 36 unverified, 31 no source, locked cards hold no `full`, the preview
  never finds a card by a word only its locked text holds), and an iOS test of the preview queries (timed) and the same contract on
  Native. Mutation checks: making `cardContent` ignore `unlocked`, or the search ignore its scope, fails 7 tests.
- Gates (2026-10-07): `check -x iosX64Test` (both variants, corpus included), `ktlintCheck`, tech-debt audit (37 files),
  `iosSimulatorArm64Test`, `linkDebugFrameworkIosSimulatorArm64`, `assembleRelease` and `assembleMinifiedTest` plus
  `check_r8_guide.py` (5 models and serializers kept, bundle byte-identical): green. Release has no Guide class in its mapping and
  the release APK is 69,539,777 bytes, identical to E4 (the Guide is still dark). Pixel 9: `run_guide_minified_smoke.sh` passed; by
  eye, an unverified card (HBA) shows its amber "Unverified point" chip on the search row and on the card with the "still being
  checked" line, and an ordinary card shows no chip.
- Baselines. Tests: shared JVM 852 / 852, composeApp JVM debug 683 / 683 and release 657 / 657, iOS shared 805, iOS composeApp 458
  (all grew from E4). iOS preview search, 38 realistic queries: 34 ms (budget 1.5 s). Cold start and method count not re-measured
  (E5 adds nothing at launch; EP 12).
- Locked panel checked on the Pixel afterwards (debug build, Force Free); see EP 15. Search scope edge closed (EP 16).
- Not done by design: the chips do not filter or sort; no per-card "read" memory; no price on the locked panel (the existing sheet
  shows the store price).

## E6 Personalisation from the Pay Audit profile
**Owner decisions for E6 (2026-10-07/08, all closed before any code).**
| Topic | Decision |
|---|---|
| DA escalator | Step rule: multiplier = 1 + 0.25 x floor(DA / 50). DA 0-49% x1.00, 50-99% x1.25 (today, DA 60%), 100-149% x1.50. Stored once in `figures.json`, not in Kotlin. |
| DA source | The DA of the latest payslip month (existing Pay Audit source). No dated DA table to maintain. No payslip DA means no line. |
| Profile month | The latest payslip month supplies `level`, `tptaCity`, basic pay and DA. The line says which month it is based on. |
| Unknown facts | Show the base figure with its assumption named, never a guess: CTG "about Rs X (80% of last basic, move of 20 km or more)"; food "Rs Y a day, full day, before taxes". The card's bullets already cover the other cases. |
| Food rate (T181) | Only the levels the card lists: 9-11 base 900, 12-13B base 1,000, 14+ base 1,200. Levels below 9 get no line. Amount is before taxes. |
| Transport allowance (P051) | Only levels 10-13A and 14+. **Revised 2026-10-08:** Pay Audit's `tptaCity` is a class (`HIGHER` or `OTHER`, recovered from the payslip's own TPTA base, null at level 14+), not a city name, so the card's city list cannot be matched. Use the class: `HIGHER` 7,200, `OTHER` 3,600 (levels 10-13A); level 14+ is a flat 15,750 with no class needed; no class at 10-13A hides the line. Amount = base x (1 + DA/100), no 25% step (same shape as `TptaEntitlementAuditor`; owner confirmed). |
| HRA (P116) | City class inferred from the payslip's HRA / basic ratio: 24/27/30% = X, 16/18/20% = Y, 8/9/10% = Z. A ratio that fits none hides the line. **Added 2026-10-08:** a valid rate that is not the rate for that class at the payslip's DA (24% when DA is 60%) also hides the line; that mismatch is Pay Audit's finding. The HRA amount is on the payslip, not in `TimelineMonth`, so the provider reads it from the same month's `ParsedPayslip`. The line restates the rate (a cross-check), it adds no new input and no new UI. |
| Approval | Owner approves `figures.json`. Every figure carries value, effective date and the primary letter (same standard as CITE); `compile.py` fails a figure with no source. No figure ships unapproved. |
| Figures approved | **2026-10-08**, all four, evidence labels unchanged: food and CTG rest on the primary MoD letter text (read); transport allowance and HRA rest on the P&A Handbook 2023 (it prints the letter numbers and rates; letter text not in the source folder), HRA step dates 01-07-2021 and 01-01-2024 are web-corroborated only. The labels stay in `figures.json` and in the EP table. |
| Food wording | Owner kept "before taxes" (2026-10-08). The letter text read does not mention taxes, so that phrase rests on the owner decision (EP 17). |
| Not asked, default | A figure whose source letter is unverified is not asked about here; if one turns up while drafting, stop and ask the owner. |

**Goal:** a "your figure" line on T181 (food rate), T254 (CTG), P051 (transport allowance) and P116 (HRA).
**Prerequisite:** a small authored `figures.json` (base rates, DA escalator, effective dates), validated by
`compile.py`, bundled, and approved by the owner. No rupee figure is written into Kotlin.
**Files:** `shared/.../guide/domain/PersonalFigureResolver.kt`, `GuideProfile.kt`; one composeApp provider
that reads the latest `ServiceTimeline` month (`level`, `tptaCity`) and DA from the existing source.
**Tests:** each `personal=` spec with and without a profile; DA at 50% and 58%; missing data hides the line and
the card stays complete; the effective date is shown.
**Exit:** gates green; Pixel check with a real profile. **Tech-debt checkpoint:** no copy of Pay Audit logic.
**Security:** figures are computed on the device and never shared.
**Premium (from E5):** the "your figure" line is part of the paid half. It is drawn only when `GuideCardContent.full` is set (the
card is unlocked), and a locked state must not hold the resolved figure.

### E6 phase plan (written 2026-10-08, before any Kotlin)
Branch `feature/guide-e6-personal-figures` off `main`. Gate for this phase is "Rules for every phase" plus: `assembleRelease`,
`assembleMinifiedTest`, `check_r8_guide.py`, the Pixel 9 smoke, and a Pixel check of the line with the real profile (debug build).

**Existing code read first.** `ServiceTimeline`/`TimelineMonth` (`level`, `basicPay`, `daPercent`, `tptaCity`), `ServiceTimelineBuilder`
(DA = DA / (Basic + MSP) as a whole percent; null when arrears are folded in), `TptaEntitlementAuditor` (base x (1 + DA/100)),
`PayLevel` (L10, L10B, L11, L12A, L13, L13A, L14-L18), `CardTemplate`/`GuideCardBody.figureTemplates`, `GuideCardContent.full`,
`GuideViewModel.card`, `GuideTab`, `GuideModule`, `GuideBundleValidator`, `bundle.py`, `compile.py`, `check_r8_guide.py`,
`SyntheticGuideBundle`, `GuideBundleContract`. Facts that shaped the plan: `tptaCity` is a class, not a city; HRA is on the payslip
only; the four cards carry `personal=` specs but only T181 and T254 have placeholder bullets.

**Design (one source each).**
- *Data:* `figures.json` (authored, approved) -> `compile.py` validates (card exists and has `personal`, ISO dates, authority present,
  approved date present, no overlapping level bands, HRA steps ascending) -> `bundle.py` ships it as `figures` (additive, bundle
  version stays 1; it fails if any figure is unapproved) -> `GuideBundle.figures`. No rupee figure or rate in production Kotlin.
- *Model:* the `@Serializable` figure types live in `GuideModels.kt`, so `check_r8_guide.py` covers them with no script change.
- *Domain (shared, commonMain):* `GuideProfile` (level label, basic, DA, TPTA class, HRA, payslip month; plain types),
  `GuideProfileBuilder.from(history)` (calls `ServiceTimelineBuilder`; latest timeline month; no copy of Pay Audit logic),
  `PersonalFigureResolver.resolve(cardId, figures, profile)` -> `PersonalFigure?` (null = hide the line). Integer arithmetic, no regex.
- *UI (composeApp):* `GuideProfileProvider` (flow of `GuideProfile?` from `PayslipRepository.getAllPayslips()`, a Koin single in
  `guideModule`), `GuideViewModel.card(..., profile)` puts the resolved figure inside `GuideCardFull.figure` (so locked state holds
  none), `GuideYourFigure.kt` draws a "Your figure" block above Key points, copy only in `GuideStrings`. The provider is collected only
  for an unlocked card that has a figure spec; a locked card never touches the payslips.
- *Hiding:* any missing input (level, basic, DA, class, HRA) or unlisted level returns null; the card stays complete as today.

**Tests first (each fails before its code).** Python: figure with no authority, no approved date, unknown card, or overlapping bands
fails `compile.py`; `bundle.py` ships figures; unapproved figure blocks the bundle; stale bundle guard. Kotlin shared: resolver per card
with and without a profile; DA 50 and 58 (food 1,125 and 1,125 / base x 1.25, DA 100 -> x1.5 as a boundary); levels below the listed
bands get no line; TPTA `HIGHER`/`OTHER`/level 14+/null class; HRA ratio to class, a ratio that fits none, a lower tier than DA warrants
(hidden); CTG basic only, no DA needed; profile builder takes the latest month, null DA/level pass through; figures validated by the
bundle validator (unknown card, bad level label). composeApp: the line appears only when unlocked, locked state has `full == null` so no
figure, missing profile leaves the card complete, effective date and payslip month shown, nothing recorded by `FakeCrashReporter`.
Contract on the real bundle: four figures, every `PayLevel` lands in at most one band, transport bases equal Pay Audit's
`TptaCityClass` bases (so the two sources cannot drift). Test oracles hold expected rupee values by design; production code holds none.

**R8 / iOS risks.** New `@Serializable` types: `minifyReleaseWithR8`-equivalent proof is `assembleMinifiedTest` + `check_r8_guide.py`
(release keeps the Guide dark). `figures` is a `Map<String, ...>` of plain types; no reflection. iOS: arithmetic and the existing
`ServiceTimelineBuilder` only, but the builder now runs on card open, so an `iosTest` timing check feeds it a full history (budget as
`ParserUtilsIosPerfTest`). No lookaround or backreference.

**Regression controls.** Dark launch unchanged (`GUIDE_ENABLED`, debug only). Characterization: existing card-screen tests (locked,
unlocked, placeholder bullets hidden) stay green first. Pay Audit files are not edited. Corpus 139/139 unchanged.

**Navigation and state.** No navigation change. The figure is derived on each composition from the saved stack, the bundle and the
payslips; it is not saved, so nothing survives process death except what already does. Lock screen: no Guide content is composed.

**Versioning, DI, fixtures, storage.** Bundle `version` stays 1 (unknown key ignored by older builds). One new Koin single
(`GuideProfileProvider`) in `guideModule`. `SyntheticGuideBundle` gains a figures block and a `FakeGuideProfileProvider` goes in
`shared-test-fixtures`. No storage, no Room change.

**Security.** Computed on device from payslips already on device; nothing is sent, logged, shared (E8 share text excludes it) or put
in crash keys. A test asserts the reporter sees no call when figures resolve.

**Owner decisions:** none open (all answered 2026-10-08, see the table above).

**E6 result (2026-10-08).** Done, with these recorded choices:
- **Data chain.** `figures.json` (owner-approved 2026-10-08, evidence label on every figure) -> `tools/figures.py` (`compile.py` fails a figure with no authority, a bad date, an unknown evidence level, an unknown card or a card without `personal=`, a level claimed twice, HRA steps that do not rise, or a `personal=` card with no figure) -> `bundle.py` ships only value, date, bands and assumption as `figures` (it stops if any figure is unapproved; authority and evidence stay in the repo). Bundle `version` stays 1. 18 tests in `test_figures.py`, 3 more in `test_bundle.py`.
- **Resolver.** `PersonalFigureResolver` (shared, integer arithmetic, no regex) returns `FoodRate`, `Ctg`, `Transport`, `Hra` or null (hide). Rules as decided: food = base x (100 + 25 x floor(DA/50)) / 100, listed levels only; CTG = 80% of latest basic, no DA needed; transport = base x (100 + DA) / 100 with the `HIGHER`/`OTHER` class from Pay Audit (flat for level 14+); HRA = class whose rate at the payslip's DA equals HRA / basic within 0.1 point, else hidden (so a valid-but-lagging rate such as 24% at DA 60% is hidden). Any missing level, basic, DA, class or HRA hides the line. An unknown `mode` hides it too (forward compatible).
- **Profile.** `GuideProfileBuilder` calls Pay Audit's own `ServiceTimelineBuilder` and takes the latest usable month, so level, DA and the TPTA class are read by one piece of code; only the HRA amount is read from that month's payslip. `PayslipGuideProfileProvider` is a cold flow over `getAllPayslips()`.
- **Locked state.** The figure is `GuideCardFull.figure`, so a locked `GuideCardContent` (`full == null`) cannot hold it. `rememberGuideProfile` subscribes to the payslips only while an unlocked card with a figure is on screen and drops the profile when it leaves (tests: locked card and figure-less card never subscribe; reading stops on back). Nothing is written to telemetry (test).
- **UI.** A "Your figure" block above Key points: amount, how it was worked out (level, base, DA step, the assumption named), and "From your Sep 2026 payslip, DA 60%. Rate in force from Jul 2017." Copy is in `GuideStrings`; colours are theme tokens; `GuideFigureText.kt` is pure so the facts each line must carry are unit-tested. The two placeholder bullets that used to sit on T181 and T254 are deleted (EP 22).
- **No rupee figure in production Kotlin.** The only numbers in production code are the integer rounding constants (100, 50) and the HRA tolerance (0.1). Test oracles hold expected amounts by design. `SyntheticGuideFigures` is checked equal to the shipped figures, and the contract pins the bundle's transport bases to Pay Audit's `TptaCityClass`, so the two sources cannot drift.
- **Owner answers (2026-10-08, first round):** ship transport and HRA on handbook evidence; HRA step dates correct; use the `tptaCity` class directly; transport is plus DA with no step; keep "before taxes" (no source, EP 17); hide HRA on a tier mismatch; approve all four. Second round (same day, closing the sprint): keep the food wording as their decision (EP 17), accept handbook evidence as final and confirm the HRA step dates (EP 18), accept the three limits (EP 21), delete the placeholder bullets (EP 22), correct the P116 OM number, iOS check at E9 (EP 20), merge and push.
- Tests added: shared 29 (resolver 18, profile 4, provider 5, validator 2), composeApp 22 (card state 7, text 6, on screen 6, contract and iOS 3), Python 21. Mutation checks: a wrong DA step and a wrong HRA tier rule failed 7 of 18 resolver tests; ignoring the lock in the profile read, or answering `hasFigure` true for every card, failed 3 of 13 UI tests.
- Gates (2026-10-08): `check -x iosX64Test` (both variants, corpus included), `ktlintCheck`, tech-debt audit (31 changed files, full scan 395), `iosSimulatorArm64Test`, `linkDebugFrameworkIosSimulatorArm64`, `assembleRelease`, `assembleMinifiedTest` and `check_r8_guide.py` (10 models and serializers kept, up from 5; bundle byte-identical): green. After the gate run, the only change was one equality assertion in the contract test, rerun green with ktlint.
- Baselines (after the closing round on the same day). Tests: shared JVM 878 (E5: 852; three placeholder tests were deliberately removed with EP 22), composeApp JVM debug 705 / release 679 (683 / 657), iOS shared 831 (805), iOS composeApp 473 (458); none failed. The one skipped test is the existing `GemmaModelPathsAndroidTest` debug-sideload case in the shared release variant (`assumeTrue(isDebugBuild())`; it runs in the debug variant). Release APK 69,540,177 bytes (E5: 69,539,777, +400; the Guide is still dark). iOS: 10 profile builds over 140 payslips 12 ms (budget 1.5 s). Cold start not re-measured (EP 12).
- Pixel 9 (2026-10-08): the Play 1.3.0 build was uninstalled from profile 0 after the owner confirmed a `.pcda` backup, the `minifiedTest` build installed, and `run_guide_minified_smoke.sh` passed. After the owner restored the backup, all four cards were checked on the real September 2026 payslip (BPAY 1,49,000, MSP 15,500, DA 98,700 = 60%, TPTA 3,600, no HRA but RHA 21,125): food Level 12A shows 1,250 a day (matches the FAQ for a Major), CTG about 1,19,200 (80% of 1,49,000), transport 5,760 a month (3,600 plus 60%, other places), and HRA correctly shows no line (no HRA on the payslip). Screenshot checked by eye: the block reads cleanly above Key points, with the stale-rates nudge above it. **The phone now runs the debug-signed `minifiedTest` build, not the Play build; returning to Play needs a backup, an uninstall and a Play install.**
- **Closing round (2026-10-08, same sprint).** The owner answered every open item (EP table). Changes: the two placeholder bullets on T181 and T254 deleted, `figureTemplates` removed, and the validator now rejects any raw `{` on any card (new test; two stale placeholder tests rewritten or removed); the P116 cite now reads "2(5)/2017-E.II(B)"; the HRA step dates are `OWNER_CONFIRMED` with a `confirmed` date (enforced by `figures.py`, 3 more tests); `compile.py`, `render_md.py` and `bundle.py` rerun. The full gate chain was rerun green on the final tree and the minified smoke passed again on the Pixel, and the food card was rechecked by eye.
- **Phase Handoff.** Debt incurred and how it was resolved in the phase: (1) the test copy of the figures could drift from the shipped data: resolved by an equality assertion in the contract; (2) two sources for the transport bases (Pay Audit's enum and `figures.json`): resolved by the contract test that fails on any difference; (3) a profile held in an app-scoped object after the card closes: resolved by reading it only inside the card's composition; (4) two never-shown placeholder bullets and `figureTemplates`: deleted in the same sprint (EP 22), the validator now rejects any raw placeholder. Closed with the owner on 2026-10-08: EP 17, 18, 19, 21, 22 (see the table). Open: EP 20 (iOS walkthrough, E9).
- Not done by design: no city picker, no computed 100% CTG case, no figure in the share note (E8 excludes profile figures), no figure for locked users.

## E7 Pay Audit link
**Goal:** a finding opens its matching card, and back returns to the finding.
**Owner review first:** the table mapping each finding type (and pay line) to a card, for MISSING_ALLOWANCE,
TPTA_ENTITLEMENT, ARREARS_AUDIT, INCREMENT_MISSED and MSP_SHORTFALL.
**Files:** `shared/.../guide/domain/GuideLinkMap.kt`, one link control in `PayAuditFindingsSection.kt`,
`Screen.GuideCard` in `App.kt` and its iOS host case in `MainViewController.kt`, `GuideViewModel.openCard`.
**Premium (from E5):** the link is a paid feature (`guideUnlocked`); a locked user sees `PayslipUpgradeSheet`, never a card.
**Tests:** findings rendering pinned first; every mapped id exists; the link pushes `GuideCard` and back returns
(a `NavBridge` test covers the iOS path); a restored `GuideCard` with no target pops itself; the link is blocked while
locked; a new finding type with no decision fails a test; flag off means no link; users without Premium see the paywall.
**Exit:** gates green; Pay Audit suites unchanged. **Tech-debt checkpoint:** none carried.

### E7 phase plan (written 2026-10-08, before any Kotlin)
Branch `feature/guide-e7-pay-audit-link` off `main`. Gate: "Rules for every phase" plus `assembleRelease`, `assembleMinifiedTest`,
`check_r8_guide.py`, the Pixel 9 smoke, and a Pixel check with the real profile (a real finding opens its card; Back returns to the finding).

**Owner decisions for E7 (2026-10-08, closed before any code).**
| Finding / pay line | Card | Title |
|---|---|---|
| MISSING_ALLOWANCE, `houseRentAllowance` | `RB-SS-P114` | HRA: who gets it, and at which place's rate? |
| MISSING_ALLOWANCE, `militaryServicePay` | `RB-C13-05` | Military Service Pay: who gets it and how much? |
| TPTA_ENTITLEMENT | `RB-SS-P051-rates` | Transport allowance rates by pay level |
| ARREARS_AUDIT (`arrearsDa`, `arrearsTptaDa`) | `RB-RP-052` | Dearness Allowance: when is it revised and what counts as pay? |
| INCREMENT_MISSED | `RB-C13-08` | On which date is my annual increment due? |
| MSP_SHORTFALL | `RB-C13-05` | Military Service Pay: who gets it and how much? |
Also `SALARY_LOSS` on `arrearsDa` / `arrearsTptaDa` (the under-paid arrears issue Pay Audit adds outside `PayAuditFindingTypes`) -> `RB-RP-052`.
No card covers DA arrears or "HRA stopped because I took quarters"; the nearest cards are linked (owner accepted). Link states: owner chose
"issues and waiting, verified rows only for arrears"; in code every verified row is `ARREARS_AUDIT`, so the rule is "every row whose finding has a card".
Any other (type, field) has no link. Not chosen: `RB-SS-P114-gap`, `RB-SS-P116-rates`, `RB-SS-P047`, `RB-SS-P014`.

**Existing code read first.** `Screen`/`App.kt` (`ScreenContent`, `DetailContent`, `isTabRoot`), `AppNavState`, `NavBridge`, `AppNavStateSaver`,
`MainViewController` (`IosNavHost.detailViewController`), `PayAuditScreen`, `PayAuditFindingsSection`/`Logic`, `PayAuditFindingTypes`, the five
auditors, `GuideViewModel`, `GuideTabRoute`, `GuideAccess`, `GuideCardScreen`, `rememberGuideProfile`, `GuideModule`, `GuideBundleContract`,
`SyntheticGuideBundle`.

**Design (one source each).**
- *Mapping (SSOT, shared):* `GuideLinkMap.cardFor(type, field): String?` over one decision table; `GuideLinkMap.decidedTypes` must cover every
  `PayAuditFindingTypes.TYPES` member, so a new finding type without a decision fails a test.
- *Entry point:* `GuideViewModel.openCard(cardId)` records the pending target (`pendingCard: StateFlow<String?>`) and starts the load; nothing else
  opens a card from outside the Guide tab. Nothing is persisted: after process death the target is gone.
- *Navigation:* `Screen.GuideCard` is a detail (not a tab root). Android pushes it inline (`navState.push`), iOS through `NavBridge.navigateToDetail`
  into a native view controller; Back is the existing detail Back on both. `Screen`, the bridge and the saver carry no arguments.
  `GuideCardRoute` (composeApp) is the one host for both platforms: no target, an unknown id, or a failed load pops itself.
- *Saver:* `GuideCard` is restored only when the Guide is enabled (like the Guide tab); a restore with no target is handled by the host.
- *UI:* `payAuditFindingsItems(..., onOpenGuideCard: ((String) -> Unit)? = null)`; null (flag off) draws no link. `PayAuditScreen` passes a handler
  that opens the card when `guideUnlocked`, else shows `PayslipUpgradeSheet`. Copy in `GuideStrings`; Pay Audit suites unchanged (defaulted parameter).
- *Entitlement:* the host reads `rememberGuideUnlocked()`, so a locked state still holds no key points, cite or details (E5 rule).

**Tests first.** Characterization (green before any edit): findings rendering (`PayAuditFindingsSectionTest`), `AppNavStateSaver` round-trip,
`NavBridge` lock rule, 4-tab bar with the Guide off, `switchTab` clears a pushed detail. Then failing-first: every mapped id exists in the
real bundle (contract) and in `SyntheticGuideBundle`; decided types cover `PayAuditFindingTypes.TYPES`; unknown (type, field) -> null; link
pushes `GuideCard` and Back returns (`AppNavState` and `NavBridge`, iOS path); restored `GuideCard` with no target pops itself; link blocked while
locked; flag off -> no link (and a restored `GuideCard` is dropped); locked user taps -> upgrade sheet, never a card, `openCard` not called;
unlocked tap -> `openCard(id)` then navigate; unknown id pops; telemetry untouched (`FakeCrashReporter`).

**R8 / iOS risks.** No new `@Serializable` type, no reflection. `GuideLinkMap` is a plain `when`/map (no regex). iOS: a new native view controller
case in the existing `when` (exhaustive; compile-checked by `linkDebugFrameworkIosSimulatorArm64`); the map lookup is O(1), so no timing test is
needed. The simulator walkthrough stays in EP 11/15/20 for E9.

**Regression controls.** Dark launch unchanged. Pay Audit logic is not edited and not copied; only the parameter on the findings section and the wiring
in `PayAuditScreen`. Corpus untouched.

**Navigation and state.** Arguments: none (pending target). Back: both platforms pop the detail to the finding. Tab re-tap: the bottom bar is hidden
under a detail; `switchTab` clears the detail stack (pinned). Lock screen: Android swaps the tree, iOS `NavBridge` blocks the push (pinned); nothing
Guide is composed while locked. Process death: the saver restores `GuideCard`, the target is gone, the host pops (pinned).

**Versioning, DI, fixtures, storage.** No bundle, schema or Room change. `GuideViewModel` stays a Koin single. Fixtures: `SyntheticGuideBundle` gets
the mapped ids only if the contract is run against it (the real bundle is used for the existence check). No storage.

**Security.** Local navigation only; the card id is never logged, sent or put in crash keys; nothing new leaves the device.

**E7 result (2026-10-08).** Done, with these recorded choices:
- **Mapping.** `GuideLinkMap` (shared) is the one table, owner-approved in the plan above; ids are asserted equal to the approved list (so an unapproved change fails) and exist in the real 402-card bundle on both platforms (`GuideBundleContract.assertPayAuditLinksPointAtRealCards`, also an `iosTest`). `decidedTypes` must cover `PayAuditFindingTypes.TYPES` (a new finding type fails a test). Under "issues and waiting, verified for arrears only" every verified row is `ARREARS_AUDIT`, so in code the rule is "every row whose finding has a card"; the under-paid arrears `SALARY_LOSS` issue links `RB-RP-052` as well.
- **Entry point and state.** `GuideViewModel.openCard(id)` records `pendingCard` (memory only) and starts the load. `Screen.GuideCard` is a plain detail: Android pushes it inline, iOS through `NavBridge.navigateToDetail` into a native view controller. `GuideCardRoute` is the one host on both platforms; no target, an unknown id or a failed load pops instead of a blank screen (a failed load pops rather than offering retry; the Guide tab keeps retry). The saver cuts a saved `GuideCard` when the Guide is off. Nothing about the card is persisted; after process death the card pops.
- **Premium.** `rememberOpenGuideCard` returns null with the Guide off (no link at all); a locked user's tap shows `PayslipUpgradeSheet` and never calls `openCard`; the card host reads `rememberGuideUnlocked()`, so a locked card still holds no key points, cite or details. The link resolves `GuideViewModel` on tap, not on composition, so Pay Audit renders without the Guide's DI (its suites are unchanged).
- **Back returns to the finding (found on the Pixel, fixed).** First build: Back landed on Pay Audit but reset the month to the latest. Cause: Android draws only the top detail, so Pay Audit's `rememberSaveable` month and tab were dropped. Fix: `DetailStateKeeper` (a `SaveableStateHolder` per stack position, cleared when the detail leaves the stack, so a fresh visit still starts empty). It wraps every inline detail, so any detail under another (for example Premium over Tax Planning) now also keeps its saved state on Back; judged an improvement, flagged here.
- **Telemetry.** Nothing written: tests assert `FakeCrashReporter` sees no call, key or log when a card opens (only the existing `error_guide_load` code on a failed load).
- Tests added: shared 4 (`GuideLinkMapTest`), composeApp 26 on debug (`GuideOpenCardTest` 7, `GuideCardHostTest` 6, `PayAuditGuideLinkTest` 5, `DetailStateKeeperTest` 2, nav 6: `AppNavStateTest` 2, `AppNavStateSaverTest` 2, `NavBridgeTest` 2; contract check on both platforms, one `iosTest`). Pinned before any edit (green, unchanged): `PayAuditFindingsSectionTest`, `AppNavStateSaverTest`, `NavBridgeTest`, `AppBottomBarTest` (four tabs, Guide off), `AppNavStateTest`. Mutation checks: ignoring the lock, restoring `GuideCard` with the Guide off, a changed mapping, and removing the state cleanup each failed the intended test.
- Gates (2026-10-08, final tree): `ktlintCheck`, `check -x iosX64Test` (both variants, corpus included), `iosSimulatorArm64Test`, `linkDebugFrameworkIosSimulatorArm64`, `assembleRelease`, `assembleMinifiedTest`, `check_r8_guide.py` (10 models kept, bundle byte-identical), tech-debt audit (22 changed files): green. One failure on the way (two Koin-injected Pay Audit tests: eager `koinInject` of the Guide view model), fixed by resolving on tap rather than editing those tests.
- Baselines. Tests: shared JVM 882 (E6: 878), composeApp JVM debug 731 / release 705 (705 / 679), iOS shared 835 (831), iOS composeApp 487 (473); none failed. One skipped: the existing `GemmaModelPathsAndroidTest` debug-sideload case in the shared release variant. Release APK 69,540,176 bytes (E6: 69,540,177; the Guide is still dark). `App.kt` is exactly 300 lines (the limit); the next change there must extract first.
- Pixel 9 (2026-10-08, `minifiedTest` over the existing install, same signature, no uninstall): `run_guide_minified_smoke.sh` passed twice (before and after the fix). With the real profile: Pay Audit, Apr 2026, "Verified · not an issue" DA arrears row, "Read the rule" opens "Dearness Allowance: when is it revised and what counts as pay?" (`RB-RP-052`); header Back and system Back both return to Pay Audit on Apr 2026 with the same rows. The real history has no open issues (33 months, 0 issues), so only the verified-arrears links were driven.
- **Phase Handoff.** Debt incurred and resolved in the phase: (1) eager Guide DI in Pay Audit: resolved by tap-time lookup; (2) Back lost the month on Android: resolved by `DetailStateKeeper` with a test; (3) mapped ids could drift from the dataset: resolved by the contract on both platforms. Open (EP 23-25): locked-user and waiting-row checks on a device, the iOS walkthrough of the link, and the two dataset gaps.
- Not done by design: no link for `SALARY_LOSS` rows outside arrears; no link on the Pay Audit verdict card; no deep link into a card from outside the app.

## E8 Pins, copy cite and share as claim note
**Goal:** pinned cards on Home, Copy cite, and Share as claim note.
**Files:** `GuidePinsStorage` with Android and iOS implementations and a fake (no Room change, not in
backup), `domain/GuideShareText.kt`, the existing `ShareUtils.kt` and platform clipboard.
**Premium (from E5):** pins, copy cite and share are paid (`guideUnlocked`); the controls are absent from the locked card state,
and a locked user's tap offers the upgrade sheet.
**Tests:** pins persist; stale ids are dropped after a bundle update; a fixed-text test of the share note
(card text and cite only, no profile figures, name or service number); the copied cite equals `cite`; nothing leaves without a tap.
**Exit:** gates green; `minifiedTest` smoke. **Tech-debt checkpoint:** none carried.

### E8 phase plan (written 2026-10-08, before any Kotlin)
Branch `feature/guide-e8-pins-share` off `main` (E7 is merged and pushed). Gate: "Rules for every phase" plus `assembleRelease`,
`assembleMinifiedTest`, `check_r8_guide.py`, and the Pixel 9 smoke plus a Pixel check of pin, Copy cite and Share. The iOS simulator walkthrough stays in the EP table (E9).

**Owner decisions for E8 (2026-10-08, one batch, closed before any code).**
| Topic | Decision |
|---|---|
| Share note | Full note, no app attribution: header "Claim note", title, "Answer:", "Key points:", "Attach:", "Watch out:", "Authority: <cite>" (empty sections left out). No details, no "your figure", no name, service number or profile figure. |
| Warning line | Share adds "Unverified point: ..." under the answer for a flagged card. Copy cite stays exact: the copied text equals `cite`. |
| Pins | No limit, newest pinned first, in a "Pinned" section above the area tiles on Guide Home. Section is not drawn when empty (Home looks as today). |
| Controls | An action row (Pin, Share) under the answer; a Copy cite button beside the Authority line. A locked card has none of them (only the existing Unlock panel, which already names these as Premium). |

**Existing code read first.** `GuideCardScreen`, `GuideCardHeader`, `GuideCardSections` (cite item), `GuideLockedPanel`, `GuideHomeScreen`, `GuideTab`
(`GuideDestinationContent`), `GuideCardRoute`/`GuideCardHost` (the Pay Audit card host: the second user of `GuideCardScreen`), `GuideAccess`,
`GuideViewModel`, `GuideUiState` (`GuideFeedRow`, `cardContent`), `GuideTrust`, `GuideModule`, `GuideStrings`, `OnboardingStorage` (+ Android/iOS actuals, fake,
contract test: the storage pattern), `ClipboardCopier` (`rememberClipboardCopier`), `ShareUtils` (`shareText`), `RepresentationScreen` (existing copy/share use).
Facts that shaped the plan: `allowBackup="false"` and the `.pcda` backup is separate, so plain prefs are not backed up; `GuideCardScreen` has two hosts;
`GuideHomeScreen` is a `LazyVerticalGrid`; `GuideTab.kt` is 173 lines, `App.kt` stays untouched.

**Design (one source each).**
- *Storage (shared):* `GuidePinsStorage { load(): List<String>; save(ids) }`, `expect fun provideGuidePinsStorage()` with Android `SharedPreferences` and iOS
  `NSUserDefaults` actuals (one newline-joined string of ids; no Room, no backup, no `@Serializable`). `load` drops any entry that is not a plain card id
  (`[A-Za-z0-9._-]`, at most 64 characters) and duplicates, so a damaged value cannot inject anything. `FakeGuidePinsStorage` in `shared-test-fixtures`.
- *Domain (shared, pure):* `GuidePins` (oldest-first list; `toggle`, `isPinned`, `newestFirst`, `retainKnown(isKnown)`); `GuideShareText.build(parts, labels)`
  (labels are passed in, so the words live only in `GuideStrings`; the builder takes only title, answer, key points, attach, watch-out, cite and the unverified flag, so
  no profile data can reach it by type).
- *State (composeApp):* `GuidePinsModel` (Koin single in `guideModule`): `pins: StateFlow<List<String>>` newest first, `toggle(id)` saves at once,
  `retainKnown` drops ids the bundle no longer holds and saves. Stale ids are also filtered when Home draws, so a bundle update never shows or opens a missing card.
- *UI:* `GuideCardActions(isPinned, onTogglePin, onShare, onCopyCite?)`, null for a locked card, built in `rememberGuideCardActions(card)` from the platform's
  `rememberClipboardCopier` and `shareText`; `GuideCardScreen` takes `actions: GuideCardActions?` and both hosts (Guide tab, Pay Audit card host) pass it.
  Copy cite is offered only when the card has a cite. Home gets `pinned: List<GuideFeedRow>` (empty when locked or none). A tap on a pinned row pushes `Card(id)`.
- *Entitlement:* `card.full == null` (locked) means `actions == null`, and the Home pinned list is empty when `!access.isUnlocked`; nothing reads pins while locked.
  Pins stay stored if the user later locks, and reappear on unlock. Flag off: no Guide, so none of it exists.

**Tests first (each fails before its code).** Characterization on `main` (green, unchanged): card screen (unlocked sections, locked panel with no key points, cite or
details), Guide Home rendering (areas, no pinned section), `ShareUtilsTest` / `ShareUtilsIosTest`, `GuideTabTest`. Then failing-first: pins persist across a new
model on the same storage; toggle pins newest first and unpins; stale ids dropped after a bundle update (state and storage); damaged stored value filtered;
fixed-text share note (exact string, with and without attach/watch, unverified line only when flagged, no details, no figure); Copy cite copies exactly `cite`;
nothing is copied or shared until a tap (fakes see zero calls on composition and navigation); locked card has no Pin, Copy cite or Share nodes and holds no actions;
locked Home has no pinned section even with stored pins; empty pins draw no section; Pay Audit card host shows the actions when unlocked; `FakeCrashReporter` sees nothing
and no card id reaches telemetry; Android prefs round trip (Robolectric); contract on the real bundle: every card's share note stays within the system share limit
and contains no `{`, details or "Your figure".

**R8 / iOS risks.** No new `@Serializable` type, no reflection, no keep rule; `check_r8_guide.py` still proves the 10 models. iOS: `NSUserDefaults` and string split on
a plain character (no regex); the lists are at most a few hundred ids, so no timing test is needed beyond one `iosTest` round trip of the storage.

**Regression controls.** Dark launch unchanged. Edits to existing files are limited to `GuideCardScreen`, `GuideCardSections` (cite item), `GuideHomeScreen`, `GuideTab`,
`GuideCardRoute` (the host), `GuideModule`, `GuideStrings` and `GuideUiState` (a pinned-row mapper); `App.kt` is not touched. Corpus untouched.

**Navigation and state.** No navigation change and no argument. Back from a pinned card returns to Home (the stack is Home then Card); tab re-tap pops to Home as today;
the pin star state is derived from the model, which loads from storage, so it survives process death; lock screen composes no Guide content.

**Versioning, DI, fixtures, storage.** Storage key `guide_pins_v1` (the value is a newline-joined id list; a future format gets a new key). One Koin single (`GuidePinsModel`)
with `provideGuidePinsStorage()`. Fake in `shared-test-fixtures`. Not in backup (format stays v3), no Room change.

**Security.** Nothing leaves the device without a tap: the copy and share calls run only from button taps. Pins are plain card ids, never PII; no card id, text or pin
list is logged or put in crash keys (test). The share text is built only from public card text and cite, so a profile figure cannot reach it by type.

**Owner decisions:** none open.

**E8 result (2026-10-08).** Done, with these recorded choices:
- **Pins.** `GuidePins` (shared, immutable, plain class so no `copy()` can skip its id check) holds ids oldest first and shows them newest first; `fromStored` keeps only ASCII ids of `[A-Za-z0-9._-]`, 1 to 64 characters, without duplicates. `GuidePinsStorage` (`SharedPreferences` file `payslipmax_guide_prefs` on Android, `NSUserDefaults` on iOS, key `guide_pins_v1`; Android `allowBackup` is false and the `.pcda` backup is untouched) plus `InMemoryGuidePinsStorage` (the view model's default) and `FakeGuidePinsStorage` in `shared-test-fixtures`. `GuidePinsModel` (Koin single, shared with `GuideViewModel.pins`) saves on every change; `GuideTab` drops pins for cards the bundle no longer holds once it loads, and Home filters them as well.
- **Share note.** `GuideShareText` (shared) takes `GuideShareParts` (title, answer, key points, attach, watch-out, cite, unverified flag) and labels from `GuideStrings`; there is no field for details, a figure, a name or a service number, so none can reach the note. Exact layout is pinned by a fixed-text test (shared) and on screen. The unverified line sits under the answer; Copy cite copies exactly `cite` (tested on screen, and the contract checks every real card's note holds its cite).
- **UI.** `GuideCardActions` is null for a locked card, so Pin, Share and Copy cite are absent from the locked state (the Unlock panel stays); Pin and Share sit in a row under the answer, Copy cite beside "Authority" (absent for the 31 cards with no cite; the label becomes "Copied"). Home draws "Pinned" above the tiles only when unlocked and non-empty. Both card hosts (Guide tab, Pay Audit card) use the same actions. Platform calls go through `GuidePlatform` (`rememberClipboardCopier`, `shareText`), invoked only from taps (tested: nothing before a tap). `App.kt` untouched.
- **Telemetry.** Nothing written: a test taps pin, copy and share and asserts `FakeCrashReporter` saw no log, key or exception.
- Tests added: shared 18 (pins 8, share note 4, storage contract 3, Android prefs 3), iOS 2 (`GuidePinsStorageIosTest`) plus 1 Native contract, composeApp 26 (pins model 5, share 5, on screen 13, Pay Audit host 2, DI 1) and the real-bundle contract on both platforms (all 402 ids pinnable, note holds title and exactly the cite, warning on exactly the 36 unverified cards, no `{`, no details). Mutation checks: building actions for a locked card, and ignoring the lock on Home, each failed the intended test.
- Gates (2026-10-08, final tree): `ktlintCheck`, tech-debt audit (31 files), `check -x iosX64Test -x iosSimulatorArm64Test` (both variants, corpus included), `iosSimulatorArm64Test`, `linkDebugFrameworkIosSimulatorArm64`, `assembleRelease`, `assembleMinifiedTest`, `check_r8_guide.py` (10 models kept, bundle byte-identical): green.
- Baselines. Tests: shared JVM 900 (E7: 882), composeApp JVM debug 757 / release 731 (731 / 705), iOS shared 852 (835), iOS composeApp 499 (487); none failed. One skipped: the existing `GemmaModelPathsAndroidTest` debug-sideload case in the shared release variant. Release APK 69,540,176 bytes (identical to E7; the Guide is still dark). Cold start not re-measured (EP 12).
- Pixel 9 (2026-10-08, `minifiedTest` over the existing install, same signature, no uninstall): `run_guide_minified_smoke.sh` passed. By hand with the real profile: Food charges card shows Pin and Share under the answer and Copy cite by Authority; no share sheet before a tap; Pin turns to Unpin, Copy cite to "Copied"; Share opens the system sheet with the exact claim note; after a force-stop and relaunch Home shows "Pinned" with the card; unpinning removes the section (phone left unpinned).
- **Phase Handoff.** Debt incurred and resolved in the phase: (1) a data-class `GuidePins` would have exposed `copy()` past the id check: made a plain class with equals; (2) the view model needed a pins default so existing tests stayed untouched: `InMemoryGuidePinsStorage`; (3) two hosts for the card screen: both wired through one `rememberGuideCardActions`. Carried (EP 26, 27): iOS walkthrough and the locked-state check on a device. No other debt.
- Not done by design: no pin limit, no reorder, no pin from a feed row, no unpin from Home, no share of details or "your figure", no attribution line, pins not in backup.

## E9 End-to-end, security review and release
**Work:** end-to-end tests (tile, feed, card, pin); the `security-review` skill on the branch; confirm no new
outbound path, nothing Guide-related in telemetry, and no `from`/`open` in the bundle; a release build and an iOS
simulator walkthrough; baseline comparison; then turn on `GUIDE_ENABLED` for release in its own commit.
**Exit:** full pre-push gate green; HANDOFF.md, docs and memory updated.
**Tech-debt checkpoint:** a known-gaps register (RP-073, unverified cards, the DA 60% flag).
**Carried here from earlier phases (EP):** 11, 15, 20 and 23 (the iOS simulator walkthrough: search field, locked panel, upgrade sheet,
chips, the "Your figure" block, the Pay Audit "Read the rule" link and its Back), 24 (locked-user and waiting-row link checks on a device), 25 (the two
dataset gaps for the known-gaps register), 12 (cold start and APK size), 14 (the purchase and Premium-row checks at the paywall flip), 26 (the E8 iOS walkthrough: Pin, Share sheet, Copy cite, Pinned section), 27 (E8 locked-state check on a device; clipboard half closed). 7, 13 and
every other E6 item (17-19, 21, 22) are closed.

### E9 sprint prompt (owner decisions closed 2026-10-09; paste this to start)
```
Continue the PayslipMAX KMP Claim Guide. Run the whole of E9 as ONE sprint of strict phases (CLAUDE.md rules 0-5, Phase
Handoff Protocol after every phase, TDD, 300-line limit, copy in GuideStrings, no hard-coded colours, no PII or card id in
telemetry). Read first, in order: CLAUDE.md; the top block of docs/Plan/rule_cards/HANDOFF.md; then in
docs/Plan/rule_cards/16_guide_phase_plan.md: section E9, "Rules for every phase", the E8 result, the EP table; the memory index
(Pixel/adb gotchas, rule-cards note). Gradle from the shell: -Dorg.gradle.java.home=/opt/homebrew/opt/openjdk@21; on "jmod ...
antigravity" or "JVM has changed" loop `./gradlew --stop; sleep 12`; never two Gradle commands at once; git push needs
JAVA_HOME=/opt/homebrew/opt/openjdk@21; zsh: file lists through xargs.

OWNER DECISIONS (closed, do not re-ask): merge E8 and push; author the two missing cards and fix the MSP wording; switch the Pay
Audit links to the new cards; paywall stays OFF (Guide ships free, GUIDE_PAYWALL_ENABLED=false, EP 14 stays open post-launch);
flip GUIDE_ENABLED in its own commit with NO store upload; install a debug build on the Pixel over the current one (same
signature, adb install -r, never uninstall); perf bar: cold start within +100 ms of E2 (190 ms) and release APK within +1 MB of
69,540,176 B. SKIP the iOS simulator walkthrough (EP 11, 15, 20, 23, 26 stay open for the owner; say so in the final report).

PHASE 0 Setup. Fast-forward E8 (feature/guide-e8-pins-share, tip 44699255 or later) into main, run the pre-push hook push
(~5 min, never bypass), delete the branch. Create feature/guide-e9-release off main. Write the E9 phase plan into the plan file
(CLAUDE.md "Writing a phase plan" checklist) before any code; one owner batch for anything still open.

PHASE 1 Dataset gaps (EP 25). Source text is in ~/Downloads/rulecards_workdir (never commit it). Draft, in own words with a primary
cite only: (a) a card on DA arrears, (b) a card on "HRA stops when you take quarters", (c) the Level 14+ MSP exception on
RB-C13-05. List every line plus where each rests in the handbook/FAQ, and STOP for ONE owner approval batch; legal readings are
asked, never guessed. Only then edit authoring/*.txt, run compile.py, render_md.py, copycheck.py (CLEAN), regenerate
13_review_queue.md, run bundle.py; update every count in GuideBundleContract and tools/test_bundle.py (402 becomes 404, domain
split, unverified/no-source counts) and the real-bundle contracts (Pay Audit links, 402-card share/pin contract). Propose the exact
(finding type, field) -> card table for owner approval, then update GuideLinkMap and its pinned-ids test (ARREARS_AUDIT and the
arrears SALARY_LOSS -> the DA arrears card; HRA missing -> the quarters card where it fits; keep every other E7 mapping). Gate: the
full chain below.

PHASE 2 Device checks and perf (EP 12, 24, 27). On the Pixel 9 (serial from `adb mdns services`; if `adb connect` is refused run
`adb shell input keyevent KEYCODE_WAKEUP` and `svc power stayon true`): (1) measure cold start (am start -W) on the minifiedTest
build like for like against E2 190 ms and the Play 1.3.0 baseline, and record APK size and method count. (2) Install the debug
build over the current one (adb install -r), use Settings "Force Free" to check: a locked card shows no Pin/Share/Copy cite, a
locked Home shows no Pinned section while pins stay stored and return on unlock, a locked user's Pay Audit link opens the upgrade
sheet and never a card (EP 24, 27). The waiting (TPTA on hold) row can only be driven if the real history has one; if not, record
that it stays covered by tests. (3) Reinstall the minifiedTest build over it so the phone ends as it started, and re-run
scripts/run_guide_minified_smoke.sh. Never uninstall; if an install is refused, stop and ask.

PHASE 3 End-to-end, security review, known-gaps register. Add end-to-end tests (tile -> feed -> card -> pin -> Home -> reopen;
Pay Audit link -> card -> Back). Run the security-review skill on the branch and fix findings. Prove with tests/greps: no new
outbound path; nothing Guide-related in telemetry (card ids, queries, pins); no `from`/`open` in the shipped bundle; pins hold
plain ids only. Write the known-gaps register in the plan: RP-073 TA Allowance, the unverified cards (36) incl. RP-088 HBA 8.5%,
the DA 60% / rates-as-of flag, the open iOS walkthrough items, EP 14 (paywall off by owner decision).

PHASE 4 Release flip (own commit). Flip GUIDE_ENABLED for release in a commit that touches only that and the tests that pinned
"dark in release" (GuideDarkLaunchReleaseTest, AppKoinModulesTest expectations, 4/5-tab bar tests): update them to expect the
Guide, with the paywall flag still false. Then the FULL pre-push gate. Verify: ./gradlew :composeApp:minifyReleaseWithR8
-PallowPlaceholderGemmaModel=true, assembleRelease (APK within +1 MB of 69,540,176 B; report the exact delta), R8 mapping keeps the
Guide models (check_r8_guide.py adapted to the release build if needed), and the Pixel minified smoke again. Do NOT upload or run
fastlane.

PHASE 5 Close-out. Add an "E9 result" section (style of E7/E8), update the EP table (close 12, 24, 25, 27; 14 stays open as post-
launch; 11, 15, 20, 23, 26 stay open for the owner), HANDOFF.md (top block and a paste-to-start for the post-E9 / release
follow-up) and the memory notes. Commit on the E9 branch. Do not merge or push until I say so.

GATES at the end of EVERY phase (all green, none skipped): ./gradlew check -x iosX64Test -x iosSimulatorArm64Test; ktlintCheck;
python3 scripts/check_tech_debt_limits.py --strict (changed .kt files, via xargs); iosSimulatorArm64Test;
:composeApp:linkDebugFrameworkIosSimulatorArm64; assembleRelease + assembleMinifiedTest + python3 scripts/check_r8_guide.py.
After each phase give the Phase Summary: debt incurred, exact steps that resolved it, build and tests confirmed. Anything
unfinished: say so loudly and put it in the EP table and the post-E9 list; never drop one silently. Keep answers short and
checkpoint after each significant step.
```

### E9 phase plan (written 2026-10-09, before any change)
Branch `feature/guide-e9-release` off `main` (E8 merged; push gate run 2026-10-09). Five phases, one at a time, each ending with the full gate list in the sprint prompt above.
No iOS simulator walkthrough in this sprint (owner decision): EP 11, 15, 20, 23, 26 stay open for the owner.

**Existing code and files read first.** `LaunchFlags` (`GUIDE_ENABLED`, `GUIDE_PAYWALL_ENABLED`), `GuideAvailability` (`isGuideEnabled()` = flag or `GUIDE_PREVIEW` build field), `GuideAccess`, `GuideLinkMap`
and its pinned-ids test, `GuideBundleContract`, `tools/test_bundle.py`, `GuideDarkLaunchReleaseTest`, `AppKoinModulesTest`, the 4/5-tab bar tests, `scripts/check_r8_guide.py`, `scripts/run_guide_minified_smoke.sh`.

| Phase | Work | Success criteria |
|---|---|---|
| 1 Dataset gaps (EP 25) | Three content items drafted from the handbook/FAQ, owner approves every line, then authoring, tools chain, counts and link table | `compile.py` ok, `copycheck.py` CLEAN, `bundle.py` fresh, 404 cards, contracts and link tests updated, full gate chain |
| 2 Device checks and perf (EP 12, 24, 27) | Cold start on `minifiedTest` vs E2 190 ms and Play 1.3.0, APK size, method count; "Force Free" checks on a debug build; reinstall `minifiedTest`; smoke | Cold start within +100 ms of 190 ms; locked card shows no Pin/Share/Copy cite; locked Home no Pinned; locked Pay Audit link opens the upgrade sheet; phone ends as it started |
| 3 End-to-end, security, gaps register | Two end-to-end tests; `security-review`; tests/greps for no new outbound path, no Guide data in telemetry, no `from`/`open` in the bundle, plain-id pins; known-gaps register | All green; findings fixed or listed |
| 4 Release flip | Own commit: `GUIDE_ENABLED = true`, only the tests that pinned "dark in release" change; paywall flag stays false; full pre-push gate; R8, APK size, mapping, smoke | Release APK within +1 MB of 69,540,176 B; Guide models kept; no upload, no fastlane |
| 5 Close-out | E9 result, EP table, HANDOFF, memory | Committed on the branch, not merged or pushed |

**Risks.** Release/R8: flipping the flag makes the Guide UI reachable in release, so `minifyReleaseWithR8` plus `check_r8_guide.py` against the release build is mandatory (`minifiedTest` was the only build that kept it before). iOS: no Kotlin/Native hot path is added; the only new `commonMain` code is the link table (a map lookup). The release flip reaches iOS too (same constant), unverified on a simulator by owner decision (EP 11, 15, 20, 23, 26).
**Regression controls.** Characterization first: the existing dark-launch tests are edited in the flip commit only. Data: card ids stay stable; new cards get new ids; no `from`/`open` ships.
**Navigation and state.** No change. **Versioning, DI, storage.** None; bundle schema unchanged (card count 402 to 404).
**Owner decisions.** All closed in the sprint prompt, except the Phase 1 wording batch and the link table (one batch, asked before any authoring edit).

**E9 result (2026-10-09).** Done on branch `feature/guide-e9-release` (not merged, not pushed; no store upload, no fastlane). Recorded choices:
- **Phase 0.** E8 merged (fast-forward) and pushed after the full pre-push gate (300 s); branch deleted.
- **Phase 1, dataset (EP 25).** Three owner-approved items: new card `RB-RP-053-arrears` (DA arrears; ships with the Unverified chip because no source prints an arrears procedure), new card `RB-SS-P114-quarters` (HRA stops when quarters are allotted; primary cite MoD 1(5)/97), and the Level 14 MSP exception on `RB-C13-05` (answer plus a Details sentence). 404 cards (220 travel, 184 pay), 37 unverified, 31 no-source. To keep `pay-cpc` within the 16-card case cap, RP-035 (aviator Qualification Allowance, misfiled there) moved to the aviation case `allow-air` (now exactly 16). `GuideLinkMap`: ARREARS_AUDIT and the arrears SALARY_LOSS lines open the arrears card, HRA missing opens the quarters card; every other E7 mapping unchanged; `RB-RP-052` (DA revision) is no longer linked from Pay Audit but is still in the Guide. `compile.py`, `render_md.py`, `copycheck.py` (CLEAN), `review_queue.py`, `bundle.py` run; counts updated in `GuideBundleContract` (404/184/37, trust 37, warned notes 37) and `test_bundle.py`.
- **Phase 2, device (EP 12, 24, 27), Pixel 9, nothing uninstalled.** Cold start, `am start -W` WaitTime: empty state (a throwaway second user, created and removed, owner data untouched) median ~187 ms (E2: 190; bar +100); real data ~470 ms for both the E8 and E9 `minifiedTest` builds (Play 1.3.0 with real data was 3,100 ms). Release APK 69,540,500 B before the flip (+324 B vs 69,540,176), method ids 55,323 (E2 55,269). Debug installed over the existing app, "Force Free": the locked MSP card shows the unlock panel and no Pin, Share, Copy cite, key points or authority; locked Home shows no "Pinned" while the pin stays stored and returns on "Follow Flag"; the unlocked card shows the new MSP wording. Not drivable on a device: a locked user's Pay Audit link (Force Free locks Pay Audit itself, so there is no finding to tap) and the waiting row (no open issue in the real history); both are covered by `GuideOpenCardTest`, `GuideCardHostTest`, `PayAuditGuideLinkTest` and the new end-to-end tests. Phone ended as it started: `minifiedTest` installed, Follow Flag, no pins, smoke passes.
- **Phase 3.** `GuideEndToEndTest` (real bundle: tile, case, card, Pin, Home, Pinned row reopens, rebuilt model keeps the pin, telemetry silent; Pay Audit link opens the arrears card and Back returns to the finding; a locked user gets the upgrade ask and no card) and `GuideSecurityContractTest` (no network/link/analytics token in any Guide source, only `GuideViewModel` touches telemetry and writes the load code only, no intent/clipboard code in Guide files, shipped bundle has no `"from"`, `"open"` or `http`; mutation-checked). `security-review` skill on the branch: no findings. Known-gaps register above.
- **Phase 4, release flip (commit 77faaee9, only `LaunchFlags.GUIDE_ENABLED` and the tests that pinned dark: `GuideLaunchFlagTest`, `GuideDarkLaunchReleaseTest` renamed `GuideReleaseLaunchTest`; no other test needed a change).** Paywall flag still false. Full gate green (`check`, `ktlintCheck`, both iOS test tasks, iOS link, tech-debt audit, `minifyReleaseWithR8`, `assembleRelease`, `assembleMinifiedTest`). Release APK 69,606,036 B: **+65,860 B** vs 69,540,176 (bar +1 MB). `check_r8_guide.py` passes on both the `minifiedTest` and the release mapping and APK (10 models kept, bundle byte-identical). Pixel smoke passes; cold start with real data 438-479 ms. `scripts/git-pre-push.sh` itself was not run (nothing pushed); its steps were run by hand.
- Baselines: shared JVM 900 (E8: 900; 1 skipped as before in release), composeApp JVM debug 765 / release 739 (757 / 731), iOS shared 852, iOS composeApp 499; none failed.
- **Not done, on purpose or unreachable:** iOS simulator walkthrough (EP 11, 15, 20, 23, 26: owner decision, open for the owner); EP 14 (paywall off by owner decision, open post-launch). The Guide is free for everyone, including the 37 unverified-chip cards.
- **Phase Handoff.** Debt incurred and resolved: (1) the nav cap forced the RP-035 move (a misfiled card, fixed); (2) two Gradle commands overlapped once (commit hook and gate script) and both passed; (3) the `check_r8_guide.py` docstring said release keeps the Guide dark (updated). Carried: the items under "not done". No other debt.

### E9 known-gaps register (2026-10-09; nothing here is silently dropped)
| Gap | State at release | Where it is tracked |
|---|---|---|
| RP-073 TA Allowance | No card and no search alias (owner decision 10-06). | Post-E9 list |
| Unverified cards | 37 ship with the "Unverified point" chip (the 36 from 10-06 plus the DA arrears card added 10-09: no source prints an arrears procedure). The `open` text never ships. | Post-E9 list |
| RP-088 HBA 8.5% | Unverified until the owner confirms the rate from a primary letter. It is a Rates-chip card, so it blocks the paywall flip (EP 14). | EP 14 |
| DA 60% / rates-as-of | `rates_as_of` is 2026-01. The 60% step rests on a MoF OM read via a circular; the MoD letter was not seen. The staleness nudge shows from Oct 2026. | RB-RP-052 `O:` point |
| iOS walkthrough | Not done in this sprint (owner decision): EP 11, 15, 20, 23, 26. The Release flip reaches iOS through the same constant. | EP table |
| Paywall off | `GUIDE_PAYWALL_ENABLED=false` by owner decision, so the Guide ships free; EP 14 (sandbox purchase unlocks a card, Premium row appears) stays open for the post-launch flip. | EP 14 |
| Locked Pay Audit link on a device | Not reachable: "Force Free" locks Pay Audit itself, so no finding is shown to tap. Covered by `GuideOpenCardTest`, `GuideCardHostTest` and `GuideEndToEndTest`. | E9 result |
| Waiting (TPTA on hold) row on a device | The real history has none; covered by tests. | E9 result |

## Open items carried into E1 (all closed 2026-10-07)
Bundle location: compose resources (E1 spike). `rates_as_of`: `2026-01`. GUIDANCE vs empty cite: one rule, empty cite.
CI emulator job: no. See the owner decisions table.

## EP Pending items (consolidated from finished phases; fail loudly, never dropped)
Each item names the phase that closes it. A phase may not exit while an item assigned to it is still open.
| # | From | Item | Closed in |
|---|---|---|---|
| 1 | E1 | Pixel cold-start baseline. **Closed in E2:** recorded on the Play 1.3.0 build and, like for like, on E1 vs E2 release (E2 result). | E2 start |
| 2 | E1 | R8 Check 2 not run in E1. **Closed in E2:** `minifiedTest` and `:guideSmokeTest` pass on the Pixel 9 (3 runs). | E2 |
| 3 | E1 | Guide keep rule pinned unused serializers. **Closed in E2:** removed entirely, not narrowed; Check 1 on `minifiedTest` and the device smoke prove the models survive without it. | E2 |
| 4 | E1 | `FakeGuideRepository` had no consumer. **Closed in E2:** used by `GuideViewModelTest`, `GuideTabTest`, `GuideTabInAppTest`. | E2 |
| 5 | E1 | Report the load error code. **Closed in E2:** `GuideViewModel` records a non-fatal with only `error_guide_load=<code>` (`FakeCrashReporter` test). | E2 |
| 6 | E1 | E0 to E4 were stacked branches, none merged to `main`. **Closed 2026-10-07:** fast-forwarded `main` to the E4 commit, gate green on `main`, branches deleted. From E5 on: one short-lived branch per phase off `main`, merged as soon as its gate is green, then deleted. | Before the E1 PR |
| 7 | E2 | With five tabs, "Dashboard" wrapped to two lines at 320dp. **Closed 2026-10-07 (owner):** the first tab is now labelled "Home" (`AppStrings.navigationHome`); `GuideSmallScreenTest` checks all five labels on one line at 360dp and 320dp (fails with "Dashboard"). Also checked on the Pixel 9 at its normal size and with the display forced to 320dp (`wm density 540`, then reset): all five labels on one line. | E9 |
| 8 | E2 | Placeholder routes: `GuidePlaceholderScreen.kt`, `GuideStrings.comingNext` and `searchTitle`, and the smoke's last step. **Closed in E4:** the file and `comingNext` are deleted, `searchTitle` is the real screen's title, and the smoke searches and opens a card from the results. | E4 |
| 9 | E3 | The two E3 device checks. **Closed 2026-10-07 on the Pixel 9** (debug-signed Play-installer `minifiedTest` build, dark theme): `run_guide_minified_smoke.sh` passed (tab, area, case, card, "Key points"); walk-through: Home, area (case rules and counts), feed with chips and counts (All 16, Who qualifies 3...), chip filter, all six "also relevant here" rows naming their real home, card with breadcrumb to the feed it was opened from, Watch out in amber, cite in monospace, Details expand (food-charge card shows no placeholder), back and a tab switch keep the scroll place, breadcrumb up. | E3 |
| 10 | E4 | Search read every card field for everyone. **Closed in E5:** `GuideSearchScope.PREVIEW` (titles and rule numbers only) for free users, `FULL` for Premium; the locked card state holds no key points, cite or details. Tests: scope unit tests, view-model and on-screen tests, and the 402-card contract (no hidden word finds a card in the preview). | E5 |
| 11 | E4 | The search field on iOS (auto-focus on a fresh search, the Search key, keyboard over the results, back from a card) is covered only by the Native timing and correctness tests; iOS UI cannot be driven from here. Check it in the E9 iOS simulator walkthrough. | Open for the owner (iOS walkthrough skipped in E9, 2026-10-09) |
| 12 | E4 | Cold start and APK size were not re-measured on the device against E3 (like E3: nothing is added at launch). Re-measure at E9 against the E2 figures (190 ms, 188 ms `minifiedTest`) and the Play 1.3.0 baseline. | Closed in E9 (2026-10-09) |
| 13 | E5 | Staleness threshold. **Closed 2026-10-07 (owner):** 9 months confirmed (`GuideStaleness.STALE_AFTER_MONTHS`, pinned by test). The nudge shows from Oct 2026 for rates dated 2026-01. | E9 |
| 14 | E5 | **Owner rule (2026-10-07):** `GUIDE_PAYWALL_ENABLED` is flipped only when no card carrying the Rates chip is still an "Unverified point" (RP-088 HBA 8.5% is one); other unverified cards may stay. `GuideBundleContract.assertPaywallOnlyWhenNoUnverifiedRateCard` fails if the flag is on while such a card exists. **Still open at the flip:** a sandbox purchase on each platform unlocks an open Guide card, and the Claim Guide row then appears in the Premium screen and hub (`isAdvertised`). Check both. | Post-launch (paywall off by owner decision 2026-10-09) |
| 15 | E5 | **Android half closed 2026-10-07 on the Pixel 9** (debug build, Settings "Force Free"): a locked card shows its title, "Unverified point" chip, warning line and one-line answer, then the Unlock panel with no key points, authority or details; Unlock opens the upgrade sheet. **Still open:** iOS cannot be driven from here, so the locked panel, the sheet and chip wrapping at large text sizes go into the E9 iOS simulator walkthrough with EP 11. | Open for the owner (iOS walkthrough skipped in E9, 2026-10-09) |
| 16 | E5 | Search results cached under one scope could be drawn for a moment after the entitlement changed. **Closed 2026-10-07:** `GuideSearchState.Results` records its scope and `visibleTo(unlocked)` holds back results from another scope until the model has searched again (unit test, plus an on-screen test that revokes Premium mid-search). | E5 follow-up |
| 17 | E6 | Food line wording "full day (over 12 hours away), before taxes": the MoD letter text read does not mention taxes and the T181 Details sentence "plus taxes" has no source in hand. **Closed 2026-10-08 (owner):** the owner keeps the wording as their decision; no source is claimed. The assumption stays in `figures.json` with an explicit note. | E6 |
| 18 | E6 | Evidence below primary text for transport allowance and HRA (letters 12630/Tpt.A/Mov C/246/D(Mov)/17 and DoE OM 2(5)/2017-E.II(B) not read; handbook prints their numbers and rates), HRA step dates web-only, P116 OM number format. **Closed 2026-10-08 (owner):** the owner accepts handbook evidence as final for both figures (`HANDBOOK_ONLY` stays in `figures.json` as the permanent record); the HRA step dates 01-07-2021 and 01-01-2024 are confirmed by the owner (`OWNER_CONFIRMED`, dated, enforced by `figures.py`); the P116 card now cites the OM as printed in the handbook, "2(5)/2017-E.II(B)". If a letter ever turns up, upgrade the label then. | E6 |
| 19 | E6 | **Pixel check of the "Your figure" line with the real profile. Closed 2026-10-08 on the Pixel 9:** `run_guide_minified_smoke.sh` passed on the `minifiedTest` build, and after the owner restored the `.pcda` backup all four cards were driven on the real September 2026 payslip (food Level 12A 1,250 a day; CTG about 1,19,200; transport 5,760 a month; HRA correctly hidden, the payslip has RHA and no HRA). See E6 result. | E6 |
| 20 | E6 | The iOS simulator walkthrough must include the "Your figure" block (large text sizes, a card with and without a profile). Native correctness and timing are covered by `GuideLoaderIosPerfTest`; the UI is not driven from here. **Owner decision 2026-10-08:** merge E6, and the owner checks this in the existing E9 simulator walkthrough with EP 11 and 15. Open. | Open for the owner (iOS walkthrough skipped in E9, 2026-10-09) |
| 21 | E6 | Three E6 limits by design: the DA step is additive (differs from compounding only above DA 100%); CTG shows the 80% case (the 100% island case is named in the assumption, not computed); level labels 9, 10A, 12, 12B, 13B are in `figures.json` because the letter lists them but the Pay Audit matrix has no such levels. **Closed 2026-10-08 (owner):** accepted as final design. Revisit only when DA nears 100% or Pay Audit adds levels. | E6 |
| 22 | E6 | Two placeholder bullets on T181 and T254 were never shown. **Closed 2026-10-08:** deleted from the authoring file, and `figureTemplates` with its tests removed. The validator now rejects a raw `{` in any text of any card (`aRawPlaceholderAnywhereOnAnyCardIsRejected`). The `personal=` spec stays as the marker that a card has a figure; `compile.py` requires `figures.json` to hold it. | E6 |
| 23 | E7 | The Pay Audit link on iOS: the native view controller push, Back to the finding (month kept), and chip/button wrapping at large text. The iOS UI cannot be driven from here; the `NavBridge` path and the id contract are tested on Native. Add to the E9 simulator walkthrough with EP 11, 15, 20. Open. | Open for the owner (iOS walkthrough skipped in E9, 2026-10-09) |
| 24 | E7 | Not driven on a device: a locked user tapping the link (upgrade sheet, no card; Settings "Force Free" on a debug build) and a waiting (TPTA on hold) row, because the real history holds no open issue (only verified arrears links were driven on the Pixel). Both are covered by unit and on-screen tests. Open. | Closed in E9 (2026-10-09; link by tests, device unreachable) |
| 25 | E7 | Dataset gaps behind owner-accepted nearest-card links: no card on DA arrears (the arrears link opens `RB-RP-052`, whose DA 60% point is still flagged) and none on "HRA stops when you take quarters" (opens `RB-SS-P114`); `RB-C13-05` does not state the Level 14+ MSP exception. Content work, owner's call. Open. | Closed in E9 (2026-10-09) |
| 26 | E8 | Pin, Copy cite and Share on iOS: `NSUserDefaults` persistence and `UIActivityViewController` (the Native storage test and the 402-card contract run on the simulator; the UI is not driven from here), the Pinned section and button wrapping at large text. Add to the E9 simulator walkthrough with EP 11, 15, 20, 23. Open. | Open for the owner (iOS walkthrough skipped in E9, 2026-10-09) |
| 27 | E8 | Not driven on a device: the locked card and locked Home with pins stored (Settings "Force Free" exists only in a debug build, and the phone runs `minifiedTest`; pins wait for an unlock). Covered by on-screen tests. **Clipboard half closed 2026-10-08 on the Pixel 9:** after Copy cite on the food card, pasting into the Guide search field gave exactly the cite text. Open (locked half). | Closed in E9 (2026-10-09) |
| 28 | M4 | iOS walkthrough of the maintenance UI, on a build whose bundle has a replaced pair and a change entry (the real bundle has none, so a debug build with a temporary bundle, as on the Pixel in M4): the "What's new in the Guide" row on Home; the list and its card links; Back through Home, list, card, Earlier rule, See current rule on the native navigation; the Updated chip and the "Replaced on" chip wrapping at large text; a pinned replaced card on Home; search never listing the replaced card. Logic is covered by iOS unit tests (`GuideLoaderIosPerfTest` chips test, commonTest). Open. | Open for the owner |
| 29 | M5 | iOS walkthrough of Suggest a correction (the UI cannot be driven from here; the mailto encoding, the 1000-character cap and the email text are covered by `GuideSuggestionMailtoIosTest` and commonTest): the button wrapping next to Pin and Share at large text sizes; the dialog with the keyboard up and a full-length text (the field stops at eight lines and scrolls; title, notice and both buttons stay on screen), a long text, special characters (`& " # % +`, Devanagari, an emoji) and an empty text (Open email stays off); Open email hands over to Mail (or the chosen mail app) with the right recipient, `[Guide] <card id>` as subject and only the allow-listed lines in the body, and nothing is sent until Send; with no mail account the share sheet opens (existing `shareTextViaEmail` fallback); Cancel and the tap outside the dialog discard the text; native Back/swipe while the dialog is open; the action is absent on a locked card. Open. | Open for the owner |

## Post-E9 list (release follow-up; nothing dropped)
1. **Owner:** iOS simulator walkthrough (EP 11, 15, 20, 23, 26) before the iOS build ships with the Guide on; the release flip already reaches iOS.
2. **Owner:** clear the open points on the main rate cards (RP-088 HBA 8.5% first), then flip `GUIDE_PAYWALL_ENABLED` and run EP 14 (sandbox purchase unlocks a card on each platform; Claim Guide row in Premium screen and hub).
3. **Owner:** confirm or revise the 37 unverified cards, the DA arrears card first; RP-073 TA Allowance has no card.
4. Release train: merge `feature/guide-e9-release`, push (pre-push gate), then a normal release build per `project_release_1_3_0` procedure (versionCode, Gemma model path); nothing was uploaded in E9.
5. Next DA step (01-07-2026) is not issued yet: update `rates_as_of` and the DA card when it is.

## Maintenance track (M1-M9; plan in `~/.claude/plans/make-a-phase-wise-shiny-mochi.md`)
Content-maintenance work after E9. Each phase is a short-lived branch off main, merged when its gate is green. Nothing here changes what the Guide shows until M4.

| Phase | What | Status |
|---|---|---|
| M1 | One-command `tools/refresh.py`; stable card ids (`ids_lock.json`, `--- retire`) | Done 2026-10-09 (`feature/guide-m1-refresh-ids`, in main) |
| M2 | Expert review pack, `tools/review_pack.py [--pdf]`, intake template `17_expert_review_intake.md` | Done 2026-10-09 (in main) |
| M3 | Rule-change metadata: `effective=`/`replaces=`, `authoring/changes.txt`, per-card `rev`, bundle `changes`, `rates_report.py`; Kotlin model fields and two validator rules | Done 2026-10-09 (`feature/guide-m3-change-metadata`, committed, not merged or pushed) |
| M4 | "What changed" UI: change log and rule history domain, replaced cards out of search, Updated/Replaced chips, What's new row and list, Earlier rule / See current rule links, validator loop rule | Done 2026-10-09 (merged and pushed, main at 65221c10) |
| M5 | Suggest a correction: `GuideSuggestion` (allow-listed email, user text trimmed and capped at 1000), action and dialog on unlocked cards, email through the existing helper | Done 2026-10-09 (merged and pushed, main at 83e21934, pre-push gate passed in 316 s; branch deleted); gate and Pixel walkthrough passed |
| M6 | Personal notes, data layer: `GuideNote` domain, encrypted `guide_notes` rows (Room 13 to 14), `GuideNotesRepository`, backup v4 | Done 2026-10-10 (`feature/guide-m6-notes-data`, committed, not merged, not pushed); gate passed; Pixel: migration and Guide walked, on-device note proof NOT run (see M6 result) |
| M7-M8 | Personal notes UI, runbook and release readiness | Not started |
| M9 | Carry-over list of anything left unfinished | Collects in the plan file |

**M3 result.** Bundle 254,528 → 261,409 bytes (+6,881: an 8-hex `rev` on every card and an empty `changes` list). Bundle version unchanged (1); the Kotlin parser already ignored unknown fields (characterization test run before the model change). New `GuideCard` fields `rev`, `effective`, `replacedBy`, `until`; `GuideBundle.changes`; `GuideChange`/`GuideChangeItem`. `GuideBundleValidator` rejects a `replaced_by` that is not another card and a replaced card that is still homed in a case or an `also` list (a replaced card needs no home). `check_r8_guide.py` now covers 12 models (was 10) and passes. Python tool tests 142 (M2: 81). No EP item opened or closed.

**M4 result.** No bundle change (still 261,409 bytes, version 1; `check_r8_guide.py`: 12 models kept, bundle byte-identical). New in `shared`: `GuideChangeLog`, `GuideRuleHistory` (every walk keeps a visited set), `GuideIndex.trust(card)` (the one place chips are decided; `GuideTrust` gained `updated` and `replacedUntil`), replaced cards left out of `GuideFeedLogic` and `GuideSearchIndex`, and two `GuideBundleValidator` rules (a `replaced_by` chain may not loop; a replaced card needs `until`). In `composeApp`: `GuideDestination.Changes` (saved as `changes`), `GuideMaintenanceStrings.kt`, `GuideMaintenanceModels.kt`, `GuideWhatsNew.kt`, `GuideCardHistoryLinks.kt`; Home shows the row only when the bundle has a change entry. Fixture: `SyntheticGuideRuleChange` in `shared-test-fixtures`. Tests added: change log, rule history, trust, validator, feed and search characterization plus new behaviour, view-model, saver, 11 on-screen tests, 2 end-to-end tests, host test, a security-contract scan, a bundle-contract invariant check (run on the real bundle on both platforms and on the fixture on iOS) and a Native timing test (10 passes over 404 cards: 3 ms). Pixel 9: smoke on the real `minifiedTest` bundle passed before and after; three real cards and Home unchanged; on a debug build with a temporary bundle (reverted, `git status` clean of it, `bundle.py --check` up to date) the row, list, links, chips, search, pin of a replaced card, process death with the old card open and Back through the chain all behaved. Opened EP 28. Carry-over items are in the plan file's M9 section.

**M4 follow-up (2026-10-09, same branch).** Cleared the small items found in the M4 review: validator date-shape and change-log card-id checks, a `compile.py` guard against rupee figures in change text, no stale-rates nudge on a replaced card, no stack growth when tapping Earlier rule / See current rule back and forth, and the Pixel checks for light theme, a locked user and the tab re-tap. Still open: EP 28 (iOS).

**M4 share decision (2026-10-09, owner).** A replaced card keeps Share and Copy cite. The claim note of a replaced card carries one line under the answer, "Replaced on <date>. It applies only to claims for earlier periods."; a current card's note is unchanged. Merged and pushed (main at d1a36d58).

**M5 result.** No bundle change (261,409 bytes; `check_r8_guide.py`: 12 models kept, bundle byte-identical; `minifyReleaseWithR8` passes; no keep rule needed, nothing `@Serializable` was added). New in `shared`: `GuideSuggestion` with `GuideSuggestionParts` (six fixed fields: card id and title, bundle `generated`, card `rev`, app version, the user's text) and `GuideSuggestionLabels`; the text is trimmed and capped at 1000 UTF-16 characters, a cut through an emoji drops the half pair, and a blank `rev` or `generated` leaves its line out. Subject is `[Guide] <card id>`. In `composeApp`: `SupportMessageDialog` (the layout of the old `ReportIssueDialog`, copy passed in; `ReportIssueDialog` now delegates to it, behaviour pinned first by `ReportIssueDialogTest`), `GuideSuggestionBuilder.kt` (the `GuideIndex.suggestionMail` extension and `GuideViewModel.suggest`), a third `email` callback on `GuidePlatform` (the only Guide file that names `shareTextViaEmail`; default no-op for tests), `onSuggest` and the dialog in `GuideCardActions.kt` (the action row is now a `FlowRow` so three buttons wrap at large text), 13 strings in `GuideMaintenanceStrings.kt`. Owner decisions (2026-10-09): the user's text is not rewritten (the sanitizer would turn `Rs 1,500` into `[REDACTED_AMOUNT]`; the dialog warns instead), extract the shared dialog, discard a half-typed text, button reads "Open email". Tests added: 10 domain, 7 view-model, 12 on-screen (`GuideSuggestionUiTest`), 5 characterization (`ReportIssueDialogTest`), 1 end-to-end on the real bundle, 3 security-contract scans (mutation-checked: a planted `mailto:` and a `.cite` read in the builder made two of them fail), 4 iOS tests on the simulator. Gate green: tool tests, `bundle.py --check`, `refresh.py --check --allow-no-sources`, `ktlintCheck`, size audit, `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`, full `iosSimulatorArm64Test` (1428 tests) and the iOS link check. **Pixel 9 (`minifiedTest`, real bundle, owner's payslips kept):** `run_guide_minified_smoke.sh` passed before and after the fix below. On the food card (`RB-SS-T181`, which shows the owner's own "Your figure", level and DA) Suggest a correction opened the dialog; Open email stayed off for empty and blank text; with text containing `& # ? " %` Gmail opened with To `founder@ai-borne.in`, subject `[Guide] RB-SS-T181` and a body of exactly the allow-listed lines (card id and title, Guide data 2026-10-09, card revision `7df629d0` = the bundle's, app version 1.3.0, the text with specials intact); none of the figure, level or DA was in it. A 1,300-character text was cut to 1,000 (ends at the 200th word, none after) in both the field and the email. Cancel opened no mail app and a reopened dialog was empty; Android Back with the dialog open closed only the dialog. Every draft was discarded; Gmail holds no draft and no sent mail from the session. **Found on the phone and fixed in the same phase:** with a full-length text and the keyboard up the dialog outgrew the screen and its title and Open email sat behind the keyboard (Report an Issue had the same flaw); the field now stops at eight lines and scrolls (`SupportMessageDialog`, `maxLines`), and the walkthrough was repeated on the rebuilt app (title, notice and both buttons visible above the keyboard). Robolectric cannot show this (it reports one line either way), so there is no unit test for it; the device walk is the evidence. Not driven on the phone: a locked card (the `minifiedTest` build has no Force Free; covered by on-screen tests), Devanagari and emoji input (no adb path; covered by domain and iOS tests), light theme and large text. Opened EP 29.

**M6 result (2026-10-10).** No UI, no strings, no composables, no bundle change (261,409 bytes, `check_r8_guide.py`: 12 models kept, bundle byte-identical). New in `shared`: `GuideNote` (private constructor behind `GuideNote.of`: plain card id, revision a plain token or empty, trim, 2000-character cap that never splits an emoji, blank text is no note; `toString` prints a length only), `GuideNoteEdit` (the one place "blank means delete" lives: Save, Erase or Reject), `GuideNotePlacements` (own, carried to the END of the `replaced_by` chain and always stale, or on a removed card; walk is `GuideRuleHistory`'s, so a loop or dangling chain lands in "removed"), `GuideNoteEntity` (`guide_notes`: card id key plus one AES-GCM ciphertext; the card id is repeated inside the ciphertext because GCM does not authenticate the key beside it, so a row moved to another card is refused), `GuideNoteUnreadableException` (fixed message, no cause: serialization errors quote their input), `GuideNotesRepository` / `RoomGuideNotesRepository` (observe as a Flow with an `unreadable` count; unreadable device rows are skipped and kept, never deleted) and `FakeGuideNotesRepository` in `shared-test-fixtures`. Notes are `PayslipDao` methods, not a second DAO, so they join `replaceWithBackup` / `mergeBackup` / `clearAllUserData` in one transaction ("Delete all data" erases them). `PayslipDatabase` 13 to 14 by `AutoMigration`; `14.json` exported. Backup `CURRENT_VERSION` 4 with `guideNotes`; notes travel under the backup password and are re-encrypted to the device key on restore. Owner decisions (2026-10-09): an unreadable backup note is skipped and the rest restores; MERGE keeps the newer `updatedAt` for a card (tie goes to the backup); a note follows `replaced_by` to the end of the chain; notes are in the archive for everyone (the Premium gate on backup is in Settings, not the data layer). Wired in `GuideModule` (`single<GuideNotesRepository>`). `GuideSecurityContractTest` gained a scan of the seven notes files (no CrashReporter, Logger, println, share or clipboard seam, no interpolated exception message), that `GuideShareParts` has no note field, and that the entity holds only the key and one ciphertext.

**M6 gate.** Tool tests 144 OK, `bundle.py --check`, `refresh.py --check --allow-no-sources`; `./gradlew check -x iosX64Test -x iosSimulatorArm64Test` green; `iosSimulatorArm64Test` 1,496 tests, 0 failures, plus the iOS link; `assembleMinifiedTest`, `check_r8_guide.py`, `minifyReleaseWithR8` green (mapping shows `GuideNoteEntity`, `GuideNotePayload` and both serializers kept: no keep rule added); pre-commit hook green. Four deliberate bugs (merge comparison, row-swap check, cap, notes missing from clear-all) were each caught by their own test, then reverted.

**M6 Pixel (2026-10-10).** The owner took a `.pcda` backup first. `run_guide_minified_smoke.sh` installed the new `minifiedTest` over the existing app (`firstInstallTime` unchanged, 8 Oct; no uninstall) and passed. After a cold start the app opened on the owner's data (Home, History with its months, no crash in logcat), and a Guide food-charges card showed "Your figure" from the stored payslips, which proves payslip decryption after the 13 to 14 migration. **Not run on the phone, said plainly:** (1) the on-device proof that a note written through the repository survives an app restart and is absent in plaintext from the database file. It needed a temporary debug-only receiver in a build sideloaded onto the phone that holds real payslips; the tool's safety check refused that (an exported receiver is a remote-code surface) and it was not retried another way. That proof is covered off-device by `GuideNotesRoomTest` (real SQLite file on the JVM: note survives close and reopen; no note text or revision in any `payslips.db*` file) and `GuideNotesRoomIosTest` (Kotlin/Native, Keychain key). M7's UI gives the real on-phone proof (add a note, kill the app, reopen; backup and restore round trip). (2) A note encode/decode under R8 at runtime: only static evidence (mapping and the release R8 build); there is no notes UI to reach it. (3) Individual corrections were not inspected on the phone; the migration only adds a table (`PayslipDatabaseUpgradeTest` covers payslips and corrections across 13 to 14 on real schemas).

**M6 flaky test found (not caused by notes, but trigger-able).** Adding any test to `AppKoinModulesTest` made `:composeApp:testDebugUnitTest` fail intermittently (3 of 5 runs) with `UncaughtExceptionsBeforeTest` in an unrelated later test; clean `main` passed 4 of 4, the branch without that test 6 of 6. Cause: that class builds `PayslipViewModel`s whose background `observePayslips` collection races `Dispatchers.resetMain()` and throws "Main dispatcher missing" on a worker thread; the kotlinx-test collector then blames the next `runTest`. M6's wiring test lives in its own class (`GuideNotesWiringTest`, no Main dispatcher). The latent race is untouched (owner's code, not in scope).

**M6 decision (owner, 2026-10-10).** A REPLACE restore of an older (pre-v4) backup erases the device's notes, like corrections and letters. Kept as built.
