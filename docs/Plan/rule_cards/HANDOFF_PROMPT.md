# Prompt for the next Claude Code session (one phase per session)

Paste everything in the block below into a fresh session. Update the numbers and the phase name here after each phase.

```
You are continuing the PayslipMAX KMP "Claim Guide" rule-card dataset. This session is exactly ONE phase: PHASE G1, verify the pending review points. Do not start any other phase, and do not touch Kotlin or app code.

Read first, in order: CLAUDE.md (strict phases, no hard-coded UI strings, phase summary at the end), then docs/Plan/rule_cards/README.md, then docs/Plan/rule_cards/HANDOFF.md. Work on branch docs/rule-cards-dataset (already pushed; PR not yet opened). Do not re-read other files unless a task needs them.

Context: 411 cards (229 travel, 182 pay) are authored in docs/Plan/rule_cards/authoring/*.txt and compile to rulebook.json. 82 cards still carry an open point; 38 cards are guidance-only. Goal of the whole project ("Gold Standard"): our app should be the best, most complete, most current source of PCDA(O) rules. That means every rule traces to its primary text (TR 2014, MoD/MoF/DoPT letter, Army order), with an effective date and, for DA-linked rates, the base figure plus the 25%-per-50%-DA step.

PHASE G1 task:
1. Open docs/Plan/rule_cards/14_review_decisions.md (91 pending entries; DECISION lines are blank unless the owner filled some in). Apply any OK / FIX / KEEP / DROP decisions the owner has already written: edit authoring/*.txt, never rulebook.json, and move applied points to 15_confirmed_rulesets.md with their evidence level.
2. For the remaining entries, try to settle them from primary sources: first the full local source text (tools/pa.py, tools/packet.py; the TA handbook is in ~/Downloads/rulecards_workdir/, output is not truncated if you use the full text, not the 140-word packet), then the web (prefer mod.gov.in, finmin/doe.gov.in, dopt.gov.in, cgda.nic.in, pcdaopune.gov.in; PDFs often have a text layer, try pdftotext). Treat gconnect/staffnews-type sites as secondary: they may support a PROPOSED line but not a confirmation.
3. Confirm and apply only what you are sure of (primary document read, or full source text read). Everything else stays in 14_review_decisions.md with a PROPOSED line giving evidence and source.
4. Priorities: (a) TR 2014 vs FAQ conflicts (e.g. LTC advance 95 vs 125 days, Technical Allowance Tier II rule, CTG on a local move); (b) confirm DA after 01-07-2024 from MoD letters and every DA-linked rate card's effective date and 25% step (which allowances carry an escalation clause in their original order); (c) the Rs 9,000 dependency limit; (d) letter-number and date discrepancies; (e) NE/J&K LTC air concession and MoD adoption. Leave old Army orders and 1960s-70s letters that are not online as KEEP.

Rules that always apply: own words only; the user-facing CITE names only a primary authority, never the handbook or website; keep card limits (title <= 14 words, answer <= 25, <= 3 bullets per section, bullet <= 12 words, visible text <= 90, details <= 120); conflicts: TR 2014 beats FAQ, newer FAQ beats older handbook, record each as an O: line; store DA-linked figures as base + escalator + effective date. Do not spawn agents. Do not commit or push without asking. Ask rather than guess on legal interpretation.

After every batch of edits run, from the repo root:
  python3 docs/Plan/rule_cards/tools/compile.py --check
  python3 docs/Plan/rule_cards/tools/copycheck.py          (must print CLEAN)
At the end: compile (without --check), render_md.py, then regenerate 13_review_queue.md numbers in HANDOFF.md.

Finish with a short Phase Summary (tech debt incurred, how it was resolved or flagged, confirmation that compile passes and copycheck is CLEAN, counts of points confirmed vs still pending). Then STOP and tell me what phase comes next. Do not draft the app feature (Phase E) until I say the dataset is Gold.
```

## Phase order (one per session)
- G1: verify pending review points (above).
- G2: primary-source verification of every TR-cited card (read each cited rule in full, not numbers only) and of every `RATES` card.
- G3: scope decision and gap-filling (deferred chapters: tax, pension, ECHS, insurance; gallantry; TLA district lists; bed-linen rate).
- G4: owner sign-off; add a `gold` status to the compiler; define the update path (change log, last-verified date).
- Phase E (app feature): only after the owner declares the dataset Gold. First deliverable is a phase plan for approval, not code.
