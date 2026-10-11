# TA/DA pre-claim worksheet (Guide tab) and FAQ change watcher: facts and decisions

Status: **plan only, nothing built (2026-10-11).** Written for the owner to approve before P0 starts. Execution plan: `22_ta_da_preclaim_execution_plan.md`.
Source: the owner's walk of the PCDA(O) Pune portal (TA/DA Requisition list, form, instruction pop-up; 5 screenshots, 2026-10-11)
and one issued advance voucher (permanent duty, 3 persons). The portal was **not** driven by the agent. The voucher PDF holds
a name and a service number: it stays in Downloads and is never committed.

## What the feature is, and is not
The officer still files on the portal, logged in as themselves. The Guide tab prepares the claim: the form fields in the order the
portal shows them, with values taken from the latest payslip, the supporting-document checklist for the move type, and the
rule-based amounts the app can compute. **No credentials, no automation of the portal, no new outbound path.**

## Portal facts (from the screenshots; verify before shipping copy)
- Form: Type of Move (Permanent Duty / LTC / Temporary Duty). Basic info: PCDA A/C No, Officer's Name (both locked), SPR,
  Re-Employed, Marital Status, Spouse Employed (in Army), Basic Pay, NPA, MSP. Journey: Move Sanction ID (max 40 chars), Move
  Sanction Date, From Station, To Station, "Destination is Andaman Nicobar", From Date.
- Upload: **one** scanned PDF, **more than 50 KB and less than 2 MB**. Temporary duty: move signal or course detail letter.
  Permanent duty: signal copy or proper posting order. LTC: undertaking/intimation memo countersigned by OC/CO, plus the leave
  sanction certificate when the officer travels.
- Output: a "Requisition for Advances" voucher (Fare per person, CTG, Baggage, Conveyance, Distance, Total).
- Mapping to our data: Pay in Pay Band, MSP, NPA are fields the parser already extracts. CTG on the sample voucher is
  80% x 149000 = 119200, which matches the shipped `ctg` figure (NPA and MSP excluded).

## Open facts that block parts of the scope
| # | Fact | Why it matters | Owner action |
|---|---|---|---|
| F1 | **Closed 2026-10-11 (owner):** the sample voucher total (435922) is an advance; the printed lines add to only 171200. The owner says the advance "is adjusted when the user submits the final claim to PCDAO". | We cannot reproduce the portal's advance. | Decision: the app **never shows a voucher or advance total**. It shows only the components it can compute, each labelled an estimate, with the line "the portal sets the advance and adjusts it at the final claim". |
| F2 | **Closed 2026-10-11 (owner):** baggage by road is Rs 50/km base (handbook) x the `da_step` multiplier (Rs 62.5 at DA 50-99%, the FAQ figure). Not a conflict: base versus escalated. | One rule, one home. | Done: `baggage_road` is a base figure plus `da_step`, no flat Rs 62.5. |
| F3 | RMA rate source. **Closed 2026-10-11:** PCDA(O) Wing Circular no. 10 dt 03-09-2024 (34 states/UTs, Rs 12 to 30 per km), recorded as the `rma` figure in `figures.json` (P2). | RMA = km x rate of the originating state. | Done. |
| F3a | **Closed 2026-10-11 (owner):** Lakshadweep and Dadra and Nagar Haveli and Daman and Diu have no rate in the circular because the Army has no presence there. | None. | The state picker does not list them. |
| F3b | **Closed 2026-10-11 (owner):** Wing Circular no. 10 is still the latest RMA table, although it dates from the DA 50% revision. | Rates follow DA steps, so re-check at the next one. | `approved` on the `rma` figure in `figures.json` is now 2026-10-11; the FAQ watcher (P0) is the trigger to re-check. |
| F3c | The circular is by **state**; the FAQ says "originating city". | The officer picks the state the journey starts in; no station table is needed for the rate. | Confirm the state reading. |
| F0 | Fare and distance (2881 km Dimapur to Pune) are computed by the portal; we hold no station-distance table. | Baggage and RMA need km. | Decided here: the officer types the km; the app never claims it is the portal's figure. |

## How we build it
The rules, the Phase Handoff Protocol, the gate matrix and the ten phases (P0 to P9) are in
`22_ta_da_preclaim_execution_plan.md`. One feature branch, `feature/ta-da-preclaim`, for the whole plan. This file keeps the facts and the
owner's decisions only.

## Settled by the owner, round 2 (2026-10-11)
- Stage: **advance (pre-claim) only**; the final claim is a later plan, after its portal screens are seen.
- CTG with both spouses serving: **note only**, no calculation, no spouse-date question.
- Never show a voucher or advance total (F1). Baggage by road = Rs 50 base x `da_step` (F2). RMA table approved (F3, F3a, F3b).
- Move types: **permanent and temporary duty both fully computed**; LTC gets the field list and document checklist.
- Profile gap found in the code: `GuideProfile` has level, basic pay and DA% from the latest payslip, but **no MSP or NPA**.
  P4 extends the profile (derived from the same payslip, nothing stored, no migration). DA% for `da_step` also comes from it.
- TD computes **four elements**: daily food (existing `food_rate`), lodging, local conveyance/taxi, and own-car RMA (rail and air
  fare stay portal-computed). Lodging and conveyance have **no figure in `figures.json` yet**: P1 reads their rules in the TA
  handbook, FAQ and MoD letters and adds figures (none ships unapproved). The calculator is split into **P4 permanent duty** and **P5
  temporary duty**, each its own phase with its own gate (see 22).
- Wording: every amount shows "Estimate, rule and date, the portal sets the final advance". No acknowledgement sheet, no total.
- **Received 2026-10-11:** the TD and LTC create forms, their dropdowns and two TD claims (recorded in the sections below). Any TD/LTC field not
  seen on the portal is still marked "unverified against the portal" (P3).
- Round 4 (2026-10-11): **LTC gets a rule picker** (each portal TR Rule option with a one-line "use this when" and a link to its card)
  plus the document checklist, no amounts. The **long-course tick is a note only**: a warning when the dates pass 180 days
  (Rules 114 and 123), nothing computed from the tick. Choosing TR Rule 124 sets TD food and lodging to nil.
- Round 5 (2026-10-11, the last doubts):
  - **Watcher covers the FAQ feed and the public notifications feed** (`homepage/content`, 14 items with document links, e.g. SPR
    declaration, Part II order for TD, outstanding DTS advances). **Wing circulars (the RMA table, DA-step letters) are not public**:
    the owner got Circular 10 from a unit or group, so no automatic alert can see the next one. Runbook rule: a circular received from
    a unit goes through `18_content_update_runbook.md` intake by hand.
  - **TD RMA** asks "do you hold the Rule 40 Note 2 sanction?": no gives RMA nil and "cost of warrant instead"; yes gives km x state rate.
  - **Copy is per field only**: no copy-all, no export, no share sheet.
  - **Telemetry: breadcrumb only** (`CrashReporter.log` "preclaim opened, move type", no values); no new outbound path, no store
    privacy-form change. The owner first asked for a count event, then chose this after hearing `CrashReporter` has no event API.
  - **Commit:** the plan and the data files (`rma_rates.json`, `td_rates_sheet.json`) fold into the P0 commit.
  - **Order:** P0 watcher, then P1 rate research. **Launch:** dark (`PRECLAIM_ENABLED` false in the release), flipped after the
    Pixel and iPhone walks pass.
  - No payslip imported: the worksheet takes basic, MSP, NPA and level by hand (decided by the agent, not asked).
- P0 is **built and verified locally (2026-10-11)**; the open P0 points are in P10 of `22_*.md`.

## Temporary-duty facts (two TD claims viewed on the portal, 2026-10-11; no names or Dak IDs kept here)
- View-detail layout. Basic info: PCDA A/C No, Dak ID (portal-assigned; its date part is the filing date), name, Re-Employed, Home
  Station, SPR, Marital Status, Spouse Employed (In Army), Basic Pay, NPA, MSP. Journey: Move Sanction ID and Date, From and To
  Station, From Date, To Date. Requisition (temporary duty): TR Rule (119 on both), Mode of Journey (Road / Rail), Total Fare,
  Total Absence (days), Food Charges, RMA, Accommodation Charges, Amount Required. "Home Station" is not on the permanent-duty
  form we saw: **unverified** for the TD create form (screens still to come).
- Checked on both claims: Amount Required = Fare + Food + Accommodation + RMA exactly (1500+57000+256500+0 = 315000;
  0+33500+91500+0 = 125000). Total Absence = To minus From, inclusive (57 and 61 days).
- Claim A (Level 13A basic, 57 days, filed 26-01-2024, DA 46%): food 57 x 1000 matches `food_rate` (Level 12-13B, base 1000, no
  escalation below DA 50%); accommodation 57 x 4500 equals the handbook ceiling for Level 12-13B ("up to Rs 4,500 per day").
- Claim B (61 days, filed 26-03-2024): accommodation 61 x 1500, below the ceiling; food 33500, which the owner **claimed lower on
  purpose** (not a rule result).
- Claim A asked 315000, admitted 186500. Owner: PCDA(O) gives a lesser advance and releases the rest in the final claim. The share
  is unknown, so the app promises no admitted or advance amount (consistent with F1).
- **F4 (largely closed 2026-10-11, by derivation; still unapproved):** the owner's TD rates image is recorded in `td_rates_sheet.json`.
  Every TD figure on it equals the handbook base x 1.25 (food 1250/1125/1500, hotel 5625/2812.50/9375, non-AC taxi 422.50 at Level 9-11),
  which is exactly our `da_step` rule at DA 50-99%. So "TD rates increased" is the DA-50% step the engine already models; **no new
  figure shape is needed**, only `hotel` and `taxi` figures built the same way as `food_rate`. The sheet itself has **no letter number,
  issuer or date** and its HRA line is one DA step behind our approved figure, so it is a cross-check, not a source (F4a **answered 2026-10-11**: a third party compiled it after DA passed 50% and it circulated on social media, so
  its evidence level is WEB_SECONDARY and P1 still has to find the MoD letter and its effective date). Its "RMA Rs 30" is only the top rate of Wing Circular 10; the circular wins.
- **TD conveyance, as the sheet gives it:** Level 9-11 non-AC taxi capped at Rs 422.50/day (computable); Level 12-13B AC taxi up to
  50 km a day, and Level 14 and above AC taxi at actual: **no rupee cap**, so the app shows the km limit and never an amount.
  Food by hours away: under 6 h 30%, 6 to 12 h 70%, over 12 h 100% (so a part-day is a percentage of the day's lump sum).
- **F5 (closed 2026-10-11, from the blank TD create form):** the portal fills PCDA A/C No and Officer's Name (locked), **Total
  Absence** (greyed, from the two dates) and **Amount Required** (greyed, the sum). The officer types **Total Fare, Food Charges, RMA
  and Accommodation Charges**, plus SPR, Marital Status, Basic Pay, NPA, MSP. Read from the fields' greyed or enabled look on a blank
  form, not from typing into it. So on TD the fare is the officer's own number (the ticket cost), unlike the permanent-duty voucher
  where the portal computes it.
- **TD create form field order:** Type of Move; SPR, Marital Status, Basic Pay, NPA, MSP; Move Sanction ID (max 40), Move Sanction
  Date, From Station, To Station, "Destination is Andaman Nicobar" tick, From Date, To Date, **"TD For Long Duration Course" tick**;
  TR Rule (dropdown), Mode of Journey (dropdown), Total Fare, Total Absence, Food, RMA, Accommodation, Amount Required; a
  certificate tick **"I have NOT availed any Advance on a/c of DA portion of TD from DTS"**; Submit.
  **TD has no Re-Employed or Spouse Employed question** (permanent duty does), so P6 asks per move type, not one fixed list.
- **F7 (closed 2026-10-11, from the owner's dropdown screenshots).** TD **TR Rule**: Rule 4(i) TR; Rule 4(ii) TR; Rule 119 TR;
  Rule 124 TR (DA not admissible). **Mode of Journey** (TD and LTC): Air, Rail, Road, Sea, Multiple. Reading: the handbook calls
  4(ii) the "special sanction" rule; P1 read TR 2014: 4(i) is moves ordered by the Appendix III authorities, 119 is the ordinary TD entitlement (readings in `23_*.md`, owner approval pending). Rule 124 (courses): conveyance
  only, no DA (handbook and FAQ), which the dropdown label itself says, so choosing it sets food and lodging to nil.
- **F8 (partly closed):** the handbook caps course DA at the **first 180 days** and then nil, where free boarding/lodging is not
  provided (Rule 123 TR; also the 180-day continuous-halt rule, Rule 114). The portal's own meaning of the "TD For Long Duration
  Course" tick is **not documented in anything we hold**; assume it relates to that cap, unverified, and show the cap as a note.
  Free boarding/lodging changes the entitlement (the old 25% DA was discontinued, MoD letter 12650 of 2018).
- **F6 (partly closed by P1, 2026-10-11): when does the +25% DA step reach the food lump sum?** DA reached 50% from 01-01-2024 but was notified in March 2024;
  claim A (filed 26-01-2024) used no escalation. A civil order (UT Ladakh Finance Dept Order 231-F of 2024) applies the step **w.e.f. 01-01-2024** and matches every TD figure;
  the **Army (MoD) letter was not found**, so the Army date is an owner action (P10 of 22, register in `23_*.md`).
- Lodging design (owner): the officer enters the expected nightly rate and the app caps it at the ceiling for their level
  ("capped at Rs X"); the handbook says hotel bills are mandatory, so the ceiling is a limit, never the amount.

## LTC facts (blank LTC create form and three dropdowns, owner's screenshots 2026-10-11)
- Basic info: PCDA A/C No and name (locked), Home Station, SPR, Re-Employed, Marital Status, Spouse Employed (in Army), Basic Pay,
  NPA, MSP. Journey: **Leave Sanction Letter No** (max 40) and Date, From Station, To Station, "Destination is Andaman Nicobar",
  From Date, To Date. (No move-sanction wording and no "long duration" tick.)
- Requisition for LTC: Self / Spouse / Children (a count dropdown) / Dependents (ticks with counts); **No of Persons Travelling**
  (greyed, so portal-computed); **TR Rule**; Visit To; Mode of Journey; Year (greyed, source unverified); **Nature of Leave**
  (Annual, Casual, Forlough [portal spelling], Maternity); **Total Fare** (typed). **No Amount Required and no food or lodging**:
  the officer types only the fare. Submit.
- LTC **TR Rule** list as seen (8 options, the screenshot may be cut off): 177(A) for Home Town; 177(B) for any Station in India;
  177(C) for Officers Serving in Field Concessional Area; 177(D) for Officers on Study Leave (Self and Family); 177 children of
  service Personnel Studying in Outstation; 177(A) visit to J&K, A&N and NE in lieu of Home Town; 177(A)(iii) Sick Leave;
  177(A)(i)(e) Sea Passage.
- Product reading: for LTC the useful help is **which TR rule to pick** (maps to the 50 LTC FAQ entries and the existing LTC
  cards, each option linking to its card) plus the document checklist; no amount is computed, only the fare the officer enters.

## Owner decisions already taken (2026-10-11)
v1 scope = worksheet + CTG + baggage/RMA (gated by P1) · profile = ask every time · tier = Premium · watcher = GitHub Action,
tested by hand first.
