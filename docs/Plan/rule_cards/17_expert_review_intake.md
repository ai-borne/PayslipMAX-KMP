# Expert review intake

Paste the expert's replies here, one row per card ID, exactly as they send them. This file is a template and a
work log: nothing in it ships. Build the hand-out with `python3 docs/Plan/rule_cards/tools/review_pack.py --pdf`
(writes `review/guide_review_<date>.pdf`, git-ignored; HTML is written even if Chrome is missing).

## Verdict meanings
- **OK**: card is right. No change.
- **Wrong**: the card says something the authority does not.
- **Outdated**: it was right, a later order changed it. Needs the new authority and its effective date.
- **Missing**: a rule users need that no card covers. Give the heading and authority.

## Replies (date received, expert name or initials)

| Card ID | Verdict | Correction | Authority | Owner decision (accept / reject / ask) | Applied in |
|---|---|---|---|---|---|
| RB-... | OK / Wrong / Outdated / Missing | what the card should say | letter or rule, number and date | | commit |

## Steps to apply an accepted row
1. Copy the row into `14_review_decisions.md` (pending decisions), then edit the card in `authoring/*.txt`, or the
   rate in `figures.json`. Keep the card in our own words; update `C:` (cite) with the authority.
2. If it settles an open point, remove the card's `O:` line. If the authority could not be read, keep the point and say so.
3. Run `python3 docs/Plan/rule_cards/tools/refresh.py` (rebuilds everything; stops at the first failure).
4. Move the row's outcome to `15_confirmed_rulesets.md` with its evidence level (PRIMARY text read, LOCAL full text, OWNER).
5. Commit the authoring change together with the regenerated files.

Rejected or unclear rows stay in the table with the reason; never delete them.

## Intake: hospital-stay regularisation (2026-11-06)

Source: a local HQ letter on hospital-stay regularisation (kept local, never committed, never named in the app;
owner decision a). Cards and pages cite only ODP 2026 Chapter 6 and the Leave Rules for the Services (Army) Vol I.
Local text for `copycheck`: `~/Downloads/rulecards_workdir/prim/hosp_regularisation.txt` (untracked).

| Card ID | Verdict | Correction | Authority | Owner decision (accept / reject / ask) | Applied in |
|---|---|---|---|---|---|
| (new) hospital stay needs Part II orders | Missing | A hospital stay with no Part II orders is treated as irregular absence and pay is recovered; publishing more than 2 years late needs a time-barred sanction first | ODP 2026 Ch 6 pp 142-150; Leave Rules Vol I | accept | Phase 1 |
| (new) which Part II orders for a stay | Missing | Admission, discharge, reported for duty, plus the regularising code for the length of stay | ODP 2026 Ch 6 serials 6.1-6.14 | accept | Phase 1 |
| (new) how a long stay is split | Missing | Annual leave for the year first, then sick leave: 180 days at full pay; the balance at furlough rates | Leave Rules Vol I; ODP 2026 Ch 6 | accept | Phase 1 |

### Claim table

"Traced" = found in an ODP 2026 / Leave Rules source held locally. "Letter only" = rests on the letter alone.

| # | Claim | Status | Note |
|---|---|---|---|
| 1 | Absence from first day of admission to resuming duty must be regularised by Part II orders | Traced | RP-016 already says it counts from the first day of admission; codes in ODP 6.1-6.14 |
| 2 | Orders are mandatory for every hospital stay | Letter only (owner accepted 2026-11-06) | Not stated as a rule in ODP; ship only as "needs Part II orders or pay may be recovered", cite ODP and Leave Rules |
| 3 | Without orders the days are treated as irregular absence and pay is recovered | Letter only (owner accepted 2026-11-06) | Consequence is stated by the letter alone; keep, wording "can be recovered" (owner to accept) |
| 4 | Publishing beyond 2 years needs a time-barred sanction first | Letter only (owner accepted 2026-11-06) | Fin Regulations Part I not held locally; needs owner OK or an `O:` open-point line |
| 5 | Up to 30 days: regularise with SLC | Traced | ODP 6.9 SLC (From, To required); RP-017 same rule |
| 6 | Over 30 days: REGALMS (military service) or REGAL (not), with GTDALHOS and GTDSLHOS | Traced | ODP 6.10-6.13 columns match (order no of first admission, AL days, SL days) |
| 7 | 60 days AL for the year + up to 120 days SL = 180 days at full pay | Letter only (owner accepted 2026-11-06) | RP-016 says "six months including annual leave due"; consistent, not the same words |
| 8 | Balance beyond 180 days at furlough rates, code HOSFURPY | Traced in part | ODP 6.14 HOSFURPY "Not due to military reasons" traced. See conflict C1 |
| 9 | Stays with sick leave also need SLDISCH, GTDSL, SLHOSADM | Traced | ODP 6.x serials exist |
| 10 | MEDTRAN to be published as applicable | Traced | ODP 6.x serial exists |
| 11 | Discharge, report for duty and the regularisation go in the same Part II order | Letter only (owner accepted 2026-11-06) | |
| 12 | The 16 worked examples add up | Checked | Re-added in the planning phase |

### Conflicts and open points
- **C1 (ODP governs):** ODP 6.10 REGAL has a data column "total No of days of leave on half pay", while the letter splits the balance as furlough-rate pay (HOSFURPY). Cards will say "furlough rates" only where the letter and ODP's HOSFURPY agree, and will not state half pay.
- **C2:** the letter's examples show REGALMS with no furlough part; ODP 6.11 also has none. No conflict, noted.
- **O1:** the matrix has no column for "over 6 months, attributable to military service"; the letter's table has none either. No card will state that case.
- **O2:** claims 2, 3, 4, 7 and 11 were "letter only"; owner accepted all five on 2026-11-06 (no `O:` line).
- Existing cards RB-RP-016 / 017 / 026: no contradiction found (016 on sick leave from the first day of admission and furlough rates beyond six months; 017 on the 30-day concession; 026 on irregular leave). None needs editing.
