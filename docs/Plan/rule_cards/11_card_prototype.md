# Card prototype: five cards in the phone template (draft for review)

Purpose: test the card format before drafting hundreds. Text is written in our own words from the FAQ answers and the MoD/DoE letters read earlier; no source passages are copied. **Not yet read by a domain owner: treat every line as a draft.**

## Template and limits (a validator would fail the build on any breach)

- Order: Answer, Key points, Attach (optional), Watch out (optional), Cite. Collapsed 'Details' below.
- Answer <= 25 words; each section <= 3 bullets; each bullet <= 12 words; visible text (answer + bullets + cite) <= 90 words.
- `{level}`, `{food_rate}`, `{ctg_estimate}` are filled from the officer's Pay Audit profile and payslip; without a profile the card shows the table instead.
- Chips: RATES = shows a figure with an 'as of' date; AMENDED = figure comes from a letter that amended the TR. The user sees the Cite line only, never the FAQ/handbook ids.

## What the five cards taught us

1. The fixed 'You get' heading did not fit a deadline card, so it is now 'Key points'.
2. Two cards (food, CTG) become *personal* with one profile field, so the officer sees one figure, not a table.
3. Every card had at least one claim in the FAQ that we could not source (the 30-day advance rule, the DA-rise date). The template makes it easy to leave such claims out; the 'Open points' under each card list them.
4. All five fit the limits at 45 to 79 visible words. The longest, 79 words, is about 10 lines on a phone.

## PT-001 Food charges on TD: how much do I get?

Temporary duty · visible 77 words · chips: RATES, AMENDED · personal: level -> food_rate

```
Food charges on TD: how much do I get?
----------------------------------------
ANSWER
  A fixed daily amount for your pay
  level. No bills needed. How much you
  get depends on hours away.

KEY POINTS
  • Under 6 hours away: 30% of the
    day's amount
  • 6 to 12 hours: 70%. Over 12 hours:
    100%
  • Your amount: Level {level} = Rs
    {food_rate}/day

ATTACH
  • No food bills or vouchers

WATCH OUT
  • Hours count midnight to midnight,
    each day
  • Rates rise with DA; check the 'as
    of' date

CITE
  MoD letter 12630/Mov C/242/D(Mov)/2017
  dt 15-09-2017; Rule 114 TR

[ Details v ]    [ Copy cite ]
```

Details (collapsed): Base daily amount by level, before the DA-linked 25% rise: Level 14+ Rs 1,200; 12 to 13B Rs 1,000; 9 to 11 Rs 900; 5A to 8 Rs 800; 5 and below Rs 500.

Internal source: ORD-03 / FAQ x3 / MoD 15-09-2017 (read).
Open points: Effective date and order for the 25% DA-linked rise (FAQ shows 1,125/1,250/1,500; letter base 900/1,000/1,200).

## PT-002 Own car on TD: can I claim mileage?

Temporary duty · visible 79 words · chips: none · personal: no

```
Own car on TD: can I claim mileage?
----------------------------------------
ANSWER
  Only with a sanction from the
  competent authority. Without it, you
  get the cost of the warrant only.

KEY POINTS
  • With a Rule 40 Note 2 sanction:
    road mileage allowance
  • Without it: cost of the warrant
    journey

ATTACH
  • The sanction under Rule 40 Note 2

WATCH OUT
  • Sanction must come from the GOC-
    in-C of the Command, or their
    delegate
  • NCC officers: only DGNCC can
    sanction
  • A Rule 47(iii) sanction covers
    cost of warrant only

CITE
  Rule 40 Note 2 and Rule 47, TR 2014

[ Details v ]    [ Copy cite ]
```

Details (collapsed): Audit asks one question: did travelling by own car serve a public interest, for places connected by rail?

Internal source: TR 40/47 / FAQ x4 (own car, RMA, NCC).
Open points: Confirm 'cost of the warrant' wording means the fare the warrant journey would have cost.

## PT-003 Home Town LTC: how often can I take it?

Leave Travel Concession · visible 64 words · chips: none · personal: no

```
Home Town LTC: how often can I take it?
----------------------------------------
ANSWER
  Once per calendar year, to your home
  town. Not in a year you take Anywhere-
  in-India LTC.

KEY POINTS
  • Home Town LTC (177A) or Anywhere-
    in-India (177B): one per year
  • Parents: yearly visit to your home
    town
  • Parents living there can visit you
    instead

ATTACH
  • Dependent Part II order for
    parents

WATCH OUT
  • A home-town trip on a warrant
    counts as that year's LTC

CITE
  Rule 177A(i)(a), TR 2014

[ Details v ]    [ Copy cite ]
```

Details (collapsed): Field and CI postings have extra entitlements under Rule 177C; see the field-area LTC card.

Internal source: LTC-01 / FAQ x5 (177A).
Open points: Confirm 177C can be taken with 177B in the same year (FAQ says yes) before the field-area card is written.

## PT-004 CTG on a permanent move: how much?

Permanent move · visible 73 words · chips: AMENDED · personal: payslip basic -> ctg_estimate

```
CTG on a permanent move: how much?
----------------------------------------
ANSWER
  80% of your last month's basic pay,
  for a permanent move of 20 km or more.

KEY POINTS
  • To or from Andaman, Nicobar or
    Lakshadweep: 100%
  • Under 20 km, or same city: one-
    third of the 80%
  • Your CTG is about Rs
    {ctg_estimate}

ATTACH
  • Local move: vacation report,
    occupation report, allotment
    letter

WATCH OUT
  • Basic pay only: NPA and MSP are
    not counted
  • Permanent posting only, not after
    study leave

CITE
  MoD letter 12630/Mov C/242/D(Mov)/2017
  dt 15-09-2017

[ Details v ]    [ Copy cite ]
```

Details (collapsed): The same percentage applies to field and peace postings. If both spouses serve, separate rules apply.

Internal source: ORD-06 / FAQ x4 / MoD 15-09-2017 (read).
Open points: The one-third local-move rule needs a local-move example before drafting the spouse variants.

## PT-005 How long do I have to submit a TD or move claim?

Claims · visible 45 words · chips: none · personal: no

```
How long do I have to submit a TD or move claim?
----------------------------------------
ANSWER
  60 days from the day the journey ends.
  Retirement journeys get 180 days.

KEY POINTS
  • The clock starts when the journey
    is completed
  • Submission date is the day you
    sign the contingent bill

WATCH OUT
  • A late claim risks being refused.
    File early

CITE
  MoF DoE OM 19030/1/2017-E.IV dt
  15-06-2021

[ Copy cite ]
```

Internal source: ORD-08 / FAQ x2 / DoE OM 15-06-2021 (read).
Open points: The FAQ also says 30 days when an advance is drawn. That is NOT in the OM and is left out; find its source before adding.
