# Claim Guide, Phase E plan (E1 to E9)

Status: E0 (this plan) done 2026-10-07. **E1 done 2026-10-07** (branch `feature/guide-e1-bundle`). **E2 done 2026-10-07**
(branch `feature/guide-e2-tiles`, off E1). **E3 done 2026-10-07** (branch `feature/guide-e3-feed`, off E2). **E4 done 2026-10-07** (same branch). **E5 done 2026-10-07** (branch `feature/guide-e5-trust-gating`, off `main`); E6 next.
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
**Owner decision (2026-10-07), Premium with a free preview.**
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
- Not checked on the Pixel: the locked panel (it needs a debug build with `FORCE_FREE`; the `minifiedTest` build is open). Covered
  by the Compose tests and the real-app tests; added to EP 15.
- Not done by design: the chips do not filter or sort; no per-card "read" memory; no price on the locked panel (the existing sheet
  shows the store price).

## E6 Personalisation from the Pay Audit profile
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

## E8 Pins, copy cite and share as claim note
**Goal:** pinned cards on Home, Copy cite, and Share as claim note.
**Files:** `GuidePinsStorage` with Android and iOS implementations and a fake (no Room change, not in
backup), `domain/GuideShareText.kt`, the existing `ShareUtils.kt` and platform clipboard.
**Premium (from E5):** pins, copy cite and share are paid (`guideUnlocked`); the controls are absent from the locked card state,
and a locked user's tap offers the upgrade sheet.
**Tests:** pins persist; stale ids are dropped after a bundle update; a fixed-text test of the share note
(card text and cite only, no profile figures, name or service number); the copied cite equals `cite`; nothing leaves without a tap.
**Exit:** gates green; `minifiedTest` smoke. **Tech-debt checkpoint:** none carried.

## E9 End-to-end, security review and release
**Work:** end-to-end tests (tile, feed, card, pin); the `security-review` skill on the branch; confirm no new
outbound path, nothing Guide-related in telemetry, and no `from`/`open` in the bundle; a release build and an iOS
simulator walkthrough; baseline comparison; then turn on `GUIDE_ENABLED` for release in its own commit.
**Exit:** full pre-push gate green; HANDOFF.md, docs and memory updated.
**Tech-debt checkpoint:** a known-gaps register (RP-073, unverified cards, the DA 60% flag).
**Carried here from earlier phases (EP):** 7 (Dashboard label at 320dp), 11 and 15 (the iOS simulator walkthrough: search field,
locked panel, upgrade sheet, chips), 12 (cold start and APK size), 13 (staleness threshold), 14 (paywall flip checks), 16 (search scope edge).

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
| 7 | E2 | With five tabs, the existing "Dashboard" label wraps to two lines at 320dp (fine at 360dp and up). Owner decision before launch: accept, or shorten the label. | E9 |
| 8 | E2 | Placeholder routes: `GuidePlaceholderScreen.kt`, `GuideStrings.comingNext` and `searchTitle`, and the smoke's last step. **Closed in E4:** the file and `comingNext` are deleted, `searchTitle` is the real screen's title, and the smoke searches and opens a card from the results. | E4 |
| 9 | E3 | The two E3 device checks. **Closed 2026-10-07 on the Pixel 9** (debug-signed Play-installer `minifiedTest` build, dark theme): `run_guide_minified_smoke.sh` passed (tab, area, case, card, "Key points"); walk-through: Home, area (case rules and counts), feed with chips and counts (All 16, Who qualifies 3...), chip filter, all six "also relevant here" rows naming their real home, card with breadcrumb to the feed it was opened from, Watch out in amber, cite in monospace, Details expand (food-charge card shows no placeholder), back and a tab switch keep the scroll place, breadcrumb up. | E3 |
| 10 | E4 | Search read every card field for everyone. **Closed in E5:** `GuideSearchScope.PREVIEW` (titles and rule numbers only) for free users, `FULL` for Premium; the locked card state holds no key points, cite or details. Tests: scope unit tests, view-model and on-screen tests, and the 402-card contract (no hidden word finds a card in the preview). | E5 |
| 11 | E4 | The search field on iOS (auto-focus on a fresh search, the Search key, keyboard over the results, back from a card) is covered only by the Native timing and correctness tests; iOS UI cannot be driven from here. Check it in the E9 iOS simulator walkthrough. | E9 |
| 12 | E4 | Cold start and APK size were not re-measured on the device against E3 (like E3: nothing is added at launch). Re-measure at E9 against the E2 figures (190 ms, 188 ms `minifiedTest`) and the Play 1.3.0 baseline. | E9 |
| 13 | E5 | `GuideStaleness.STALE_AFTER_MONTHS = 9` is a judgement, not an owner decision (DA is revised twice a year). With `rates_as_of` 2026-01 the nudge shows from Oct 2026 on every rate card. Owner confirms or changes the number (one constant and one test line). | E9 |
| 14 | E5 | `LaunchFlags.GUIDE_PAYWALL_ENABLED` is false, as decided: the owner clears the open points on the main rate cards (RP-088 and the rest) first, then flips it, in its own commit. What no test can prove before the flip: a sandbox purchase on each platform unlocks an open Guide card, and the Claim Guide row then appears in the Premium screen and hub (`isAdvertised`). Check both at the flip. | E9 |
| 15 | E5 | iOS cannot be driven from here. The locked panel, the Unlock button opening the upgrade sheet, and the trust chips wrapping at large text sizes are covered by Compose tests on Android only, and the locked panel was not looked at on the Pixel either (needs a debug build with `FORCE_FREE`). Add them to the E9 iOS simulator walkthrough with EP 11. | E9 |
| 16 | E5 | Accepted edge, not fixed: if a user's entitlement is revoked while a search is stacked under a card, the first frame after returning shows the results found under the old scope until `GuideTab` pushes the new scope (one effect later). It exposes only titles and answers the user could read a moment ago. Fixing it would need the scope in every result, which is more code than the risk warrants; revisit if refunds ever revoke Premium mid-session. | E9 |
