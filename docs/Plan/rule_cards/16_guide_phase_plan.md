# Claim Guide, Phase E plan (E1 to E9)

Status: **proposed 2026-10-07, awaiting owner approval.** Phase E0 (this plan) is docs only; no app code.
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
- **Narrow keep rule.** Keep `com.payslipmax.pdfparser.guide.model.**` serializers (`$$serializer` and
  `Companion.serializer()`) only, never package-wide (doc 05). Audited with the `r8-analyzer` skill.
- **Check 1 (CI and pre-push), new `scripts/check_r8_guide.py`.** After `minifyReleaseWithR8`, it reads
  the R8 mapping and fails unless every guide model and its serializer survive. It also confirms the
  bundle file is in the shrunk output, byte-identical to the source (optimized resource shrinking).
  `git-pre-push.sh` gains the `minifyReleaseWithR8` step, which neither hook runs today.
- **Check 2, minified runtime smoke.** A `minifiedTest` build type (release R8 config, debug signing) and
  one instrumented test: load and validate the bundle, then open Home, a case and a card. Run on the Pixel
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
  Card(cardId), Search. It changes only through `push`, `pop` and `popToHome`.
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
  it survives restore. Expanded details and scroll position use `rememberSaveable` keyed by id.
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
  `unverified`; No official source = one rule fixed in E1; Amended = `AMENDED` chip.
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

## E4 Search (secondary)
**Goal:** a search icon on Home that finds words and rule numbers ("177", "177B", "Rule 114").
**Files:** `shared/.../guide/domain/GuideSearchIndex.kt`, `GuideRuleNumberParser.kt` (plain string scans),
`ui/screens/guide/GuideSearchScreen.kt`, `GuideSearchViewModel.kt`.
**Tests:** "177" does not match "1770"; case and punctuation are ignored; empty and one-letter queries; a title match outranks
a details match; the query is never stored, saved or logged (gone after simulated process death); opening a result pushes
the card and back returns to the results; iosTest timing over all 402 cards and a fixed query set.
**Exit:** gates green, including the iOS timing test. **Tech-debt checkpoint:** ranking weights in one constant block.

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

## E7 Pay Audit link
**Goal:** a finding opens its matching card, and back returns to the finding.
**Owner review first:** the table mapping each finding type (and pay line) to a card, for MISSING_ALLOWANCE,
TPTA_ENTITLEMENT, ARREARS_AUDIT, INCREMENT_MISSED and MSP_SHORTFALL.
**Files:** `shared/.../guide/domain/GuideLinkMap.kt`, one link control in `PayAuditFindingsSection.kt`,
`Screen.GuideCard` in `App.kt` and its iOS host case in `MainViewController.kt`, `GuideViewModel.openCard`.
**Tests:** findings rendering pinned first; every mapped id exists; the link pushes `GuideCard` and back returns
(a `NavBridge` test covers the iOS path); a restored `GuideCard` with no target pops itself; the link is blocked while
locked; a new finding type with no decision fails a test; flag off means no link; users without Premium see the paywall.
**Exit:** gates green; Pay Audit suites unchanged. **Tech-debt checkpoint:** none carried.

## E8 Pins, copy cite and share as claim note
**Goal:** pinned cards on Home, Copy cite, and Share as claim note.
**Files:** `GuidePinsStorage` with Android and iOS implementations and a fake (no Room change, not in
backup), `domain/GuideShareText.kt`, the existing `ShareUtils.kt` and platform clipboard.
**Tests:** pins persist; stale ids are dropped after a bundle update; a fixed-text test of the share note
(card text and cite only, no profile figures, name or service number); the copied cite equals `cite`; nothing leaves without a tap.
**Exit:** gates green; `minifiedTest` smoke. **Tech-debt checkpoint:** none carried.

## E9 End-to-end, security review and release
**Work:** end-to-end tests (tile, feed, card, pin); the `security-review` skill on the branch; confirm no new
outbound path, nothing Guide-related in telemetry, and no `from`/`open` in the bundle; a release build and an iOS
simulator walkthrough; baseline comparison; then turn on `GUIDE_ENABLED` for release in its own commit.
**Exit:** full pre-push gate green; HANDOFF.md, docs and memory updated.
**Tech-debt checkpoint:** a known-gaps register (RP-073, unverified cards, the DA 60% flag).

## Open items carried into E1
Bundle location (E1 spike); the `rates_as_of` month (owner); the GUIDANCE vs empty-cite rule (owner); a CI
emulator job, yes or no (owner).
