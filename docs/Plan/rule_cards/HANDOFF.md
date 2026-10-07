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
- **402 cards**: **220 travel** and **182 pay** (nine travel cards dropped by the owner in phase G1).
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
