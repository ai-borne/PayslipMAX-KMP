# TA/DA pre-claim: rate register (P1 output, 2026-10-11)

Status: **researched and approved by the owner on 2026-10-11 (every row), with the step date confirmed as 2024-01-01.** This file is the one home for "where does each pre-claim
figure come from". P2 copies a row into `figures.json` only after its approval cell holds a date; until then nothing ships (plan 22, P2).
Facts and decisions: `21_ta_da_preclaim_plan.md`. Build rules: `22_ta_da_preclaim_execution_plan.md`.

Evidence labels are the ones `tools/figures.py` accepts (`LETTER_TEXT`, `HANDBOOK_ONLY`, `WEB_SECONDARY`, `OWNER_CONFIRMED`). No new label is
invented to make a row look stronger; where a civil order backs an Army figure, that is said in the detail column, and the label stays `HANDBOOK_ONLY`.

## Sources read in P1
| Tag | What was read | Read how |
|---|---|---|
| S1 | MoD letter 12630/Mov C/242/D(Mov)/2017 dt 15-09-2017 (Table E, para 2(iv)) | already `LETTER_TEXT` in `figures.json` (`da_step`, `food_rate`) |
| S2 | Handbook on Travelling Allowances 2023 (the owner's PDF text): TD rates table, baggage by road, Rules 114, 123, 124 | text read; prints the S1 letter number and the base rates, not the letter itself |
| S3 | Travel Regulations 2014 (OCR text): Rules 4, 114, 119, 123, 124, 40 Note 2 | text read; OCR, so numbers in it are not used, only rule wording |
| S4 | Finance Department, UT Ladakh, Order 231-F of 2024 dated 03-05-2024 ("Revision of rates of various allowances with increase in the DA rate by 50%"), 5 scanned pages | read page by page. **Civil**, not MoD. It lists the DoE OM 19030/1/2017-E.IV and DoE OM 1/1/2024-EII(B) dt 12-03-2024 as its authority, and applies the 25% step **w.e.f. 01-01-2024** |
| S5 | Wing Circular no. 10 dt 03-09-2024 (RMA table) | `rma_rates.json`, `LETTER_TEXT` |
| S6 | The owner's phone-image rates sheet | `td_rates_sheet.json`, `WEB_SECONDARY`; now only a cross-check |

S4 is a public government order (cdnbbsr.s3waas.gov.in, `.../2024/05/202405061031185669.pdf`). Only its number, date and figures are recorded here; the
text is not copied into the repo.

## Figure rows (proposed for P2)
Step columns are DA 50 to 99%. Base figures are the S2 handbook rates (7th CPC, effective 2017-07-01); `da_step` (existing, approved) turns base into current.

| Figure | Base value | At DA 50-99% (x1.25) | Evidence | Authority | Step effective from | Cross-check | Approval |
|---|---|---|---|---|---|---|---|
| `td_hotel` (ceiling per day) | L14+ 7500; L12-13B 4500; L9-11 2250 | 9375; 5625; 2812.50 | `HANDBOOK_ONLY` | MoD letter 12630/Mov C/242/D(Mov)/2017 (S2 prints it); the step by para 2(iv) (S1) | 2024-01-01, civil text only (S4); owner-confirmed for the Army 2026-10-11 | S4 prints exactly 7500/4500/2250 to 9375/5625/2812.50; S6 agrees | **approved 2026-10-11 (owner)** |
| `td_taxi` (non-AC, L9-11, per day) | 338 | 422.50 | `HANDBOOK_ONLY` | as above | 2024-01-01, civil text only (S4) | S4 prints 338 to 422.50; S6 agrees | **approved 2026-10-11 (owner)** |
| `td_taxi` L12-13B | AC taxi up to 50 km a day | unchanged (S4: step "Nil") | `HANDBOOK_ONLY` | as above | n/a | S4 prints no rupee cap | **approved 2026-10-11 (owner)**: shown as a km note, no amount |
| `td_taxi` L14+ | AC taxi at actual, with official engagements | unchanged | `HANDBOOK_ONLY` | as above | n/a | S4 same | **approved 2026-10-11 (owner)**: note only, no amount |
| `food_rate` (existing) | 900 / 1000 / 1200 | 1125 / 1250 / 1500 | `LETTER_TEXT` | S1 Table E(i) | 2024-01-01 civil text (S4 lists 1125/1250/1500) | S6 agrees | approved 2026-10-08; step date 2024-01-01 `OWNER_CONFIRMED` 2026-10-11 (F6) |
| food timing | under 6 h 30%, 6-12 h 70%, over 12 h 100% | | `HANDBOOK_ONLY` | S2 Table (ii) | | S6 agrees. **S3 (TR 2014) says nil under 6 h; the 7th CPC letter and handbook replace it with 30%**, so 30% | **approved 2026-10-11 (owner)** |
| `baggage_road` (personal effects, per km) | 50 (L10 and above) | 62.50 | `HANDBOOK_ONLY` | S1 letter, Handbook para E(a) "rates will further rise by 25%" | 2024-01-01, civil text only (S4 prints 50 to 62.50) | S6 (62.50); owner F2 | **approved 2026-10-11 (owner)** (shape decided in F2) |
| `rma` (per km, by originating state, 34 rows) | n/a | table in `rma_rates.json` | `LETTER_TEXT` | S5 | 2024-09-03, from immediate effect | top rate 30 equals S4's step on 24 (civil default) | **approved 2026-10-11** |
| `ctg` (existing) | 80% of basic | | `LETTER_TEXT` | S1 para 3(ii)(a) | | owner voucher 149000 gives 119200 | approved 2026-10-08 |

Notes on the rows:
- **Why the label stays `HANDBOOK_ONLY` for hotel, taxi and baggage:** the Army letter text itself was not read. The civil order S4 reproduces the same base
  numbers and the same step, which makes the figures very likely right, but it is not the MoD's own text.
- **F6 answer (the date):** every civil source we could read puts the step at **2024-01-01** (the day DA reached 50%), and DoE said in March 2024 that no separate
  order is needed. Claim A (filed 2024-01-26, no escalation) fits an Army notification that came later than 2024-01-01. Whether the Army pays the step from 2024-01-01 or from the date of an MoD letter is not settled by any document we hold; **the owner decided on 2026-10-11: 01-01-2024, recorded `OWNER_CONFIRMED` with that `confirmed` date** (no document; the MoD letter would upgrade it).
- `da_step` multiplier at the payslip's DA is `1 + 0.25 x floor(DA / 50)`: 1.25 from DA 50% to 99%, so the date question only matters for claims whose move
  or absence started before the Army's effective date. v1 can show the claim's start date against the date the owner approves.

## Rule readings (words from TR 2014 (S3) and the handbook (S2); readings, not legal advice)
| Portal option | What the rule text says | Use in the app | Approval |
|---|---|---|---|
| TR Rule 4(i) | "The authorities empowered to order moves on duty are shown in Appendix III." | Picker line: the move was ordered by an authority named in Appendix III (the ordinary case) | **approved 2026-10-11 (owner)** |
| TR Rule 4(ii) | A competent authority may authorise a move "which are not covered by any specific rule or order" if it is in the public interest and keeps to the TA principles (the handbook calls it the sanction for conferences and meetings not covered elsewhere) | Picker line: a special sanction, not covered by another rule | **approved 2026-10-11 (owner)** |
| TR Rule 119 | "Entitlement of Travelling Allowance on Temporary Duty Journeys": for service officers, conveyance as on permanent duty, DA as Rule 114, baggage as listed | Picker line: the ordinary temporary-duty move; the portal's default (both owner claims used it) | **approved 2026-10-11 (owner)** |
| TR Rule 124 | Conveyance for courses and examinations; DA for the course is Rule 123 | The dropdown label says "DA not admissible", so food and lodging are nil | closed (owner, round 4) |
| Rule 123 / handbook para 78 | Where free board and lodging are not provided: first 180 days full DA, beyond 180 days nil, each station separately | The 180-day cap note | closed (round 4: note only) |
| Rule 114 Note 4 (handbook para 53) | Over 180 days continuous at one TD point: no DA, the point becomes the temporary HQ; MA still paid | Same note | closed |
| "TD For Long Duration Course" tick | **Not defined in any document we hold.** Most likely tied to the 180-day rule; not confirmed | Note only, "unverified against the portal" (P3) | **closed 2026-10-11 (owner): note only, marked unverified; the portal hint text can still upgrade it** |

## What P1 found that changes later phases
1. `td_rates_sheet.json` (S6) is now corroborated line by line by S4, but it stays a cross-check and is not approved from. Its HRA line (27/18/9) is a step behind our approved 30/20/10; S4 prints HRA 9% to 10% for class Z, which agrees with our approved figure, so the sheet is the one that is wrong and the approved HRA figure stays.
2. The food timing note differs between the 2014 rules and the 7th CPC orders (nil versus 30% under six hours). P5 must use 30%, with a test that names this row.
3. P2 needs no new figure mode for hotel, taxi or baggage beyond `da_step` with bands (taxi bands carry a note, not an amount, for levels 12 and above). The RMA per-state table needs the new mode.
