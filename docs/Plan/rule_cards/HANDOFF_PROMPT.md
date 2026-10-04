# Handoff prompt (paste this at the start of the next Claude Code session)

## Handoff prompt

You are continuing a multi-session task in the PayslipMAX KMP repo (`/Users/sunil/Downloads/PayslipMAX KMP`). Read `CLAUDE.md` first (strict phases, 300-line files, no hard-coded UI strings, phase summaries), then read `docs/Plan/rule_cards/HANDOFF.md` fully before doing anything else. It is the single source of truth for the current state.

**The project.** We are building a paid in-app "Claim Guide": a phone-friendly rulebook of short cards (one situation per card: one-line answer, a few bullets, the authority to cite) for PCDA(O) Army officers, covering travel (TD, permanent move, LTC, claims, retirement) and pay and allowances. Before any app code, we are authoring the whole dataset as one file, `docs/Plan/rule_cards/rulebook.json`, then scanning it for errors, then building the feature.

**Where we are.** 275 cards are authored: all 229 travel cards (complete) and 46 pay cards (Risk & Hardship, HRA/SPR, leave encashment with LTC, CEA and hostel subsidy, medical OPD, transport allowance). 418 of 474 source entries are covered. 55 pay FAQ entries remain (promotion pay fixation, commencement of pay, technical, flying, specialist, parachute, para reserve, post-graduate, ration money, Siachen/HUACA, HAFA/CFAA/CMFAA, sports increments, accommodation and transit charges). 89 of 95 pay handbook topics still have no card, including the large 7th-CPC pay chapter 13 (handbook pp. 77-104). Nothing is committed to git yet.

**How to work.** Cards live in `docs/Plan/rule_cards/authoring/*.txt` (compact line format described in HANDOFF.md). Never edit `rulebook.json` or `RULEBOOK.md` by hand: run `python3 docs/Plan/rule_cards/tools/compile.py` then `python3 docs/Plan/rule_cards/tools/render_md.py` from the repo root. My working tools and the extracted source text are in `~/Downloads/rulecards_workdir/` (the helper scripts are now in `tools/`; see `README.md`). If that folder is missing, say so and ask me before rebuilding it: the extracted text comes from my PCDAO PDFs in `~/Downloads/PCDAO PDFs/`.

**Rules you must follow.**
1. Write every card in our own words; the CITE names only the primary authority (TR 2014 rule, MoD/MoF/DoPT letter, Army order), never the handbook or the website. Run `copycheck.py`; it must print CLEAN.
2. Limits (the compiler enforces them): title <= 14 words, answer <= 25, <= 3 bullets per section, each bullet <= 12 words, visible text <= 90.
3. When sources conflict, the TR 2014 text wins over the FAQ, and the newer FAQ wins over the older handbook; always record the conflict as an `O:` line. Store DA-linked figures as base rate plus the 25%-per-50%-DA escalator with an effective date; do not hard-code unexplained rupee figures.
4. Do not gate cards on finding primary letters; flag them instead. Do not spawn agents unless I ask. Do not commit without asking.
5. Follow the phased protocol: finish a phase fully (compile and copy checks clean), give a short Phase Summary, then move on.

**What to do now, in order.**
1. Finish the 55 pay FAQ entries (the handbook text for chapters 4-5 is summarised in HANDOFF.md; read the other topics' sections with `pa.py` and the FAQs with `todo_pay.py`).
2. Write cards for the 89 pay topics that have none (`from=RP-nnn`), starting with chapter 13, then DSOP, accommodation, leave, promotions, additions to pay, allowances, advances, medical. The list is at the end of HANDOFF.md.
3. Phase D, the error scan (the checklist is in HANDOFF.md), then ask me for a domain-owner review.
4. Only after that, plan Phase E (the app feature): propose the phases and wait for my approval before touching Kotlin.

Start by running `python3 docs/Plan/rule_cards/tools/compile.py --check` and confirming the numbers above, then continue with step 1.
