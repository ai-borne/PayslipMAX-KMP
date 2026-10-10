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
| A7 | R8 gate (`check_r8_guide.py`) checks `minifiedTest`, not the shipping release build | P4 | open |
| A8 | Internal fields `status`, `topic`, `domain` ship in the bundle and `GuideCard` | P5 | open |
| A9 | Guide dead code: `GuideSuggestion.canSend`, `openCardHint`, `CardTemplate.body`/`GuideCardBody`, `GuideStaleness.epochDays` | P6 | open |
| A10 | Duplicate limit (`SUPPORT_MESSAGE_MAX_CHARS`) and duplicate colour literals vs `ThemeColors` | P6 | open |
| A11 | `guideSmokeTest` dependencies outside `libs.versions.toml`; stale "402" / "252 KB" comments | P6 | open |
| A12 | Pre-1.3.0 dead code: `PremiumSettingsCard`, 9 strings, 4 dimensions, literal glyphs, `Products.storekit` description | P7 | open |
| A13 | Stale comments and docs (CLAUDE.md schema v14 / backup v4 / R8 wording, HANDOFF top block, 36 vs 37 count) | P9 | open |
| A14 | Scratch tools `todo_pay.py`, `pa.py`, `packet.py` outside `tools/archive/` | P9 | open |
| A15 | `refresh.py` prints `FAILED at step "b"` on a passing run (seen in the P0 baseline) | P9 | open |
| A17 | Pixel smoke and walk for P1 and P2 (Pay Audit link opens its card) not run (no device attached, 2026-10-10); later phases may add to it | P11 | open |
| A16 | Old local/remote branches not yet checked against `main` | P10 | open |

## B. Carried over from M1-M8

| ID | Item | Phase | Status |
|---|---|---|---|
| B1 | Flaky `GemmaInstallTelemetryIntegrationTest` (`UncaughtExceptionsBeforeTest`, leaking coroutine resumes on Main); cause unknown | P8 | open |
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
| B18 | Release prep (version bump, versionCode, store notes) | - | owner (D6, separate step) |

## C. Baseline for this branch (P0, 2026-10-10, HEAD `d6424c11`)

See "P0 baseline" at the end of `20_prerelease_debt_plan.md` for the numbers (build, tests, APK size, bundle hash).
Per-card `rev` values are saved outside git (scratchpad `rev_baseline.json`, 404 cards) and re-derivable at any time
from `git show d6424c11:composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json`; P5 compares against that.
