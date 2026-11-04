# 14 — Backup & Restore Hardening — Phase Plan

Goal: a `.pcda` backup written by **any** shipped version, on **either** platform, restores on every later version — and the
officer always sees what is happening and what came back. Base branch: `feature/orders-bank` (backup v5 lives there).

## Findings that drive this plan (all verified in code, 2026-11-03)

| # | Pitfall | Where |
|---|---|---|
| F1 | The archive serializes Room entities directly, so a DB schema change silently changes the file format | `PortableBackup.kt` (`EncryptedPayslipEntity`, `PayslipPdfEntity`, `AppSettingsEntity`, draft entities) |
| F2 | The inner payslip JSON uses strict `Json` (unknown key → whole restore fails); outer/notes/corrections are lenient | `EncryptedPayslipEntity.kt:82` |
| F3 | Goldens exist only for v1–v3, all synthetic; nothing pins v4 (1.5.0) or v5, nothing from iOS | `GoldenBackups.kt` |
| F4 | A backup is never read back; a bad file is found only at restore time | `PayslipBackupService.exportTo` |
| F5 | Unclassified failures show one generic line and log nothing diagnosable | `RestoreFailureStatus.kt` |
| F6 | One unreadable payslip/correction fails the whole restore (Part II already skips-and-counts) | `PayslipBackupService.restore` |
| F7 | A failed backup shows raw `exception.message` on screen (can carry a path) | `BackupRestoreSettingsCard.kt:225` |
| F8 | REPLACE restores the backup's PIN lock and telemetry consent onto the device | `AppSettingsEntity` in `PortableBackup` |
| F9 | PBKDF2 iterations are fixed (10,000) and not recorded in the file, so they can never be raised without breaking old backups | `CryptoHelper.kt:38` |
| F10 | An older app can't recognise a newer file: production 1.5.0 (v4) given a v5 `PCDB` file shows the raw "OPENSSL_internal:BAD_DECRYPT" error instead of "update the app" (reproduced on the Pixel 2026-11-03; the likely cause of the owner's original failure) | no plaintext version in the file; `restoreFailedStatus` passthrough in 1.5.0 |
| U1 | No progress, buttons stay live (double tap), sheet dismissable mid-operation, no preview, no result summary, single password field | `BackupRestoreSettingsCard.kt` |

## Invariants (become tests, never relaxed)

1. Every committed golden (v1…vN, Android + iOS) restores on head with exact counts.
2. A backup is handed to the share sheet **only after** it has been decrypted and decoded back successfully.
3. A restore writes nothing until everything is decoded; the write is one transaction (already true — keep it).
4. Nothing is skipped silently: every skipped row is counted and shown **before** the officer commits.
5. Wire format changes only by a `CURRENT_VERSION` bump plus a new golden; every added field has a default.
6. No PII in any error, log or telemetry key (`TelemetrySanitizer`).

## Phases

### P0 — Real-world goldens (characterization, no production code)
- Owner installs 1.5.0 from Play (Android) and App Store/TestFlight (iOS), adds the synthetic test payslip set, backs up with the golden password, and hands over both files.
- Add `v4Android150`, `v4Ios150` (hex) and a `v5` golden (this branch, with Orders Bank entries, orders and 2 pages) to `GoldenBackups`; split the file per version if it nears 300 lines.
- Tests: each restores with exact counts. **Expected: this is where the earlier 1.5.0 failure reproduces; fix it here, with root cause written in this doc's changelog.**
- Exit: all goldens green on JVM + `iosSimulatorArm64Test`.

### P1 — Backup format decoupled from the DB (F1)
- New `repository/backup/` package: `@Serializable` wire DTOs (`PayslipBackupRow`, `PdfBackupRow`, `SettingsBackup`, `DraftBackupRow`, …) with **the same JSON keys** as today, plus mappers to/from entities.
- `PortableBackup` holds DTOs only; Room entities drop `@Serializable` where nothing else uses it.
- `SettingsBackup` carries user preferences only; device-bound fields (`isPremiumEnabled`, `appPinHash`, `isLockEnabled`) are excluded at the type level (today they're stripped by code).
- Tests: all P0 goldens still restore (proves wire compatibility); a DB-only column added in a test entity doesn't change the exported JSON keys.
- R8: run `minifyReleaseWithR8`; add narrow keep rules for the DTOs if needed.

### P2 — One lenient decoder (F2)
- `BackupJson` SSOT (`ignoreUnknownKeys`, `coerceInputValues`, `explicitNulls = false`) used by the outer doc, payslip payload, corrections, notes and Part II rows. The device-side payslip read gets the same tolerance.
- Guard test: each DTO decodes from its oldest golden shape plus an unknown key; a test fails when a DTO field has no default value (except ids).

### P3 — Prepare / commit split + verify-after-write (F4)
- `restore` becomes `prepare(source, password): RestorePreview` (decode, re-encrypt, counts, skipped, backup date and version) followed by `commit(preview, mode)`. Same transaction as today.
- `exportTo` runs `prepare` on the just-written file (dry run, no DB write); if it fails, the file is deleted and the officer is told **before** the share sheet.
- Memory: pages stay a lazy sequence; the verify pass re-reads by range (no second copy of the vault). iOS timing test for a 250 MB vault.

### P4 — Skip, count, show (F6)
- Payslips, PDFs, corrections and drafts adopt the Part II rule: unreadable → skipped and counted per kind.
- `RestorePreview` lists skipped counts; on REPLACE with skips, the confirm step says so explicitly ("2 payslips could not be read and will not be restored").

### P5 — Classified failures + telemetry (F5)
- `BackupFailure` enum: `WRONG_PASSWORD`, `NOT_A_BACKUP`, `NEWER_VERSION`, `DAMAGED`, `TOO_LARGE`, `STORAGE_FULL`, `WRITE_FAILED`, `UNKNOWN`. Each maps to one fixed line in `AppStrings` and to a sanitized Crashlytics key (code + backup version + platform, nothing else).
- Tests: every failure path maps to its code; `TelemetrySanitizerTest` covers the new keys.

### P6 — Backup & Restore UI (U1)
- ViewModel state `BackupUiState`: `Idle` · `Working(stage, done, total)` · `Preview(RestorePreview)` · `Done(summary)` · `Failed(BackupFailure)`. Progress comes from page/row callbacks in the service.
- Sheet: spinner/progress bar with stage text ("Encrypting…", "Checking backup…", "Restoring 34 of 120 pages…"); buttons disabled and the sheet not dismissable while working.
- Backup: password + confirm field; the "Last backup: <date>" line (`LastBackupStorage` already exists).
- Restore: pick → preview card (backup date, version, payslips/orders/pages, skipped) → Replace / Merge / Cancel → result summary ("Restored 48 payslips, 12 orders, 120 pages").
- State: the operation runs in `viewModelScope` (survives rotation); process death mid-restore = nothing applied (transaction), mid-backup = partial file discarded. Back/swipe blocked only while `Working`.
- Strings in `AppStrings`/`BackupRestoreStrings`, colours in `Theme.kt`; composable length check respected (split components).
- Pixel walk (debug build stays installed) + iOS simulator check.

### P7 — Release gate
- Full `check`, iOS tests, `minifyReleaseWithR8`, pre-push. Add to the release checklist: **"New release → export a golden from it and commit it."**

## Phase handoff
Each phase ends with: build green, all tests green (incl. goldens on iOS), tech-debt summary, then stop for the owner.

## Owner decisions (2026-11-03)
- D1 Branch: `feature/backup-hardening`, cut from `feature/orders-bank`.
- D2 No dark-launch flag: one backup/restore code path; the goldens are the regression control.
- D3 No automatic pre-restore snapshot: prepare-then-commit already guarantees a failed restore changes nothing.
- D4 All of F1–F6 and U1 are in scope.

Execution detail (phases P0–P9, gates, R8, security): `14a_BackupRestore_Execution.md` — it supersedes the phase list above.

## Changelog
- 2026-11-03 — plan drafted; D1–D4 recorded; F7–F9 found while writing 14a.
- 2026-11-03 — **P0 done (accepted)**: golden harness (`golden-backups/` + `manifest.json`, disk reader on JVM and iOS, generated path
  constant, goldens declared as test inputs); 7 goldens (v1–v3 converted byte-identical, hex file deleted; 1.5.0 v4 from `9841fc4a` on JVM
  and iOS; v5 with Orders Bank on JVM and iOS); local-only `LocalBackupRestoreTest` on the owner's 4 real files. Every golden restores; no
  production fix needed. Gate green: shared 1698 → 1703, iOS shared 1549 → 1553, composeApp 1440; 6/6 mutations caught; no security findings.
- 2026-11-03 — **P1 done**: wire rows in `repository/backup/` (`PayslipBackupRow`, `PdfBackupRow`, `SettingsBackup`, `DraftBackupRow`,
  `DismissedDraftBackupRow`, `CorrectionBackupRow`, `GuideNoteBackupRow`; every key `@SerialName`-pinned, same keys and defaults as before) and
  `BackupRowMappers.kt`; `PortableBackup` holds rows only; `@Serializable` removed from all 7 entities (grep: no other serializer use).
  D5 at the type level: `SettingsBackup` has no Premium/lock/PIN/telemetry; REPLACE lays the backup's preferences over the device's own row
  (F8 closed). `Part2RowBackup`/`Part2LegacyDraft` keys pinned too. R8 gate walks `repository/backup` (7 models kept). Gate green: shared
  1703 → 1708, iOS shared 1553 → 1558, composeApp 1440/856; 7/7 mutations caught; local real backups restore; security review: no findings.
- 2026-11-03 — **P2 done**: `BackupJson` (`repository/backup/`, `ignoreUnknownKeys` + `coerceInputValues` + `explicitNulls = false`) is the
  one reader for the document, payslip payloads (backup and device `toDomain`), corrections, notes, Part II rows and the page trailer;
  `lenientJson`, `jsonDecoder` and the notes `payloadJson` are deleted, and Part II keeps only its `encodeDefaults` writer. Owner decisions:
  decode-only (writers unchanged, so 1.5.0's strict reader still reads new files: `BackupWriterCompatibilityTest`); no defaults added,
  today's required fields frozen by `BackupDefaultsGuardTest` (descriptor walk, `isElementOptional`); device write keeps plain `Json`;
  trailer in, `FieldSource` map values to P10. RED first: 7 tolerance tests (payslip unknown key, device read, null lists, unknown enum,
  missing nullables, Part II `data: null`, trailer key). Gate green: shared 1708 → 1721, iOS shared 1558 → 1571, composeApp 1440/856;
  8/8 mutations caught; 7 goldens and local real backups restore; Pixel launch clean; R8 not triggered; security review: no findings.
  Pending: 14a P10 "From P2".
- 2026-11-03 — **P3 done**: versioned crypto envelope, format v6 (F9, D6). A new file is a 29-byte plaintext header (`PCDX`, envelope 1,
  KDF 1 = PBKDF2-HMAC-SHA256, 600,000 iterations, format 6, salt length 16, salt) then the `PCDB` page archive, always. One key is derived
  per archive (`ArchiveKey`, AES-256-GCM, random IV per seal, the header as AAD, zeroed on close) and seals the document, every row and every
  page; rows are no longer sealed per row with the password. `BackupEnvelope.read` is the one version check: an unknown envelope, KDF or
  format → `UnsupportedBackupVersionException` before any decrypt (F10); iterations outside [10,000, 5,000,000], a salt that is not 16 bytes
  or a cut header → `BackupDamagedException` before the KDF; no header → the legacy path unchanged. Owner decisions: same row keys under the
  archive key, pages sealed with it, `PCDX` + `PCDB` body, 600,000 iterations. New goldens `jvm-head-v6`, `ios-head-v6`. RED first: 16 new
  tests (envelope bytes, later versions, bounds, tampered header, v6 round trip, `ArchiveKey`, iOS KDF + seal timing). iOS: derive 88 ms;
  250 MB v6 vault restore 2.97 s. Gate green: shared 1721 → 1735, iOS shared 1571 → 1587, composeApp 1440/1414/856; 13/13 mutations caught;
  9 goldens and the owner's local backups restore; R8 triggered and green with no new rule; security review: no findings. Pending: 14a P10
  "From P3".
- 2026-11-03 — **P4 done**: prepare / commit split and verify-after-write (F4). `BackupRestoreEngine` holds the restore:
  `prepare(source, password, progress)` opens, decodes and checks every row and page and re-seals them for this device, writing nothing,
  and returns a `PreparedRestore` with its `RestorePreview` (format, `exportedAt`, counts per kind, Part II skips); `commit(prepared, mode,
  progress)` is the one transaction (MERGE reconciles notes and Part II against the device then). `restore()` keeps its signature
  (prepare + commit). Export: `BackupFiles.write(verify, block)` reads the closed file back through a dry-run `prepare`; a failure or any
  skipped row → `BackupVerifyException`, file deleted, never shared, "last backup" not moved (`backUp()` is the one path the ViewModel uses; the in-memory `export()` that tests use neither reads back nor records, which keeps the
  120-page JVM scale test's export peak at 159 MB instead of 206–224 MB against its 230 MB bound).
  Owner decisions D15–D18 (v6 `exportedAt`; tolerant `FieldSource` map in P5; P8 walk may Back Up and Merge on the Pixel; `BackupJson`
  stays). Pages whose order does not travel are no longer exported (they could only be skipped). RED first: 6 prepare/commit tests (golden
  previews = manifest, nothing written; commit = preview; pages checked once and re-sealed lazily; `exportedAt`; MERGE; progress), 4 verify
  tests, 3 file read-back tests, a ViewModel no-share test. 11/11 mutations caught. Pending: 14a P10 "From P4".
- 2026-11-03 — **P5 done**: skip, count, show (F6).
  - Skip and count: a payslip, correction or note that will not open is skipped and counted, never fatal (the Part II rule). Only
    opening a row is forgiven (`openedOrNull`); a failure to seal for this device stays fatal.
  - `SkippedRows` (`RestorePreview.kt`) is the one per-kind count: payslips, corrections, notes, Bank entries, orders and pages, plus
    a `total`. It is carried by `RestorePreview.skipped`, `RestoreReport.skipped` and `Part2Restore.skipped`. Part II's one number is
    split by kind, and frozen-branch drafts count as entries, the kind that replaced them.
  - A file whose every row that opens one at a time fails (at least one) is `BackupDamagedException`, with nothing written
    (`requireARowThatOpened`).
  - Verify-after-write requires `SkippedRows.NONE`.
  - D16: `FieldSourceMapSerializer` drops a `FieldSource` value this app does not know, on restore and on the device. Writes are
    byte-identical, and `FieldSourceWireTest` pinned them before `Model.kt` was touched.
  - Decisions taken, owner to confirm: a skipped payslip's PDF and corrections still restore; the "every row" definition; drafts count
    as entries.
  - UI: `restoreStatus` shows the total only (P8 owns the per-kind copy).
  - Rewritten, not deleted, because they pinned "one bad row fails the restore": `PayslipBackupServiceReplaceTest` (2 tests) and
    `PayslipBackupServiceV3Test` (1 test).
  - RED first: 18 new tests (characterization 4, tolerance 3, skip/count 6, Part II per kind 4, verify per kind 1), the 3 rewritten
    ones, and 12 existing Part II assertions made per kind. 16/16 mutations caught; the first run missed one, which a production-wired
    MERGE test then closed.
  - Gate green: shared 1748 → 1766 (release 1766, 1 pre-existing skip), iOS shared 1600 → 1618, composeApp 1441/1415/857.
  - R8 triggered and green with no new rule. 9 goldens and the owner's 4 local backups restore with 0 skips of every kind.
  - Pixel: debug build installed over the existing one; launches with no FATAL.
  - Security review: no findings.
  - Pending: 14a P10 "From P5".
- 2026-11-03 — **P6 done**: classified failures and telemetry (F5, F7).
  - `BackupFailure` (shared, `repository/BackupFailure.kt`): 9 values with a stable `code`; `BackupFailure.of` is the one mapper, by type
    and by the type of a cause, never by text. One fixed line each in `BackupRestoreStrings`. `restoreFailedStatus` replaced; the backup
    path no longer shows `exception.message` (F7).
  - New typed refusals: `NotABackupException` (a headerless file under 48 bytes), `BackupTooLargeException` (no page archive, over 128 MB),
    `BackupStorageFullException`. A document that opens but does not decode is a message-less `BackupDamagedException`, so kotlinx's
    text (which quotes the JSON) never reaches a `Result`.
  - STORAGE_FULL before anything is written: a backup needs the vault plus 100 MB, a restore twice the file plus 100 MB.
  - `BackupTelemetry`: one non-fatal (`BackupFailureEvent`, fixed message) with `backup_failure_code`, `backup_format_version`, `backup_op`
    (allow-listed by exact name in `TelemetrySanitizer`), fixed codes only; nothing when the officer turned telemetry off or the choice
    cannot be read. Sent from the view model, where a backup or restore ends.
  - Fixed log lines at 6 sites (export payslip and corrections, rebuild, device read, audit repair, `toDomain`): no month, no exception.
  - iOS legacy `CryptoHelper.encrypt`: salt and IV from the checked generator (fails closed), layout unchanged.
  - Rewritten, not deleted: `RestoreStatusTest` (damaged/newer showed their messages), `PayslipBackupWrongPasswordTest` (tiny file),
    `DecryptFailureSourceTest` (scanned `restoreFailedStatus`).
  - RED first: 29 new tests failed on assertions (shared 17, composeApp 10, iOS 2). 32/32 mutations caught.
  - Gate green: shared 1766 → 1790 (release 1790, 1 pre-existing skip), iOS shared 1618 → 1645, composeApp 1441 → 1448 / 1415 → 1422,
    iOS composeApp 857 → 864. R8 triggered and green with no new rule. The owner's 4 local backups restore with 0 skips.
  - Pixel: debug build installed over the existing one; launches with no FATAL. Security review: no findings.
  - Pending: 14a P10 "From P6".
- 2026-11-04 — **P7 done**: `BackupRestoreViewModel` and state (MVVM).
  - New `ui/backup/BackupRestoreViewModel` (Koin `single`, one per process), with `StateFlow<BackupUiState>`: `Idle(lastBackupAt)` ·
    `Working(op, stage, done, total)` · `Preview(RestorePreview)` · `Done(BackupSummary)` · `Failed(BackupFailure)`. States hold counts,
    enums and codes only.
  - Progress goes through `ProgressThrottle`: about 10 updates a second, and the latest is always delivered.
  - Restore runs pick → prepare → Preview → commit (Replace or Merge) or cancel. The `PreparedRestore` is held only while the preview is
    up. Every exit closes it (the key is wiped) and deletes the picked file: Cancel, dismiss, `onCleared`, and a commit that works or
    fails. New `PreparedRestore.isClosed` shows it.
  - A tap while Working or Preview is ignored, and an ignored pick is deleted. A cancelled preview never overwrites the operation after it.
  - The password is passed per call and kept in no state or field. It moved from Settings' state into the sheet's.
  - Telemetry: one code-only report per failure. A failed commit carries the preview's format, a backup carries `CURRENT_VERSION`, and
    nothing is sent with the opt-out on or for a cancel.
  - The backup extensions on `PayslipViewModel` are deleted, along with its `backupService`/`backupFiles`/`backupTelemetry` (grep:
    no user left). `seedMockData`/`clearAllData` show fixed lines (P10 "From P6" item 8).
  - The sheet keeps today's look (P8 builds the new one) and reopens by itself on anything but Idle, so a result shows after unlock.
  - RED first: 33 new or rewritten tests failed on assertions (3 Koin resolves). Five PayslipViewModel test files were moved with their
    WHY, and none deleted.
  - 28/28 mutations caught. The first run missed one (the view model bypassing the throttle); a test with the service on its own
    dispatcher closed it.
  - Gate green:
    - shared 1790 → 1791 (release 1791, 1 pre-existing skip);
    - iOS shared 1645 → 1646;
    - composeApp 1448 → 1473 / 1422 → 1447;
    - iOS composeApp 864 → 889.
  - R8 triggered and green with no new rule. The owner's 4 local backups restore with 0 skips. Security review: no findings.
  - **Not finished: the password as a `CharArray`** (P3 item 3). It is still a `String` down to the KDF; see 14a P10 "From P7" item 1.
  - Pending: 14a P10 "From P7".
- 2026-11-04 — **P8 done**: the Backup & Restore sheet (U1).
  - Components, each at most 50 lines in its own file: `BackupProgressPanel` (a spinner while the total is 0, then a bar and the stage's fixed line),
    `RestorePreviewCard` (what the backup holds, the date from v6 files only, format, skipped rows in the warning colour), `BackupResultCard`
    (counts only), `RestoreChoice` (Replace, Merge or one Restore button, and Cancel), and the Back Up confirm-password field. The body scrolls.
  - Rules in one place (`BackupSheetRules.kt`): no close while working (D12), a password mismatch stops Back Up, Replace with skipped rows asks a
    second time and names the loss (D11).
  - D10: the preview always shows. P7's empty-device auto-Replace is gone (`BackupSheetHostUiTest`, with the real view model).
  - The per-kind skipped wording moved into `BackupRestoreStrings`; `OrdersBankStrings.restoreSkipped` is deleted. A `BackupColors.skipped()` token
    in `Theme.kt` has a light and a dark value. All new copy is on the owner's D13 list (14a P10 "From P8" item 5).
  - No ViewModel change and no new type, so R8 was not triggered.
  - Password: owner chose its own `CharArray` phase after P8 and before P9 (14a P10 "From P8" item 1).
  - RED first: 30 of 35 new tests failed on assertions (the other 5 were control cases and two source scans). 19 mutations; the first run
    missed 3 (two dismiss guards covering each other, and the result card's colour), so one guard was dropped and a colour assertion added. Every
    remaining mutation is now caught (the dropped guard's own mutation no longer exists).
  - The Pixel walk found the sheet could sit lower than its content; the body now scrolls (RED first). Root cause not isolated.
  - Gate green: shared 1791 (release 1791, 1 pre-existing skip), iOS shared 1646, composeApp 1473 -> 1509 / 1447 -> 1483, iOS composeApp 889 -> 898.
    The owner's 4 local backups restore with 0 skips. Security review: no findings.
  - Pending: 14a P10 "From P8".

