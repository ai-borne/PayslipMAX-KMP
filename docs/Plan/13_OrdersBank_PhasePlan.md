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

Backlog (not v1): order photo thumbnails, scanner quality for yellowed paper, PDF/image import, HRMS summary-sheet back-fill, OCR,
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

| Phase | Status | Size (estimate) |
|---|---|---|
| 0. Branch, baseline, general fixes, storage proof | planned | small |
| 1. ODP content: the 303-row column table, descriptions, related, bundle | planned | large |
| 2. Learn about Part II (free) | planned | medium |
| 3. Orders Bank: note it | planned | medium |
| 4. Orders Bank: prove it (scan, attach, counter, chain, shelf) | planned | large |
| 5. Print or PDF of the index | planned | medium |
| 6. Release: hardening, walks, store wording | planned | medium |

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

### Phase 6: Release
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
| 5 | Plain-English descriptions | About 30, taken from four groups: leave and ration / R&H; postings and moves; year-end leave, claims and cancels; career and family. The exact list is fixed with the owner at the start of Phase 1. |
| 6 | Gate cadence | The full gate at every phase end. `assembleRelease` and the R8 keep-check run in the phases that add serialized models or Room tables (0, 1, 3, 4) and at release (6); phases 2 and 5 run R8 minify only. |
| 7 | Entry point | The "Insights ribbon" is the Part II row on the Guide home. The old Dashboard "Due" card is dropped. |

Still to be answered when the phase starts: nothing blocking Phase 0.
