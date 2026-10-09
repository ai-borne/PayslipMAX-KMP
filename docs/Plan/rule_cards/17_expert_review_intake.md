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
