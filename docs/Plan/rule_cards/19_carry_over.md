# Carry-over and debt register (single source of truth)

Open debt for the Claim Guide lives **here and nowhere else** (owner decision D8, 2026-10-10). Phases of
`20_prerelease_debt_plan.md` close rows in this file; no other document keeps its own open list.
Status: `open` · `done (P#)` · `owner` (needs the owner, not code) · `accepted` (known limit, no action planned).

Sources: the M9 carry-over list (M1-M8, moved from the maintenance plan) and the pre-release audit of 2026-10-10.

## A. Audit findings (2026-10-10)

| ID | Item | Phase | Status |
|---|---|---|---|
| A3 | Notes are held after Search closes (`GuideSearchViewModel.state` is `Eagerly`, not `WhileSubscribed`) | P1 | done (P1: `WhileSubscribed(UI_STOP_TIMEOUT_MS)`, one shared constant; notes released about 10 s after Search closes: Search's grace, then the notes model's) |
| A4 | Pay Audit "See the rule" stays `Failed` after a failed load (`openCard` does not retry) | P2 | closed (P2; `openCard` retries after `Failed`, test-covered) |
| A6 | Debug-preview path (`isGuidePreviewBuild`, `BuildConfig.GUIDE_PREVIEW`) beside the `LaunchFlags.GUIDE_ENABLED` kill switch | P3 | done (P3: path deleted, scan in `GuideSecurityContractTest`; on-device smoke still owed, see P11) |
| A7 | R8 gate (`check_r8_guide.py`) checks `minifiedTest`, not the shipping release build | P4 | done (P4: defaults, CI and pre-push target release; on-device smoke still owed, see P11) |
| A8 | Internal fields `status`, `topic`, `domain` ship in the bundle and `GuideCard` | P5 | done (P5: bundle, `GuideCard` and fixture drop them; validator rejects them; `rev` of all 404 cards unchanged; bundle 261,409 to 240,327 bytes; on-device check owed, see P11) |
| A9 | Guide dead code: `GuideSuggestion.canSend`, `openCardHint`, `CardTemplate.body`/`GuideCardBody`, `GuideStaleness.epochDays` | P6 | done (P6: dead code removed, `GuideCardFull` holds the three lists, `epochDays` in `GuideTestClock`) |
| A10 | Duplicate limit (`SUPPORT_MESSAGE_MAX_CHARS`) and duplicate colour literals vs `ThemeColors` | P6 | done (P6: one `SUPPORT_MESSAGE_MAX_CHARS`; `ThemeColors` aliases, pinned by `ThemeColorsSourceTest`) |
| A11 | `guideSmokeTest` dependencies outside `libs.versions.toml`; stale "402" / "252 KB" comments | P6 | done (P6: smoke deps in `libs.versions.toml`; the two comments fixed; the remaining "402" in test comments go to P9) |
| A12 | Pre-1.3.0 dead code: `PremiumSettingsCard`, 9 strings, 4 dimensions, literal glyphs, `Products.storekit` description | P7 | done (P7: 37 strings + 1 getter, 4 dimensions, the card, `AppStrings.checkMark`, false "Cloud Backup" copy; `UiResourceUsageSourceTest` keeps it clean; on-device look owed, see P11) |
| A13 | Stale comments and docs (CLAUDE.md schema v14 / backup v4 / R8 wording, HANDOFF top block, 36 vs 37 count) | P9 | done (P9: stale wording swept in 8 files, CLAUDE.md schema v14 + notes + backup v4 + kill switch, HANDOFF top block rewritten and linked here, unverified count is 37 everywhere, phase plan M8/M9 rows) |
| A14 | Scratch tools `todo_pay.py`, `pa.py`, `packet.py` outside `tools/archive/` | P9 | done (P9: moved with `git mv`, each puts `tools/` on `sys.path` and was run once from the new place; README, HANDOFF and HANDOFF_PROMPT paths updated) |
| A15 | `refresh.py` prints `FAILED at step "b"` on a passing run (seen in the P0 baseline) | P9 | done (P9: the banner came from `test_stops_at_the_first_failure` printing through the default `say`; it now captures it and asserts the message; `test_a_passing_tool_test_run_never_prints_failed` guards it) |
| A18 | P7 deleted two strings that only the Swift iOS app reads (`appProtectedTitle`, `appProtectedShieldDesc`); caught by the pre-push XCTest step | P10 | done (P10: restored; `UiResourceUsageSourceTest` scans Swift uses; see P10 result) |
| A17 | Pixel smoke and walk for P1-P7 | P11 | done (P11, 2026-10-11, Pixel 9: smoke passed, which also proves the P4 path that builds `minifiedTest` itself; note-only search found the card marked "In your note", query kept after opening the card and after leaving Search for another tab, no "updated since your note" marker; Guide tab re-tap returns to the root; card sections, Share, Suggest a correction dialog (typed, cancelled) and Report an Issue dialog render; Settings shows the ✓ badge; Premium screen shows "✓ Included"; the throwaway note was deleted). One item **waived by the owner**: the Pay Audit "See the rule" link could not be tapped, because all 33 audited months on the device have 0 issues; covered by `GuideOpenCardTest` and the Pay Audit tests) |
| A16 | Old local/remote branches not yet checked against `main` | P10 | done (owner-approved 2026-10-11; deleted, after a green pre-push gate of 307 s incl. native XCTest: remotes `ci/gate-web-prototype-on-changes`, `feature/insights-impr`, `feature/pay-audit`, `feature/security`, `fix/ios-storekit-test-config`, `release/ios-1.0.0-v6`, plus `feature/closetest_01` (local and remote, patch-equivalent to main). Kept, not proven merged: `docs/rule-cards-dataset`, `docs/rule-cards-nav`, `feature/insightsgemma`, `feature/pay_audit_1.0`, `test/baseline-909f2ef`; see B19) |

## B. Carried over from M1-M8

| ID | Item | Phase | Status |
|---|---|---|---|
| B1 | Flaky `GemmaInstallTelemetryIntegrationTest` (`UncaughtExceptionsBeforeTest`, leaking coroutine resumes on Main); cause unknown | P8 | done (fixed before this branch, per the owner, 2026-10-11: `18e58901` / #19, the fake intelligence repository no longer hops to `Dispatchers.Default`; it is on `main`, not yet on this branch, so merge `main` before the PR; the 10-run loop was started and stopped on the owner's word, not run to the end) |
| B2 | `copycheck` cannot run in CI (needs the copyrighted source text, outside git); runs on the owner's machine only | - | accepted (decide later: scrubbed n-gram index) |
| B3 | `retire` directive and "ID vanished" error untested on production data (no card retired yet) | - | accepted |
| B4 | CLAUDE.md has no "Rule 12"; "Fail loud" was applied. Owner to confirm that is what was meant | - | owner |
| B5 | Expert review PDF (`review/guide_review_2026-10-09.pdf`) generated, not sent; regenerate if cards changed | - | owner |
| B6 | `rates_report.py` is deliberately broad (234 of 404 cards); narrow after the first real 8th CPC sweep | - | accepted |
| B7 | Validator checks date shape only (month 1-12, day 1-31); `compile.py` checks the calendar | - | accepted |
| B8 | `GuidePlatform.email` defaults to a no-op; `rememberGuidePlatform()` is the only production constructor | - | accepted |
| B9 | No mail app: `shareTextViaEmail` falls back to the share sheet without the recipient (also for Report an Issue) | - | accepted |
| B10 | Card id is the one plaintext on disk for notes (row key); unreadable rows skipped on read, left on disk | - | accepted |
| B11 | No committed v4 golden backup archive (v3 golden restores; v4 round-trips in tests) | - | accepted |
| B12 | Notes: a note on a twice-replaced card follows the chain to its end; notes shown for locked users in the removed-cards row and unreadable line | - | accepted |
| B13 | `run-as` works on the debug build (debuggable) | - | accepted |
| B14 | 8th CPC rehearsal stops at `GuideFiguresContract` by design (a new figure needs its value from the order) | - | accepted |
| B15 | Not driven on a phone: locked card / Force Free, large text, Devanagari or emoji input, replaced-card share note, Pay Audit link to a replaced card (covered by on-screen tests) | - | accepted |
| B16 | iOS UI walkthrough, 13-row checklist in `16_guide_phase_plan.md` (EP 11, 14, 15, 20, 23, 26, 28, 29, 30) | - | owner |
| B17 | Light-theme status bar | - | done (status bar follows the effective theme, commits b51789f6 / 06f0983a) |
| B19 | Five branches not proven merged into `main` (`docs/rule-cards-dataset`, `docs/rule-cards-nav`, `feature/insightsgemma`, `feature/pay_audit_1.0` (39 unique commits), `test/baseline-909f2ef`): owner decides whether each is squash-merged or abandoned; the dataset ones are probably squash-merged | - | owner |
| B18 | Release prep (version bump, versionCode, store notes) | - | owner (D6, separate step) |

## C. Baseline for this branch (P0, 2026-10-10, HEAD `d6424c11`)

See "P0 baseline" at the end of `20_prerelease_debt_plan.md` for the numbers (build, tests, APK size, bundle hash).
Per-card `rev` values are saved outside git (scratchpad `rev_baseline.json`, 404 cards) and re-derivable at any time
from `git show d6424c11:composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json`; P5 compares against that.
