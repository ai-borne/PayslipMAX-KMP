# Claim Guide app work: where we are (read this block first; the rest of this file is dataset history)

Updated 2026-10-10 (after M7). App phases E0-E9 are done. E8 is merged and pushed (main at 442c57ff). **E9 (dataset gaps, device checks, end-to-end and security tests, release flip) is complete, merged into main and pushed 2026-10-09 (main at ea364291, pre-push gate passed in 249 s); the branch is deleted.** `LaunchFlags.GUIDE_ENABLED` is true (commit 77faaee9); `GUIDE_PAYWALL_ENABLED` stays false, so the Guide ships free. Nothing was uploaded to a store. Plan and status: `16_guide_phase_plan.md` (sections "E9 result", "E9 known-gaps register", "Post-E9 list", EP table).

**Maintenance track (plan `~/.claude/plans/make-a-phase-wise-shiny-mochi.md`, M1-M9):** M1 (one-command `tools/refresh.py`, stable ids), M2 (expert review pack `tools/review_pack.py [--pdf]`, intake `17_expert_review_intake.md`), M3 (rule-change metadata) and **M4 are merged into main and pushed 2026-10-09 (main at d1a36d58 after the share-note decision; pre-push gates passed, branches deleted)**. M4 (2026-10-09) added the "What changed" UI: `GuideChangeLog` and `GuideRuleHistory` (bounded walks), replaced cards out of search and feeds (a pin still opens them), the Updated and "Replaced on" chips (decided in `GuideIndex.trust`), a "What's new in the Guide (<month>)" row on Home (only when the bundle has a change entry) opening a list with card links, and "Earlier rule" / "See current rule" links; `GuideDestination.Changes` saves as `changes`. Everything is free for all users (owner decision). The real bundle has no change or replaced card, so Home and cards look as before; the fixture `SyntheticGuideRuleChange` carries one pair and two entries. The bundle did not change. **M5 ("Suggest a correction") is merged into main and pushed 2026-10-09 (main at 83e21934, pre-push gate passed in 316 s; branch deleted):** a "Suggest a correction" button on unlocked cards opens a dialog; "Open email" hands the user's mail app a message to `AppStringsSupport.supportEmail` with subject `[Guide] <card id>` and a body of only card id/title, bundle date, card rev, app version and the user's text (`GuideSuggestion`; the app sends nothing, telemetry hears nothing). `ReportIssueDialog` now shares its layout with it (`SupportMessageDialog`). The Pixel smoke and Gmail walkthrough passed (nothing sent, no drafts left); the walk also found and fixed a dialog that outgrew the screen with a full-length text. iOS walkthrough: EP 29. **M6 (personal notes, data layer) is merged into main and pushed 2026-10-10:** encrypted `guide_notes` rows (Room 13 to 14), `GuideNote` rules in the domain, `GuideNotesRepository` (Room and fake), notes in the `.pcda` backup (v4) and in the restore transaction; no UI, no strings. Owner decisions: bad backup note skipped, MERGE newer `updatedAt` wins, chain followed to the end, notes in the archive for everyone, a REPLACE restore of an older backup erases notes. Pixel: the new `minifiedTest` installed over the real data, the app and Guide worked; the on-device note round-trip proof was NOT run (a debug receiver on the real phone was refused); M7's UI will give it. **M7 (personal notes UI) is done on `feature/guide-m7-notes-ui` (2 commits + docs; NOT merged, NOT pushed; waiting for the owner, 2026-10-10):** a "Your note" section on unlocked cards (add, edit, delete after a confirmation, a counter to 2000 characters, "not official", "updated since your note", notes carried from an earlier rule marked stale and moved when edited; a carried note is editable only when the card has no note of its own), `GuideNotesViewModel` (reads the store only while a screen collects it, so a locked card never reads a note), a "Note" / "In your note" marker on feed, search and pinned rows, note words in Guide search (memory only, unlocked users only), a quiet "N notes could not be read" line and a "Notes on removed cards" row and list on Guide Home. No telemetry and no share, suggestion or saved-state path for note text (`GuideSecurityContractTest`). The first commit fixed the flaky `AppKoinModulesTest` race (not reproducible before the fix this time; 3 of 3 green after). Gate green (tools, `check`, 1,529 iOS tests, iOS link, R8 checks); Pixel 9: smoke, dark and light walkthrough, note survives a force-stop, backup then Replace-all restore brings it back, test data and throwaway backup removed. iOS walkthrough: EP 30. **Next: M8 (runbook, end-to-end, release readiness), after the owner merges M7.** Details: the phase plan's "M7 result" (this folder, `16_guide_phase_plan.md`) and the plan file's M9 "From M7". Carry-over list: the plan file's M9 section ("From M4"); iOS walkthrough items: EP 28 (M4), EP 29 (M5), EP 30 (M7). Owner action still open: send the regenerated review PDF to the expert (`review_pack.py --pdf`; git-ignored).

**Dataset now:** 404 cards (220 travel, 184 pay), 37 unverified, 31 no-source; new cards `RB-RP-053-arrears` and `RB-SS-P114-quarters`; Pay Audit arrears and HRA-missing links open them. Release APK 69,606,036 B (+65,860 vs 69,540,176); cold start empty state ~187 ms.

**Still open:** iOS walkthrough (EP 11, 15, 20, 23, 26, owner); EP 14 (paywall flip checks, post-launch); RP-073 TA Allowance has no card; 37 unverified cards (RP-088 HBA 8.5% blocks the paywall).

Rules that still bind: no rupee figure in Kotlin; copy only in `GuideStrings.kt`; 300-line files; locked state must not hold a card's paid half; no PII, card id, pin list or figure in telemetry (`GuideSecurityContractTest` enforces it). Gradle from the shell: `export JAVA_HOME=/opt/homebrew/opt/openjdk@21` (on "jmod ... antigravity" loop `./gradlew --stop; sleep 12`); never two Gradle commands at once (the commit hook runs Gradle too). The phone runs the debug-signed `minifiedTest` build with the owner's payslips; never uninstall.

Paste to start the next session: "Continue the PayslipMAX KMP Claim Guide after E9. Read CLAUDE.md, the top block of docs/Plan/rule_cards/HANDOFF.md and the Post-E9 list at the end of docs/Plan/rule_cards/16_guide_phase_plan.md. First: plan the release build (do not upload or run fastlane until I say). Ask me which Post-E9 item to take next."

---

# Rule-card dataset: handoff (read this first)

Last updated 2026-10-04 (end of pay authoring; Phase D automated scan done, domain-owner review pending). Committed on branch `docs/rule-cards-dataset`. See `README.md` for the layout.

## What we are building
A paid in-app **Claim Guide**: a phone-friendly rulebook for PCDA(O) Army officers. One card = one situation: a one-line answer, a few short bullets, the authority to cite, optional collapsed details. Before any app code, we are building the whole dataset in one file, `rulebook.json`, so it can be scanned for errors and then bundled into the app.

## Decisions already made by the user (do not re-litigate)
1. **Own words only.** Never copy source passages. The user-facing CITE names only the *primary* authority (TR 2014 rule, MoD/MoF/DoPT letter, Army order), never "the handbook" or "the PCDA(O) FAQ". Internal provenance (`from` ids) stays in the data, hidden from users. A copy guard enforces this (see Tools).
2. **No gating on finding every primary letter.** The goal is simplification for a phone user. Support is a *label*, not a filter. Cards that rest on letters we could not find stay in, flagged by an `O:` open point.
3. **Card template and limits** (tone, wording and the DA "as of" line were approved): Answer, Key points, Attach (optional), Watch out (optional), Cite, collapsed Details. Limits enforced by the compiler: title <= 14 words, answer <= 25, up to 3 bullets per section, each bullet <= 12 words, visible text (answer + bullets + cite) <= 90, details <= 120.
4. **Conflicts:** the **TR 2014 text wins** over the FAQ and the handbook when they disagree; the **newer FAQ wins** over the older handbook; record every conflict as an `O:` line on the card. Figures that rise with DA must be stored as base rate plus the 25%-per-50%-DA escalator; never hard-code a rupee figure without its base and effective date.
5. Scope of "100% of travel and pay": travel = TD, permanent move, LTC, claims, retirement/death, transport allowance. Pay = Pay & Allowances Handbook chapters 4-9, 12-15, 17-23 and 28. Tax, insurance, pension and ECHS (ch. 24-27, 29-35) are **deferred**; the app already has a Tax Planner.

## Current numbers (from `python3 docs/Plan/rule_cards/tools/compile.py`)
- **404 cards** (2026-10-09; was 402): **220 travel** and **184 pay** (nine travel cards dropped by the owner in phase G1).
- Source entries covered: all 474 (1 skipped with a reason, 0 uncovered). Pay topics with a card: **94 of 95** (RP-073 Territorial Army Allowance lost its card to an owner DROP).
- Cards carrying an `O:` open point: 36 (2 flagged CONFLICT); guidance-only (no cite): 31. GUIDANCE now means exactly "no cite"; `compile.py` enforces it (owner, 2026-10-07).
- Every authoring batch passes the compiler (limits, ids, topics) and the 8-gram copy guard (`copycheck.py` prints CLEAN).
- `13_review_queue.md` lists every open point for the domain owner. Regenerate it after edits.

## Files
| Path | What |
|---|---|
| `docs/Plan/rule_cards/authoring/*.txt` | **The source of truth for card text** (compact line format). Edit these, never the JSON. |
| `docs/Plan/rule_cards/rulebook.json` | Compiled dataset (the app will load this). Generated. |
| `docs/Plan/rule_cards/RULEBOOK.md` | Human reference, generated from the JSON. Do not hand-edit. |
| `docs/Plan/rule_cards/ssot.json` | The 474 source entries (FAQ and handbook items) and topic lists; the coverage target. Frozen ids (SS-Txxx travel, SS-Pxxx pay, RP-nnn pay topics). |
| `tools/` | All scripts: `compile.py`, `render_md.py`, `copycheck.py`, `packet.py`, `pa.py`, `todo_pay.py` (see `README.md`). |
| `docs/Plan/rule_cards/00_inventory_summary.md` and 01-11 | Earlier inventory, registers, verification of 21 order letters (08), card prototype (11). Background only. |
| `~/Downloads/rulecards_workdir/` | **Working copy of the tools and extracted source text** (outside the repo on purpose: derived from copyrighted PDFs). See Tools. |

## Authoring format (one block per card)
```
=== topic=RR-LTC-01 from=SS-T068,SS-T060 [id=RB-x] [chips=RATES,AMENDED,GUIDANCE] [personal=level:food_rate]
T: question-style title
A: one-line answer
K: key point            (1-3, required)
H: attach item          (optional)
W: watch-out            (optional)
C: authority to cite    (required unless chips=GUIDANCE)
D: collapsed details    (optional)
O: open point for the reviewer (never shown to users)
--- skip SS-T123 reason
```
A dated rule adds `effective=YYYY-MM-DD` and, on the new card, `replaces=RB-old` to the `===` line (see README, "Rule changes"); the dated change log is `authoring/changes.txt`.
Card id defaults to `RB-<first from id>`; add `id=` when two cards start from the same source. Travel topics are `RR-*`, pay topics `RP-nnn`. A card may cite a pay-topic id (`from=RP-nnn`) when no FAQ entry exists.

## Tools (all in `tools/`; source text stays in `~/Downloads/rulecards_workdir/`, override with `RULECARDS_SOURCES`)
- `python3 docs/Plan/rule_cards/tools/packet.py SS-P001,SS-P002` or `.../tools/todo_pay.py "Technical Allowance|Flying Allowance"` prints the FAQ/handbook text behind entries that are still uncovered. `todo_pay.py` with no args lists uncovered counts per pay topic.
- `.../tools/pa.py "<handbook heading>" <words> [occurrence] [skip]` prints a section of the Pay & Allowances Handbook (occurrence 1 is usually the body; 0 is the index). Handbook, TA handbook and the OCR'd TR text are plain .txt files there; `tr_rules_full.json` holds TR 2014 rule text by rule number.
- `.../tools/copycheck.py` flags any run of 8+ identical words between card text and the sources. Must print CLEAN.
- Then from the repo root: `python3 docs/Plan/rule_cards/tools/compile.py --check` then without `--check`, then `render_md.py`.
- Workflow per batch: read sources (handbook section + FAQs), write cards in `authoring/NN_*.txt`, compile, fix limit errors (bullets are the usual offender: keep them under 12 words), run copycheck, repeat. Use `python3 docs/Plan/rule_cards/tools/compile.py --check --uncovered` for what is left.

## DONE
- Source inventory, TR 2014 OCR (macOS Vision), dedup into 474 entries / 162 topics, 21 order letters checked against primary text (`08_order_letter_verification.md`).
- **All travel: 229 cards** covering all 347 travel entries (LTC incl. all of Rule 177, permanent move, TD, general rules and claims, advances and penal interest, retirement and death, transport allowance, warrants, order-based topics).
- Pay so far (46 cards): Risk & Hardship (all 21 FAQs), House Rent Allowance and SPR (all 18 FAQs, plus handbook rules), leave encashment with LTC (7), CEA and hostel subsidy (9), medical OPD (9), transport allowance (8).
- Prototype cards reviewed and approved by the user; compiler, validator and reference generator built.

## DONE this session (authoring batches 33-48)
Pay FAQs (55 left earlier) all written (33-35). Pay handbook-only topics written for chapters 4-9 (41-44), 13 (40), 14 (45), 15 (46), 17-20 (47) and 21 DSOP (48). Chapters 10-11 (gallantry, AFL cell) were out of scope. TLA district lists, bed-linen rate, HAFA/other rate tables beyond those on cards are not reproduced.

## Phase D scan results (automated, 2026-10-04)
- Similarity scan: no duplicate cards left (one duplicate TA Allowance card merged into RB-ORD-14 via `from=...,RP-073`).
- Letter-date consistency: 2 inconsistencies flagged on cards (4(10)/2017/D(Med) 28 vs 29 Sep 2017; TLA letter 1(16)/2017 dt 16-11-2017 vs 18-09-2017).
- TR cross-check (numbers only, not a full legal read): 164 TR-cited cards scanned against `tr_rules_full.json`. Fixed: family on course is "more than 90 days" (Rule 124(iv)), not "90 or more". Conflict flagged: LTC advance 95 days (TR 2014 Rule 17) vs 125 days (MoD letter 19-07-2016). Other mismatches were figures from 7th-CPC letters, not in TR 2014.
- Figures: transport allowance rate card gained its effective date; DA ladder stops at 42% (Jan 2023) per handbook and must be updated; whether a 25% step was notified when DA crossed 50% (01-01-2024) is unverified everywhere.
- Not done (needs a human or primary sources): domain-owner read; the 5 never-found letters; re-verification of all DA-linked rates against current MoF/MoD orders.

## Phase G1 (2026-10-04): review points verified
Owner decisions applied (see `15_confirmed_rulesets.md`, evidence level per row). DA ladder now to 58% (60% flagged). Escalator clauses checked per allowance (none printed for Technical or Transport Allowance). Pending list archived to `archive/14_review_decisions_G2.md` (G2 close); the O: lines stay on 36 cards. G2 done: 177C follows the FAQ (owner), Rajdhani full DA restored. Owner declared the dataset GOLD on 2026-10-04. Next: Phase E (the app feature), only after the owner approves the phase plan.

## Navigation (tile UI), added after Gold
`nav.json` places every card in one of 9 areas > 44 cases (limits in the file: 8 to 10 areas, up to 8 cases per area, 4 to 16 cards per case). `compile.py` fails on an orphan card, a double home, or a size breach, and writes `nav` plus each card's `nav` (case id) into `rulebook.json`. Pending: per-card facet tags (who qualifies / how much / how to claim / limits), case names tested with officers.

## TO DO (in order)
1. **Domain-owner review** of `13_review_queue.md` and the rate cards (18 `RATES` chips); resolve or keep each flag; update DA and the 50%-DA escalation. Then rerun compile, `render_md.py`, copycheck.
2. **Phase E, the feature.** Phase plan in `16_guide_phase_plan.md`. E0 (plan), **E1 (bundle, models, loader, R8 gate)** and **E2 (Guide tab, Home and Area tiles, debug only) done 2026-10-07**; next is E3. After any dataset edit, also run `tools/bundle.py`: it writes the app bundle, and CI fails on a stale one. Original scope: bundle `rulebook.json` (strip `from` and `open`), a generic loader, the 3-level UI (home tiles + search, topic list, answer card) as a fifth tab "Guide" in `AppBottomBar`, copy and colours in `AppStrings.kt` and `Theme.kt`, personalisation from the Pay Audit profile (`personal=` fields), "as of" chips, copy-cite and share-as-claim-note buttons, search (words and rule numbers), pinned cards, tests, and the project rules in CLAUDE.md.
3. Commit the dataset and tooling (ask the user first; the pre-commit hook runs gitleaks, ktlint and tests).

## Pitfalls learned
- Bullets over 12 words are the most common compile error; also the 8-gram copy guard catches phrases lifted from the FAQ, so reword key phrases (not just swap nouns).
- FAQ answers sometimes contradict themselves or state a rule loosely; read the TR/handbook before trusting them (examples fixed: 177C is an alternative to 177A/B, not an extra LTC; the "30 days if advance drawn" rule is the LTC adjustment limit; CEA classes before Class 1 is now three).
- Several handbook paragraphs are cut off in the extraction; cards note this in an `O:` line. Read the full text before extending.
- TR 2014 rates are pre-7th-CPC; take figures from the MoD letter of 15-09-2017 (base rates) and flag the escalator.
- Keep the TA/PA handbook text out of the repo (copyright); only authored cards and the register are committed.

## Prompt to paste into the next Claude Code session
See the "Handoff prompt" block at the end of `HANDOFF_PROMPT.md` in this folder.
