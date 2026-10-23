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

## Round 5: The order-drafting flow (owner's vision; supersedes parts of Rounds 1 and 3)

The owner's flow: the officer returns from leave, opens Guide → Part II Order, searches or taps the Leave
ribbon, fills in the details, and sees the draft occurrence(s) plus the orders that go with them. He prints
the draft for the clerk, who types it into HRMS and publishes it. The officer photographs the published
order into an encrypted vault in the app, and learns the process by using it.

| Topic | Decision |
|---|---|
| Output | Occurrence rows only (a "clerk slip"): code, narrative, From/To, Data 1–4 per occurrence. No identity data at all (no name, Pers No, rank, arm, PAN or CDA A/c): the officer hands the slip over in person and the clerk's HRMS has the rest. Not an IAFF-3010 replica. |
| Clerk | Types the slip into HRMS. |
| Validation | The manual's computable notes are checked before printing (e.g. GTDAL notes 1, 2, 4, 9, 10: day count, 60-day annual cap, tally, prefix/suffix > 5 days, two years → two occurrences). |
| Linked orders | Shown in the flow (grant → rejoin / extend / early return / recall); a follow-up order quotes the grant order's no. and date from the vault. |
| Sharing | Print only, via the system print dialog (which also offers Save as PDF). No in-app share sheet. Pay Audit's letter sharing is unchanged. |
| Vault | Encrypted photos of the published order, linked to the draft it came from; the officer types only the published order no. and date. No parsing, no OCR. |
| Inbound | Photo only, taken with an in-app camera (never written to the gallery or cloud photo sync); EXIF/GPS stripped; several pages per order. New camera permission on both platforms. |
| Lock | Follows the existing app-lock setting. |
| Backup | Orders travel in the `.pcda` backup (v5), encrypted with the backup password like corrections and Guide notes. |
| First ribbon | Leave: all 30 Chapter 5 occurrences, including AWL/REJAWL; ACCLVE computed as (a − b + c, max 300) from the year's saved leave orders and cross-checked against Leave Bank's December figure; ENCASH with its year-wise break-up prefilled from Leave Bank. Each code's template and rules are hand-written from the manual and owner-reviewed; the annual leave set is built first. |
| Paywall | Code lookup free; draft generator and vault Premium (Android stays free while `FREE_LAUNCH_MODE_ANDROID`). |

Superseded: Round 1 "self-check only" (the draft is now printed for the clerk); Round 3 "minimal code page" (the
generator needs each Leave code's full template, dates, Data 1–4 and notes); Round 3 "all four event groups" (Leave
is the first ribbon); Round 3 paywall "content free, automation Premium" (now as above).

## Round 6: Roadmap after Round 5

| Topic | Decision |
|---|---|
| Reminders | Driven by drafts, replacing "ask once": a draft sets its own follow-ups (e.g. "draft REJOINAL" on the leave's return date, "add the published order" a few weeks after a draft). In-app and OS notification (Round 1). |
| Payslip nudges | Later, each with the ribbon it opens (field/HAA nudges with a Moves ribbon, promotion with Career). Not in the Leave release. |
| Payslip check | Dropped. The vault is a record; Pay Audit stays separate. Code-to-payslip-line mapping is no longer needed. |
| Ribbon layout | Layout and sequencing of ribbons across all codes to be proposed holistically (separate agent, modelled on the Guide's `nav.json`); next ribbon after Leave decided from that proposal. |

## Round 7: What a real HRMS order showed (owner's own order, reviewed in chat; no identifiers recorded)

| Topic | Decision |
|---|---|
| Vault and RESTRICTED | Published orders are marked RESTRICTED. Owner keeps the Round 5 decision: photos stored encrypted and carried in the password-encrypted `.pcda` backup ("personal filing" copy). Owner accepts the policy risk. |
| HRMS draft check | The officer sometimes sees the HRMS draft before signing. Offer an optional "Check the HRMS draft" step that shows the app's slip for a field-by-field comparison; the flow does not depend on it. |
| Evidence | Real published orders may be used as evidence of which orders go together, shown to the officer labelled "seen in practice", separately from manual-stated links (cited to the page). Only de-identified structure (codes, column use) is kept. Amends Round 2's "manual only". |
| Slip extras | Besides the occurrence rows: an authority line per occurrence where the manual requires one (e.g. the posting signal no. and date), and the officer's last Part II order no. and date, from the vault. Amends Round 5's "rows only". |

## Round 8: Ribbon layout (agent proposal, 10 ribbons / 48 cases / 303 serials)

| Topic | Decision |
|---|---|
| Build order | As proposed: 1 Leave → 2 Postings and moves + Field, high-altitude and hard areas (one release; if too big, R&H, field, HAA and SCCIA first) → 3 Promotion, courses and awards → 4 Family and children → 5 Leaving the Army → 6 Hospital and health → 7 Allowances for your role → 8 Deputation and special postings → 9 Records and unit-only orders. |
| Leave + field re-grant | ~~"Also publish" link, no draft~~. Superseded by Round 9: drafted in the Leave release. |
| Tiles | Only built ribbons show tiles, plus the lookup-only Records ribbon. Search reaches all 303 code pages from release 1. |
| Stop on proceeding on leave | Answered by the owner's HRMS summary: see Round 9. |
| Awards and TA | Ch 10 (Territorial Army) lookup-only. Ch 7 drafts only 7.9 GALASSPP (monetary allowance); the other 12 are lookup-only. |
| Tile size | 2 to 16 codes per tile (the Guide uses 4 to 16), so natural pairs such as CANCEL/CANCELS stay together. |
| Blank groups | 3.28 STPTLA and 11.8 REMAROF show "Group not printed in ODP-2026"; no group is inferred. |
| Stale note references | Notes that cite old serial numbers are mapped by occurrence name, cited to their page, and listed for owner review. |
| Layout file | `RIBBON_LAYOUT.md` (this folder) is the agreed layout; where it differs from Rounds 8–9, this record wins. |

## Round 9: What the owner's order summary changed (supersedes Round 8's "also publish" link)

| Topic | Decision |
|---|---|
| Ration four | Drafted in the Leave release: the flow asks "Are you on ration strength?"; if yes, the slip adds SORS + GTDRSHNA from the first day away (incl. prefix) and TORS + STPRSHNA from the rejoining date. Manual-backed (ration notes 1.1, 1.5, 3, 4). |
| Area allowance around leave | Drafted in the Leave release for an officer in an R&H/field/HAA/CI area. R&H: stop from the first day away (incl. prefix) and re-grant from the rejoining date, labelled "seen in practice". HAA/field/SCCIA: re-grant only, cited to the manual (pp. 49, 53, 55) with its casual-leave thresholds; no stop until there is evidence. This pulls the area-allowance templates into the Leave release (about 60 serials in all), so the plan phases it internally: annual leave set + ration + R&H first. |
| Rejoining code | The slip prints `REJOIN`, as HRMS publishes it; the code page notes that ODP-2026 lists `REJOINAL` (5.23). |
| Cancellation | CANCEL / CANCELS ship with Leave, offered from any vault order ("this order is wrong → cancel and re-draft", para 11). |

## Round 10: Engineering choices for the Leave release (asked with the phase plan, `docs/Plan/12_Part2Orders_PhasePlan.md`)

| Topic | Decision |
|---|---|
| Notifications | Local only, on both platforms (Android WorkManager, iOS `UNUserNotificationCenter`); no push. Permission asked the first time a draft sets a reminder; if declined, the in-app Due list only, never asked again. The text is generic and never names a code or date. Drafts are the source of truth; scheduled notifications are rebuilt from them. Cadence: return date at 09:00 ("draft REJOIN"); 14 days after printing, then every 30 days, at most 6 ("add the published order"); 1 March of Y+1 ("ACCLVE for Y", note 7 p.134). |
| Camera | System document scanners: Android ML Kit Document Scanner (gallery import off), iOS VisionKit. |
| Vault storage | Every page re-encoded to JPEG (long edge 1600 px, at most 500 KB), which drops EXIF/GPS; one encrypted Room row per page; at most 10 pages per order; in backup v5 as Base64 ciphertext; Settings warns above 60 MB of vault. |
| Templates | Hand-written `docs/Plan/rule_cards/part2/templates.json`, checked by `tools/part2.py` and again in the app on load; an additive `part2` bundle section (BUNDLE_VERSION stays 1). Narratives keep the manual's wording, so the own-words guard skips them. |
| Paywall | New `FeatureGate.PART2_ORDERS` for drafting, printing, vault capture and reminders; lookup free; independent of `GUIDE_PAYWALL_ENABLED`. After Premium lapses, existing drafts and vault orders can still be viewed, deleted and backed up. |
| TPTA | Drafted with Leave: STPTRAN and GTDTRAN1–5 when leave covers a whole calendar month (p.71 notes iii, v). The release drafts 59 serials. |
| Branch | `feature/part2-orders` off `main` @ `d9274bdd`. |

## Round 11: Content batch 1 (asked at the start of phase 2b, 2026-10-23)

| Topic | Decision |
|---|---|
| Phase 2a | Accepted (534ba5cc). |
| Engine behaviours left open in 2a | Each Group B claim is an order of its own (practice: claims were single-occurrence orders). The order header lists each group letter its occurrences carry, once, in the manual's order (A, AM, B, C, CM); none is inferred. Two identical rows from two requests stay one row. |
| Prefix and suffix | The manual's way: From and To exclude them, Data 1 holds the first prefix day, Data 2 the last suffix day (empty when there is none), and the narrative prints "Nil". Ration and R&H start at the earlier of the first prefix day and the first day of leave; the rejoining day is the day after the later of the last day of leave and the last suffix day. The practice date lists are not used. |
| `ack` | An `ack` finding must be ticked before printing; `warn` is only shown. `ack`: annual leave over 60 days a year (note 2, p.115) and casual leave beside another leave (p.117). `warn`: prefix and suffix over 5 days (note 9, p.115) and a grant order not yet quoted on EXTNAL, REJOIN, VOLREJ and RECALL (note 7, p.115; note 2, pp.131-132). |
| R&H scope | Annual leave only: the stop (from the first day away) and the re-grant under the officer's own GTDRH code (from rejoining) link from GTDAL and GTDALCF, labelled "seen in practice". Casual and special casual leave draft the ration four only. |

## Round 12: Judgment calls in content batch 1 (owner, 2026-10-23, from the review pack)

| Topic | Decision |
|---|---|
| 5.5 GTDSPCL narrative | The manual prints its "w.e.f." block twice; the slip prints it once. |
| Order number "----/----" (5.23, 5.24, 5.25) | Read as the order number and its date; printed "no. dated date" in the narrative and the Data column. |
| 5.25 RECALL From date | The day the officer actually left the leave station (note 3), not the date of the recall signal. |
| Ration Money Allowance certificate | Items 1 to 5 print; items 6 and 7 are conditional and left to the clerk. |
| R&H Data 3 | "Corps, notification no., date", comma-separated, in the column's order. |
| EXTNAL, VOLREJ, RECALL and the ration and R&H rows | Nothing drafted now (no new rows, no new warning); how to reissue them is decided in phase 8 with evidence, alongside CANCEL (6c). |
| VOLREJ and RECALL leave kind | The kind the officer picks prints, in place of the manual's "Annual/Furlough Leave". |
| Link limit | Raised in phase 9 with its reason recorded; no link-group feature. |
| GTDALCF cap | No 60-day cap check on a carry-forward (it is carried from earlier years). |
| GTDSPCL length | `warn` when a special casual leave is over 30 days (row 5.5 reads "upto 30 days"); never blocks printing. |
| Casual leave beside special casual leave | Only GTDCL beside the annual set is checked; special casual leave beside annual leave raises nothing. |
| Reissue warning on EXTNAL, VOLREJ, RECALL | None now (confirmed). |

## Facts established (2026-10-22)

- `part2_occurrences_2026.json`: 289 entries against 303 Annexure II serials (321 distinct HRMS codes: 8 serials
  carry variants, e.g. 3.53 GTDTRAN1–5). Missing 2.14–2.16, 3.25, 3.26 (`STPFIELD`), 3.28, 3.53, 3.67, 3.80, 3.97
  (`GTDHARDS`), 3.98, 3.99 (`GTDNTSSA`), 11.8, 13.12 (`BATTLCAS`). Column-wrap damage (3.27 code `GTDTLA1/G`).
  (An earlier count of 302 and "9.7 not found" were wrong: 9.7 `WTHDPAY` is on p.23, detail p.171.)
- Two serials have no group printed: 3.28 STPTLA and 11.8 REMAROF. Para 7.1 names a group "D" that is never defined.
- Rejoining from leave re-grants area allowances: HAA except casual leave under 14 days (p.49), field area
  allowances except casual leave or hospital under 15 days (p.55), SCCIA (p.53), transport allowance after an
  absence covering a whole calendar month (p.71). Leave grant is published within 24 hours of proceeding, and
  rejoining within 24 hours of return (para 6.3).
- Group B claims (3.96, 8.1–8.10) may not share an order with A/AM/C/CM; all occurrences of one event go in one
  order in date order (paras 7.1, 6.4).
- `SOSFRCRS` (cited in the analysis doc) is not in the manual.
- STPFIELD notes 2–3 (manual p. 56): the stop order is published every time an officer leaves a field area,
  including a move between field area types; "a mere grant of the new allowance will not suffice".
- NOKCHNG (11.4) is for remarriage after death of spouse or divorce only. A first marriage is `MARRIAGE` (11.1),
  or `MARSEROF` (11.2) when the spouse is in service or a government/PSU employee. The blueprint's
  "MARRIAGE + NOKCHNG" bundle is wrong.
- Part II orders cannot be amended: the order is cancelled and notified afresh (manual para 11).
- The previous edition was 2014; codes may differ, which matters for a later past-orders stage.
- Each occurrence has a fill-in-the-blank narrative and per-code date and Data 1–4 rules (Annexure III), so a
  draft is mechanical once the template is written; the text dump's tables wrap too badly to extract templates.
- REJOINAL, VOLREJ and RECALL must quote the Part II order no. that granted the leave (col 3 and 7).
- The unit, not the officer, fills the IAFF-3010 header (SUS No, order nos, formation, authority, signature).
- In the app today: a PDF builder on both platforms (`PdfLetterFormatter`, `PdfExporter`), an optional app lock,
  Android `allowBackup="false"`, and `.pcda` backups (v4) encrypted with a user password that already carry PDFs.

- The payslip's `Officer` holds only name, CDA A/c and a masked PAN: no Pers No, rank or arm. When no PAN is
  found, `ParserUtils.extractPanNumber` returns a placeholder `"AR*****90G"`, which must never reach a slip.
- ACCLVE's narrative is arithmetic (previous balance − LTC encashment + this year's accumulation, max 300), and
  ENCASH needs a year-wise break-up of accumulation over the whole service; Leave Bank parses both figures.

- A real HRMS order (Ver 2.60.15) prints the IAFF-3010 (Revised) layout, marked RESTRICTED, with a barcode of
  SUS No + order no./year + account digits, and the watermark "For Draft approval & personal filing only. Not to
  be used for PCDA(O) submissions". The authoritative order is a digitally signed XML; PCDA(O) accepts only
  digitally signed orders through the OASIS portal.
- One posting-out order held POSOUT (13.2), SORS (3.86), STPRH (3.12), RELAPPT (4.2) and STPTRAN (3.54) with the same
  date, under GROUP 'A'/'AM'/'C'. The manual's POSOUT notes do not list this set; note 2 says the TOS date at the new
  unit equals the SOS date at the old one.
- Every order's header quotes the officer's last Part II order no. and date; authority is printed per occurrence
  (only POSOUT had one in that order).

- Practice evidence from the owner's HRMS order summary (Sep 2024 – Oct 2026, one officer in an R&H station;
  patterns only, no identifiers kept):
  - Annual leave orders carried, in the same order as the grant: SORS + GTDRSHNA from the first day away, then
    TORS + STPRSHNA from the rejoining date (ration allowance during leave, manual-backed: ration notes 1.1, 1.5,
    3, 4), and STPRH from the first day away **including prefix**, re-granted (GTDRH12) from the rejoining date
    (not stated in the manual's R&H section).
  - A 9-day casual leave carried the ration four but no R&H stop.
  - HRMS published rejoining as code `REJOIN` ("Rejoined from CL/AL w.e.f. …, initially granted vide Pt II Order
    No …"). `REJOIN` is not in ODP-2026, which has only `REJOINAL` (5.23).
  - Prefix/suffix are printed as lists of dates ("prefix on dd/mm/yyyy, dd/mm/yyyy and suffix on …"), or "-" / "Nil"
    when none.
  - Posting in carried POSIN, JNTIME, GTDRSHNA (from TOS, through joining time), TORS + STPRSHNA and ASMAPPT and
    GTDRHxx (from end of joining time); GTDTRAN1 came in a separate order the next day.
  - Publication lagged badly: leave published 5–8 months after it was taken (para 6.3 asks for 24 hours).
  - A wrong order (LAPPROC) was cancelled with CANCEL and republished with corrected data (para 11 in practice).
  - Group B claims (REIMBCEA) were always single-occurrence orders.
  - ACCLVE for year Y was published in May of Y+1. Its total "as on 31/12/Y" equals the balance on the PCDA(O)
    December statement of Y+1 (Leave Bank), e.g. 282 as on 31/12/2023 ↔ Dec 2024 statement, 301 as on 31/12/2024
    ↔ Dec 2025 statement. The Leave Bank cross-check must compare ACCLVE(Y) with the statement of Y+1.
  - The latest ACCLVE put the uncapped total in line (a) instead of the previous balance: the published narrative
    does not always follow the manual's a − b + c = d template.

## Open

- None. Notification design was closed in Round 10.
