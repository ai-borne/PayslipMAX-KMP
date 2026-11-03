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
