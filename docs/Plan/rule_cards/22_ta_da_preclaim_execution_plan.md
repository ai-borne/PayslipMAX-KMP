# TA/DA pre-claim: strict phase-wise execution plan

Status: **P0, P1, P2 and P3 done (P3 committed on the branch, 2026-10-11); P4 onward not started. The pending points of P0, P1 and P3 are consolidated in P10; P2 left none.** Facts, portal screens, open items and every owner decision live in
`21_ta_da_preclaim_plan.md` (read it first; this file does not repeat them). This file is **how we build**: the rules, the
handoff protocol and ten phases (P0 to P9, plus the pending-points phase P10). Branch: **one feature branch `feature/ta-da-preclaim`** for the whole plan, one commit
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
- **Built (2026-10-11):** `23_ta_da_preclaim_rate_register.md` (one row per figure: base, step value, evidence, authority, effective date, cross-check, approval);
  `td_rates_sheet.json` note updated. The new primary-text source is a **civil** order (UT Ladakh Finance Dept Order 231-F of 2024, read page by page): it matches
  every TD hotel, taxi, food and baggage figure and puts the 25% step at 2024-01-01. No MoD (Army) letter was found, so the evidence labels stay `HANDBOOK_ONLY`
  and nothing was approved by the agent. Outcome per item: F6 date, hotel, taxi, baggage and the rule readings are **researched, approval pending**; RMA was already
  approved; the long-course tick is **not resolvable from any document**. No figure was cut from v1 and no unsourced number was added. The approvals and the two
  open items are in P10; P2 may start on a row only once its approval cell holds a date.

### P2. New figures in the bundle (tools, bundle, models)
- **Goal:** approved rates reach the app through the existing figures pipeline. **Depends:** P1 approvals.
- **Tests first:** `test_figures.py`/`test_bundle.py`: a per-km-by-state mode validates (34 states, all positive, missing state rejected), unknown mode
  rejected by the validator but ignored by the app, unapproved figure still blocks bundling. Kotlin `GuideFiguresContract`: the real bundle loads and an
  unknown mode shows no line (existing behaviour, now with the new modes). Integration: regenerated `guide_bundle.json` passes `refresh.py --check`.
- **Build:** `tools/figures.py` new mode(s); figures `rma`, `baggage_road`, `td_hotel`, `td_taxi` in `figures.json` (RMA table moved there, `rma_rates.json`
  then deleted so the table has one home); additive field on `GuideFigure`; bundle regenerated. Schema major stays 1.
- **Files and SSOT:** one figures file; `PersonalFigureResolver` reused for DA steps, not copied. **Strings:** none new. **Security:** bundle validator rejects unknown internal fields.
- **R8:** `minifyReleaseWithR8` + `check_r8_guide.py` (bundle byte-identical, models kept). **Debt:** `GuideModels.kt` size (split if over 300).
- **Built (2026-10-11):**
  - `tools/figures.py` (217 lines): mode `rate_by_state` (`RMA_STATES` is the validator's list of the circular's 34 names; each rate a whole number 1 to 100); a card-less figure
    must say `use: "preclaim"` and a preclaim figure may not name a card; a `da_step` band takes a `base` **or** a plain-text `limit` (1 to 120 characters, no angle brackets), which is how
    the taxi bands for levels 12 and above carry "no rupee cap" without an amount; `da_step.first_step` records the step date (2024-01-01, `OWNER_CONFIRMED` 2026-10-11) and ships as `first_step_effective_from`.
  - `figures.json`: `td_hotel`, `td_taxi`, `baggage_road` (all `da_step`, `HANDBOOK_ONLY`, bases 7500/4500/2250, 338, 50 per km for levels 10 and up) and `rma` (`rate_by_state`, `LETTER_TEXT`), each `approved` 2026-10-11.
    `rma_rates.json` is deleted; its metadata moved into the `rma` figure.
  - Kotlin: `GuideFigure` gains `use` and `rates` (`card` defaults to empty), `GuideFigureBand` gains `limit` (`base` defaults to 0), `GuideDaStep` gains `firstStepEffectiveFrom`; `GuideFigureValidator` rechecks card/use and band/rate shape on load
    (an unknown mode is not checked for shape); `PersonalFigureResolver` never matches an empty card id (found by a test: `resolve("")` would otherwise have picked up the first preclaim figure).
  - Knock-on: `rates_report.py` and `review_pack.py` crashed on a card-less figure; both now list it as "(pre-claim)". `SyntheticGuideFigures` regenerated from the bundle (the contract test fails if the two drift).
  - Tests: Python 209 (0 skipped; `test_figures_preclaim.py` split from `test_figures.py` to stay under 300 lines); shared JVM 1028, composeApp JVM 901, iOS shared 983, iOS composeApp 559, all 0 skipped, 0 failed.
    Gate: `check`, `ktlintCheck`, tech-debt audit, `refresh.py --check`, `assembleRelease` + `check_r8_guide.py` (12 models kept, bundle byte-identical), iOS framework link.
  - Not done in P2 and not pending: nothing user-visible (no calculator, no UI), by design. Handed to P5, not debt: see the rounding line in P5.

### P3. Worksheet content (bundle `preclaim` section)
- **Goal:** field order, documents and the LTC rule picker are data, not code. **Depends:** P2.
- **Tests first:** validator rejects unknown move type, missing document list, field without label, LTC picker option without a card link, label over the length limit.
  Integration: every picker option's card id exists in the rulebook; field order equals the portal order in `21_*.md` per move type (PD, TD, LTC).
- **Build:** authoring source + compiler in `tools/`, additive bundle section `preclaim` (moves, fields marked `portal_filled` / `officer_types` / `app_suggests`, documents with the 50 KB / 2 MB rule, TD and LTC dropdown options, TD certificate tick); Kotlin models.
  Any field not seen on the portal is marked "unverified against the portal".
- **Strings:** worksheet text is bundle content, reviewed under the copy-guard (own words). **Security:** length limits, no HTML in labels.
- **R8:** yes (new `@Serializable`). **Debt:** a model file over 300 lines; split by move type.
- **Built (2026-10-11):**
  - `preclaim.json` (147 lines, the authoring source, same pattern as `figures.json`) and `tools/preclaim.py` (189 lines: `validate`, `for_bundle`, `texts`). `compile.py` validates it against the compiled cards and `bundle.py` ships it as the additive `preclaim` section (`BUNDLE_VERSION` stays 1; `provenance` and any other key stay in the repo). The bundle build stops on an invalid file.
  - Content: three moves in the portal's create-form order (PD 15 fields, TD 24, LTC 28), each field marked `portal_filled` / `officer_types` / `app_suggests`; one-PDF upload rule (more than 50 KB, less than 2 MB); documents per move (the LTC leave certificate carries "needed when you travel"); option groups for Mode of Journey, TD TR Rule (4 options), LTC TR Rule (the 8-option rule picker, each linked to a live card) and Nature of Leave; the TD DTS certificate as the last field. Eight fields not confirmed on the portal are `unverified` with a hint saying what (see P10).
  - Length limits live once, in `preclaim.py` `LIMITS`, and ship in the section (`limits`); Kotlin reads them from there (found while reviewing: two hard-coded copies would drift).
  - Kotlin: eight `@Serializable` models appended to `GuideModels.kt` (263 lines; the R8 gate reads that folder), `GuideBundle.preclaim` (nullable), `GuidePreClaimValidator` (144 lines) wired into `GuideBundleValidator`. Unlike figures, an unknown move, kind or source is refused rather than ignored: the bundle ships inside the app, so such a value is a build mistake, and the contract test catches it before release.
  - Own-words guard: `copycheck.py` now also scans every user-read sentence in `preclaim.json` (CLEAN), and a test pins that `texts()` offers them all.
  - Knock-on: `scripts/check_r8_guide.py` read only `GuideModels.kt`, so a future split would have dropped models from the R8 gate; it now scans the whole model folder (2 tests). Runbook `18_*.md` names `preclaim.json`.
  - Tests: Python 242 (33 new in `test_preclaim.py`), `scripts/tests` R8 gate 12 (2 new); shared JVM 1043 (15 new `GuidePreClaimValidatorTest`), composeApp JVM 901 (the real-bundle `GuidePreClaimContract` runs inside the existing Android contract test), iOS shared 998, iOS composeApp 559 (contract also runs in `GuideLoaderIosPerfTest`), all 0 skipped, 0 failed. A field-order mutation of the real bundle turned the Android contract red, then was restored.
    Gate: tech-debt audit, `ktlintCheck`, `check`, `refresh.py --check` (copycheck CLEAN), iOS simulator tests, iOS framework link, `assembleRelease` + `check_r8_guide.py` (20 models kept, bundle byte-identical).
  - Not done in P3: nothing user-visible (no UI, no flag), by design; the three open points are in P10.

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
- **Rounding (found in P2):** three TD ceilings land on half a rupee at DA 50-99% (hotel 2812.50, taxi 422.50, baggage 62.50 per km). The food resolver rounds to whole rupees, which would show 2813 and 423 and overstate a ceiling. P5 must compute ceilings and the baggage rate exactly (paise or a `Double`-free ratio) and round only a total claimed amount, with a test that names 422.50 and 2812.50.
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
| P1 | **Figure rows and rule readings: approved by the owner 2026-10-11** (`td_hotel`, `td_taxi`, food timing, `baggage_road`, TR 4(i)/4(ii)/119 lines). | Done. | Nothing; row kept for the record. **P2 took these rows into `figures.json` with the approval date (2026-10-11).** |
| P1 | **F6 step date: decided by the owner 2026-10-11 as 01-01-2024**, recorded `OWNER_CONFIRMED` (no Army letter exists in hand). | Done as a decision, not a document. | Nothing now; shipped in P2 as `da_step.first_step` (`OWNER_CONFIRMED`). The optional upgrade row below covers the letter. |
| P1 | **"TD For Long Duration Course" tick: closed 2026-10-11 (owner) as note only, marked unverified against the portal.** Meaning still undocumented. | Only the portal knows. | Optional: the portal's hint text or a screenshot would let P3 word it with certainty. |
| P1 | **Evidence upgrade (optional):** hotel, taxi and baggage stay `HANDBOOK_ONLY` because the Army letter text was not read. | Same as F6: the MoD letter is not public. | If the MoD letter turns up, read it, raise hotel, taxi and baggage to `LETTER_TEXT`, and replace the owner-confirmed step date with the letter's. |
| P0 | **Notification churn: resolved 2026-10-11 (owner).** The monthly "Statement of Account for the month of ... has been uploaded" notice is skipped (`IGNORED_NOTIFICATIONS` in `faq_watch.py`, 5 tests; manifest re-seeded, 13 notifications). Other routine items (Form 16, DSOP Fund Statement uploaded) are still reported; add a pattern, with a test, if they become noise. | Done. | Nothing; row kept for the record until P10 is closed. |
| P3 | **Rule-picker links and worksheet wording are the agent's reading, not owner-reviewed.** Eight options map to cards: 177(A) Home Town to `RB-SS-T088`, 177(B) to `T074`, 177(C) to `T070`, 177(D) to `T128`, children studying away to `T073`, J&K/A&N/NE in lieu to `T062`, Sick Leave to `T079`, and **Sea Passage to `T058`** (owner chose this on 2026-10-11 over `T250`; still the closest card, not the 177(A)(i)(e) clause itself, which was not found in the OCR). The `use_when` lines, notes and hints are own-words (copy-guard CLEAN) but unread by the owner. | The agent cannot approve its own content, and the TR 2014 clause text for 177(A)(i)(e) was not found in the OCR. | Owner reads `preclaim.json` once (the Sea Passage link was decided 2026-10-11); re-point any card id or reword a line (one-file edit, `refresh.py`). Before the flag is flipped at the latest. |
| P3 | **Eight fields are `unverified` against the portal:** TD "TD For Long Duration Course" (meaning), LTC Children (largest count), LTC Dependents (exact list), LTC Visit To (typed or dropdown), LTC Year (greyed; source), and Marital Status on all three moves (dropdown values). | Only the portal shows them; the owner's screenshots did not capture these details. | A screenshot or the portal hint text per field; then drop `"unverified"` and the hint in `preclaim.json`. P6 must treat an unverified field as optional input, and P7 shows its hint. |
| P3 | **`GuideModels.kt` is 263 lines.** The next model added there should go in a new file in the same folder (the R8 gate now reads the folder). | Under the 300 limit, so no action is due; recorded so P6/P7 do not push it over. | Add new models in a sibling file. |
| P4/P5 | **Owner decisions of 2026-10-11 to build in (not pending, recorded so they are not re-asked):** (1) profile = payslip default, every value editable, fully manual when there is no payslip (amends round 5 of `21_*.md`: no payslip import); (2) CTG case = the officer picks normal / local move / island, island pre-ticked from the Andaman Nicobar tick, no km threshold in code; (3) TD food = full daily rate on every day of a multi-day move, the 30/70/100% timing share applies to a same-day trip only. | Decided. | P4 and P5 implement exactly this; delete the row when both are built. |
