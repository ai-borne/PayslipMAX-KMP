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

## Round 13: Phase 3 storage (owner, 2026-10-23, asked at the start of phase 3)

| Topic | Decision |
|---|---|
| Phase 2b | Summary accepted. The owner has NOT yet checked the 24 templates against the printed pages (plan Phase 11 item 7), so 2b's exit is not met. Phase 3 touches no content, so it went ahead. (Closed later the same day: Round 15.) |
| Atomic restore and the DAO | `Part2Dao` holds all Part II reads and per-item writes. `PayslipDao` gains only the bulk inserts and clears for the three tables (the restore and "delete all" must share one transaction with everything else the officer made). It grew 204 -> 233 lines. |
| Backup MERGE | Match by id; the newer `updatedAt` wins; a tie goes to the backup. A page is immutable, so a page the device already holds is left alone. |
| Deleting | Deleting a draft keeps its orders. Deleting an order deletes its pages (done in one transaction in code, with no foreign key, because `INSERT OR REPLACE` on a parent row would cascade). |
| Lapsed Premium | Drafts, orders and pages stay viewable, deletable, backed up and restorable. The data layer is ungated; the Premium gate stays in the screens and Settings. |
| Vault size | Measured as the sum of stored page ciphertext bytes. Warning at 60 MB (Phase 6b builds the Settings line). A write that would pass the 100 MB ceiling is refused. |

## Round 14: Backup memory (owner, 2026-10-23, after the phase 3 summary)

| Topic | Decision |
|---|---|
| Problem | Measured with a full vault (120 pages of 500 KB, about 61 MB): backing it up needed about 0.8 GB of heap (iOS process peak 1.2 GB), because the archive was one JSON string holding every page as Base64. An Android app heap is 256-512 MB. |
| Choice | A chunked backup, built before the scanner (6a) can add any page, instead of lowering the 60 MB warning or accepting the risk. The warning and the 100 MB ceiling stay. |
| Acceptance | Phase 3 (8cb0a40b) and Phase 3b (32ca5316) accepted by the owner, 2026-10-23. The check of the 24 templates against the printed pages is still outstanding (plan Phase 11 item 7; closed later the same day: Round 15). |
| Shape | Backup v5 is unreleased, so its format changed: with scanned pages the archive is the encrypted metadata document followed by one binary record per page and an authenticated page count; a vault with no pages still writes the old single encrypted document. A restore reads one page at a time and re-encrypts each page only as it is written. |

## Round 15: 2b exit, the check against the printed pages (owner delegated it, 2026-10-23)

| Topic | Decision |
|---|---|
| Who and how | The owner asked Claude to do the check. It compared all 24 templates with the rendered ODP-2026 pages 46-48, 97-99, 114-118 and 131-132 (read as images, not from the text dump) and with Annexure II in the PDF text (serial, name, group), plus a fragment-by-fragment check of every narrative against the page text. |
| Result | No correction needed. Serial, code, name, group, page, narrative, From/To/Data 1-4 rules, certificates (RMA items 1-5, R&H items 1-3) and every note cited by a check or link (p.97 1.1; p.98 1.5, 1.8, 3, 4; p.99 8; p.115 notes 2, 7, 9; p.117 casual leave; p.131 and p.132 note 2) agree with the manual, apart from the settled Round 11 and 12 choices. |
| REJOINAL (5.23) order no. | The manual is inconsistent: its row puts the grant order no. in Data 3, its note 2 says "column 7" (Data 2). Keep Data 3, as the row says (owner). |
| NCC and re-employed officers | Note 3 (p.115) limits them to 30 days of annual leave a year, and note 13 sets the retirement-year entitlement (30, 58/59 or 60 days). The 60-day check does not know either. Handled in phase 8 with the rest of Chapter 5 (owner). Plan Phase 11 item 11. |
| Noted, kept | VOLREJ prints a full stop after "cancelled"; row 5.24 has none (row 5.25 has one). Punctuation only. The review pack's citation text reads "p.117 note note under 5.6" because the stored note already begins with "note"; the data is right, the wording is cosmetic. |

## Round 16: Phase 4a, the lookup UI (build-time choices, 2026-10-23; no owner questions were open)

The plan and Rounds 3, 5, 8, 10 and 15 settled everything 4a needed, so no question was asked. These are the choices made while building, for the owner
to see on the device walk and change if they disagree. The Pixel 9 walk method is the owner's (2026-10-23): back up first, then walk the debug build.

| Topic | Choice |
|---|---|
| Where search lives | On the Part II home, under the title (Round 5: "searches or taps the Leave ribbon"). Not in the Guide's own search. Typing replaces the ribbons with matching codes; coming back from a code page keeps the query; each entry from Guide Home starts empty. |
| What a search matches | Every word must match: a whole serial (`5.1`, never `5.10`), a code (exact first, then by its start; any of a variant serial's codes) or a word start in the manual's name. Case never matters. Two letters or digits are the minimum. It reaches all 303 serials, built ribbon or not. |
| Code page | The minimal fields of Round 3: HRMS code(s), the manual's name, the group with what it means (or "Group not printed in ODP-2026", nothing inferred), serial, chapter and printed page. REJOINAL's page says the slip prints REJOIN (Round 9). |
| Tiles | Home lists the built ribbons (Leave, Records). A ribbon lists its cases with the chapters and pages their codes cover ("Ch 5, pp 114-117") and a code count. A case lists its codes, marks the lookup-only ones, and shows the codes listed from elsewhere under "Also see". |
| Entry | Free for everyone; absent while `PART2_ENABLED` is off or when the bundle has no Part II section. A restored Part II place under a flag-off build is cut back to Guide Home. |
| Reuse over new look | No new colour or component: the Guide's tile, pill and back header. |
| Acceptance and the tab wiring | Phase 4a accepted by the owner, 2026-10-23 (19416f6e, 34c2f296). Asked whether `GuideTab.kt`'s growth (257 -> 265) should be accepted or fixed: the owner chose to fold the pre-claim and Part II hosts into one `GuideHosts` holder now (Phase 11 item 13, closed). |

## Round 17: Phase 4b, the leave draft flow (owner answers, 2026-10-23, asked at the start of phase 4b)

Four things the plan did not settle were asked in one batch; the owner took the recommended option each time. The rest was settled by the plan and Rounds 5, 7, 9, 10, 11, 13 and 16.

| Topic | Decision |
|---|---|
| Where drafts live | A "Your drafts" section on the Part II home, above the ribbons, shown only when the device holds a draft. Each row opens the draft's slip; each row has "Delete draft" with a confirmation. The Leave cases carry only "Draft this order" rows. |
| More than one occurrence | The form starts with the chosen code and offers "Add another occurrence", which lists the codes this version drafts (the 8 leave and rejoining codes). Each added occurrence is its own section and can be removed; one always stays. Covers two leave years, AL plus EXTNAL, and REJOIN with its grant, in one order (para 6.4). |
| `ack` findings | Shown now. An `ack` finding is a prominent card with its citation and a tick box ("I have read this. The order may be rejected."); a `warn` is a plain note with its citation. Ticks live in the model, are never saved, and any edit clears them. The state says whether the slip is ready to print (no `ack` unticked); Phase 5 only adds the Print button. |
| Prefill | Area details only, as the plan says (station, corps, notification, serial from the newest draft of the same R&H code). The ration answer, the R&H code and every date start empty. Built as: choosing an R&H code fills the inputs that choice newly asks for (the stop's station, the grant's corps, notification and serial) from the newest other draft that chose the same code, only where empty. |

Phase 4b was accepted by the owner on 2026-10-23 (bace0c4b, 1ff29b95), after the Pixel 9 walk, with the build-time choices below as built.

Build-time choices made without a question (change them if you disagree):

| Topic | Choice |
|---|---|
| Free user | The case rows carry "Draft this order" marked Premium; a tap opens an upgrade panel (the TA/DA worksheet's pattern). Nothing is read or stored. |
| Lapsed user | A user with no entitlement who still holds a draft: they see "Your drafts", read each slip (read-only, with a note) and delete; the case rows still lead to the upgrade panel; the form is never drawn for them. No "was Premium" flag is stored: holding a draft is the test. |
| Android free launch | `hasAccess(PART2_ORDERS)` is true while `FREE_LAUNCH_MODE_ANDROID`, so Android drafts for everyone until it ends; "Force Free" reaches the locked and lapsed states. |
| Autosave | From the first change, one write 300 ms after the last (never one per key, because every save re-derives the key); leaving the form or a lock writes a pending change at once; nothing is saved until something is typed, so opening and backing out leaves no draft. |
| Last Part II order | The vault order with the latest date among those with a number the slip can quote and a real date (`VaultOrders.lastOrder`); blank when none. Read live, so a later vault change updates the preview. |
| Opening a saved draft | From the list it opens on the slip; "Edit" goes to the form (entitled users only). |
| Gone draft | A restored stack naming a draft the repository no longer holds is cut back to the Part II home once the repository answers. |

## Round 18: Phase 5, print and "Check the HRMS draft" (owner answers, 2026-10-23, asked at the start of phase 5)

Four things the plan did not settle were asked in one batch; the owner took the recommended option each time. The rest was settled by the plan and Rounds 5, 7, 9, 10, 11, 16 and 17.

| Topic | Decision |
|---|---|
| Slip layout | Blocks per occurrence, as in the on-screen preview: the code in bold, the narrative at full width, then From/To, Data 1-4, the authority line and the certificate on their own lines. Not the manual's table of columns (on A4 portrait the narrative column would get about 4 cm and the long certificates cannot sit in a cell). |
| Certificate | Printed exactly as the manual words it, with no label of ours in front. Every shipped certificate already starts with the word CERTIFICATE, so the row no longer reads "Certificate: CERTIFICATE: ...". The preview, the paper and the HRMS check share the change. |
| What counts as printed | The system reports the job completed. Android: the `PrintJob` the print manager returns is watched and only `COMPLETED` records (Save as PDF completes within a second); a cancelled or failed job does not, and the watch gives up after 30 minutes. iOS: the print sheet's `completed` flag. A printer that never reports back records nothing and the officer can print again: the cost is a missing reminder, never a wrong one. |
| After printing | The snapshot (time, template revision of every code, rows as printed) is kept through every edit. The preview shows "Printed on <date>" and, when the rows now differ, "Changed since it was printed" or, when a code's template revision moved, "The manual's template for this order has been updated since it was printed". A new completed print replaces the snapshot. |

Build-time choices made without a question (change them if you disagree):

| Topic | Choice |
|---|---|
| What the paper carries | A title, the last Part II order line, one notice ("Draft for your clerk to key into HRMS. It is not a Part II order and carries no name or number of yours."), then each order with its heading and rows, and a page label at the foot of each page. Not on the paper: the checks, the "seen in practice" or manual citations, the ticks, the list of orders not drafted yet. Those are for the officer. |
| Print job name | Fixed: "Part II slip". The system keeps it (the print queue, the saved PDF's name), so it names no code, date or number. |
| Check the HRMS draft | Reached from the slip preview only: the drafts list opens the slip, so that is the one way in. A level of its own (`part2|hrms|<id>`, ids only), one card per row, each field with its label in HRMS column order (code, narrative, From, To, Data 1-4, authority, certificate), columns the manual leaves empty not listed. No ticks, nothing saved. It is a read, so someone whose Premium ended still has it; Print is for entitled officers only. |
| Printing and reminders | The printed time starts the Phase 7a "add the published order" reminders. A reprint replaces it, so the reminders run from the latest print. |
| The day shown | First built as the UTC day the drafts list used for "edited"; the owner accepted that for now, then asked (2026-10-24) for the open items to be fixed. Now the day where the device is: "Printed on" and the drafts list's "edited" use the device's offset at that instant (plan Phase 11 item 17, closed). |

Phase 5 was built, walked on the Pixel 9 and gated on 2026-10-23/24 (32c9617c, ad91c255) and accepted by the owner on 2026-10-24, who then started Phase 6a.

## Round 19: Phase 6a, the page pipeline (build-time choices, 2026-10-24; no owner question was open)

The plan and Rounds 5, 7, 10, 13, 14 and 18 settled everything 6a needed, so no question was asked. These are the choices made while building, for the owner to change if they disagree. The spike ran on the Pixel 9 with the owner's help (plan Phase 11 item 19).

| Topic | Choice |
|---|---|
| Spike result | Passed: ML Kit Document Scanner 16.0.0 adds no `CAMERA` permission, no `uses-feature` and no exported component; it scans with the phone offline once its module is on the device; pages are `file://` JPEGs in `<app cache>/mlkit_docscan_ui_client/` and are never deleted by ML Kit. A phone that has never scanned, offline, was not tried (item 19). So the CameraX fallback was not needed. |
| ML Kit | `play-services-mlkit-document-scanner` 16.0.0 (published 2025-09-29, well past two weeks; no advisory found), pinned in `libs.versions.toml`. Options: gallery import off, JPEG only (no PDF), page limit = the room left in the order, `SCANNER_MODE_FULL` (the scanner's own cleaning, as walked on the device). The scanner's code is delivered by Google Play services outside the app binary. |
| iOS scanner | VisionKit's document camera in a Swift `DocumentScanPresenter` registered at startup (the crash reporter's pattern); Kotlin reaches it through `IosDocumentScannerDelegate`. Pages go to Kotlin as in-memory JPEG data (quality 0.9, re-encoded again by the shared encoder); nothing is written to disk. VisionKit has no page limit, so Kotlin drops pages beyond the room left and counts them. This needed one entry on the closed allow-list of `stateful_singleton_guard.py` (`IosPageScanner.Companion`, a Swift-set delegate like `IosCrashReporter.Companion`). |
| Page rules | Long edge at most 1600 px; JPEG quality 85 stepping down by 5 to a floor of 40, the first that fits 500 KB, judged on the bytes that are stored; a page that does not fit at the floor is refused, not degraded further. A scanner page is read only up to 25 MB and decoded only up to 100 megapixels. All in `Part2Limits`. |
| Metadata | Everything between the start marker and the picture except the JFIF header, the quantisation and Huffman tables and the Adobe segment is cut (Exif, GPS, XMP, IPTC, comments, and any bytes after the end marker), by a byte-level cut shared by both platforms. Side effect: the colour-profile segment goes too (item 24). The Exif orientation is applied to the pixels first, so pages are upright. |
| Where a page lives | One place, `PageCapture`, holds a page in plaintext; it deletes the scanner's copy of each page as soon as it is stored and on any failure or cancel, and overwrites its byte arrays. The Android scanner also sweeps its folder before a scan, on cancel and on failure; `discardLeftovers()` (called by 6b when the vault opens and on lock) clears what a killed process left. |
| Camera prompt (iOS) | "PayslipMax uses the camera only when you scan a published Part II order into your vault. Pages are stored encrypted on this device and are never added to your Photos." in `Info.plist`, equal to `Part2Strings.cameraUsageDescription` (a test). Apple reviews this text; change it freely. No other privacy key. |
| Manifest guard | `scripts/check_merged_manifest.py` and a committed baseline with a reason per entry, in CI and the pre-push gate. The baseline today is the release manifest before ML Kit; ML Kit changes none of its entries. |
| Deferred to 6b | The words and screens for every capture outcome, the entitlement gate on capture, `discardLeftovers()` calls, the 60 MB warning (item 23). |

Owner answers, 2026-10-24 (asked after the 6a commits, one batch; each took the recommended option):

| Topic | Decision |
|---|---|
| First scan on a phone that has never scanned, offline (item 19) | Accept. No pre-download. Phase 6b words the message for `Failed(UNAVAILABLE)` ("needs a connection once"), part of plan Phase 11 item 23. |
| Colour profile (item 24) | Leave it dropped. Revisit at the 6b walk only if a coloured stamp or seal looks wrong. |
| Scanner mode | `SCANNER_MODE_FULL`, as walked on the Pixel 9. |
| Phase 6a | Accepted as built, 2026-10-24 (76404e21, 1b05efea), including the one allow-list entry in `stateful_singleton_guard.py` (`IosPageScanner.Companion`). |

The camera prompt text and the quality ladder (85 down to 40) stay as built; change them freely.

## Round 20: Phase 6b, the vault UI (owner answers, 2026-10-25, asked at the start of phase 6b)

Three things the plan did not settle were asked in one batch; the owner took the recommended option each time. The rest was settled by the plan and Rounds 5, 7, 10, 13, 14, 17, 18 and 19.

| Topic | Decision |
|---|---|
| Where the vault is reached | A "Your published orders" section on the Part II home, under "Your drafts", with "Scan a published order" for an entitled officer. It shows for anyone who holds an order (so a lapsed officer still reaches view and delete). Orders can be scanned with no draft, which is the only way an officer who never drafted here gets a "last Part II order" line. "Add the published order" on a slip also scans. There is no separate Part2Vault level: the section is the vault, and `Part2Destination.Order(id)` is the one new level. |
| Linking | Set when the order is scanned from a draft's slip (an order scanned from the vault starts unlinked). The order screen shows "Open the draft" while the draft exists; the slip lists its linked orders. Relinking was first left out; Round 21 added it. |
| Grant order on a follow-up | The officer picks it: the grant box gains "Use an order from my vault", a list of numbered, dated orders (newest first). A pick fills both boxes, which stay editable. Nothing is filled in silently. Only the `grant` box has it (the Corps notification and the sanction signal are not vault orders). |

Build-time choices made without a question (change them if you disagree):

| Topic | Choice |
|---|---|
| Order no. and date | The draft form's own date box and the order-reference bound (`ORDER_NO_MAX` 40, plain text): a number with `<` or `>` is stored as typed and named, never quoted. Saved as typed (one write 300 ms after the last key, through one shared `AutoSaver`, which the draft form now also uses). |
| Viewing a page | One page at a time, on request ("View page N" / "Hide page"). It is decrypted only inside the call that builds the picture, the bytes are zeroed when that call returns, and the picture is dropped on hide and when the screen leaves composition (the app lock included). Never to a file, the gallery or the clipboard. |
| Delete | Asks first, like a draft. An order's delete removes its pages; a draft it came from is not touched. A single page could not be deleted on its own at first (Round 21 added it). |
| A scan that saved nothing | Leaves no order: the empty order made for the pages is deleted, and an order a killed process left with no page and nothing typed is removed when the vault next opens. |
| Lapsed Premium | Reads and deletes; cannot scan, add pages, type or change anything (the data layer stays ungated; the gate is in the host). |
| What a scan says | Words for every outcome: needs a connection once (offline, a never-scanned phone), the scan did not work, order full (10 pages), vault full, partly saved with the count, order gone. A cancelled scan says nothing. |
| Settings | One line, only above 60 MB (rounded up, with the 100 MB ceiling), in the Data management card. |
| Restore | "Restore Complete! N items could not be restored." in the warning colour when Part II rows were left out. |

## Round 21: Phase 6b follow-ups (owner, 2026-10-25, after the 6b summary)

The owner asked for the open items to be fixed in the same sprint and, asked which, chose all four.

| Topic | Decision |
|---|---|
| Delete one page | Built. "Delete page" beside each page, asking first; for an entitled officer and for one whose Premium ended (a delete, as Round 13 allows). An order's only page takes the order with it, and the confirmation says so. Not while a scan is open. |
| Picker on the carry-forward order | Built. "Order allowing the carry forward" (GTDALCF) offers the vault like the grant boxes. The Corps notification stays out (it is not a Part II order); a test names the one reference box left out, so a new one must be decided. |
| Linking later | Built; reverses Round 20's "link at scan time only". An entitled officer can link an order with no draft to one of their drafts ("Link to a draft", a list of drafts) and unlink one ("Unlink from the draft"). An order whose draft is gone shows only "Unlink". |
| Template revision (plan Phase 11 item 6) | Recomputed in the app: on every load the validator hashes each template (SHA-256 of the shipped template as key-sorted compact JSON, first 8 hex, the Python tool's own rule) and refuses a template whose `rev` is not its own. Pinned against the tool by two known-answer tests (a sample with quotes, a backslash and accented characters; one with every control character) and by all 24 shipped templates. The fixtures keep readable revisions for the tests that print them; the validator suites use sealed copies. |

## Round 22: Phase 6c, CANCEL and CANCELS (owner answers, 2026-10-25, asked at the start of phase 6c)

Phase 6b was accepted by the owner on 2026-10-25 ("accepted & proceed"). Three things the plan did not settle were asked in one batch; the owner took the recommended option each time. The rest was settled by the plan and Rounds 5, 7, 9, 10, 11, 12, 13, 15, 17, 18, 19, 20 and 21.

| Topic | Decision |
|---|---|
| "Cancel and re-draft" | Offered only on an order linked to a draft the device still holds. One tap makes the CANCEL slip (the order's number and date filled in) and a corrected draft copied from the linked draft (its codes, dates and answers; not the print record, ticks or last-order line), and opens the cancel slip. The two drafts point at each other ("Open the corrected draft" / "Open the cancel slip"); both stay editable; the old draft and the order are untouched. Either both drafts exist or neither does. An order with no linked draft gets "Cancel this order" only. |
| What the vault says of a cancelled order | Worked out, never stored (so no payload, schema or backup change). The order's screen, its row in the vault list and its row on the slip it came from show "Cancel slip printed on <date>." once a cancel slip quoting its number and date has been printed, and "Cancelled by order <no.> dated <date>." once the officer has scanned the published cancel order and linked it to that cancel slip. A slip that cancels one occurrence says so ("A slip cancelling one occurrence was printed on ..." / "One occurrence cancelled by order ..."); a CANCEL outranks a CANCELS. Phase 7a reads the same answer (`CancelStatus`) to stop reminders. |
| Sharing a slip | CANCEL and CANCELS share a slip only with each other (several CANCELS for several occurrences of one order). "Add another occurrence" offers the codes of the ribbon the slip's first code belongs to, read from the layout, so a Leave slip never offers a cancellation and a later batch needs no code. |

Build-time choices made without a question (change them if you disagree):

| Topic | Choice |
|---|---|
| Form | CANCEL and CANCELS are drafted in the same form as the Leave codes (the form is derived from the template), with no flow of their own. They start from "Cancel this order" on a vault order, which needs the order's number and date typed first (otherwise the screen says so), and from the Records case "Cancelling a published order". Only an entitled officer sees the offer (it makes drafts); a lapsed officer reads and deletes. The order being cancelled can be picked from the vault or typed. |
| Which occurrence a CANCELS covers | The officer types the occurrence's code and the serial it carries in the published order. The vault keeps no occurrence rows, and the serial HRMS published need not equal the row's place in the draft, so nothing is guessed. |
| Columns and wording | As the manual prints them (pp.226-227, notes 2, 3, 5, 7): CANCEL puts "ALL" in col 6, nothing in col 7 and the order's number and date in cols 8 and 9; CANCELS puts the occurrence code in col 6, the serial in col 7 and the order in cols 8 and 9; From and To are not required. The certificate prints as the manual words it, with the order and serial filled in and no full stop. The order is quoted in the narrative too (note 2). |
| The code's name | Para 12.3 (p.8) misprints the one-occurrence code as "CANCELCS". The annexure (p.227), the code list and HRMS say CANCELS, so the slip prints CANCELS. Listed in the review pack. |
| Time bar | A `warn`, never an `ack`: notes 4 (p.226) and 6 (p.227) say the sanction "may be enclosed", and an `ack` is kept for "will be rejected" (Round 11). The check fires when the order being cancelled is more than 2 years older than the day where the officer is (29 February turns 2 on 28 February); it judges nothing without a quoted order, a real date or a device date. Para 13 counts the 2 years from the occurrence, which the app does not know, so the copy says the occurrence may be older still. The engine stays pure: the device's day is handed in. |
| Not quoted | A cancellation with no order yet still drafts and prints blanks for the clerk, with a `warn` (p.226 note 2, p.227 note 2), as a follow-up with no grant order does. |
| The link between the two drafts | A draft id in an optional field of the encrypted draft payload (`v: 1`), bounded like every id on the way in and out. A deleted partner hides the button; the stored pointer is left. A restored backup keeps it. |
| Lapsed Premium | No cancel offer and no hint (creating a cancel makes drafts); the status line is shown, because reading is open (Round 13). |

## Round 23: Phase 6c follow-ups (owner, 2026-10-25, after the 6c summary)

The owner asked for the open 6c items (Phase 11 items 27-30) to be fixed in the same sprint. Two needed a decision; the owner took the recommended option each time.

| Topic | Decision |
|---|---|
| Time bar from the occurrence (item 29) | Built. Both templates get an optional "Date of the earliest occurrence" box that only the time-bar check reads and nothing prints (the 60-day box's pattern). Built as: the check counts from the earlier of that date and the date of the order being cancelled, since an order is never published before its occurrence (so a young order with an old occurrence warns, and an old order warns whatever was typed); with neither box it judges nothing, as before. It never blocks a slip. |
| A typed CANCELS code (item 30a) | Built as a warning only. `cancels_code_unknown` (p.227 note 5, where col 6 carries the occurrence code) fires when the typed code, trimmed, is not a code the manual lists or the slip prints (REJOIN), compared as typed (HRMS codes are capitals). The code prints as typed. |

Built without a question: the corrected-draft button follows its partner live (a partner deleted or saved after the slip loaded shows or hides it at once, item 30b); "Cancel and re-draft" is not offered when the order's linked draft is itself a cancel slip, and the host says why if asked (item 30c).

The walk on the Pixel 9 (item 28) found one thing to change, built without a question: a finished cancel slip has no dates, so the drafts list called it "Not finished"; it now says "Ready" (a draft whose answers are complete and has no dates) and keeps "Not finished" for one still being filled in.

Item 27, the check against the printed pages (the owner delegated it, as in Round 15): both templates were compared with pp.226 and 227 read as images and with the page text. Serial, code and group A; the narratives (CANCEL: "Cancelled all occurrence of Part II Order Number ... dated ..."; CANCELS: "Cancelled occurrence at Ser No ... of Pt II order No ... dated ..."); cols 4 and 5 not required; CANCEL Data 1 "ALL", Data 2 not required, Data 3 and 4 the order's number and date; CANCELS Data 1 the occurrence code, Data 2 the serial, Data 3 and 4 the order; both certificates; and notes 1-4 (p.226) and 1-7 (p.227), including note 7 naming the code CANCELS: all agree, no correction. This is a check by Claude, not the owner's own read; the owner's review of `part2_review.md` stays welcome, and the exit is recorded as "checked, not yet read by the owner".

## Round 24: Phase 6c accepted (owner, 2026-10-25)

The owner accepted Phase 6c as built, with its follow-ups (Round 23) and the walk fix (216b14c5, 42b5a46a, d5999a23): "I accept 6c". Still open from it: the owner's own read of `part2_review.md` (plan Phase 11 item 27, checked by Claude but not read by the owner), the 7a hand-off (item 31) and the iPhone walk (items 14 and 20). Phase 7a is next.

## Round 25: Phase 7a, the reminder planner and the in-app Due list (owner answers, 2026-10-25, asked at the start of phase 7a)

Three things the plan and Rounds 5-24 did not settle were asked in one batch; the owner took the recommended option each time. The rest was settled by Rounds 6, 10, 13, 17, 18, 20, 22, 23 and 24 and the plan's Phase 7a and 7b sections.

| Topic | Decision |
|---|---|
| The card after a payslip import | A card at the top of the Dashboard, shown after a payslip import completes while something is due now. Tapping "See what is due" opens Guide, Part II, Due. "Not now" hides it until the next import; it is held in memory only, so nothing is stored (no payload, schema or backup change) and a new launch brings it back. A split of `App.kt` (297 lines) was the price: `DetailContent` moved into `AppDetailContent.kt` unchanged, leaving `App.kt` at 254 lines. |
| When "draft the rejoining slip" stops | When any draft holds a rejoining request (the rejoining, voluntary rejoining and recall codes) dated from the leave's first day up to its return day, inclusive. Worked out each time, never stored. Until then the item stays on Due, marked overdue after its day. |
| After the sixth "add the published order" | Each reminder is current until the next one; the sixth stays current for 30 days after its day, then the item leaves Due. The draft and its slip are untouched, and the order can still be added from the slip. |

Build-time choices made without a question (change them if you disagree):

| Topic | Choice |
|---|---|
| Where Due lives | Its own level, `Part2Destination.Due` (`part2|due`, no id), reached from a "Due" row at the top of the Part II home. The row shows only for an entitled officer while the planner has something to show ("N things are due." or "Nothing is due right now."). A row on Due opens the draft's slip, which is where the order is added, printed or edited. |
| Who sees it | Reminders are Premium (Round 10): the row, the card and the list are for an entitled officer. A restored Due level for anyone else shows the upgrade panel, never the list. The planner and the repository are ungated (Round 13); drafts, orders and the vault stay readable and deletable on their own screens. |
| Printed | A draft sets reminders only once a print job has completed (it has a print snapshot), in line with Round 18: a missing reminder costs less than a wrong one. A reprint restarts "add the published order" from the latest print. |
| Published | A vault order linked to the draft with a quotable number and a real date (`VaultOrders.quotable`). A scanned order whose number or date is not typed yet does not stop the reminder, because the point of the reminder is a number and date to quote. |
| Return date | The day after the later of the last day of leave and the last suffix day, from the annual and casual leave grants (those that carry a last day of leave), not from the rejoining, voluntary rejoining or recall codes. One reminder per draft; two leave years in one draft use the later return. |
| Year end | A printed annual-leave grant of leave year Y gives "draft the year-end accumulation for Y" on 1 March of Y+1, until a draft holds that year-end request for Y (printed or not). ACCLVE has no template until Phase 8, so the request is named by role in `ReminderRules` and found by the `year` slot every annual template shares; Phase 8's template must use that slot (Phase 11 item 33). |
| Cancelled | A draft whose linked, quotable order is cancelled in full (scope ALL in `CancelStatus`, a printed cancel slip or the published cancel order) is void: it sets no reminder and answers none. A cancel of one occurrence (CANCELS) stops nothing. The corrected draft is an ordinary draft. A published order already ends "add the published order", so a cancel adds nothing further to that one reminder (Phase 11 item 31). |
| Time | Every reminder is at 09:00 where the officer is (one hour, `ReminderPolicy.hour`; Round 10 gave 09:00 for the return date only). The day is the day where the device is (`localUtcOffsetMillis` at that instant); the planner is handed the clock and the offset. A zone that changes its offset on the day is followed. |
| The plan | `due` is what the officer has to do now, oldest first, each marked overdue once its day has passed; `upcoming` is the nearest 20 future reminders, which is all a scheduler (7b) needs. The Due list shows a repeating reminder once, as its next date, and not again under "Coming up" while it is due. |
| Stale items | Nothing is dismissed or expires: a return or year-end reminder stays until it is answered or its draft is deleted (Phase 11 item 32). |

## Round 26: Phase 7a follow-ups, closing the sprint's open items (owner, 2026-10-25, after the 7a summary)

The owner asked for the open 7a items (Phase 11 items 32-36) to be closed in the same sprint, and answered four questions in one batch, each as recommended.

| Topic | Decision |
|---|---|
| Stale items (item 32) | Built. Each item on the Due list, including the ones coming up, has a "Dismiss" button. A dismissal is remembered with the draft (an optional `dismissed` list inside the encrypted draft payload `v: 1`: no schema or backup version change, no new `@Serializable` class, so the R8 gate is unchanged) and stops the whole series of that kind for that draft: the list and what a scheduler is given. |
| An extended leave (item 34) | Built in the planner now. A printed leave that starts on or before another leave's return day and returns after it takes over that return reminder: the officer is still away, and the later leave asks in its turn. Chains work. A duplicate slip for the same days, a leave that ended before that day, an unprinted draft and a leave whose order was cancelled in full do not take over. |
| The Due screens over Room (item 36) | Built: a composeApp test runs the list, a dismissal and the Dashboard card over the production schema and the real encrypted repository, including closing and opening the database. It needed Room and the SQLite driver on composeApp's test classpath only (the app already ships Room through `shared`). |
| The card on a real import (item 35) | The owner imports a payslip on the Pixel 9 and the card is driven then (see the plan). |

Built without a question (change them if you disagree):

| Topic | Choice |
|---|---|
| What a dismissal is called | A task name per kind: `return`, `add-order` (every repeat of the series) and `year-end-<year>`. A year end is one reminder per leave year across drafts, so a dismissal on any draft that stands covers it; a dismissal on a draft whose order was cancelled in full does not count. Names are plain tokens, at most 32 per draft, bounded on the way in and out like every stored list. |
| Undoing it | A completed print of the draft clears its dismissals, because a print starts the reminders afresh (Round 18); a print that did not complete clears nothing. There is no separate "undismiss" button. |
| What it writes | Only the `dismissed` field. The draft's edit time, answers and print record are untouched, so it does not read as "edited" in the drafts list. |
| Who can | Dismiss is on the Due list, which is for an entitled officer only (Round 25). |
| No confirmation | Dismiss acts at once: it hides a reminder and deletes nothing. |

## Round 27: Phase 7a accepted (owner, 2026-10-25)

The owner accepted Phase 7a as built, with its follow-ups (Rounds 25 and 26) and the Pixel 9 walks (aceb6dba, 5d2de477 and the docs commits 99000afa and 642ce3c9): "I accept phase 7a". Still open from it: Phase 11 item 33 (Phase 8's ACCLVE template must use the `year` slot and `ReminderRules.YEAR_END_REQUEST`) and item 35 (only the light theme, accessibility and the iPhone are not walked), besides the older items 8, 11, 14, 20, 22 and 27. Phase 7b (OS notifications on both platforms) is next.

## Round 28: Phase 7b, OS notifications on both platforms (owner answers, 2026-10-26, asked at the start of phase 7b)

Three things the plan and Rounds 5-27 did not settle were asked in one batch; the owner took the recommended option each time. The rest was settled by Rounds 6, 10, 13, 18, 25, 26 and 27 and the plan's Phase 7b section.

| Topic | Decision |
|---|---|
| When the permission is asked | After the first completed print (Round 25: a draft sets reminders only once a print has completed), the first time the officer is in Part II with a reminder: our own one-line explainer ("Allow reminders" / "Not now"), then the system dialog. "Not now", tapping outside and going back are a decline. Either way it is never asked again; the Due list still works. A device with nothing to ask (Android before 13, or an iOS authorization already decided) is settled silently. |
| Where the "asked" flag lives | A device-local preference (`ReminderPromptStore`: private SharedPreferences on Android, `NSUserDefaults` on iOS). Not Room (schema stays 15), not the `.pcda` backup (a restore on a new phone asks again, which is right: the permission is the device's), no new `@Serializable` class. It is written before the system dialog opens, so a kill while the dialog is up cannot lead to a second ask. |
| What the notification says | One fixed sentence for every reminder: title "PayslipMax", body "A Part II reminder is due." It says nothing about the kind, so a lock screen shows nothing about leave or orders. The kind and the draft id travel in the tap link only, for the check. |

Built without a question (change them if you disagree):

| Topic | Choice |
|---|---|
| Android delivery | WorkManager 2.11.2 (pinned, published 2026-03-25, no advisory found), inexact: "09:00" means "around 09:00", and Doze or battery saver can delay it. Exact alarms were not used: they add a permission Play restricts. WorkManager starts on demand (its automatic initializer is removed from the manifest and `PayslipApplication` supplies the configuration), so a build with Part II off starts exactly as before. |
| The worker's input | Only the reminder kind and the draft id, read as untrusted text. The words are not stored in the work request: they are the injected `ReminderNotificationText` (built from `Part2ReminderStrings`, bound in Koin), which resolves the plan's "worker input holds an id only" against "platform code receives text by injection". |
| Who decides at fire time | The worker asks `ReminderFireCheck` (the planner, over the drafts and orders as they are then) whether that reminder is still due, so an answered, dismissed or deleted one stays silent even if the app was not opened to rebuild. It does not check Premium: a lapse is noticed the next time the app opens (Phase 11 item 38). |
| Premium lapses, or Part II off | `ReminderSync` gives the scheduler an empty list, so everything queued is cancelled and nothing is scheduled. Following the entitlement is `PayslipViewModel.hasAccessFlow`, the same verdict as `hasAccess`. |
| When the schedule is rebuilt | At app start (`MainActivity`, the iOS nav host), and on every change to the drafts, the orders or the entitlement (a completed print, a dismissal, an added order, a deletion). An edit that moves no reminder gives the scheduler nothing. Not started from `Application.onCreate`, so a worker's cold process never cancels the job that is running. Stale ones are removed by cancelling every queued job and queuing the planner's list again by `Reminder.key`; a running job is never cancelled. |
| Boot, time zone | iOS keeps its pending notifications across a restart itself. On Android the first walk showed a restart drops every queued job (7 -> 0, the app never opened), so a non-exported `ReminderBootReceiver` (BOOT_COMPLETED only) rebuilds the schedule once from the drafts (`ReminderSync.rebuildOnce`; it has no billing, so it schedules as for an entitled officer until the app opens: item 38). Proved after a real restart: 7 jobs, app never opened. A change of time zone is not followed until the app is next opened (a new receiver would add a component): Phase 11 item 38. |
| Tap | Explicit, immutable PendingIntent to our own activity with the kind and the draft id; iOS user info with the same two strings. Both go as text to the Koin-scoped `PendingPart2Link`, which keeps only a known kind and a plain id and checks the database when taken. Locked: held until unlock. Unlocked: Guide, Part II, Due, with every pushed detail closed first (`NavBridge.popToRoot` on iOS). |
| iOS | 20 requests (the planner's cap) against the system's 64; the foreground shows a banner with no sound. |
| Debug walk aid | A debug-only Settings switch brings every reminder to 20 seconds from now (`ShortDelayReminderScheduler`, the `DevOverride` pattern: in memory, inert unless `isDebugBuild()`). |
| Closing questions (owner, 2026-10-26, asked after the build) | Each took the recommended option. (1) No in-app note after a decline: strictly "Due list only, never asked again" (Phase 11 item 39 keeps it for a later request). (2) A Premium lapse is noticed when the app next opens; the worker does not check Premium (item 38). (3) The `IosReminderBridge.Companion` entry in `scripts/stateful_singleton_guard.py` is approved (item 41, closed). (4) The Pixel 9 walk comes before the 7b commit. (5) A change of time zone is not followed until the app next opens (item 38). (6) WorkManager stays on 2.11.2; 2.12.x waits for a quiet release with its own R8 and manifest check. (7) The iPhone walk closes with the Phase 10 walk, items 14 and 20. (8) The Play Console declarations and privacy-policy wording for the new permissions are Phase 10 (item 40). |
| Permissions | `POST_NOTIFICATIONS` (declared by the app) and `RECEIVE_BOOT_COMPLETED` (WorkManager), plus two exported WorkManager components guarded by system-only permissions (`SystemJobService`, `DiagnosticsReceiver`): each a reviewed entry in `scripts/merged_manifest_baseline.json` with its reason. The Play listing will show the new permissions. |

## Round 29: Phase 7b accepted (owner, 2026-10-26)

The owner accepted Phase 7b as built (6421b5bb): the notification flow on both platforms, the boot receiver, the three faults the Pixel 9 walk found and their fixes, and the accepted limits of Phase 11 item 38. Still open from it: Phase 11 items 14 and 20 (the iPhone walk, now including the notification checks), 39 (no in-app note after a decline, by the owner's choice), 40 (the Play listing wording, Phase 10) and 42 (the worker proof and a cold worker on the real flag-on release, Phase 10), besides the older items 8, 11, 22, 27, 33 and 35. Phase 8 (rest of Chapter 5, incl. ACCLVE and ENCASH) is next.

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
