# Part II Orders Bank — Phase Plan

Status: draft for owner sign-off, 2026-10-30. Nothing is built. This replaces Phase 13 and the rest of
`docs/Plan/12_Part2Orders_PhasePlan.md`, which is frozen as an archive (tag `archive/part2-orders-2026-10-30`, see Phase 0).
Branch: `feature/orders-bank`, created in Phase 0 from `main` (676ce2d2 on 2026-10-30; re-checked before branching).

## What it is

A wiki and a personal index, not an HRMS. The clerk keeps HRMS. The officer learns the 303 ODP 2026 casualties, notes the ones that
happen to him, and keeps a photo of each published order on paper, in date order, after the paper yellows.
The loop: event, then the officer names the casualty himself (muscle memory), then the clerk's paper order is photographed against it.
The app never reads the photo (no OCR), and it never drafts, checks or prints orders.

Design source: the owner's Freeform board and the one-page artifact (https://claude.ai/artifact/FDy2JH7Kv33JJMFKwYeN9p).

## v1 scope (frozen; owner, 2026-10-29 and 2026-10-30)

Learn about Part II (free):
1. Wiki of all 303 casualties in 10 ribbons; a ribbon is one flat list with the case names as headings; search over all 303.
2. Casualty page: name, group meaning, chapter, manual page, ODP columns with the manual's captions, a description (manual data for all
   303; plain English for about 30), "Usually published with" (only where data exists, about 60), and "Add to Orders Bank".

Part II Orders Bank (Premium; in the `.pcda` backup):
3. One merged list in date order. Entry = casualty + From, optional To, the columns ODP requires for that casualty, one line of note.
4. Status is derived: waiting for an order (orange) or order received (blue). Chips: All, Waiting, Received. Waiting rows show their age;
   the list is grouped by year.
5. Attach order: scan the paper order (camera), then type its number and date (expected, searchable), "No. of Occurrences" and the
   quoted last Part II order number. A counter shows "3 of N". Tick other waiting entries, or add a casualty from the wiki. An order
   covers at least one casualty; one scan covers many.
6. Chain check: an order that quotes a last order number not in the Bank shows "previous order not in your Bank".
7. Orders shelf: a second lens of the same data, text rows by order date grouped by year; a row opens the pages and its casualties.
8. Print or PDF of the index. A "last backup" line. Scans are labelled "personal reference copy, not for PCDA(O) submission".

Home: the existing Insights Part II card opens two rows, Learn about Part II and Part II Orders Bank.
Android first; one iPhone walk; then the stores. Dark-launched behind `LaunchFlags.PART2_ENABLED` (default `false`).

Backlog (not v1): the manual's note wording for the 19 captionless required columns (Version B), order photo thumbnails, scanner quality for yellowed paper, PDF/image import, HRMS summary-sheet back-fill, OCR,
reminders, smart checks, drafting and printing slips, CANCEL as a special flow (it is an ordinary casualty), order-to-order links.

## Facts established before planning (read 2026-10-30)

Repository:
- `main` is one docs commit ahead of the old branch's parent `d9274bdd`. A dry-run merge of `origin/feature/part2-orders` into
  `main` is clean today. The old branch has 71 commits and about 162 non-test Kotlin files with `part2` in the path (mostly the draft
  engine, templates, rules, slip, print and reminders).
- The old branch edited 52 shared non-test, non-doc files; the Kotlin touch points are 14 files, +206/-69 lines:
  `App.kt`, `AppModule.kt`, `PremiumFeaturesCatalog.kt`, `GuideHomeScreen.kt`, `GuideNavState.kt`, `GuideNavStateSaver.kt`, `GuideTab.kt`,
  `GuideTabRoute.kt`, `PayslipDao.kt`, `Koin.kt`, `PayslipBackupService.kt`, `PortableBackup.kt`, `LaunchFlags.kt`, `SubscriptionManager.kt`.
- Main today: Room schema 14; `PortableBackup.CURRENT_VERSION` 4; `FeatureGate` has no Part II entry; `GuideDestination` is a sealed
  interface; line counts near the limit: `App.kt` 297, `GuideTab.kt` 257, `AppStrings.kt` 288 (nothing new goes into these without an extraction).
- The Pixel's database is at schema 15 (the old branch's tables) and holds the owner's real payslips plus synthetic Part II rows.
  Room has no destructive fallback, so the new branch must migrate it, never replace it.
- Old storage (to copy): three tables `part2_drafts`, `part2_orders`, `part2_pages`; each row is an id plus one AES-GCM ciphertext
  (pages keep `orderId` and `position` readable). Payloads carry `v: 1`, new optional fields stay inside `v: 1`, an unreadable row is
  kept and counted. `Part2Repository` is the interface; limits live in `Part2Limits` (page 500 KB, long edge 1600 px, 10 pages per
  order, `ORDER_NO_MAX` 40). `PageScanner` and `PageEncoder` are interfaces with ML Kit and VisionKit implementations.
- Content: `codes.json` has 303 serials, 321 codes, chapters 1-14, groups A 204, C 53, AM 23, B 11, CM 10, two with none printed;
  `nav.json` has 10 ribbons (41, 23, 23, 31, 20, 33, 18, 34, 28, 52 serials). Only the 63 built templates carry the Annexure III columns.
- A real order (structure only; nothing recorded): two pages (table; authority, distribution, signature); header with this order's
  number and date, the occurrence count and the last order number; rows with serial, code, From, To, Data 1-4; some orders are single
  occurrences, some bundle several events published months late; orders quote one another.

Not verified: how cleanly Annexure III reads from the PDF pages; test counts on a fresh branch (recorded in Phase 0); scan legibility on
yellowed paper (the owner's own test after the MVP; nothing read or recorded); phase sizes (estimates).

## Architecture

```
Annexure II/III (local PDF)  ──► tools/part2_codes.py (copied) ─► codes.json
                             └─► tools/part2_columns.py (new)  ─► columns.json   (303 rows, reviewed)
nav.json (copied) · about.json (new, ~30) · related.json (new, from the 63 templates' links + nav "also")
        └─► bundle.py: additive `part2` section ─► GuideBundleParser ─► guide/model/Part2*Models.kt ─► GuidePart2*Validator
shared/part2/domain (pure): BankEntry, EntryForm (columns → fields), BankStatus, BankIndex (order, year, age), OrderCompleteness, ChainCheck
shared/part2/data: Part2Repository (entries, orders, pages) ─► Room part2_entries · part2_orders · part2_pages (schema 16)
                   Part2Backup (chunked; entries, orders, pages) ─► PortableBackup v5
platform services (interfaces in commonMain): PageScanner · PageEncoder · IndexPrinter
composeApp ViewModels ─► stateless composables in ui/screens/guide/part2/
```

Module map (every file ≤ 300 lines; composables ≤ 50):

| Layer | Files | Source | Phase |
|---|---|---|---|
| Content tools | `part2_codes.py` (local), `part2_codes_check.py`, `part2_nav.py`; new `part2_columns.py`, `part2_about.py` (+ tests) | copy + new | 1 |
| Bundle models and checks | `Part2LookupModels.kt`, `GuidePart2LookupValidator.kt` (copy, trimmed); new `Part2CasualtyModels.kt`, `GuidePart2CasualtyValidator.kt` | copy + new | 1 |
| Domain | `Part2Limits.kt` (trimmed), new `BankEntry.kt`, `EntryForm.kt`, `BankStatus.kt`, `BankIndex.kt`, `OrderCompleteness.kt`, `ChainCheck.kt` | new | 2-4 |
| Data | `Part2Entities.kt` (orders, pages copied byte-identical; `Part2EntryEntity` new), `Part2Dao.kt`, `Part2Codec.kt`, `Part2Payloads.kt`, `RoomPart2Repository.kt`, `Part2Backup.kt`, `PageArchive.kt`, `Part2PageRestore.kt` | copy + adapt | 0, 3 |
| Platform | `AndroidPageScanner`, `MlKitScanSource`, `AndroidPageEncoder`, `IosPageScanner`, `IosPageEncoder`, `ScanResultBridge`; print: `SlipPdf`/`SlipPrintAdapter`/`IosSlipPdf`/`IosSlipPrinter` reworked as `IndexPrinter` | copy + adapt | 4, 5 |
| ViewModels | copy `Part2LookupViewModel`; new `BankViewModel`, `EntryEditViewModel`, `AttachOrderViewModel`; adapt `VaultOrderViewModel` | | 2-4 |
| UI | `Part2HomeScreen`, `Part2RibbonScreen`, `Part2CodeScreen` (copy, adapt); new `BankScreen`, `EntryScreen`, `AttachOrderScreen`, `OrdersShelfScreen`, `OrderScreen` | | 2-5 |
| Resources | new `OrdersBankStrings.kt`, `LearnStrings.kt` (all copy); theme tokens for waiting/received colours in `Theme.kt` | new | 2 on |
| DI and fakes | `Part2Module.kt` (loaded only while enabled); `shared-test-fixtures`: `FakePart2Repository`, `FakePageScanner`, `SyntheticBank` | adapt | 0 on |

Design rules: the engine of the old branch is not copied; no slip type, rule registry, template or reminder code exists on this branch.
Entries store the casualty code, dates, the column values typed and a note, never rendered text. Bank status is computed from the order link,
never stored. SSOT: `Part2Limits` for caps; `codes.json` for names and groups; `columns.json` for what each casualty needs; strings only in
the two new string files; colours as theme tokens.

## Cross-cutting checks (CLAUDE.md "Writing a phase plan")

1. Files read for this plan: the old branch's `Part2Entities`, `Part2Dao`, `Part2Repository`, `Part2Payloads`, `Part2Limits`,
   `PageScanner`, `PageEncoder`, `Part2Destination`, the `PayslipDatabase` diff, the touch-point diffstat; on `main` `GuideNavState`,
   `PortableBackup`, `FeatureGate`, `LaunchFlags`; plan 12, DECISIONS Rounds 37-50, `IPHONE_CHECKLIST.md`, `10_part2_store_wording.md`.
   Nearest patterns: `GuideNoteEntity` (encrypted row), `PreClaim` destinations and `GuideNavStateSaver` (ids only), `BackupGuard`.
2. Release and iOS risks:
   - New `@Serializable` models go through `check_r8_guide.py`; `minifyReleaseWithR8` every phase; `assembleRelease` where models or Room change.
   - One new Android library, `play-services-mlkit-document-scanner` (16.0.0 on the old branch; `main` does not have it). Play services
     hosts the scanner, so no camera permission is added. The version is re-checked for age (at least two weeks) and advisories in Phase 4.
     On iOS: `NSCameraUsageDescription` and the VisionKit presenter. No WorkManager, no notification permission. The merged-manifest check
     runs from Phase 0 against a baseline regenerated from main's release; Phase 4 reviews what the library adds, entry by entry.
   - Kotlin/Native: no regex in `commonMain` hot paths; search over 303 casualties and the Bank list get an `iosTest` timing bound;
     page encoding runs off the main thread; Swift (VisionKit presenter) keeps its XCTest cases.
3. Regression controls: `PART2_ENABLED` stays `false` until the owner says; characterization tests of the copied lookup and vault
   before any edit; the corpus (139/139) is untouched; a flag-off build behaves exactly as main does today.
4. Navigation and state: new `Part2Destination`s with ids only (`Home`, `Ribbon`, `Code`, `Bank`, `Entry`, `Attach`, `Order`, `Shelf`);
   `retainKnown` cuts unknown ids; Android back pops one level; tab re-tap returns to Guide Home; a lock screen discards the entry form
   and any scan in flight (the officer rescans). Entries save when the officer taps Add, so there is no draft to restore after process death.
   A notification link does not exist (no reminders).
5. Versioning, DI, storage:
   - Room 14 to 16: `AutoMigration(14, 15)` as on the old branch (copy `15.json`), then `AutoMigration(15, 16)` that deletes
     `part2_drafts` with a `@DeleteTable` spec and adds `part2_entries`. `part2_orders` and `part2_pages` stay byte-identical so the Pixel's
     schema-15 database opens. `PayslipDatabaseUpgradeTest` upgrades 14, and the old 15, to head; `check_schema_immutability.py` guards 15.
   - Backup `.pcda` v5 (chunked, pages as binary records) with `entries` in place of `drafts`; a v4 file restores with an empty Bank; a v6 file
     is refused; restore checks sizes and counts against `Part2Limits` before decoding. A v5 file from the old branch restores its orders
     and pages; its drafts are skipped and counted.
   - Entry and order payloads carry `v`; new fields (order count, last order no.) are optional inside `v: 1`. `FeatureGate.PART2_ORDERS`
     with its catalog entry and a flag-aware `isAdvertised`; Koin graph test covers `Part2Module`.
6. Privacy and security (every phase): synthetic data only in the repository, tests and walks; nothing from a real order or payslip is
   printed or recorded; pages are re-encoded (no Exif), never go to the gallery, ML Kit cache is deleted; nothing about an entry, code,
   date or order number reaches `Logger`, `CrashReporter` or an exception message (counts through `TelemetrySanitizer`); no network, no
   clipboard writes. The backup screen warns that a backup saved to a personal cloud leaves the device. `security-review` on each code
   phase's diff. Whether a personal phone may hold an order is the officer's unit policy, not the app's.

## Gate (every phase end; as in plan 12)

1. `./gradlew check -x iosX64Test -x iosSimulatorArm64Test`. 2. `linkDebugFrameworkIosSimulatorArm64` and `iosSimulatorArm64Test`.
3. `python3 docs/Plan/rule_cards/tools/refresh.py` and `--check`. 4. `check_tech_debt_limits.py --strict` on touched files;
`python3 -m unittest discover -s scripts/tests`. 5. `minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true`; where noted, `assembleRelease`,
`check_r8_guide.py`, `check_merged_manifest.py`. XCTest runs alone, never beside Gradle. Each phase ends with a Phase Summary (CLAUDE.md
protocol and the template in plan 12) and waits for the owner before the next phase starts. One commit per phase after acceptance.

## Phases

**Execution order from here (owner, 2026-11-01): Phase 7 remnants (the split page backup first), then Phase 8 (UX pass), then Phase 6 (release).** Phases 0 to 5 are committed. Numbers are kept so other documents still refer to the right phase; the sections below are in the order they run.

| Phase | Status | Size (estimate) |
|---|---|---|
| 0. Branch, baseline, general fixes, storage proof | committed f72cc6c4 | small |
| 1. ODP content: the 303-row column table, descriptions, related, bundle | committed ef80fc4f (owner: spot-check passed, 30 pages read) | large |
| 2. Learn about Part II (free) | committed d94e7704; owner walked it on the Pixel | medium |
| 3. Orders Bank: note it | committed cbae8f74; owner walked it on the Pixel | medium |
| 4. Orders Bank: prove it (scan, attach, counter, chain, shelf) | committed 2cd3761a (pushed); owner walked it, a real yellowed order reads well | large |
| 5. Print or PDF of the index | committed db7a9ce4 (not pushed); owner walked the print: working as intended | medium |
| 7. Close-out and the split, incremental page backup (every pending point, in one place) | NEXT, open | large |
| 8. UX pass (the owner's list of minor issues) | after 7, list not yet given | small |
| 6. Release: hardening, walks, store wording | LAST, after 7 and 8 | medium |

### Phase 0: Branch, baseline, general fixes, storage proof
- Goal: a green branch from main with the four fixes main benefits from, and a proven migration path.
- Steps: tag the old branch `archive/part2-orders-2026-10-30` locally (the push of the tag needs the owner's word). Create
  `feature/orders-bank` from main. Record the baseline: the full gate, test counts per module, corpus 139/139, line counts of every
  file this plan touches. Add `docs/Plan/13_COPIED.md`: every copied file and the commit it came from.
- General fixes, each its own commit with tests first, extracted by hand because the old commits mix Part II changes:
  the friendly restore error (`DecryptionFailedException` in both `CryptoHelper`s and the restore text); the iOS privacy-manifest
  disk-space declaration with `test_privacy_manifest.py`; the narrow-screen banner (`FlowRow`); `check_merged_manifest.py` with a
  baseline regenerated from main's release.
- Storage proof, tests first: copy `PayslipDatabaseUpgradeTest`; add 14 to 16 and (old) 15 to 16 upgrade tests; the three-table entities
  and `AutoMigration`s. The Pixel's own database cannot be copied off the phone safely, so the upgrade is proved by these tests and, before
  any install, by the owner's `.pcda` backup (restored if anything goes wrong).
- Exit: gate green; flag off; the app behaves as main.
- Debt watch: a copied file that drags in a draft or reminder type must not be copied; the build tells us. The unused-code count stays at zero.

### Phase 1: ODP content
- Tests first (`tools/test_part2_columns.py`, `GuidePart2CasualtyValidatorTest`): 303 rows, one per serial, codes equal `codes.json`; every
  `none` column blank; captions bounded; the 63 built templates' From/To/Data needs equal the new table for those casualties (an independent
  oracle that was already checked against the printed pages); a deliberately broken row is refused; `related` only names real codes; the
  `about` text has no angle brackets and is bounded; bundle deterministic; an older bundle without these keys still loads.
- Work: read Annexure III from the PDF page images into `columns.json`; script checks against codes and pages; the owner spot-checks about 25
  rows chosen by a seeded random draw, recorded in DECISIONS. Manual data for all 303; the owner picks about 30 casualties for plain-English
  text, which I write from the manual and the owner reads once. `related.json` from the 63 templates' links and nav `also`, each tagged
  `manual` (with page) or `practice`. Bundle section and validators; R8 check; bundle size noted against `MAX_CHARS`.
- Exit: gate green; `refresh.py --check` clean; the bundle ships byte-identical in `check_r8_guide.py`.
- Debt watch: the extraction tool and its local sources stay out of CI like `part2_codes.py`; the checker runs in CI.

### Phase 2: Learn about Part II (free)
- Tests first: ribbon list with case headings; casualty page content and the manual-vs-practice label; search over all 303 by code, serial and
  name; Back at each level; saver restores ids only; flag off hides the card's destination.
- Work: Part II home with two rows, ribbon, casualty page, search; "Add to Orders Bank" navigates to the entry form (disabled until Phase 3).
- Exit: gate green; Pixel walk of Learn (light and dark, process death, 2x font) on synthetic state.

### Phase 3: Orders Bank, note it (Premium)
- Tests first: `EntryForm` builds the fields a casualty needs and refuses missing required ones; dates (From before To; inclusive days
  not needed); `BankStatus`; `BankIndex` ordering by From, grouping by year, age text; entry codec round trip, ciphertext, caps, unreadable
  row counted; repository over in-memory Room; backup export and restore of entries; gate and catalog tests (`PART2_ORDERS` free launch,
  force free, force pro; a lapsed officer can view and delete only); list UI test (chips, empty state, one Add, edit, delete with confirm).
- Work: entries end to end; the "last backup" line (a device-local setting); `FeatureGate.PART2_ORDERS`.
- Exit: gate green; `assembleRelease` + `check_r8_guide.py` + manifest check; Pixel walk (add, edit, delete, kill, lapse).

### Phase 4: Orders Bank, prove it
- Tests first: attach flow state machine (scan, order number and date, count N, last order no., tick, add from the wiki); at least one
  casualty per order; `OrderCompleteness` ("3 of N"); `ChainCheck` (present, missing, no quote); one scan linked to many entries; unlink and
  delete rules (deleting an order unlinks, never deletes entries); the shelf lens (grouping, text rows); page add, view and delete (no
  reorder in v1); the label "personal reference copy"; lapsed state read-only; every scanner result has its own wording.
- Work: copy and adapt the scanner, encoder, page capture and vault order screens; the Attach and Order screens; the shelf toggle.
- iOS: VisionKit presenter, a lock while the camera is up, no new image in Photos (the iPhone walk in Phase 6).
- Exit: gate green; Pixel walk (a public page scanned, never a real order); scanner cache empty afterwards.

### Phase 5: Print or PDF of the index
- Tests first: the layout model (a table of entries with status, order number and date, paged), no identity data in the page, text-scan test;
  the Android print adapter and iOS PDF path over fakes; "printed on" local date.
- Work: `IndexPrinter` built from the old slip printer infrastructure; one entry point on the Bank.
- Exit: gate green; Pixel print to PDF and a real printer if the owner has one at hand.

### Phase 7: Close-out (everything Phases 2 and 3 left open)
Phase 2 is committed and its gate is green. Owner decisions made on the Pixel walk (2026-10-30): the search box does not take focus by
itself; the Learn row sits in its own section "Part II orders (ODP 2026)" below the handbook areas, with a line saying it is a different
manual. Still open:
1. Owner: finish reading the casualty pages on the phone (light and dark, process death, 2x font) and report any wording to change.
2. Characterization gap: the old `Part2LookupUiTest` was not run green unchanged (its screens are replaced); the UI tests were written new.
3. Debt kept: `Part2Ribbon.built` and `Part2Case.lookup` are no longer read by Learn but still ship in `nav.json`, the models and the lookup
   validator. Removing them means editing `part2_nav.py`, its tests, the models, the validator and the bundle in one data phase.
4. Native timing: the Learn search walk measured 649 to 2,279 ms on a loaded host (budget 1,500); the test takes the best of three. Raise
   the budget only if CI flakes.
5. iPhone walk of Learn: per the standing rule, only when all features are ready.
6. Push: only on the owner's word (after Phase 3, per the owner).

Phase 3 open points (2026-10-30; the Phase 3 commit waits for 1 and 2):
1. Owner: the Pixel walk on synthetic data (add, edit, delete, kill the process, lapse to Free, backup and restore through the picker).
2. Owner: read the Bank screens and wording on the phone: the empty state, the "last backup" line, "Add a casualty", the lapsed note.
3. Owner decisions taken 2026-10-31 (closed): the form follows ODP 2026 (To only where the ODP prints a To column; From always required, labelled "Date" for the 6 casualties with no From column); a lapsed officer sees a read-only form with Delete only; anyone opens the Bank, newest From first; last-backup line on the Bank screen only; commit after the owner's walk, then push feature/orders-bank (no PR).
4. Characterization gap closed: the old branch's 141 `part2.data` tests ran green unchanged against its own sources.
5. TDD gap closed by 12 more deliberate breaks of the view models, navigation and screens (30 breaks in all, each caught by an assertion, each file restored and its sha256 checked). The first view-model and screen tests were still red only as missing declarations.
6. Not broken on purpose: the generic `SubscriptionManager.hasAccess` (no Part II special case exists), the restore "never decoded" counting test, the export of unreadable rows.
7. iOS: the XCTest target was not run (no Swift changed); the Bank timing test and the two ported storage twins passed on the simulator. The iPhone walk stays with the other features (standing rule).
8. Not copied on purpose: the vault-size settings row and its warning (Phase 4); `Part2Limits.VAULT_WARN_BYTES` was removed as unused and returns with it.
9. Final checks before the commit (owner chose to run all): full `check`, iOS tests, XCTest alone.
10. Debt kept: `Part2Ribbon.built` and `Part2Case.lookup` (point 3 above).

Phase 7 REVISION by the owner (2026-11-01), replacing the split in point 11: (1) the vault ceiling becomes 250 MB (was a fixed 100 MB), with a usage meter and a warning at 80% (200 MB); the counts (1,000 orders, 2,000 casualties) stay the brake; measured on one real yellowed page: 211 KB stored, so 250 MB holds about 1,200 pages. (2) The `.pcda` backup stays ONE file carrying data and pages: no second pages file, no split, no incremental file (point 11 b and c are dropped), so there is one backup format and one place that defines it. Consequence found while planning (2026-11-01): the file is already one archive (`PageArchive`, "PCDB"), but `export` returns one `ByteArray` of the whole vault and `restore` takes one `ByteArray`; a normal Android app heap on the Pixel is 256 MB (`dalvik.vm.heapgrowthlimit`), so a 250 MB vault cannot be built or read that way (not measured; the arithmetic alone exceeds the heap). Android's own device backup is off (`allowBackup="false"`), so the `.pcda` is the only backup on Android; iCloud device backup on iOS is not verified. Open owner question: how memory stays flat at 250 MB (streaming the one file is the recommendation).

Phase 7 execution plan (2026-11-01; owner: "go" on streaming the one file, 250 MB ceiling, warn below 500 MB free and refuse below 100 MB, no format version bump). Order matters: the ceiling is raised LAST, because 250 MB cannot be exported or restored while a backup is one `ByteArray` on a 256 MB heap.
- 7a Streaming core (shared, both platforms' common code): `BackupSink` (write in order) and `BackupSource` (read by offset) in `repository`; a memory pair for small files and tests; `PageArchiveWriter` writes to a sink (no final-size buffer), `PageArchive.layout`/`restorePages` read from a source (header windows and one sealed page at a time); `PayslipBackupService.exportTo(sink)` and `restore(source)`, with the `ByteArray` forms kept as thin wrappers until 7b. Same bytes as before (no format change), proven by round-trip and by the old archive tests running green unchanged. Red test first: the 120-page scale scenario through file sink/source must stay under a small extra heap (the old path needs about the vault's size).
- 7b Platform files: Android `RandomAccessFile`/`FileOutputStream` and iOS `NSFileHandle` sinks and sources; export writes a cache file and shares it, the picker copies the chosen file by stream to a cache file and restores from it; both temp files are deleted in a `finally` (the contents are password-encrypted); `shareBytes` and `onPickBackup` stop carrying a `ByteArray`. R8 and iOS timing checks; Pixel walk of backup and restore with the owner's data (a fresh `.pcda` first).
- 7c Ceiling and warnings: `VAULT_BYTES_MAX` 250 MB, warning at 200 MB (80%), free-space warning below 500 MB and refusal below 100 MB before a scan or an attach, one text function for the meter (Settings always, Bank past the warning); tests with a fake free-space reader; a 250 MB synthetic restore memory test on both platforms.

Phase 7b BUILT and walked on the Pixel (2026-11-01); NOT committed until the owner says so. All pending points of Phase 7b, in one list:
1. Files: `BackupFiles` (interface, `FileBackupFiles` shared rules, `AndroidBackupFiles`, `IosBackupFiles`, `FakeBackupFiles` in shared-test-fixtures). Fixed names (`BACKUP_EXPORT_FILE_NAME` once in common, `PayslipMax-restore.pcda` once on Android); failures carry only the fixed `BackupFileException` text, never a path.
2. Owner decision (2026-11-01): Android cannot know when the share target has finished reading, so a shared export stays in private cache until the next launch or the next backup sweeps it; a failed export or share deletes it at once. iOS deletes it when the share sheet closes. Cost: an encrypted copy can sit in app-private cache until then.
3. Picker: Android copies the chosen URI to the restore file by stream (`copyPickedBackup`); iOS asks the OS for its own copy (`asCopy: true`) and hands Kotlin a path. A dismissed Replace or Merge question deletes the picked file. `shareBytes` and the iOS `prepareShareFile` are removed.
4. Pixel walk DONE with the owner's data: export shared (1 non-empty cache file), relaunch swept it (0), restore with MERGE through the picker (the restore file existed while the question was up, 0 left afterwards), owner confirmed the data intact. The first restore attempt failed once; the owner did not report the message, so the cause is NOT KNOWN (suspected password). Not walked on the phone: a cancelled picker, a dismissed Merge question, Replace, a large vault.
5. Not verified: the iOS share-sheet completion cleanup and the `asCopy` picker on a real iPhone (iPhone walk stays last, per the standing rule); VisionKit and print unchanged. Deliberate breaks not made on the iOS file classes.
6. Characterization: the existing backup, Part II backup, page archive, vault-scale and golden tests ran green unchanged. Tests CHANGED: `PayslipViewModelBackupTest` and `PayslipViewModelRestoreReportTest` (the view model's `exportBackup` and `importBackup` now take a file, so they use `FakeBackupFiles`; assertions unchanged), `GemmaInstallerDiWiringTest` (binds `BackupFiles`), `ShareUtilsIosTest` (two `prepareShareFile` tests removed with the function). TDD gap: the Android file tests were written alongside the code (only `FileBackupFilesTest` was shown red first); the 10 deliberate breaks prove them.
7. Debt kept, with reasons: `PayslipBackupService.export(ByteArray)` and `restore(ByteArray)` (`PayslipBackupService.kt` about lines 39 and 117), `PageArchive.metaOf(ByteArray)` and `layout(ByteArray)` (`PageArchive.kt` lines 44 and 61), `Part2Backup.restore(..., ByteArray)` (`Part2Backup.kt` line 92) and the `MemoryBackup*` pair are used only by tests and by each other; removing them means rewriting about thirty tests for no gain. The text "A range outside the backup." is repeated in three sources (`BackupIo.kt`, the Android and iOS `FileBackupIo.kt`). `FakeBackupFiles` and the Android file classes have no iOS-only twin of the break tests.
8. Gate result: check, iOS link and simulator tests, XCTest, assembleRelease, R8 guide check, merged-manifest check (11 permissions, none added), flag-on, schema immutability, scripts 112, rule-card tools 419, tech-debt audit (35 files), security review (no findings): all green. Counts: shared debug 1660, composeApp debug 1339, iOS simulator shared 1515 and composeApp 818. Pins: App.kt 297, AppStrings.kt 288, GuideTab.kt 226, GuideModels.kt 273.
9. Owner items for 7c: see "Phase 7c BUILT" below (the 7c walk replaces this line). Then Phase 8 (the owner's list), then Phase 6.

Phase 7c COMMITTED (2026-11-02) after the Pixel walk below. All points of Phase 7c, in one list:
1. Code: `VAULT_BYTES_MAX` 250 MB (the 200 MB warning, `VAULT_WARN_BYTES`, and the one meter text already existed from Phase 4 and follow the ceiling); `FREE_SPACE_WARN_BYTES` 500 MB and `FREE_SPACE_REFUSE_BYTES` 100 MB in `Part2Limits`; package `part2/space` (`FreeSpaceReader`, `FreeSpaceGate`, `SpaceVerdict`, Android `usableSpace` of `filesDir`, iOS `NSFileSystemFreeSize`, `FakeFreeSpaceReader` in shared-test-fixtures); the verdict is checked in `PageCapture` (after the order-full check) and at the start of the attach scan; words `OrderStrings.noSpace()` and `lowSpace()`. A reader that cannot answer never blocks (unknown means enough).
2. Interpretation to confirm (owner): "before a scan or an attach" is the check at the START of every scan, in both the Add pages and the Attach flow. Save is not gated again.
3. FOUND AND FIXED, a shipped 7a/7b defect: on iOS `FileBackupSource.read` and `FileBackupSink.write` leaked the autoreleased `NSData` (about 0.5 MB per call): a 256 MB vault grew the process by 505 MB on restore and 250 MB on export. Both now run inside `autoreleasepool`; measured flat (6 MB restore, 3 MB export). Caught by `Part2VaultHugeIosTest` and deliberate breaks 18 and 19.
4. Tests: `FreeSpaceGateTest`, `AndroidFreeSpaceReaderTest`, `IosFreeSpaceReaderTest`, `FreeSpaceFlowTest`, `Part2FreeSpaceUiTest`, and the 250 MB synthetic restore on both platforms (`Part2VaultHugeJvmTest` with a flat-heap check, `Part2VaultHugeIosTest` with a flat resident-size check). Changed on purpose, for the new figures: `VaultSizeTest`, `PageSizeBudgetTest`, `RoomPart2RepositoryTest`; fixtures `AttachTestBase`, `BankTestBase`, `Part2BankWiringTest` (asserts the reader is bound), `FakePart2Dao`. 19 deliberate breaks, each caught by an assertion (break 6 was first invalid, fixed and re-run).
5. Privacy manifest: `iosApp/iosApp/PrivacyInfo.xcprivacy` already declares `NSPrivacyAccessedAPICategoryDiskSpace` with reason E174.1 (unchanged). Not verified: that App Store Connect accepts it on the next upload.
6. Found by the flag-on run, not by the default gate: the Koin graph lacked `FreeSpaceGate`; bound in `Part2Module` (`PageStore(get(), get(), space = get())`). Lesson: run flag-on after any DI edit.
7. Pixel walk (2026-11-02, driven by the agent over adb, owner had a fresh .pcda; only booleans, counts and fixed strings read): Settings meter reads "of 250 MB"; Attach, "Scan the order" with about 149 GB free passed the free-space check and opened the scanner; cancel was silent and saved nothing. LARGE VAULT: a synthetic 256 MB archive (500 pages, test password, built by a temporary test, deleted) was restored with MERGE through the real picker in about 62 s with no memory error and the process alive; Settings then read 245 MB of 250 MB with the warning sentence, and the Bank showed the warning after a cold start; a Back Up of that 245 MB vault took about 51 s, opened the share sheet, left one large cache file that the next launch swept (0). NOT walked: the low-space warning (below 500 MB) and the refusal (below 100 MB) on a real reading (the phone has 149 GB free; filling it would put the device at risk), proven with the fake reader only.
7a. FOUND ON THE WALK and fixed in 7c (owner: "I want the stale-meter fix in 7c"): after a restore the Settings meter and the Bank warning kept the old figure until the screen was left or the app restarted, and the Bank figure also did not move when pages were added to an order that already existed, because it was re-read only when an entry or an order changed. Fix: `Part2Repository.observeVaultBytes()` (Room: `SUM(LENGTH(ciphertext))` as a Flow, so Room re-emits at every change to `part2_pages`; no schema change), `vaultBytesOrZero()` is now a Flow with `.catch`, the Bank state combines it, the Settings line collects it. Tests first (red by assertion): `RoomPart2RepositoryTest`, `BankViewModelTest`, `VaultSizeSettingsUiTest`, `VaultSizeTest` (changed to the flow form); 4 deliberate breaks, each caught (the first Room test re-subscribed each time and missed one; tightened to one subscription).
7b. SYNTHETIC DATA LEFT ON THE PIXEL: the walk merged 50 synthetic orders (500 pages, 245 MB) into the owner's vault. The owner's fresh .pcda holds the real data; the clean undo is a Replace restore of that backup (owner's password). Not done by the agent.
8. Not verified: the iOS free-space reader on a real iPhone (simulator only); Dynamic Type of the two new lines.
9. Debt kept: `FileBackupIo.kt` line 29 `alloc` carries a compiler opt-in warning (`BetaInteropApi`), as in HEAD (7b), not touched; `DeviceCapabilityManager.ios.kt` line 45 casts `NSFileSystemFreeSize` with `as? Long`, which may always fail and fall back to 10000 (not verified, not touched, not part of Part II); `FreeSpaceGate.verdict` swallows a reader exception on purpose (unknown means enough).
10. Carried from 7b (unchanged, not walked): the iOS share-sheet cleanup and the `asCopy` picker on an iPhone; on the Pixel a cancelled picker, a dismissed Replace or Merge question, Replace, a large vault; the unreported first-restore failure (suspected password, cause NOT KNOWN); test-only `ByteArray` wrappers (point 7 above); "A range outside the backup." repeated in three sources.
11. Commit: owner said commit after the stale-meter fix (2026-11-02). Push waits for a separate word; no PR.
- Then Phase 8 (the owner's list), then Phase 6.

Phase 4 owner decision (2026-11-01), the split page backup (open, before release; owner decided it is in):
11. Real use means 100+ orders and 300+ pages, so the 100 MB vault cap and a single `.pcda` carrying every page will not hold. Plan: (a) a per-page size budget (about 250 KB, grayscale, fixed long edge) chosen from a readability test on the owner's yellowed-order scan, not guessed; (b) `.pcda` keeps entries, order metadata, notes and the page index (ids and content hashes) and restores without the images, orders then show "pages not restored"; (c) a separate, incremental pages backup, pages named by content hash, only new pages added per backup, verified one by one on restore, resumable, size and count checked before any decode; (d) the fixed 100 MB cap replaced by a usage meter, a free-device-space warning and a generous safety ceiling; (e) check whether Android Auto Backup and iCloud device backup already carry the database (not verified); (f) backup format v5 to v6 with restore tests for partial, interrupted and mismatched files. No real users yet, so the format may change freely now.

Phase 4 open points (2026-11-01; the Phase 4 commit waits for 1 and 2):
1. DONE 2026-11-01 (driven over adb with the owner's hands for the camera, the backup password and the picker; public printed page, nothing of its content read or recorded): scan 2 pages, fill, tick, save; label, counter "2 of 3 casualties", chain flag, "Show page" readable (owner: "readable"); shelf by year with an undated group last; attach from one casualty (pre-ticked, "1 casualty" when N is blank, chain found); delete an order (casualties back to Not attached, entries untouched); Settings size line; backup note; backup and restore through the picker (order, fields, both pages, links back); lapsed (Force Free) hides Attach, Add, Add pages and Remove; process kill with a held scan leaves 1 file that the Bank sweeps on open; the cache was 0 files after every save. FOUND ON THE WALK and fixed: the scanner swept its whole folder at the start of every scan and on a cancel or failure, deleting pages an earlier scan had handed over ("Scan more pages" lost page 1); it now skips pages it handed over and nobody has discarded (`AndroidPageScannerHeldPagesTest`). Original text of point 1: the Pixel walk on a PUBLIC printed page (never a real order): scan, attach to several casualties, the counter ("3 of 5 casualties" / "3 casualties"), the chain flag, the shelf, kill the process, delete an order and see its casualties return to Not attached, lapse to Free, backup and restore through the picker, and the scanner cache empty afterwards. The debug build (flag on, built from a throwaway copy, repo flag untouched) is on the Pixel since 2026-11-01 11:05.
1a. DECIDED by the owner (2026-11-01): a lapsed officer may delete a page ("Delete page" is shown when lapsed, with its confirmation), as well as the order; Add pages and Remove from this order stay hidden. Done, with two tests.
2. Owner: read the new screens and wording on the phone (attach form, order screen with the "Personal reference copy" label, shelf, Settings size line, backup note, entry-screen buttons). One wording to confirm: the counter counts CASUALTIES (the order's printed "No. of Occurrences"), not pages as the Phase 4 question said.
3. Owner: your own scan of one old yellowed order after the phase, and your word on whether it reads (nothing is read or recorded by me). It also sets the real per-page budget: the 250 KB (`PAGE_BUDGET_BYTES`) was measured on a synthetic page only.
   DONE 2026-11-01: the owner scanned one real yellowed order and says it reads well. The stored page is 211 KB (byte length of its row only; nothing else was read), under the 250 KB budget, so `PAGE_BUDGET_BYTES` stays 250 KB. Measured on one page only; a second page was not measured.
4. Not verified on a device: the ML Kit scanner itself, VisionKit (no iPhone walk, per the standing rule), the `GmsDocumentScanningDelegateActivity` entry in the installed manifest, and the camera prompt text on a phone.
5. iOS: the Swift presenter is the old one verbatim and its 9 XCTest cases pass; `registerPresenter` runs at launch but has not been exercised on a phone. Disk-space privacy-manifest entry from the old branch not copied (nothing here reads disk space yet; add it with the free-space warning in the split-backup phase).
6. Characterization gaps: none left; the old tests ran green unchanged against a scratch export of the frozen branch (108 JVM, 26 iOS Kotlin, Swift XCTest) before porting.
7. Deliberate-break gaps: none known; 29 breaks, each caught by an assertion (see the Phase 4 summary). Not broken on purpose: the Koin binding of `PageStore`, the Settings row's flag gate, and the iOS encoder's budget.
8. Debt kept: `OrderStrings.cameraUsageDescription` is read only by the plist test (Apple needs the text in Info.plist); the plaintext-in-memory residual of decoded bitmaps is accepted as on the old branch; `FLAG_SECURE` on the order screen is recorded, not decided.
9. Still ahead: Phase 5 is BUILT (see Phase 5 open points below); the split, incremental page backup and storage-based cap (point 11 above); Phase 6 release (full gate on the final commit, release-APK walk, one iPhone walk, store wording, lane guard).

Phase 5 open points (2026-11-01). The owner walked the print on the Pixel and said it is "working as intended" (2026-11-01), so points 1 and 2 are DONE as one overall word; the owner did not itemize a real printer, the lapsed state or the disabled-while-open state, so those are recorded as not individually reported. Original text of point 1:
1. Owner: the Pixel print walk on the flag-on debug build: print the Bank to PDF from the Bank screen and check paging (15 rows per full page), year headings, order numbers and dates, status words, the "Printed on" date, "Check chain", the page number at the foot of every page, that no note or Data text is on the page, that no file is left behind (Downloads shows only the PDF you chose to save), the button is absent on an empty Bank, present when lapsed (Force Free), and is off while the dialog is open. A real printer if you have one. Nothing is verified by me on a device yet except what is listed in 3.
2. Owner: read the button wording "Print or save as PDF" and the printed words on the phone.
3. Driven by me over adb: see the Phase 5 summary (filled in after the walk) for what was and was not checked on the phone.
4. Not verified on a device: the iOS print sheet and the iOS PDF (no iPhone walk, per the standing rule); a real printer; `PrintJobWatch` against a real job.
5. Deliberate-break gaps: none known; 24 breaks (22 on JVM, 2 on the iOS simulator), each caught by an assertion. Not broken on purpose: the Koin wiring (none was added), `IndexPrinterProvider` (a three-line composable, covered by `findActivity`'s test), the Print button's `Modifier`, `Part2UiBase`'s new parameter.
6. Characterization: the old print tests (Android printer, adapter, PDF, job watch, iOS PDF, `PdfLetterFormatter`) were ported with the names changed and their assertions kept; they were NOT run green unchanged against the frozen branch first (Phase 4 did this for the page pipeline; Phase 5 ported straight to the new names, each against a stub that failed them). Gap recorded.
7. Debt kept: `PrintOutcome.COMPLETED` is returned by the printers but production code ignores it (the call returning is what re-enables the button; nothing is recorded); the Android 30-minute `PRINT_WATCH_MS` keeps the button off for as long as a printer never answers; `IndexPdf`/`IosIndexPdf` duplicate the page model's drawing (one per platform, as in the slip printer); the index has no sort other than newest From first.
8. Native timing: `IosIndexPdfTest` renders 2000 rows (the Bank ceiling) with a 3 s budget, best of three; the figure is printed in the test log.
9. Still ahead: the split, incremental page backup and storage-based cap (point 11 above); Phase 6 release (full gate on the final commit, release-APK walk including print, one iPhone walk that must include the print sheet and Dynamic Type, store wording, lane guard).

### Phase 8: UX pass (owner, 2026-11-01)
Phase 8 starts now (owner, 2026-11-02). The owner's UX list has NOT been given yet; nothing below is a Phase 8 item until it is.

**Rules for every Phase 8 update or improvement (owner, 2026-11-02):**
1. TDD: unit and integration tests are written before or alongside the implementation; 100% of tests pass before a phase is marked complete.
2. Architecture and clean code: MVVM, SOLID, DRY and SSOT. No file over 300 lines unless the Single Responsibility Principle requires it; then a comment in the file says why it must not be split. Otherwise split it under 300.
3. Resource management: no hardcoded strings or colors. Copy goes in the strings objects (`AppStrings` family) and colors come from the established theme tokens, to avoid UI/UX debt.
4. Cybersecurity: the codebase stays 100% secure; every addition or update is checked for vulnerabilities (OWASP top 10, PII leakage).

**Phase 8 item 1 (owner, 2026-11-02): Guide Home as a grid, one screen.** The 12 one-per-row ribbons hid Part II below the fold. Guide Home is now: title + search, sub text, grey divider, one-line Guide overview, two-column compact tiles (emoji beside the title, topic count under it), grey divider, Part II heading + one-line overview, ONE full-width "Part II Orders" tile ("Manage your Part II Orders") that opens a hub screen with Learn about Part II and the Orders Bank stacked. All ten tiles and the Part II tile fit one Pixel 9 screen.
- Short tile labels (tile only; area screens, breadcrumbs and search keep the bundle titles, bundle untouched): LTC, Leave, TD, Postings, Claims, Retire, Payments, Allowances, House, Health, Family, TA/DA Claim (subtitle "Advance calculator"). Source of truth: `GuideStrings.areaTileLabel`, `PreClaimStrings.tileTitle`.
- Code: `GuideHomeGrid.kt`, `GuideHomeScreen.kt`, `Part2HubScreen.kt`, new `Part2Destination.Hub` (saved as `hub`). Tests: `Part2LookupUiTest` (wide tile, hub, back one level at a time) plus the Guide and PreClaim tests moved to the new labels.
- Walked on the Pixel 9 (debug build with `PART2_ENABLED` on locally; the flag stays false in source). iPhone walk is the Phase 6 one.
**Phase 8 item 2 (owner, 2026-11-02): the Part II hub tells you where your file stands.** The hub was two bare rows. It is now: a top card, then Learn about Part II, then the Orders Bank, then "How it works" (only while the file is empty). Scope kept small on purpose (owner: "intuitive and speedy", no spiral): no progress bar, no sequence check, no backup nudge, no print shortcut, no vault meter.
- Top card, two states. Empty: "Start your Part II file", one button "Add first casualty" (a lapsed officer sees "See Premium" instead, as on the Bank), a one-line privacy promise. In progress: "N casualties noted", "X of N have their order copy attached" ("Every order copy is attached" when none is missing), and, only when something is missing, one chip "N order copies not attached" that opens the Bank filtered to Not attached. The Bank card still opens the list as it was left. Nothing is said until the first read finishes (no early "empty"). With the Bank absent (flag off) the hub is exactly the two rows.
- MVVM and SSOT: `HubStatus` (shared, pure, derived from `BankView`, so the hub and the Bank chips share one set of counts, numbers only) -> `BankViewModel.hub` (a `StateFlow`) -> stateless `Part2HubHero` / `Part2HubHowItWorks` in `Part2HubStatus.kt`; `GuideTabPart2` wires `Part2HubLevel`. Copy only in `OrdersBankStrings` (`hub*`; drafts for the owner to read on the phone, reusing `LearnStrings.casualtyCount`); colours from `BankColors` (waiting/attached) and theme tokens. No new query, storage, flag, `@Serializable` type or dependency, so no schema, backup or R8 change.
- Tests (written first): `HubStatusTest` (shared), `HubStatusViewModelTest`, `Part2HubStatusUiTest` (10: empty, order top to bottom, Add first, lapsed, in progress, chip to filtered Bank and back, all attached, Bank card unfiltered, flag off, 2x font). Two mutations (chip filter dropped; "not read yet" treated as empty) each fail a test.
- Pixel 9 walk (2026-11-02, debug build with the flag on in a scratch copy; source flag still false, `LaunchFlags.kt` unchanged): empty hero, Add first casualty -> Learn -> back one level at a time, a test entry (GTDCL), populated hero (orange stripe, one chip), chip -> Bank, the test entry deleted and the hub back to empty. Two bugs found and fixed test-first: (1) the entry form's To field said "(optional)" while the casualty's row requires it (`OrdersBankStrings.toLabel(required)`, `Part2EntryToLabelUiTest`; `BankTestBase` now takes a lookup); (2) the hub's Bank card now always opens the whole list (a filter left by the chip no longer hides the rest; test extended).
- Add to Orders Bank is now pinned under the casualty page's list (it was the last item, so the path from "Add first casualty" ended in a scroll to the end). `Part2AddPinnedUiTest` (phone-sized screen, one Add, on screen without scrolling). Walked on the Pixel 9 (2026-11-02): on screen at once, unmoved while the page scrolled, tap opens the form (To shows "To"), back returns.
- Known and accepted: on the very first open of a session the card appears a frame after the rows (it waits for the first read); the 48 dp touch target comes from the Material button default and is not asserted. Walk on the Pixel and the iPhone (Phase 6 walk) still to do.
**Phase 8 item 3 (owner, 2026-11-02): Add lands on the Bank, says it saved, and the keyboard stops hiding the field.** Found on the Pixel: after Add the officer was back on the casualty page, which looked unchanged and still offered Add, so a second tap opened a blank form (a duplicate in waiting); Back then took four taps to reach the Bank. The Year field was also hidden behind the keyboard. DECIDED (owner, grilled 2026-11-02):
- A. After a successful Add of a NEW entry the officer lands on the Orders Bank (Casualties tab, filter All) and the new row is scrolled into view. The Learn levels leave the stack: entered from the Bank the stack is cut back to the Bank; entered from the hub it becomes Home, Hub, Bank. Back from the Bank goes to the hub. Saving an edit still pops one level (it was opened from the Bank or an order).
- B. A shared snackbar (none existed), copy in `OrdersBankStrings`, code only (never dates or notes), memory only: Add says "<code> added. Order copy not attached yet" with an Attach order action that opens the attach flow with this casualty ticked; edit save says "<code> saved"; delete says "<code> deleted"; neither has an action. Sits above the bottom bar, disappears by itself.
- C. No duplicate warning and no discard prompt on Back (both as before).
- D. Keyboard, Part II only, ONE shared helper used by the entry form, the attach-order form and the Learn search: bottom space for the keyboard plus the focused field scrolled into view; on the entry form Next on each text field, Done on the last (dates are pickers, skipped). DEBT logged, not fixed here: the Guide search, the notes editor and the TA/DA worksheet have the same gap. iPhone keyboard check goes in the Phase 6 walk.
- Order: A, then B, then D; tests first; each step ends on a green build.
- A BUILT (2026-11-02): `GuideNavState.landOnBank()`, `BankViewModel.revealEntry` + `bankRowIndex` (scroll to the new row; mirrors `BankScreen`'s item order, guarded by `Part2AddLandsOnBankUiTest`), tests `GuideNavLandOnBankTest`, `Part2AddLandsOnBankUiTest`. Mutation checked.
- B BUILT (2026-11-02): `BankNotice` + `BankNoticeHost` (Material snackbar, Long, over the foot of every Part II level via `Part2LevelWithNotices`), `BankViewModel.announce`; copy `OrdersBankStrings.notice*`; test `Part2SaveNoticeUiTest` (6; two mutations fail it). The Attach action pushes `Part2Destination.Attach(entryId)`.
- D BUILT (2026-11-02): `Modifier.aboveKeyboard()` (`ui/components/KeyboardAware.kt`, = `imePadding()`) on the entry form, the attach-order form and the Learn list, plus `Modifier.scrollToWhenFocused()` on every text field of the two forms; Next on every text field but the last, Done on the last (entry form: the note; attach form: the last order number). CORRECTION from the Pixel walk: a first cut dropped `scrollToWhenFocused` because a Robolectric test with a simulated 300 dp keyboard passed without it; on the phone the Next key moved focus to Year BEHIND the keyboard (the list had shrunk, but nothing scrolled the new field up). The helper is back and the phone shows Year, Note and the attach form's fields above the keyboard. The simulation does NOT reproduce that path (it passes with the helper switched off), so `KeyboardAwareTest` (3) guards the padding half only; the scroll half is proven by the walk. DEBT: Guide search, notes editor and TA/DA worksheet lack `aboveKeyboard()`.
- BUG FOUND BY THE WALK, fixed test-first: tapping the entry just added opened the NEW-entry form (title "Add to the Orders Bank", Add, no Delete), because the Bank kept the editor it opened for the new entry and handed it back for the same id (older than this item; landing on the Bank made it one tap away, and it would have shown "added" and the Bank landing on a plain edit). `BankViewModel.markSaved` now lets the editor go, so a reopened entry is read from the store. Tests: `BankViewModelTest` (reopens as a stored entry) and `Part2AddLandsOnBankUiTest` (tap the new row: edit title and Delete, no Add form); the UI test fails without the fix.
- WALKED on the Pixel 9 (2026-11-02, debug build with the flag on in a scratch copy, deleted afterwards; `LaunchFlags.kt` unchanged, sha256 b683d1d6...; the phone keeps that debug build): Guide Home on one screen with the Part II tile; hub card ("1 casualty noted", chip); Learn search with keyboard (results reachable); casualty page with the pinned Add; entry form with date pickers, Next from No of days to Year (scrolled up) to Note (Done); Add lands on the Bank with the new row in view, All (2) chip and the snackbar "GTDCL added. Order copy not attached yet" with Attach order; Attach order opens the attach form with that casualty ticked (Next skips the date picker, Done on the last field); Back from Attach returns to the Bank; opening an entry shows the edit form; Save shows "GTDCL saved" with no action; Delete (confirm) shows "GTDCL deleted"; Back from the Bank is the hub, then Guide Home. The two walk entries were deleted; the owner's GTDAL entry was not touched. Seen but left: the snackbar covers the foot of the last row for about ten seconds (scrollable behind it); iPhone walk is the Phase 6 one.

**Phase 8 item 4 (owner, 2026-11-02): the entry form's prefix/suffix take the calendar picker, and the day count pre-fills.** Found on the Pixel in the Orders Bank entry form (GTDAL): prefix/suffix were plain text boxes, and "No of days of AL" had to be typed although From and To already say it.
- A. A Data box whose manual caption asks for dd/mm/yyyy (prefix, suffix, "Part II Order dd/mm/yyyy", ...) opens the same picker as From/To, with Clear. Stored as dd/mm/yyyy text in the same Data column: NO schema, backup or migration change. A typed value that is not a real date now fails the form check (`INVALID_DATE`).
- B. A Data box whose caption is a day count ("days" as a word, not a date, not a running "total ...") is pre-filled with the From-To span, both ends counted (`DutyDays` rule, 01/11 to 02/11 = 2). It follows later date changes only while it still holds the old span or is empty; a count the officer typed stands, and clearing it to retype is not undone.
- Rule SSOT: `EntryDataRules` (caption scan, no regex: `Part2LearnSecurityContractTest` bans it in Part II domain code) and `EntryForm.withSpanDays`. Captions are the only type information the bundle has, so a caption the manual words differently ("Gazette / Notfn Date") stays a text box.
- BUILT (2026-11-02): `EntryDataRulesTest` (10) green, `EntryFormTest` and all Part2 composeApp tests green, ktlint and tech-debt audit clean. Pixel walk pending.

PENDING FROM 7c, owner's word given ("clear your synthetic data from app"), NOT DONE: the 50 synthetic orders (order number SYN/0001/2026, date 20 March 2026, 500 pages) merged into the owner's vault on the Pixel. Plan: match only that marker on the shelf and delete those orders one at a time (Delete order); never "Reset App & Clear Data"; real orders are never opened or read. Alternative: Replace-restore the owner's own backup (owner's password, owner's hands). The walk was stopped before any deletion; no order has been deleted yet.

The owner found a few minor UX issues while walking Phases 4 and 5 and will list them all together. They are addressed in one pass, after the remnants of Phase 7 (the split page backup and the rest of its open points), not scattered through earlier phases. Nothing is written down here until the owner gives the list. DECIDED by the owner (2026-11-01): the order is Phase 7, then Phase 8, then Phase 6 (release), so the release walk and the one iPhone walk cover the final screens once.

### Phase 6: Release (runs LAST: after Phases 7 and 8; owner, 2026-11-01)
- Work: full gate on the final commit; `assembleRelease`, R8 and manifest checks; a release-APK walk on the Pixel (after the owner's
  `.pcda` backup); one iPhone walk from a new `IPHONE_CHECKLIST.md` for this feature (no TestFlight before it, and a lane guard in
  `iosApp/fastlane` that refuses an upload while the Part II flag is on and no walk marker exists); store wording rewritten for the smaller
  feature (no notification permission); the owner enters Play and App Store Connect. The flag is flipped only on the owner's word, in its own commit.
- The owner's own check: scan one real, old, yellowed order and say whether it reads (nothing is read or recorded by me).

## Risks and controls

| Risk | Control |
|---|---|
| Annexure III is read wrongly | 63 rows have an independent oracle; script checks; owner spot-check of 25 |
| The Pixel database fails to open | Upgrade tests from 14 and the old 15; the owner's backup before any install |
| A copied file drags in the old engine | Copy with tests, build after each group; the provenance list shows what came |
| Scope grows again | v1 list above is frozen; ideas go to a backlog file; only a defect in v1 reopens it |
| iOS first runs on a real phone only at the end | One walk, with a checklist written in Phase 4; Swift XCTest and Native timing tests earlier |
| Premium becomes real money | The Bank's gate is tested in Phase 3, not hidden behind the free-launch flag |

## Owner decisions (2026-10-30)

| # | Topic | Decision |
|---|---|---|
| 1 | Storage | Schema 16 with a new `part2_entries` table; `part2_drafts` is deleted (the Pixel's synthetic drafts are lost); `part2_orders` and `part2_pages` stay byte-identical. |
| 2 | General fixes | The four fixes are the first commits of `feature/orders-bank` (no separate PR to main). |
| 3 | Old branch | Local tag `archive/part2-orders-2026-10-30` created on `22554101`. Pushed to origin on 2026-10-30 together with `main` (one pre-push gate run, 229 s, passed); no workflow triggers on tags. |
| 4 | Names | Reuse `FeatureGate.PART2_ORDERS` and `LaunchFlags.PART2_ENABLED`; the flag defaults to `false` and is flipped only on the owner's word, in its own commit. |
| 5 | Plain-English descriptions | Exactly 30 pages, taken from four groups: leave and ration / R&H; postings and moves; year-end leave, claims and cancels; career and family. The list was fixed by the owner on 2026-10-30 and is in the appendix below. |
| 6 | Gate cadence | The full gate at every phase end. `assembleRelease` and the R8 keep-check run in the phases that add serialized models or Room tables (0, 1, 3, 4) and at release (6); phases 2 and 5 run R8 minify only. |
| 7 | Entry point | The "Insights ribbon" is the Part II row on the Guide home. The old Dashboard "Due" card is dropped. |

| 8 | Sibling serials | Serials that differ only by a rate or class share one plain-English page, counted once: R&H grants 3.1 to 3.11, HRA 3.55 to 3.57, children's education 8.2 to 8.5, hostel subsidy 8.6 to 8.9. The 30 pages cover 48 serials. Every other serial shows manual data only. |
| 9 | Page shape | Four short parts, about 90 words: what it is; when you will see it; what to keep ready (the columns); one thing to watch. No rates or amounts. |
| 10 | Sources | ODP 2026 for the order wording, the columns and every rule. The 2023 Pay and Allowances Handbook only for what an allowance is, and each such sentence carries the handbook page. Nothing from memory or common practice. |
| 11 | Column spot-check | 25 rows: 10 from the 30 pages plus 15 seeded-random from the rest, with the oddest wording (long "Required as ..." cells, blank cells) guaranteed in. Seed and draw recorded in DECISIONS. Done on a local text sheet beside the ODP PDF; ODP page images are never published. Pass rule: zero wrong cells, otherwise the chapter is re-extracted and 10 more of its rows are checked. |
| 10a | Sources, amended (owner, 2026-10-30) | The app quotes no book. The 30 pages are written in our own simple language, culled from the ODP and the PCDA handbooks; no quoted sentences and no page citations ship in the app. For the owner's reading, each page's sources (ODP page, handbook page) are kept in a local audit sheet that is never shipped. Nothing from memory. |
| 10b | ODP vs old templates (owner, 2026-10-30) | Where the printed ODP and the old hand-built templates differ (8 cells in 7 casualties), the ODP page wins. |
| 12 | Column captions | The manual's own words minus the "Required as" boilerplate (for example "Prefix (dd/mm/yyyy)", "No. of days"). Required, optional or none is a separate field. No paraphrase. |
| 13 | Phase 2 answers (owner, 2026-10-30) | Learn entry row sits after the existing Insights Part II ribbon on the Guide home. A required column with no printed caption (19 casualties) shows "required (see ODP page N)"; the manual's own note wording for those columns is backlog (Version B, casualty by casualty, owner reads). "Usually published with" shows only where data exists (15 casualties). The branch is not pushed until after Phase 2. |
| 14 | Phase 2 walk and Phase 3 answers (owner, 2026-10-30) | Learn search box never takes focus by itself. Learn row sits in its own Guide-home section "Part II orders (ODP 2026)" below the handbook areas. Phase 3: the two status labels read "Part II Order Copy not Attached" (orange) and "Part II Order Copy Attached" (blue); chips read All / Not attached / Attached. Age text "Waiting 6 months" (days under 30, then whole months, then years and months), counted from the From date. The Bank is Premium with no free allowance; Learn stays free; a lapsed officer can view, delete and back up but not add. An entry starts from a casualty page or from an Add button on the Bank list that opens Learn's search. Form: From required date, To optional date, every other used column a short text box labelled with the manual's caption (required ones filled before Add; no caption-based rules). Empty-state and "last backup" wording drafted by the agent for the owner to read on the phone. The branch is pushed only after Phase 3, on the owner's word; the debug build stays on the Pixel. |
| 15 | Phase 4 answers (owner, 2026-11-01) | All agent recommendations taken. (1) Scanner results: cancel is silent and saves nothing; unavailable, other failure, dropped pages ("N of M pages saved") and order-full each have their own sentence; none names an order, number or date. (2) OrderScreen label "Personal reference copy, not for PCDA(O) submission"; chain flag only when a quoted last order number matches none of the other orders ("This order quotes last order no. {x}, which is not in your orders yet. A page of the chain may be missing."); no quote, no flag. (3) Counter "3 of 5 pages" when N is known, "3 pages" when not; over-count shown unchanged. (4) A lapsed officer cannot scan, add pages or attach (Phase 3 rule kept). (5) Vault size shown in Settings (always) and on the Bank (warning past the threshold), one text function; the 100 MB cap stays for Phase 4 with a per-page size budget and a measuring test. (6) Shelf: year headers, newest first, "Date not entered" last; row = order number, date, "N casualties", counter, "Check chain" when flagged. A split, incremental page backup phase is added before release (Phase 7 point 11). |
| 16 | Phase 5 answers (owner, 2026-11-01: "all recommended") | (1) Columns: casualty code and name, From/To, status words, order number and date, counter, "Check chain"; never the note, the Data columns, page images or anything identifying. (2) By year of From, newest first, year headings kept with their first row, page number on every page; "about 30 rows per page" was my estimate and is wrong: a row is 2 to 3 lines, so a full page holds 15 rows with an order and more for waiting rows. (3) Title and fixed job name "Part II orders index", "Printed on dd/mm/yyyy" (local day), the Phase 4 notice "Personal reference copy, not for PCDA(O) submission". (4) A lapsed officer may print. (5) An empty Bank offers no button and prints nothing. (6) Notes and Data are never printed (text-scan test). (7) Nothing is written by the app: the system dialog ("Save as PDF" on Android, the print sheet on iOS) is the only exit; no share sheet, no file. |

## Appendix: the 30 plain-English pages (owner, 2026-10-30)

Leave and ration / R&H (10): 5.1 GTDAL, 5.2 GTDALCF, 5.4 GTDCL, 5.6 FURLEV, 5.11 TERLEV, 3.82 GTDRSHNA, 3.86 SORS, 3.87 TORS, 3.1 (R&H, shared with 3.2 to 3.11), 3.12 STPRH.
Postings and moves (7): 13.1 POSIN, 13.2 POSOUT, 13.5 JNTIME, 13.3 SPR, 13.7 TYDUTY, 3.53 GTDTRAN1, 3.55 (HRA, shared with 3.56 and 3.57).
Year-end leave, claims and cancels (7): 5.26 ACCLVE, 5.27 LVELTCAD, 5.28 LVELTC, 14.1 CANCEL, 14.2 CANCELS, 8.2 (children's education, shared with 8.3 to 8.5), 8.6 (hostel subsidy, shared with 8.7 to 8.9).
Career and family (6): 4.1 ASMAPPT, 4.2 RELAPPT, 4.3 SUBPROM, 12.1 PROCRS, 11.1 MARRIAGE, 11.9 BTHCHL.

Held back as backlog candidates: 3.58 STPHRA, 5.30 ENCASH, 5.19 OSL, 5.7 MTRNLV, 5.13 PTRNLV, 11.4 NOKCHNG, 12.2 REJCRS, 2.2 RETIRE.

Verified 2026-10-30: the ODP PDF's page numbers equal its printed page numbers (PDF page 114 is printed page 114), and Annexure III is legible there with all nine columns. Only that page was checked; Phase 1 extraction checks the rest.

Still to be answered when Phase 1 starts: nothing blocking.
