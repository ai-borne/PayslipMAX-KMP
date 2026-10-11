# TA/DA pre-claim: strict phase-wise execution plan

Status: **P0 built and verified locally (2026-10-11); P1 onward not started. The pending points of P0 are consolidated in P10.** Facts, portal screens, open items and every owner decision live in
`21_ta_da_preclaim_plan.md` (read it first; this file does not repeat them). This file is **how we build**: the rules, the
handoff protocol and ten phases (P0 to P9). Branch: **one feature branch `feature/ta-da-preclaim`** for the whole plan, one commit
per phase, made only after the phase's gate is green and the owner says OK. No push, PR or merge unless the owner says so.

## 1. Standing rules (every phase, no exceptions)

| # | Rule | How it is enforced here |
|---|---|---|
| 1 | **Phase independence.** Every phase ends with a fully successful build; the next phase does not start on a red one. | "Build" = the baseline gate below, plus the phase's extras. Nothing is user-visible until P7 and the flag `PRECLAIM_ENABLED` stays false, so each phase stands alone on the branch. |
| 2 | **TDD, unit and integration.** Tests are written before or alongside the code; the red run is recorded; 100% pass, none skipped. | Each phase lists its unit and integration tests. The Phase Summary gives the test counts and says "0 skipped, 0 ignored". An `@Ignore` or a disabled test fails the phase. |
| 3 | **MVVM, SOLID, DRY, SSOT; 300-line limit.** | Rules and rates in `shared` domain/data; ViewModel in `composeApp`; composables only draw state and forward events. One home per rate (`figures.json`) and one DA-step resolver (`PersonalFigureResolver`), never copied. **300 lines is hard** (`check_tech_debt_limits.py` fails above 300 and has no SRP exemption). See 1a. |
| 4 | **No hard-coded strings or colours.** | This project keeps copy in Kotlin string objects, not Android XML (CLAUDE.md): new `PreClaimStrings.kt` in `ui/theme/` (`GuideStrings.kt` is 182 lines, `AppStrings.kt` 288, so a sibling file). Worksheet text (field labels, documents, rule-picker lines) is bundle content. Colours only from `Theme.kt`/`ThemeColors.kt` tokens. A scan test fails on a string or colour literal in the new screens. |
| 5 | **Security.** No vulnerability, no PII leak. | The checklist in 1b runs every phase and is reported in the Phase Summary. `security-review` skill at P0, P7 and P8. |
| 6 | **R8 compliance.** | The R8 column of the gate matrix (section 3) says per phase whether `minifyReleaseWithR8` / `assembleRelease` + `check_r8_guide.py` run. A new `@Serializable` type, Koin binding, reflection or library always triggers it. Keep rules stay narrow (`r8-analyzer` skill if one is added). |

**1a. The 300-line conflict.** The owner's rule allows a file over 300 lines only when splitting would break the Single
Responsibility Principle and a comment says why. The mechanical gate (`check_tech_debt_limits.py`, pre-commit and CI) allows no
exception. Decision for this plan: **split every file to 300 lines or fewer.** If a phase meets a file that truly cannot be
split, it stops and asks the owner to approve a script exemption (a header marker the script reads, with its own test). Until
then no file over 300 lines is committed. Composable length limits also apply.

**1b. Security checklist (reported every phase).** (a) No payslip value, name, service number, km, rate or token in logs, crash
keys, exceptions or test fixtures; (b) no new network call from the app; the only new outbound code is the P0 CI job; (c) every
user input has bounds (km, days, nights, persons, text lengths such as the 40-character sanction id); (d) clipboard is per field,
never copy-all; (e) no real PDF, token dump or unscrubbed fixture committed (the owner's claim PDFs stay in Downloads); (f) CI
workflow has least-privilege permissions, pinned action versions and no secrets; (g) gitleaks pre-commit passes; (h) no regex with
lookaround or backreference in `commonMain`.

## 2. Phase Handoff Protocol (every phase, then stop)

Before the next phase starts, the agent posts a **Phase Summary** with exactly:
1. **Tech debt incurred** in the phase (including anything deferred in a comment, a TODO, a loosened test, a file near 300 lines).
2. **The exact steps taken to resolve it, immediately and fully**, in the same phase. No skipping, no "later", no carry-over row.
3. **Confirmation that the build succeeds and every test passes**: the gate commands run, the test counts per module, 0 skipped.

Plus the rule-6 R8 result, the 1b security checklist, and the files touched with their line counts. The agent then **stops** and
waits for the owner's OK; the OK triggers the phase commit. A phase with unresolved debt or a red gate is not complete.

## 3. Gate matrix

Baseline gate (every phase): `python3 scripts/check_tech_debt_limits.py --strict <changed .kt>`; `./gradlew ktlintCheck`;
`./gradlew check -x iosX64Test -x iosSimulatorArm64Test`; `python3 docs/Plan/rule_cards/tools/refresh.py --check --allow-no-sources`.
Gradle runs one task at a time, JDK 21.

| Phase | Python tool tests | iOS tests + framework link | R8 (`minifyReleaseWithR8`, `assembleRelease` + `check_r8_guide.py`) | Device walk |
|---|---|---|---|---|
| P0 watcher | yes | no | no (no app code) | no |
| P1 rate research | no (docs) | no | no | no |
| P2 figures in the bundle | yes | yes (`commonMain` model) | **yes** (bundle and `@Serializable`) | no |
| P3 worksheet content | yes | yes | **yes** | no |
| P4 calculator, PD | no | yes | no (pure Kotlin, no new type serialised) | no |
| P5 calculator, TD | no | yes | no | no |
| P6 ViewModel, flag, DI | no | yes | **yes** (Koin) | no |
| P7 UI | no | yes | **yes** | Pixel smoke |
| P8 verification | yes | yes | **yes** | Pixel walk; iPhone walk is the owner's |
| P9 release prep | out of scope | | | |

## 4. Phases

Every phase below follows the same shape: **Goal, Depends, Tests first (unit and integration), Build, Files and SSOT,
Strings and colours, Security, R8, Anticipated debt (and the fix in the same phase)**. "Anticipated debt" is a prompt for the
Phase Summary, not a licence to incur it.

### P0. FAQ and notifications change watcher (tooling, CI)
- **Goal:** know when the public FAQ or notifications feed changes. **Depends:** nothing.
- **Tests first:** unit `test_faq_watch.py` with fixture feeds: unchanged, FAQ added/removed, answer edited, question edited,
  notification added, HTML-only difference (not a change), malformed JSON, HTTP error, empty list (unreachable, not "all removed"),
  manifest holds no field longer than an id, date or hash. Integration: run `--check` against the seeded manifest through an injected fetcher.
- **Build:** `tools/faq_watch.py` (`--check` exit 0/1/2, `--update`), `tools/faq_manifest.json` (ids, section, `updatedDate`, sha256 only; **no text**),
  `.github/workflows/faq-watch.yml` (`workflow_dispatch` only after the 2026-10-11 first run, the weekly cron was dropped, see P10; issue labelled `faq-change` on change; fail without an issue on exit 2).
  Feeds: `/api/api/faqdetails/getFaqTableData` (251 entries) and `/api/api/homepage/content?locale=en` `newsAndNotifications` (14).
- **Files and SSOT:** one script under 300 lines, helpers split if needed; one manifest. **Strings and colours:** none (tooling).
- **Security:** least-privilege workflow, pinned actions, no secrets; no push or workflow run without the owner's OK. First run is manual:
  government sites may block GitHub IPs; if blocked the workflow stays manual-only and the script runs locally.
- **R8:** not applicable. **Anticipated debt:** workflow unverified on a real runner (resolve: owner-approved manual run, or record the local-only fallback).
- The uncommitted `21_*.md`, `22_*.md`, `rma_rates.json`, `td_rates_sheet.json` go into this commit (owner, round 5).
- **Built (2026-10-11):** `tools/faq_watch.py` (197 lines), `tools/test_faq_watch.py` (30 tests, 0 skipped), `tools/faq_manifest.json`
  (251 FAQ entries, 14 notifications, seeded from the live feeds the same day), `.github/workflows/faq-watch.yml` (action pinned to a commit SHA).
  Deviations from the text above, both deliberate: the manifest stores the portal's **section id**, not its name (names run to 120 characters,
  which broke the "nothing longer than an id, date or hash" rule); the integration check is the seeded round trip (`--update` then `--check` exit 0
  on the real feeds, run by hand) plus the temp-manifest cases in the test file, because a committed test cannot hold the portal's text.
  Not done: see P10.

### P1. Rate research and approvals (docs only, no code)
- **Goal:** every figure the calculator needs has a primary authority, an effective date and an owner approval. **Depends:** P0 committed.
- **Work (research, then owner approval, figure by figure):** F6 effective date of the +25% DA step for TD lump sums (MoD letter);
  TD hotel ceiling and taxi cap figures (base 4500/2250/7500 and 338, via `da_step`); baggage by road (Rs 50 base, F2); RMA table
  (`rma_rates.json`, approved 2026-10-11); meaning of TR Rule 4(i) and the portal's use of 119; the long-course tick note.
- **Tests first:** none to write; the check is that each figure row has `authority`, `evidence`, `effective_from`, and the evidence label is honest
  (`LETTER_TEXT` only if the letter text was read; the social-media sheet stays `WEB_SECONDARY`). **Integration:** `refresh.py --check` stays green.
- **Security:** no PDF or letter text copied into the repo beyond short quoted citations; no PII.
- **R8:** not applicable. **Anticipated debt:** a figure that cannot be sourced; resolve by cutting that line from v1 and recording it in `19_carry_over.md`
  as an owner-visible scope change, never by shipping an unsourced number.

### P2. New figures in the bundle (tools, bundle, models)
- **Goal:** approved rates reach the app through the existing figures pipeline. **Depends:** P1 approvals.
- **Tests first:** `test_figures.py`/`test_bundle.py`: a per-km-by-state mode validates (34 states, all positive, missing state rejected), unknown mode
  rejected by the validator but ignored by the app, unapproved figure still blocks bundling. Kotlin `GuideFiguresContract`: the real bundle loads and an
  unknown mode shows no line (existing behaviour, now with the new modes). Integration: regenerated `guide_bundle.json` passes `refresh.py --check`.
- **Build:** `tools/figures.py` new mode(s); figures `rma`, `baggage_road`, `td_hotel`, `td_taxi` in `figures.json` (RMA table moved there, `rma_rates.json`
  then deleted so the table has one home); additive field on `GuideFigure`; bundle regenerated. Schema major stays 1.
- **Files and SSOT:** one figures file; `PersonalFigureResolver` reused for DA steps, not copied. **Strings:** none new. **Security:** bundle validator rejects unknown internal fields.
- **R8:** `minifyReleaseWithR8` + `check_r8_guide.py` (bundle byte-identical, models kept). **Debt:** `GuideModels.kt` size (split if over 300).

### P3. Worksheet content (bundle `preclaim` section)
- **Goal:** field order, documents and the LTC rule picker are data, not code. **Depends:** P2.
- **Tests first:** validator rejects unknown move type, missing document list, field without label, LTC picker option without a card link, label over the length limit.
  Integration: every picker option's card id exists in the rulebook; field order equals the portal order in `21_*.md` per move type (PD, TD, LTC).
- **Build:** authoring source + compiler in `tools/`, additive bundle section `preclaim` (moves, fields marked `portal_filled` / `officer_types` / `app_suggests`, documents with the 50 KB / 2 MB rule, TD and LTC dropdown options, TD certificate tick); Kotlin models.
  Any field not seen on the portal is marked "unverified against the portal".
- **Strings:** worksheet text is bundle content, reviewed under the copy-guard (own words). **Security:** length limits, no HTML in labels.
- **R8:** yes (new `@Serializable`). **Debt:** a model file over 300 lines; split by move type.

### P4. Calculator: permanent duty (`shared/commonMain`, pure)
- **Goal:** CTG, road baggage and car conveyance estimates. **Depends:** P2.
- **Tests first:** unit golden cases: basic 149000 gives CTG 119200 (the owner's voucher); island 100%; local move one-third of 80%; MSP and NPA never in the basic;
  baggage = Rs 50 x `da_step` x km (Rs 62.5 at DA 50 to 99%); RMA-rate conveyance = km x state rate; km zero/negative/over-bound rejected; no total is ever produced (F1).
  Integration: calculator + real bundle figures + `GuideProfile`.
- **Build:** `GuideProfile` gains MSP and NPA, derived from the payslip, nothing stored, no migration; `PreClaimCalculator` (PD); each result carries figure id and `approved` date.
- **Files and SSOT:** calculator split per element (CTG, baggage, conveyance), each under 300 lines. **Security:** bounds on every input; no logging of inputs.
- **R8:** not triggered (no new serialised type); confirmed in the summary. **Debt:** duplicated DA-step maths (must call `PersonalFigureResolver`).

### P5. Calculator: temporary duty
- **Goal:** food, lodging cap, local taxi, own-car RMA. **Depends:** P4.
- **Tests first:** food per day = band base x step x timing share (under 6 h 30%, 6 to 12 h 70%, over 12 h 100%), days = inclusive dates (57 and 61 as on the owner's claims);
  lodging = the officer's nightly rate **capped** at the ceiling, never above it; taxi cap Rs 422.50-style only for Level 9 to 11, km note only for 12 to 13B and 14+ (no amount);
  RMA nil without the Rule 40 Note 2 sanction, km x state rate with it; TR Rule 124 sets food, lodging and taxi to nil; dates over 180 days raise the cap note; DA% from the payslip.
  Integration: the two sample claims reproduced as golden cases (inputs scrubbed to numbers only).
- **Build:** `PreClaimCalculator` TD elements split by element. **Security:** bounds (days, nights, hours, rate); nothing logged. **R8:** not triggered; confirmed.
- **Debt:** rules found stale in P1 leave a stub; resolve by finishing P1 first, never by guessing.

### P6. ViewModel, state, flag, DI (no UI)
- **Goal:** the whole flow works headless. **Depends:** P3, P4, P5.
- **Tests first:** unit on `PreClaimViewModel` (questions per move type: PD four, TD marital and SPR only, LTC all; defaults from payslip; manual entry with no payslip; validation; state lost on process death by design). Integration: ViewModel + real calculator + fakes in `shared-test-fixtures`; Premium gate via `GuideAccess`; kill-switch test that `PRECLAIM_ENABLED` false hides the entry.
- **Build:** `LaunchFlags.PRECLAIM_ENABLED = false`; Koin module; one `UI_STOP_TIMEOUT_MS` reused. Telemetry: one `CrashReporter.log` breadcrumb with the move type only.
- **Security:** a test that no input value reaches `CrashReporter` or logs. **R8:** yes (Koin). **Debt:** a ViewModel over 300 lines; split by move type.

### P7. UI in the Guide tab
- **Goal:** the screens. **Depends:** P6.
- **Tests first:** Compose tests: move-type choice, per-move questions, field list with per-field copy only, document checklist, estimate rows with "Estimate, rule, as of", locked panel for free users, back, tab re-tap, lock screen discards the form.
  Scan tests: no string or colour literals. iOS: framework link; no `UIViewController` outside the themed factory.
- **Build:** screens under `ui/screens/guide/preclaim/`, `PreClaimStrings.kt`, theme tokens only; entry from Guide home and "Prepare this claim" on related cards; the per-field copy button.
- **Security:** no copy-all, no share sheet, no export; no value in the semantics tree beyond what is shown. **R8:** yes. **Debt:** composable over the length limit; split into slots.

### P8. Verification
- **Goal:** prove it on devices and in release builds. **Depends:** P7.
- Full gate: `./gradlew check`, iOS tests, `assembleRelease -PallowPlaceholderGemmaModel=true` + `check_r8_guide.py`, `scripts/run_guide_minified_smoke.sh`, `security-review` skill on the whole branch diff.
- Pixel walk (each move type, island tick, spouse in Army, sanction held or not, Rule 124, locked user, rotation, back, tab re-tap); the iPhone walk is the owner's, folded into the iOS checklist in `16_guide_phase_plan.md`. Flag stays false; the owner flips it in a later release.
- **Debt:** any walk finding is fixed here with a test, not logged.

### P9. Release prep (out of scope, the owner's separate step)
Version bump, store notes, `changes.txt`, bundle regeneration, flipping `PRECLAIM_ENABLED`. Nothing is uploaded without the owner's word.

### P10. Pending points carried out of earlier phases (single final phase, owner action needed)
Filled by the Phase Summary of each phase; every row is something that could not be finished inside its own phase, with why and what closes it.
Nothing here is allowed to grow silently: a row is removed only by being done.

| From | Pending point | Why it is not done | What closes it |
|---|---|---|---|
| P0 | **Result of the first run (2026-10-11, owner-approved):** on a GitHub-hosted runner the watcher tests passed, then `--check` got `URLError` from the portal and exited 2: the job failed and opened no issue, as designed. The portal does not accept GitHub's IPs, so the workflow is now **manual-only** (schedule removed, header documents the local `--check` fallback). | Resolved per the plan's own fallback. **Still unverified:** the issue-opening step (exit 1) has never run on a runner; its shell logic was only exercised locally. | Done, except: the issue step stays untested until the check runs somewhere the portal answers (self-hosted runner), at which point re-add the schedule. Until then the owner runs `faq_watch.py --check` locally. |
| P0 | **Notification churn: resolved 2026-10-11 (owner).** The monthly "Statement of Account for the month of ... has been uploaded" notice is skipped (`IGNORED_NOTIFICATIONS` in `faq_watch.py`, 5 tests; manifest re-seeded, 13 notifications). Other routine items (Form 16, DSOP Fund Statement uploaded) are still reported; add a pattern, with a test, if they become noise. | Done. | Nothing; row kept for the record until P10 is closed. |

