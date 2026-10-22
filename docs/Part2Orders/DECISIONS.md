# Part II Orders — Decision Record

Owner Q&A, 2026-10-22. This records decisions, not a plan; a phase plan comes later, separately.
Source: "Documentation Procedure (Officers) – 2026" (ODP-2026), IHQ of MoD (Army), AG's Branch, PS Dte, 10 Apr 2026.

## Round 1: Problem and user

| Topic | Decision |
|---|---|
| Evidence | Real: owner has seen all four failures first-hand (own case, peers, unit admin): missed cessation → recovery, credit never came, wrong code/date/data, missed linked order. |
| First moment served | The life event ("I'm posted out — what must my unit publish?"). Payslip-wrong, past-orders and pre-retirement moments come later. |
| User action | Self-check only: the officer reads and follows up with the unit. No share/export, no ticking off. |
| Order access today | Officers can see orders on a portal: on-screen list, PDF per order, consolidated report, PCDA(O) status view. Makes a later order-import stage feasible (needs de-identified samples then). |
| How officers reach it | Staged: 1) Guide tile + search → 2) payslip nudges → 3) "ask once" with reminders. |
| Nudge signals | Promotion, field/HAA start or stop, transport-allowance city change, quarters in/out (all from Pay Audit's `ServiceTimeline`). The indirect two (city, quarters) must have their false-alarm rate measured on the corpus before shipping. |
| Reminders | Both in-app (on the next payslip import) and OS notifications. The app has no notification support today; this is new on both platforms. |

## Round 2: Content and trust

| Topic | Decision |
|---|---|
| Checklist basis | Manual only. Every step cites its ODP-2026 page or note. |
| Manual silent | A separately labelled "Related codes" list, with no claim that they are required. |
| Reviewer | Owner, checking every step against the manual. |
| Code names | The manual's exact wording, with citation (matches what the clerk sees in HRMS). Checklist text in own words. |
| Updates | The Guide content runbook (`docs/Plan/rule_cards/18_content_update_runbook.md`). |
| Background docs | Kept, with a header: "not a source; verify every code against ODP-2026". `ODP_2026_Comprehensive_Analysis.md` is committed. The two `MilDoc_*` docs (blueprint, super-app masterplan) hold pricing and go-to-market strategy and the repo is public, so they stay local (see Round 4, source files). |

## Round 3: Shape in the app

| Topic | Decision |
|---|---|
| Home | Content in the Claim Guide (new area); payslip nudges appear in Smart Insights / Pay Audit and open the Guide checklist. Data goes through the Guide bundle pipeline (`bundle.py`). |
| Paywall | Lookup and checklists free. Nudges and reminders Premium. Later order import/check Premium. |
| Audience | Everyone, labelled "Army officers (ODP-2026)". |
| Code page | Minimal: code, manual name, group (with what A/AM/B/C/CM means), chapter, manual page. Consequence: v1 does not help the "wrong code/date/data" failure; that moves to the order-import stage. |
| Event groups | All four: Moves, Career, Family, Health & leave. Moves is written and reviewed first (it carries the cessation rule). |
| Cross-links | Later: existing rule cards and Leave Bank rows are not linked to code pages in v1. |

## Round 4: Housekeeping

| Topic | Decision |
|---|---|
| Hidden launch | Own `LaunchFlags.PART2_ENABLED` (also needs `GUIDE_ENABLED`), off until owner content review and device walks pass. |
| Source files | Local only, in `~/Downloads/rulecards_workdir/part2/` (where the Guide keeps source text outside git): the manual PDF, its text dump `full_text_layout.txt`, the flawed `part2_occurrences_2026.json`, and the two `MilDoc_*` strategy docs. Done 2026-10-22; no `.gitignore` entries needed. |
| Dataset | Rebuilt by an extraction script with tests (all Annexure II serials, unique codes, combined rows split); owner spot-checks rows against the PDF. Replaces `part2_occurrences_2026.json`, which is never committed. |
| Write-up | This decision record first; a plan later, separately. |
| Commit/push | Docs-only commit on `main` (this record + the analysis doc); pushed with the next push needed anyway. |

## Facts established (2026-10-22)

- `part2_occurrences_2026.json`: 289 entries against 302 Annexure II serials. Missing 2.14–2.16, 3.25, 3.26
  (`STPFIELD`), 3.28, 3.53, 3.67, 3.80, 3.97 (`GTDHARDS`), 3.98, 3.99 (`GTDNTSSA`), 11.8, 13.12 (`BATTLCAS`).
  Column-wrap damage (3.27 code `GTDTLA1/G`). Serial 9.7 not found in the index text; check by hand.
- `SOSFRCRS` (cited in the analysis doc) is not in the manual.
- STPFIELD notes 2–3 (manual p. 56): the stop order is published every time an officer leaves a field area,
  including a move between field area types; "a mere grant of the new allowance will not suffice".
- NOKCHNG (11.4) is for remarriage after death of spouse or divorce only. A first marriage is `MARRIAGE` (11.1),
  or `MARSEROF` (11.2) when the spouse is in service or a government/PSU employee. The blueprint's
  "MARRIAGE + NOKCHNG" bundle is wrong.
- Part II orders cannot be amended: the order is cancelled and notified afresh (manual para 11).
- The previous edition was 2014; codes may differ, which matters for a later past-orders stage.

## Open, for later stages

- De-identified samples of the portal's per-order PDF and consolidated report (order-import stage).
- Mapping from occurrence codes to payslip line items (payslip-check stage).
- Storage, backup and notification design for "ask once" (stage 3).
