# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
Keep this file at 200 lines or below.

## What this is

PayslipMax: an offline-first Kotlin Multiplatform (Android + iOS) app that parses Indian Army PCDA(O)
payslip PDFs into structured salary data and generates wealth/error-detection insights. Everything runs
on-device; no PII leaves the device except optional cloud AI inference. Three independent sub-projects
live in this repo:

- **`shared/` + `composeApp/` + `iosApp/`** — the KMP app (Kotlin Multiplatform + Compose Multiplatform). This is the primary codebase.
- **`functions/`** — Firebase Cloud Functions backend (Node/Jest), for cloud AI proxying.
- **`web-prototype/`** — a standalone Vite/vanilla-JS prototype (separate lint/test toolchain, not part of the KMP build).

**The parser/AI-insights architecture is documented in full at `docs/AI_INSIGHTS_PIPELINE.md` — read it
before touching anything under `shared/src/*/parser/` or `composeApp/.../ui/screens/ReplicaUtils.kt`.**
It is the single source of truth for the parsing pipeline, including a SWOT analysis and changelog of
past fixes; don't re-derive that architecture from scratch by reading files piecemeal.

## Commands

All Gradle commands run from the repo root.

### KMP app (shared / composeApp / iosApp)

```bash
./gradlew check -x iosX64Test -x iosSimulatorArm64Test   # full Android + common verification (mirrors CI)
./gradlew :shared:testDebugUnitTest                       # shared module unit tests (JVM, incl. corpus regression)
./gradlew :composeApp:testDebugUnitTest                    # composeApp module unit tests
./gradlew :shared:testDebugUnitTest --tests "*SomeTest*"   # run a single test class (any module, same flag)
./gradlew ktlintCheck                                      # style check, all modules (pre-commit hook mirrors this)
./gradlew ktlintFormat                                     # auto-fix style violations
python3 scripts/check_tech_debt_limits.py --strict <files> # 300-line-file / composable-length audit (pre-commit hook runs this on staged .kt files)
./gradlew iosX64Test iosSimulatorArm64Test                  # iOS unit tests (macOS only)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64   # verify iOS framework links cleanly (mirrors Xcode build phase; pre-commit runs this when commonMain/iosMain files are staged)
```

Opt-in, developer-machine-only integration tests (never run in CI, never touch committed fixtures, ±1.0 field tolerance):

```bash
./gradlew :shared:testDebugUnitTest --tests "*PlatformPdfParserTest*" \
  -Dpayslip.localCorpus="/path/to/Pay Slip Elements" \
  -Dpayslip.localCorpus.json="/path/to/ground-truth.json"
```

Pre-commit hook (`scripts/git-pre-commit.sh`, installed via `scripts/install-hooks.sh`) runs, in order: the
tech-debt/file-size audit on staged `.kt` files → `ktlintCheck` → (if `commonMain`/`iosMain` files are
staged) an iOS framework link check. All three must pass for a commit to go through.

### Firebase Functions (`functions/`)

```bash
cd functions && npm test    # jest --coverage --verbose
cd functions && npm run lint
```

### Web prototype (`web-prototype/`)

```bash
cd web-prototype && npm run dev     # vite dev server
cd web-prototype && npm run build
cd web-prototype && npm run test    # vitest run
cd web-prototype && npm run lint         # eslint
cd web-prototype && npm run lint:css     # stylelint
```

## Non-negotiable project rules

These are enforced mechanically (pre-commit hook + CI `checkFileSizes`/`check_tech_debt_limits.py` Gradle
task), not just style guidance:

- **300-line hard limit per file** (Kotlin, JS, CSS, HTML alike). Split into smaller modules immediately if a file approaches it. Composable functions also have a max-length check.
- **No hardcoded UI strings or colors.** All copy lives in `AppStrings.kt` (KMP) / `strings.js` (web-prototype); all theme tokens in `Theme.kt` (KMP) / `theme.css` (web-prototype).
- **MVVM / SOLID / DRY / SSOT** across the codebase — Model is fully decoupled from ViewModel/View; consolidate shared config/formulas into one source of truth rather than duplicating (e.g. `PayslipPatternConfig`, `ConfidenceThresholds`, `GrammarEraMapper` are canonical SSOT objects — extend them, don't shadow them).
- **TDD**: tests written before/alongside implementation; 100% passing before a phase is considered done.
- **Phase-wise plans for non-trivial work**, each phase ending in a fully green build. At the end of every phase: state what tech debt was incurred, how it was resolved immediately, and confirm build+tests are green before moving on — don't silently defer cleanup to "later."
- **Surgical changes**: touch only what the task requires; don't opportunistically refactor or reformat adjacent code.

## Working style rules

- **Think before coding.** State assumptions explicitly; ask rather than guess when uncertain. Present multiple interpretations when a request is genuinely ambiguous. Push back if a simpler approach exists. Stop and name what's unclear rather than working around confusion.
- **Simplicity first.** Minimum code that solves the problem — nothing speculative, no features beyond what was asked, no abstractions for single-use code. Test: would a senior engineer call this overcomplicated? If yes, simplify.
- **Surgical changes.** Touch only what you must; clean up only your own mess. Don't "improve" adjacent code, comments, or formatting. Don't refactor what isn't broken. Match existing style. (Same rule as above, under Non-negotiable project rules — restated here because it governs working style, not just file mechanics.)
- **Goal-driven execution.** Define success criteria up front and iterate until verified, rather than mechanically following a fixed list of steps.
- **Use judgment calls, not deterministic transforms, for LLM-shaped work.** Classification, drafting, summarization, extraction are good fits. Routing, retries, and deterministic transforms belong in code, not in a model call.
- **Surface conflicts, don't average them.** If two existing patterns in the codebase contradict, pick one (the more recent or better-tested) and explain why; flag the other for cleanup. Never silently blend conflicting patterns together.
- **Read before you write.** Before adding code, read its exports, immediate callers, and shared utilities it touches. "Looks orthogonal" is dangerous — if unsure why code is structured a certain way, ask before changing it.
- **Tests verify intent, not just behavior.** A test must encode *why* the behavior matters, not just *what* it does. A test that can't fail when the underlying business logic changes is a wrong test.
- **Checkpoint after every significant step.** Summarize what was done, what's verified, and what's left. Don't continue from a state you can't describe back; if you lose track, stop and restate.
- **Match the codebase's conventions, even when you disagree.** Conformance beats personal taste inside this codebase. If a convention seems genuinely harmful, surface it explicitly rather than forking silently.
- **Fail loud.** "Completed" is wrong if anything was skipped silently; "tests pass" is wrong if any test was skipped. Default to surfacing uncertainty, not hiding it.

## Architecture — KMP app

### Module boundaries

- `shared/` — all business logic: parser engine, domain models, repositories, encrypted persistence, AI insight auditors. Organized into `commonMain` (platform-agnostic) plus `androidMain`/`iosMain` (`expect`/`actual` platform adapters, e.g. PDFBox vs PDFKit token extraction). Test source sets: `commonTest` (pure unit tests, no device), `androidUnitTest` (JVM tests incl. the corpus regression suite, can use real Android libs via Robolectric-free plain JVM), `iosTest`.
- `composeApp/` — Compose Multiplatform UI, ViewModels, and display-layer formatting (e.g. `ReplicaUtils.kt`). Depends on `shared`.
- `iosApp/` — Xcode project wrapping the `composeApp`/`shared` Kotlin framework for iOS distribution.

### Parser pipeline (high level — full detail in `docs/AI_INSIGHTS_PIPELINE.md`)

Both platforms extract tokens (`AndroidTokenExtractor`/PDFBox vs `IosTokenExtractor`/PDFKit) into a shared
`TokenizedPayslip` IR, then run the **same** `GrammarAwareParser` → `SharedParsingPipeline` (a 7-tier
pipeline: token extraction → grammar detection → grid reconstruction → table classification →
reconciliation solver → offline Gemma fallback → schema validator) on both platforms — there is no
platform-specific parsing logic beyond token extraction. Key architectural facts that aren't obvious from
reading any single file:

- **Grammar detection is date-primary, not text-signature-primary.** The printed statement period (parsed via `StatementPeriodExtractor`) is mapped directly to a `GrammarFamily` by `GrammarEraMapper` (open-ended past Mar 2025 — new months need no code change). Text-signature matching (`GrammarDescriptor.detectorMatcher`, priority-sorted across all 5 registered families) is only the fallback path for undated/unparseable documents, or a broad `verificationMatcher` sanity check on the date-primary path.
- **Table/earnings-deductions extraction is grammar-agnostic.** `SharedParsingPipeline` always calls the single geometry-learning `TokenTableClassifier` directly, regardless of which `GrammarFamily` won detection — there is no `IGrammarTableStrategy` extension point (it existed as an all-`emptyMap()` stub cluster and was deleted as dead code). Grammar strategy dispatch (`IGrammarHeaderStrategy`/`IGrammarPageStrategy`) only affects officer-name parsing and tax/DSOP page extraction. `ModernGridStrategySet`/`ExtendedGridStrategySet` and `LegacyStatementStrategySet`/`EarlyDualColStrategySet` are each two `object`s wired to identical underlying strategy instances (intentional, documented — not yet differentiated; see the doc's SWOT for the tradeoff).
- **Structured (`ParsedPayslip.earnings`/`.deductions`) and raw (`.rawEarnings`/`.rawDeductions`) fields are populated disjointly** by `ReconciliationSolver.route()` — an item is never in both. The display layer (`ReplicaUtils.getCreditsList`/`getDebitsList`) always merges both; treating them as either/or was a real production bug (see the doc's changelog).
- **Tier 6 offline Gemma fallback** (`GemmaFallbackExtractor`/`GemmaEngine`) only runs when `ReconciliationSolver` flags `needsReview` or leaves raw leftovers — otherwise it stays on standby for battery/thermals. Its structured-JSON output is trusted directly into the same maps the geometry classifier populates, with no distinct lower-confidence marker (a known SWOT weakness).
- **Tier 7 `SchemaValidator`** is the final gatekeeper: it checks gross/deductions/net arithmetic against `TOLERANCE = 2.0` and flags `needsReview = true` on mismatch rather than failing the parse outright.
- `PayslipTokenParser` and the legacy `PayslipTextParser`/`DynamicSpatialParser` string path are **dead in production** (only referenced by debug/test tooling) — don't extend them; `GrammarAwareParser` is the only production entry point on both platforms.
- Corpus regression fixtures live in `shared/src/androidUnitTest/resources/corpus/` (52 de-identified real-era fixtures, Jan 2022–Apr 2026) and are the primary safety net against "fix one month, break another." `TokenParseCorpusRegressionTest` running the production engine against all 52 is the main gate — but 52/52 green does not by itself guarantee no user-visible regression on real documents whose exact layout noise (e.g. footer disclaimers) the corpus doesn't model.

### Persistence & security

- Payslip data and per-field corrections are AES-256 encrypted at rest (`CryptoHelper`, Android Keystore / iOS Keychain) via Room (`EncryptedPayslipEntity`, `PayslipCorrectionEntity`, schema v9). Corrections apply on read only (`ParsedPayslip.applyCorrections`) and never mutate the original parse — this lets re-parsing overwrite only the parsed side later.
- `CorpusScrubber` strips all PII (name/account/PAN/email) before any fixture is committed; numeric values are left untouched. Never commit a real PDF, real token dump, or unscrubbed fixture.
- Real PII from before the Phase 6 scrub commit remains in git history; a destructive history rewrite (`filter-repo`/BFG) is deferred pending explicit user decision — don't attempt it unprompted.
