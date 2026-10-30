# Part II Orders Bank — Execution Plan, part B: Phases 3 to 6

Status: draft for owner sign-off, 2026-10-30. Continues `13a_OrdersBank_Execution_P0-P2.md`; the rules, the per-phase loop, the Phase Summary
and the gate in that file apply to every phase here without change.

## Phase 3: Orders Bank, note it (Premium; size: medium)

Objective: the officer adds a casualty to the Bank, sees it in date order with a waiting age, edits and deletes it; it is encrypted, in the
backup, and gated.
Read first: old `Part2Codec.kt`, `Part2Payloads.kt`, `RoomPart2Repository.kt`, `Part2Backup.kt`, `PageArchive.kt`, `Part2PageRestore.kt`,
`Part2BackupRows.kt`, `Part2Bounds.kt`, `Part2Limits.kt`; main's `PortableBackup.kt` (version 4), `PayslipBackupService.kt`, `PayslipDao.kt`
(clear-all and restore), `SubscriptionManager.kt` (`FeatureGate`), `PremiumFeaturesCatalog.kt`, `BackupRestoreSettingsCard.kt`; the nearest
pattern for a device-local setting (found in step 1); `GuideNoteEntity` for the encrypted-row pattern.

Steps (tests first in each):
1. Domain, pure: `BankEntry` (code, From, optional To, column values, note, optional order id, times), `EntryForm` (from the casualty's
   `columns.json` row: which fields exist, which are required, caption per field), `BankStatus` (derived: waiting unless the entry's order
   id names an existing order; a dangling id reads as waiting, so deleting an order needs no cascade write), `BankIndex` (order by From, group
   by year, age text such as "waiting 6 months" computed from an injected clock). Tests: each column rule; From not after To; caps from
   `Part2Limits`; a code the bundle does not know is refused; status flips with the order's existence; year grouping at the boundary; age text.
2. Data: `EntryPayload` (`v: 1`, id repeated inside the ciphertext), `Part2Codec` for entries, `Part2Repository` with entry methods, `RoomPart2Repository`
   over `part2_entries`. Tests: round trip through real crypto and in-memory Room; ciphertext holds no plaintext (scan the bytes); an unreadable row is
   counted and kept, never shown or deleted; `DaoCaps` (count and size caps hold under two racing writes); a foreign `v` is kept untouched.
3. Backup `.pcda` v5, chunked (pages as binary records, one page in memory at a time), with `entries` in place of `drafts`. Tests (copied then
   adapted): export and restore round trip, bounded restore (oversized and surplus rows skipped and counted before decoding), a v4 file restores with an
   empty Bank, a v6 file is refused, a v5 file from the old branch restores orders and pages and counts its drafts as skipped, the 100 MB ceiling,
   `Part2VaultScaleJvmTest` in a 256 MB heap and its iOS twin for memory and time. `BackupGuard` (copy): Back Up works with a payslip or any Bank
   item; restore stays free; the empty-device line reads "Nothing to back up yet."
4. Gate: `FeatureGate.PART2_ORDERS` with its catalog entry (advertised only while the flag is on); tests under free launch, force free and force pro;
   a lapsed officer can view, delete and back up but not add.
5. ViewModels and UI: `BankViewModel` (list, chips All, Waiting, Received, the shelf lens switch is Phase 4), `EntryEditViewModel`; `BankScreen`, `EntryScreen`;
   "Add to Orders Bank" on the casualty page enabled; waiting and received colours as theme tokens with a contrast test in both themes;
   the "last backup" line (device-local setting, never in the encrypted database). Strings in `OrdersBankStrings.kt`.
6. Navigation: `Part2Destination.Bank` and `Entry(entryId)`; ids only; the saver and `retainKnown` drop an entry the repository no longer has;
   the lock screen discards an open entry form.

Files (budget): `BankEntry` about 60, `EntryForm` about 120, `BankIndex` about 100, `Part2Codec` about 130, `Part2Backup` 98, `PageArchive` about 160, screens about
70 each, `OrdersBankStrings.kt` under 200, `PayslipBackupService.kt` and `PortableBackup.kt` touched by the small old-branch diff only.
Security: key points from plan 13 item 6; the telemetry test proves nothing from an entry reaches `Logger` or an exception message; restore never decodes
before checking size and count.
Gate: R8+ (new serialized models, Room); `check_merged_manifest.py` unchanged.
Debt watch: `PayslipDao.kt` (204 on main) clear-all and restore must learn the new tables; `PortableBackup.kt` size; a second place that decides "waiting".
Exit: gate green; Pixel walk on synthetic data: add, edit, delete, kill the process, lapse to Free, backup and restore through the picker.
Owner items: the wording of the three status labels and the empty state; the age text.

## Phase 4: Orders Bank, prove it (size: large)

Objective: attach a scanned paper order to many casualties, with the counter, the chain check and the orders shelf.
Read first: old `PageScanner.kt`, `PageEncoder.kt`, `PageCapture.kt`, `JpegMetadata.kt`, `PageGeometry.kt`, `AndroidPageScanner`, `MlKitScanSource`,
`ScanResultBridge`, `AndroidPageEncoder`, `IosPageScanner`, `IosPageEncoder`, `DocumentScanPresenter.swift`, `VaultViewModel`, `VaultOrderViewModel`,
`VaultSection`, `VaultOrderScreen`, `VaultNotices`, `VaultSizeWarning`; main's `Info.plist`, `libs.versions.toml`, `scripts/merged_manifest_baseline.json`.

Steps (tests first in each):
1. Characterization: copy the page-pipeline tests (`PageCaptureTest`, `PageCaptureCleanupTest`, `JpegMetadataTest`, `JpegFitTest`, `PageGeometryTest`, `PageArchiveTest`,
   `AndroidPageEncoderTest`, `AndroidPageScannerTest`, `MlKitScanSourceTest`, `ScanResultBridgeTest`, `IosPageEncoderTest`, `IosPageScannerTest`, the Swift
   XCTest) and run them green against the copied sources before editing anything.
2. The one new library: `play-services-mlkit-document-scanner` pinned in `libs.versions.toml` at a version at least two weeks old with advisories checked;
   `NSCameraUsageDescription` in `Info.plist` with a test that it equals the string object; the manifest check shows exactly what the library adds, each
   entry reviewed and recorded with a reason in the baseline.
3. Domain: `OrderCompleteness` (entered of N; unknown N shows only the entered count), `ChainCheck` (an order's quoted last order number against the
   other orders' numbers, trimmed and case-folded; no quote means no flag), `OrderLink` rules (an order covers at least one casualty; unlink and delete
   rules). Order payload gains optional `expectedCount` and `lastOrderNo` inside `v: 1`; an order row from before reads fine.
4. `AttachOrderViewModel` state machine: scan (up to `PAGES_PER_ORDER_MAX`), number and date (expected, bounded by `ORDER_NO_MAX`), count N, last order
   number, tick other waiting entries, add a casualty from the wiki, save. Tests cover every scan result (`Cancelled`, `Failed` both reasons, dropped pages),
   a cancel leaves nothing, a failed save discards the scanner's page, and process death during the flow loses only the scan.
5. Screens: `AttachOrderScreen`, `OrderScreen` (view and hide a page, add pages, delete a page, delete the order with confirmation, the casualties on it,
   the label "personal reference copy, not for PCDA(O) submission"), `OrdersShelfScreen` (text rows by order date, grouped by year, opening an order),
   the lens switch on the Bank. Thumbnails are not built (backlog).
6. Lock and lifecycle: leftover scanner pages are discarded when the Bank opens, when a restored order level appears and when the app locks.

Files (budget): `PageCapture` 105, `AttachOrderViewModel` under 250 (split state from actions if near 280), `VaultOrderViewModel` 205, screens at most 125 each.
Tests: unit, integration (real scanner fake, real encoder, real crypto, in-memory Room), Compose UI, Swift XCTest, `iosTest` for encoder time and memory.
Security: pages re-encoded (no Exif or GPS, tested), never written to the gallery or the cache after use, the ML Kit cache emptied on success and failure,
no OCR, no network, no log of an order number or date, `FLAG_SECURE` is not decided (recorded, not changed), backup screen warns about cloud storage.
Gate: R8+ with the manifest check.
Debt watch: the plaintext-in-memory residual is accepted as on the old branch (decoded bitmaps are freed, not overwritten); recorded, not hidden.
Exit: gate green; Pixel walk with a public printed page (never a real order): scan, attach to several casualties, counter, chain flag, shelf, kill,
delete an order and see its casualties return to waiting; the scanner cache is empty afterwards.
Owner items: your own scan of one old yellowed order after the phase, and your word on whether it reads (nothing is read or recorded by me).

## Phase 5: Print or PDF of the index (size: medium)

Objective: a clean paged list of the Bank, by date, through the system print dialog on both platforms.
Read first: old `SlipLayout.kt`, `SlipDocument.kt`, `SlipPrinter.kt`, `AndroidSlipPrinter`, `SlipPrintAdapter`, `SlipPdf`, `PrintJobWatch`, `IosSlipPdf`,
`IosSlipPrinter`, `PdfLayoutSpec` on main.

Steps: tests first for `IndexLayout` (pure paging: rows per page, year headings, order number and date, status text, page numbers, an empty Bank
prints nothing), a text-scan test that the page carries no identity data and only what the officer typed, local-date "printed on" (the old `localUtcOffsetMillis` pattern),
the print adapter over fakes, a print job that is cancelled leaves no record. Then `IndexLayout`, `IndexPrinter` (interface), Android and iOS implementations
adapted from the slip printer, one button on the Bank (Premium, view-only for a lapsed officer). Colours from `IndexLayoutSpec` beside `PdfLayoutSpec`.
Files: `IndexLayout` under 150; each platform printer under 100. Security: nothing is shared through a share sheet, no file is left on disk, the job name is fixed.
Gate: sections 3 items 1 to 6; R8 minify. Debt watch: copied slip names that still say "slip". Exit: Pixel print to PDF and, if available, a real printer.

## Phase 6: Release (size: medium)

Objective: a release the owner can put on the Android internal track and an iPhone walk that closes the iOS side.
Read first: `docs/Launch/10_part2_store_wording.md`, `iosApp/fastlane` and `composeApp/fastlane` lanes, `project_release_1_3_0.md` procedure, `Info.plist`,
`PrivacyInfo.xcprivacy`, the merged-manifest baseline.

Steps:
1. Full gate on the final commit; `assembleRelease`, `check_r8_guide.py`, `check_merged_manifest.py`; the flag-on run; the `security-review` skill over the
   whole branch diff, findings fixed or recorded as accepted residuals with the owner.
2. The owner's `.pcda` backup first, then a release-APK walk on the Pixel: Learn, add, scan, attach, shelf, print, backup and restore, lapse to Free.
3. `IPHONE_CHECKLIST.md` for this feature (written in Phase 4, completed here), including Dynamic Type and VoiceOver; the owner's one walk; findings
   fixed and the affected lines re-walked.
4. A lane guard in the iOS TestFlight and App Store lanes: the upload refuses to run while the flag is on and no "iPhone walk done" marker exists
   (read the lanes before writing it; tests in `scripts/tests`).
5. Store wording rewritten for the smaller feature (no notification permission; the document scanner's Play-services note; Data safety unchanged in
   kind), reviewed against the real merged manifest; the owner enters it in Play Console and App Store Connect.
6. Version bump and release notes by the existing release procedure. The flag is flipped only on the owner's word, in its own commit. Push and PR only on the owner's word.
Exit: gate green; the walk results recorded; every open item either closed or written into a backlog file (not this plan).

## Traceability: v1 scope to phase and proof

| v1 item (plan 13) | Phase | Proof |
|---|---|---|
| 1. Wiki of 303 in 10 ribbons, flat list with case headings, search | 1, 2 | Real-bundle contract (303/321/10), UI test, search timing on Native |
| 2. Casualty page: columns, about, usually published with, Add | 1, 2, 3 | Columns checker and oracle, spot-check, UI test |
| 3. Merged list in date order; entry fields as ODP requires | 3 | `EntryForm` tests for each need, `BankIndex` order |
| 4. Derived status, chips, waiting age, year groups | 3 | `BankStatus`, `BankIndex`, UI test |
| 5. Attach order: scan, number and date, count N, last order, tick, add | 4 | State-machine tests, integration over fakes, Pixel walk |
| 6. Chain check | 4 | `ChainCheck` tests |
| 7. Orders shelf (text rows) | 4 | Shelf UI test |
| 8. Print or PDF of the index; last backup line; reference-copy label | 3, 4, 5 | Layout and text-scan tests, UI tests |
| Premium gate, backup, encryption | 3 | Gate tests, backup bounds tests, ciphertext scan |
| Android first, one iPhone walk, stores | 6 | Walk record, lane guard test |

## Where the owner is needed

| Phase | The owner does |
|---|---|
| 0 | Answers the six decisions; approves the schema 16 step; says when the old tag may be pushed |
| 1 | Spot-checks about 25 column rows; picks about 30 casualties; reads their plain-English text once |
| 2 | Confirms the entry row's place; looks at Learn on the phone |
| 3 | Approves the status words and the empty state |
| 4 | Scans one old yellowed order after the phase |
| 5 | Prints one index |
| 6 | Takes the backup; walks the iPhone; enters the store wording; says when to flip, push and release |

## Risks and controls (additions to plan 13)

| Risk | Control |
|---|---|
| A copied test drags in the old engine | Copy with its test, build after each group; the provenance list names what arrived |
| Dangling order id after an order is deleted | Status is derived from the order's existence; a test deletes an order and checks every linked entry |
| The extraction misreads a column | Oracle for 63 rows, script checks, owner spot-check; a wrong row is a one-line data fix with a test |
| The release differs from the debug build | R8+ in every model phase and the Phase 6 release walk |
| The plan grows again | Anything new goes to a backlog file; only a defect in v1 reopens scope; the Phase Summary lists no new scope |
