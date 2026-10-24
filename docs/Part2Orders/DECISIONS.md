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

Phase 6a was built, walked and gated on 2026-10-24; the owner's acceptance is recorded in the commit that follows it.

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
