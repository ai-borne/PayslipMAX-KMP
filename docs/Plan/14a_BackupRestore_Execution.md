# 14a — Backup & Restore Hardening — Execution Plan

Status: draft for owner sign-off, 2026-11-03. Nothing is built. Scope and findings: `14_BackupRestore_Hardening_PhasePlan.md`.
Branch `feature/backup-hardening` off `feature/orders-bank` (D1). Phases run strictly in order, one at a time; the Phase Summary is the
only way to the next phase. Format follows `13a_OrdersBank_Execution_P0-P2.md`.

## 1. Rules and how each is enforced

| Rule | Enforced by | Proof in each Phase Summary |
|---|---|---|
| 1. Green build every phase | The gate (section 3), run in full at phase end | Commands, exit codes, test counts |
| 2. TDD, 100% pass | Tests written first and shown RED for the missing behaviour (not a compile error); no `@Ignore`, no new skip | Counts before and after |
| 3. MVVM/SOLID/DRY/SSOT, 300 lines | `checkFileSizes` + `check_tech_debt_limits.py --strict` (hard fail over 300; composables ≤ 50) | Line count of every touched file |
| 4. No hardcoded strings/colours | Copy in `BackupRestoreStrings.kt` (existing SSOT for this feature), colours as `Theme.kt` tokens | Literal grep of the diff: clean |
| 5. Security | Section 4 checklist, `security-review` skill on each phase's diff, gitleaks in hooks | Findings and fixes, or "none" |
| 6. R8 | Section 5, run in every phase that adds/removes a `@Serializable`, enum, `expect`/`actual` or library | `minifyReleaseWithR8` + `check_r8_guide.py` exit codes |

Conflicts, and which rule wins:
1. **300-line SRP exception**: our gates fail any `.kt` over 300 with no exemption, so no Kotlin file uses it. Golden backups are binary
   `.pcda` test resources, not Kotlin, so they're outside the line rule (section 6).
2. **"String resources"**: KMP project, so the copy SSOT is the Kotlin strings object, not Android XML.

## 2. The loop every phase follows
1. Read every file the phase touches and its nearest pattern. 2. Tests first (RED). 3. Smallest code to GREEN. 4. **Mutation check**:
break each guarded rule on purpose, see its test fail, restore (sha256 match). A rule no test can fail is a gap, and the phase fixes it.
5. Gate. 6. `security-review` on the diff. 7. Debt sweep (dead code, duplicates, literals, files near 300) — fixed in this phase.
8. Phase Summary, then stop. 9. On acceptance: one commit, hooks on, nothing pushed without the owner.

```
Phase N — <name>                                       Commit: <sha after acceptance>
1. Tech debt incurred:      each item, or "none"
2. How each was resolved:   exact steps (file, change, proving test)
3. Build & tests:           gate commands + exit codes; shared/composeApp/iOS test counts; skips: none
   R8:                      minifyReleaseWithR8 + check_r8_guide.py (or "not triggered: <why>")
   Security:                security-review findings and fixes
   Files:                   every touched file with its line count
```

## 3. The gate (every phase end)
```
./gradlew check -x iosX64Test -x iosSimulatorArm64Test
./gradlew iosSimulatorArm64Test
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
./gradlew ktlintCheck && python3 scripts/check_tech_debt_limits.py --strict <touched .kt files>
./gradlew :composeApp:minifyReleaseWithR8 -PallowPlaceholderGemmaModel=true    # when section 5 triggers
```

## 4. Security checklist (every phase)
- A backup file is untrusted input: lengths and counts checked before decode (`PageArchive`, `Part2Limits` pattern); size cap before reading.
- No exception text, path, file name or row content reaches the screen, logs or Crashlytics (F7 fixes the one place that leaks today).
- Telemetry keys go through `TelemetrySanitizer.isKeyAllowed`; values are fixed codes only.
- Passwords: `CharArray`/`ByteArray` buffers zeroed after use where the platform allows; never logged; never in `UiState.toString()`.
- Golden files contain synthetic data only; decrypted and PII-scanned before commit (P0).
- Entitlement and device-bound security settings never travel in a backup (D5).

## 5. R8 / release checklist
- Every new `@Serializable` wire DTO lives in `repository/backup/`, and that folder is added to `MODEL_FOLDERS` in
  `scripts/check_r8_guide.py`, so the release gate proves each DTO and its `$$serializer` survive R8.
- Wire keys are frozen with explicit `@SerialName` on every DTO property, so a Kotlin rename can't change the file format.
- Enums sent to telemetry carry an explicit stable `code: String`; `name` is never used (R8-safe and refactor-safe).
- No new keep rule unless the gate fails; if one is needed, it's narrow (class-level, never `**`) and justified in `proguard-rules.pro`.
- Removing `@Serializable` from Room entities (P1) is verified with `minifyReleaseWithR8` plus a Room `_Impl` survival check.
- P9: the minified device smoke (`scripts/run_guide_minified_smoke.sh`) gains a backup round trip and a v5 golden restore on the minified build.

## 6. Golden backup harness (built in P0, used by every phase)
- Binary `.pcda` files in `shared/src/commonTest/resources/golden-backups/<platform>-<appVersion>-v<format>.pcda`, plus a `manifest.json`
  holding the expected counts per file. Marked `binary` in `.gitattributes`.
- Read through a test-only `expect fun readGoldenBackup(name): ByteArray`: the JVM reads from the classpath; iOS reads the absolute repo path
  that a Gradle-generated constant injects into `iosTest` (the simulator can read host files). One test,
  `GoldenBackupRestoreTest`, loops over the manifest, so adding a golden is data-only.
- The existing hex goldens (v1–v3) stay as they are (never regenerate) — and they move into the manifest loop.

---

## Phases

### P0 — Baseline and real-world goldens (characterization; production code only if a golden fails)
- Cut the branch; record the baseline test counts.
- Build the harness (section 6). Test first: the manifest loop with one entry, RED until the reader exists.
- Code-generated goldens from shipped code: a worktree at `v1.3.0` and at `v1.5.0` runs a throwaway test that exports the synthetic fixture
  set (`createMockPayslip`, corrections, notes, drafts, a PDF) → `android-1.3.0-v3/v4`, `android-1.5.0-v4`. Head exports `android-head-v5`
  with Orders Bank entries, orders and 2 pages.
- Device goldens (owner): 1.5.0 from Play and from the App Store, synthetic data only, then a backup from each → `android-1.5.0-v4-device`,
  `ios-1.5.0-v4-device`. Before commit: decrypt, list every string field, scan with `TelemetrySanitizer` PAN/account/email regexes, and
  owner-confirm.
- Each golden restores with the exact manifest counts on JVM **and** iOS (both directions of platform crossing covered).
- **The 1.5.0 failure is expected to reproduce here.** Root cause + fix written into plan 14's changelog, with the golden as its test.

### P1 — Backup wire format decoupled from the database (F1)
- New `repository/backup/`: `PayslipBackupRow`, `PdfBackupRow`, `SettingsBackup`, `DraftBackupRow`, `DismissedDraftBackupRow`,
  `CorrectionBackupRow`, `GuideNoteBackupRow`, each `@Serializable` with `@SerialName` keys identical to today's JSON, plus mappers
  (`BackupRowMappers.kt`). `PortableBackup` holds DTOs only.
- `SettingsBackup` carries only the fields D5 allows; device-bound ones don't exist on the type.
- Room entities lose `@Serializable` where nothing else uses it (grep proves it).
- Tests first: every golden still restores (wire compatibility); export JSON keys equal a pinned key list per DTO; a DB-only column added to
  a test entity doesn't change the exported keys; D5 fields absent from an export and untouched on restore. R8: section 5 in full.

### P2 — One tolerant decoder (F2)
- `BackupJson` (SSOT, `repository/backup/`): `ignoreUnknownKeys`, `coerceInputValues`, `explicitNulls = false`. Used for the outer document,
  the payslip payload, corrections, notes and Part II rows; the device-side `EncryptedPayslipEntity.toDomain` read uses it too.
- Tests first: each DTO and `ParsedPayslip` decode with an unknown key added (RED today for `ParsedPayslip`); a guard test fails when a
  DTO/`ParsedPayslip` field other than an id lacks a default (checked through the serializer descriptor's `isElementOptional`, no reflection).

### P3 — Versioned crypto envelope, format v6 (F9, needs D6)
- New backups begin with a plaintext header: magic `PCDX`, envelope version, KDF id, iteration count, salt. The key is derived **once per
  archive**; rows inside are no longer re-encrypted per row with the password (the outer AEAD protects them; per-row PBKDF2 also slows
  restore). `CURRENT_VERSION = 6`.
- The reader dispatches by header; no header → the legacy path (10,000 iterations, per-row keys) exactly as today. The device DB encryption
  is untouched.
- Header fields bounded (iterations within [10 000, 5 000 000], salt 16 bytes) so a hostile file can't stall the KDF.
- F10: the envelope version sits in plaintext **before** anything is decrypted, so this release and every later one answers
  `NEWER_VERSION` ("update the app") for any envelope or format version it doesn't know, never a decrypt error. The `PCDB` page archive
  gets the same check. (1.5.0 itself can't be fixed; D9's merge order means v5/v6 files and the reader that understands them ship together.)
- Tests first: v6 round trip; every older golden still restores; tampered header → wrong-password class; out-of-range iterations → damaged;
  iOS timing test (`ParserUtilsIosPerfTest` pattern) for KDF + 250 MB archive. New golden `android-head-v6` + `ios-head-v6` (from simulator).

### P4 — Prepare / commit split and verify-after-write (F4)
- `BackupRestoreEngine` (or `prepare`/`commit` on `PayslipBackupService`, whichever keeps both files < 300 lines):
  `prepare(source, password, progress): Result<RestorePreview>` — decode + re-encrypt, counts per kind, skipped per kind, backup date, format
  version; no DB write. `commit(preview, mode, progress)` — the existing single transaction.
- Export: after the file is written, `prepare` runs on it (dry run, pages read by range, nothing kept). On failure the file is deleted
  and `BackupFailure.VERIFY_FAILED` is returned; the share sheet never opens.
- Tests first: a corrupted written byte → no share, file gone; preview counts equal the goldens' manifest; DB unchanged after `prepare`;
  `commit` writes exactly what `prepare` reported. Memory: vault pages remain a lazy sequence (test with `FakePageEncoder` counts).

### P5 — Skip, count, show (F6)
- Payslips, PDFs, corrections and drafts follow the Part II rule: an unreadable row is skipped and counted per kind (`RestorePreview.skipped`).
- All rows unreadable → `DAMAGED` (never "restore of nothing").
- Tests first: one bad payslip among three → two restore, `skipped.payslips == 1`; all bad → failure, DB untouched.

### P6 — Classified failures and telemetry (F5, F7)
- `BackupFailure` (stable `code`): `WRONG_PASSWORD`, `NOT_A_BACKUP`, `NEWER_VERSION`, `DAMAGED`, `TOO_LARGE`, `STORAGE_FULL` (checked before
  export/restore with `FreeSpaceReader`), `VERIFY_FAILED`, `WRITE_FAILED`, `UNKNOWN`. One fixed line each in `BackupRestoreStrings`.
- One mapper (exception → `BackupFailure`) replaces `restoreFailedStatus`; the backup path stops showing `exception.message` (F7).
- Crashlytics via `CrashReporter`: keys `backup_failure_code`, `backup_format_version`, `backup_op` (allow-listed in `TelemetrySanitizer`);
  non-fatal record with a fixed message. Respects the telemetry opt-out.
- Tests first: each exception type → its code; no failure string contains exception text; `FakeCrashReporter` receives code-only keys;
  `TelemetrySanitizerTest` covers the new keys.

### P7 — `BackupRestoreViewModel` and state (MVVM)
- New Koin-provided `BackupRestoreViewModel` owns `StateFlow<BackupUiState>`: `Idle(lastBackupAt)` · `Working(op, stage, done, total)` ·
  `Preview(RestorePreview)` · `Done(summary)` · `Failed(BackupFailure)`. Progress callbacks from P4 are throttled (~10/s) onto it.
- The backup/restore extensions on `PayslipViewModel` move here and are deleted (no dead path); `SettingsSectionComponents` wires the new
  ViewModel. The password lives only in the sheet's state and is passed per call (never stored in `UiState`).
- State and lifecycle: runs in `viewModelScope` (survives rotation and sheet close/reopen); process death mid-restore = nothing applied
  (transaction), mid-backup = partial file deleted on the next start (`BackupFiles.sweep`). App lock while working: the operation continues,
  and the result shows after unlock. A second tap while `Working` is ignored.
- Tests first (ViewModel, fakes from `shared-test-fixtures`): every transition; double tap = one operation; `Failed` carries the code only;
  `Preview → Cancel` discards the picked file.

### P8 — The Backup & Restore sheet (U1)
- Components (each ≤ 50 lines, split files): `BackupProgressPanel` (spinner while total is unknown, then a determinate bar + stage line),
  `RestorePreviewCard` (backup date, format, payslips/orders/pages, skipped in the warning colour), `BackupResultCard` (counts only, never
  amounts), confirm-password field on Back Up (mismatch disables the button), "Last backup: <date>" line.
- While `Working`: buttons disabled; sheet non-dismissable (`sheetState.confirmValueChange`, Android `BackHandler`, iOS swipe-down
  blocked by the same sheet state).
- All copy in `BackupRestoreStrings`, colours via `Theme.kt` tokens; dark and light both checked.
- Tests: compose UI tests per state (`runComposeUiTest`), the literal grep. Pixel walk (debug build left installed) on a 250 MB vault; iOS
  simulator check.

### P9 — Release gate
- Full gate + `assembleRelease` + `check_r8_guide.py` + minified smoke with the backup round trip (section 5); pre-push hook.
- `docs/AI_INSIGHTS_PIPELINE.md` is not touched; plan 14's changelog updated; `CLAUDE.md` gets one line: "every release commits a golden
  backup (`golden-backups/manifest.json`)".
- Release checklist item added (`docs/Launch`): export a golden from each release build, both platforms, before tagging.

## Owner decisions (2026-11-03)
- **D5** `isPremiumEnabled`, `isLockEnabled`, `appPinHash`, `isTelemetryEnabled` are device-only: never exported, never overwritten by a restore.
- **D6** P3 is in: versioned header (format v6), PBKDF2-HMAC-SHA256 at 600,000 iterations, one derivation per archive; older files restore
  through the legacy path.
- **D7** The owner's two backups (Android Play versionCode 22, iOS App Store; `Desktop/Pay Slip Elements/Pcda Backups`) hold **real
  payslips**: local-only tests (`-Dpayslip.localBackups=<dir> -Dpayslip.localBackups.password=…`), never committed, never in CI.
  Committed goldens are synthetic, generated from shipped code: v1.5.0 tag (v4), the commit that built Play versionCode 22 (v5, with
  pages), the iOS App Store build's commit (exported on the simulator), and head (v5 now, v6 after P3).
- **D8** One local commit per accepted phase; nothing pushed without the owner.
- **D9** Ships by merging into `feature/orders-bank` before that branch's PR.
- **D10** The restore preview always shows, on an empty device too.
- **D11** Replace with skipped rows needs a second, explicit confirm that names what will be lost.
- **D12** No Cancel for backup or restore; progress only (the sheet stays non-dismissable while working).
- **D13** New copy is reviewed by the owner in the P8 Pixel walk.
- **D14** iOS in P8: simulator + iOS tests; the iPhone walk waits for the all-features walk.
- **D15** (2026-11-03, P4) The restore preview shows the backup's date: v6 documents carry `exportedAt` (epoch millis, optional); older
  files show none.
- **D16** (P5, owner delegated the choice) A `FieldSource` value a later app adds is dropped from that payslip's map by a tolerant map
  serializer, so the payslip still restores; characterization tests pin today's known values first.
- **D17** The P8 Pixel walk may Back Up and Restore (Merge) on the owner's device.
- **D18** `BackupJson` stays in `repository/backup/` (P10 "From P2" item 3 closed).

P0 note (2026-11-03): the owner's earlier failure didn't reproduce. Both real files restore with this branch on the JVM and on the Pixel
(empty-device Replace, then Merge). Play production = versionCode 22, built from `681c87bc` on `main`, which writes **v4** (no page
archive). The owner's Android file is a `PCDB` v5 archive, so it came from a `feature/orders-bank` debug build (same package id), not
from production. The iOS file is a genuine App Store (build 6, `9841fc4a`) v4 file. A genuine production Android v4 file was then made
on the Pixel (Play vc22, iOS data restored, backed up) and saved to the same folder; all four files restore on this branch (JVM).
Production 1.5.0 given the v5 file shows a raw BAD_DECRYPT error (F10). Phone copies deleted; the debug build is back with the owner's data.

---

## P10 — Consolidated pending items

From P1 (2026-11-03):
1. **Owner to confirm a test reading.** P1 asked for "a DB-only field added to a test entity does not change the exported keys". A test cannot
   add a column to a production entity, so it is proven in two parts. `BackupWireKeysTest.theWireFormatIsMadeOfBackupRowsWithPinnedKeysAndNoDatabaseType`
   walks the serializer descriptors and finds no Room type in the wire format, with every class's keys pinned; it was RED before P1.
   `everyExportedRowCarriesExactlyItsPinnedKeys` exports with the settings row's DB-only columns (`id`, Premium, lock, PIN hash, telemetry)
   set away from their defaults, and shows that they add no key. If the owner wants the literal form, it needs a test-only entity and mapper,
   so it would test test code.
2. **Pre-existing log of a payslip month.** When a row can't be decrypted, `PayslipBackupService` calls `Logger.e(..., "... ${entity.dateStr} ...")`
   on export (payslips and corrections). That call writes a Crashlytics breadcrumb. It was not changed in P1. **Owner: P6** (classified failures
   and telemetry); fixed-code messages only. **Closed in P6**: both are fixed lines with no month and no exception
   (`BackupLogLeakTest.aBackupThatSkipsAnUnreadablePayslipAndCorrectionLogsFixedLinesOnly`).
3. **Older apps reading a P1+ file.** A shipped app (1.5.0 or earlier) that does a REPLACE restore of a file written from P1 on finds no
   lock/PIN/telemetry keys in it. It then writes its defaults on that old device: lock off, no PIN, telemetry on. Shipped code can't be changed.
   Format v6 (P3) and its "update the app" answer reach older readers only through new releases, so release notes for the build that ships
   this should say: "restore backups on the latest version".

From P2 (2026-11-03):
1. **A `FieldSource` value added by a later version still fails a payslip.** `coerceInputValues` reaches enum fields, not enum values inside
   a map, and `ParsedPayslip.fieldSource` is a `Map<String, FieldSource>`. A later app that adds a value (say `OCR`) makes that payslip
   unreadable here: today the whole restore fails; after P5 that payslip is skipped and counted. Owner decision in P2: recorded here, not
   fixed in P2. Options: a tolerant map serializer that drops unknown values (new code + tests), or a rule that a new `FieldSource` value
   ships only with a `CURRENT_VERSION` bump. **Owner to decide; nearest phase P5.**
2. **A payslip or correction decode error can quote its input.** kotlinx serialization puts part of the JSON (payslip values) in the
   exception message, and that exception reaches `Result.failure` on restore. This was already true before P2 and is unchanged.
   Notes and Part II rows already swallow it. **Owner: P6** (F7: one exception → `BackupFailure` mapper with fixed codes). **Closed in P6**
   with item 7 of "From P5" (see there).
3. **Package direction.** 14a puts `BackupJson` in `repository/backup/`, so `database/` (payslip, correction and note entities) and
   `part2/data/` now import it, while `repository/backup/BackupRowMappers.kt` imports `database/`. It's one module, so it builds and runs,
   but the two packages now depend on each other. **Owner to confirm**: keep it as 14a says, or move `BackupJson` to a neutral package
   (a one-file move with no behaviour change).
4. **Owner to confirm a test reading.** Plan 14 says "each DTO decodes from its oldest golden shape plus an unknown key"; 14a, which
   supersedes it, says "with an unknown key added". Implemented as 14a: every backup row, the document and every payload decode today's
   shape with an unknown key (`BackupJsonToleranceTest`, `Part2PayloadToleranceTest`). The oldest shapes are covered by the 7 goldens,
   which still restore with exact counts. Neither a golden with an unknown key nor an old shape with one is tested, since goldens are
   never regenerated.

From P3 (2026-11-03):
1. **Golden names.** 14a asked for `android-head-v6`; the goldens are `jvm-head-v6.pcda` and `ios-head-v6.pcda`, because
   `GoldenBackupRestoreTest`'s "current format on each platform" check (P0) keys on the `jvm-`/`ios-` prefix that every earlier golden uses.
   The JVM writer is the same `commonMain` code Android runs. **Owner to confirm** (renaming means changing the P0 check, not the bytes).
2. **Shipped apps given a v6 file.** 1.5.0 and earlier have no `PCDX` reader, so they still show a decrypt error for a v6 file (F10 can't be
   fixed in shipped code). From this release on, a later envelope or format answers "update the app" before any decrypt. **Release notes
   (P9)**: "restore backups on the latest version".
3. **The password can't be wiped.** It reaches the crypto as a Kotlin `String`, which is immutable on both platforms: Android now clears the
   `PBEKeySpec` and its char copy, the derived archive key is zeroed on close, but the `String` itself stays until it is collected, and iOS
   hands it to `CCKeyDerivationPBKDF` as a C string. Closing this needs a `CharArray` password from the sheet down (P7/P8 own the password
   field). **Owner: P7.** **Not finished in P7**: the password left the Settings screen's state for the sheet's and is held nowhere
   else, but it is still a `String` down to the KDF; see "From P7" item 1. **Closed in P10**: the password is a `Secret` (a `CharArray`) from the sheet to the KDF on both platforms and is wiped at every call site (`BackupSecretZeroingTest`, `BackupRestoreViewModelSecretTest`, `AndroidPbkdf2WipeTest`, `IosSecretKdfTest`); see "From P10" for what Compose still keeps.
4. **Iterations above 5,000,000** (`BackupEnvelope.ITERATIONS_MAX`) need a new envelope version, since this release refuses them as damaged.
   Recorded so a later increase is planned as a format change, not a constant edit.
5. **A headerless file whose document says version 6** is read through the legacy path and accepted (the per-row password seals still
   protect it). Only a hand-made file can be like this; refusing it would add a second version rule for no attacker gain. Recorded, not changed.
6. **"The PCDB page archive gets the same check"** is met by the header: a v6 page archive is only ever read behind a `PCDX` header that
   was checked first, and the document's own version is checked again (`requireKnownFormat`). An older headerless `PCDB` is v5 by
   definition. No separate version byte was added to `PCDB` (that would change the v5 wire shape the goldens pin).
7. **Pre-existing:** the iOS legacy `CryptoHelper.encrypt` (device DB and formats 1–5) ignores `SecRandomCopyBytes`'s status when it makes
   a salt and IV. The new v6 path checks it. The device path was out of P3's scope ("device DB encryption untouched"). **Owner: a one-line
   fix in a later phase (P6 is the nearest crypto-adjacent phase).** **Closed in P6**: salt and IV come from the checked generator,
   layout unchanged (`IosLegacyEncryptRandomTest`, iOS). The device key generation has the same flaw: see "From P6" item 6.

From P4 (2026-11-03):
1. **A page whose order does not travel is no longer exported.** A fresh backup must read back with nothing skipped (verify-after-write),
   and a restore could only skip such a page (its order is missing or unreadable on the device). Nothing restorable is lost; the page stays
   on the device. Recorded because it changes what an export contains.
2. **The preview counts what the backup holds, not what a MERGE will change.** On MERGE a same-date payslip overwrites the device's copy
   and a page the device already holds is left alone, so "adds N" would be wrong. **P8 copy** must say "This backup holds …" (D13 review). **Closed in P8**: `BackupSheetStringsTest.thePreviewSaysWhatTheBackupHoldsAndNeverThatItAdds`; the copy is on the D13 list below.
3. **Progress while a preview checks pages is one stage without a page count** (`OPENING` → `READING` rows; the page check inside
   `Part2Backup.restore` reports nothing). Export (`PAGES`), verify (`VERIFYING`) and commit (`PAGES`) count pages. It is part of the
   2.97 s a whole 250 MB restore takes on the iOS simulator (P3), so the P8 spinner covers it. **Owner: P8** decides if it needs a bar. **Closed in P8**: the spinner covers it, no bar (decision taken, see "From P8" item 6).
4. **A `PreparedRestore` holds the archive key and the open file until it is closed.** P7's ViewModel keeps it while the preview is on
   screen, so every exit (Cancel, sheet dismissed, `onCleared`, a failed commit) must close it; P7's tests must cover each one.
   **Closed in P7** (`BackupRestoreViewModelExitsTest`, 5 exits; `PreparedRestoreCloseTest`): see "From P7" item 2.

From P5 (2026-11-03):
1. **Not built, as asked: PDFs, letters, deleted-letter records and settings can't be skipped one at a time.** They are plain JSON inside
   the authenticated document, so once it opens they come back whole, and if it doesn't open the restore fails as before. `SkippedRows`
   has no field for them. 14a P5's "PDFs … and drafts … skipped and counted" is therefore met only for the kinds that are opened one
   row at a time: payslips, corrections, notes, Bank entries, orders and pages.
2. **Decision taken, owner to confirm: a skipped payslip's PDF and corrections still restore.** Each row stands alone: they are readable,
   the officer still holds the unchanged file, and a correction applies on read by month, so it rejoins its payslip if a later app
   restores that payslip or the month is imported again. Dropping them would lose readable rows for nothing. The other option is to drop
   them and count them as skipped. On MERGE the backup's PDF and corrections for that month replace the device's, as for any month the
   backup holds. Test: `BackupSkipCountTest.aSkippedPayslipsPdfAndCorrectionsStillRestoreBecauseEachRowStandsAlone`.
3. **Decision taken, owner to confirm: what "every row unreadable" means.** At least one row that is opened one at a time (payslips,
   corrections, notes, Bank entries, orders, page records), and all of them skipped → `BackupDamagedException`, database untouched
   (`BackupSkipCountTest`'s WHY, `BackupRestoreEngine.requireARowThatOpened`). Frozen-branch drafts don't count: they are never restored.
   Consequence: a file in which every payslip fails but one note opens is **not** damaged. It restores the note and reports the
   payslips as skipped. On REPLACE that empties the device's payslips, so **P8 must show D11's second confirm**, which names what will
   be lost, before such a commit. **Closed in P8**: `BackupPreviewUiTest.replaceWithSkippedRowsAsksAgainAndNamesWhatWillBeLost` and `mergeWithSkippedRowsDoesNotAskAgain`.
4. **Decision taken: frozen-branch drafts count as `part2Entries`**, the kind that replaced them (13b item 3: "`entries` in place of
   `drafts`").
5. **The status line's copy.** `restoreStatus` shows `SkippedRows.total` through `OrdersBankStrings.restoreSkipped` ("N items could not be
   restored."), which now counts payslips, corrections and notes too, yet still lives in the Part II strings object. **Owner: P8** decides
   the per-kind wording and moves it into `BackupRestoreStrings` (D13 copy review). **Closed in P8**: `BackupRestoreStrings.skippedList` names every kind and `OrdersBankStrings.restoreSkipped` is deleted (`BackupSheetStringsTest.skippedRowsAreNamedPerKindAndZeroKindsAreLeftOut`, `RestoreStatusTest.aRestoreThatLeftRowsOutSaysWhichAndIsNotShownAsAPlainSuccess`).
6. **Pre-existing log, now reached once per skipped payslip.** When a payslip will not open, `EncryptedPayslipEntity.toDomain` calls
   `Logger.w` with the row's month and the crypto exception's message. That call is a Crashlytics breadcrumb. Before P5 the first such row
   ended the restore; now every skipped row reaches it. The month is plaintext row metadata (it travels in clear beside the ciphertext), not
   decrypted content, and the message is the crypto library's. **Owner: P6**, with P1 item 2: fixed codes only. **Closed in P6**
   (`BackupLogLeakTest.aDeviceRowThatWillNotOpenLogsNeitherItsMonthNorTheCryptoMessage`).
7. **P10 "From P2" item 2 is partly closed.** A payslip or correction that fails to decode is now skipped, and its exception (which can
   quote the JSON) is dropped (`openedOrNull`): it no longer reaches `Result.failure`. The document's own decode can still put kotlinx text
   in a failure. **Owner: P6** (one mapper to fixed codes). **Closed in P6**: the document's decode failure is thrown as a message-less
   `BackupDamagedException` at the source (`BackupFailureSourceTest.aDocumentThatOpensButIsNotABackupsJsonIsDamagedAndCarriesNoneOfIt`), and
   the mapper reads types only, so no text reaches the screen or telemetry (`RestoreStatusTest.noFailureShowsItsOwnTextWhateverItHolds`,
   `BackupTelemetryTest.noExceptionTextReachesTheReportWhateverItHolds`).
8. **P10 "From P2" item 1 is closed by D16.** `FieldSourceMapSerializer` drops a `FieldSource` value this app does not know (it reads
   the enum's serial names), and the write path is unchanged. A value that is not text (a number or an object) is still a damaged
   payslip, so it is skipped and counted. The goldens carry no `fieldSource` values (`createMockPayslip` sets none), so wire identity is
   pinned by `FieldSourceWireTest`'s literals, not by a golden. A golden that carries both values would need a new file, since goldens
   are never regenerated. **Owner to decide** whether P9's release golden should include them.
9. **Owner to confirm a test reading: "a fresh backup with a skip fails verify".** Under v6 the document is sealed whole, so a real export
   can't contain an unreadable row, and a corrupted byte fails the whole file (P4's tests). The per-kind test
   (`BackupVerifyAfterWriteTest.aFileThatReadsBackWithOneRowSkippedOfAnyKindIsNotKept`) therefore uses `FakeBackupFiles.readBackInstead`:
   the read-back gets another complete backup that has one skip of each kind (payslip, correction, note, entry, order, page). Before P5 it
   was RED for the note, which was skipped silently. The payslip and correction cases failed verify because the whole prepare failed, and
   the Part II kinds were already counted. Mutation M9 (verify checks pages only) proves the per-kind check.

From P6 (2026-11-03):
1. **Decision taken (asked of P6): the mapper lives in `shared`** (`repository/BackupFailure.kt`, `BackupFailure.of`). Why: the
   exceptions it reads are shared types, telemetry (`BackupTelemetry`, shared) needs the code, and P7's `BackupRestoreViewModel` reads the
   same function, so a new failure type and its code are added in one module. The copy (code → line) stays in composeApp's
   `BackupRestoreStrings`, the copy SSOT. `restoreFailedStatus` is gone; `failedStatus` and `backupStatus` only look the line up.
2. **Decision taken, owner to confirm: the free-space margin.** A backup needs the vault's bytes plus 100 MB
   (`Part2Limits.FREE_SPACE_REFUSE_BYTES`, the line below which no scan opens); a restore needs twice the file's size plus 100 MB
   (rows go into the write-ahead log, then the database file). The read-back after a backup writes nothing and is not checked. A device
   that cannot say how much is free is never blocked. WHY in `BackupStorageFullTest`.
3. **Decision taken, owner to confirm: NOT_A_BACKUP vs WRONG_PASSWORD.** AES-GCM cannot tell a wrong password from a file that is not a
   backup, so every decrypt failure keeps the owner-approved line ("Wrong password, or this isn't a PayslipMax backup", WRONG_PASSWORD).
   NOT_A_BACKUP is only a file with no header or page archive shorter than the smallest sealed row (48 bytes). TOO_LARGE is a file with no
   page archive over 128 MB (was "damaged"). `PayslipBackupWrongPasswordTest`'s tiny-file test was rewritten for this, not deleted.
4. **Decisions taken, owner to confirm:** WRITE_FAILED covers any `BackupFileException`, a picked file that cannot be read included (its line
   says "written or read"); a restore reports `backup_format_version` = "unknown" unless the file is newer ("newer"), because a failed
   restore has no preview (P7 can pass the preview's version on a failed commit); every failure, a wrong password included, sends one non-fatal.
   **P7**: a failed commit now sends the preview's format
   (`BackupRestoreViewModelTelemetryTest.aCommitThatFailsSendsThePreviewsFormatAndACancelledPreviewSendsNothing`).
5. **New copy for the D13 review (P8 walk):** `BackupRestoreStrings.notABackup`, `newerVersion`, `damaged`, `tooLarge`, `storageFull`,
   `verifyFailed`, `writeFailed`, `failedGeneric` (the generic line now reads for both operations). `AppStrings.statusRestoreFailed` and
   `statusBackupFailed` were deleted (nothing used them after F7).
6. **Found, not changed (pre-existing, outside P6's scope): the iOS device-database key generation ignores `SecRandomCopyBytes`'s status**
   (`iosMain/crypto/CryptoHelper.kt`, `resolveDatabaseSecretKey`). If it ever failed, an all-zero key would be stored in the Keychain
   for good. Failing closed there means the app cannot open its database on that launch, a behaviour change for the owner to decide.
   **Owner to decide** (one line, same `secureRandomBytes`). **Closed in P10** (owner chose it): the key comes from the checked generator before anything is stored (`IosDatabaseKeyRandomTest`, iOS).
7. **Found, not changed: other `Logger.e(..., throwable)` callers outside repository/, database/, crypto/ and part2/data/** still send the
   exception's message to a breadcrumb and a non-fatal (`Logger.e` prints `it.message` and calls `recordException`). P6 fixed every site in
   its scope (6 sites). A sweep of the rest, or a `Logger.e` that drops the message, is a separate change. **Owner to decide.** **Closed in P10** (owner chose it): `Logger.e` has no exception parameter, and the 5 sites that passed one now name the failure's type only (`LoggerExceptionLeakTest`).
8. **Found, not changed: `seedMockData` and `clearAllData` put `e.message` into `uiState.error`** (`PayslipViewModelExtensions.kt`,
   not backup code; seeding is a developer tool). **Owner to decide**; nearest phase P7 (it moves the backup code out of that file).
   **Closed in P7**: fixed lines from `AppStrings` (`PayslipViewModelDataErrorTest`); see "From P7" item 3 for the one left.
9. **Wiring change:** the `FreeSpaceReader` and `FreeSpaceGate` bindings moved from `part2Module` to `sharedModule` (one reader for the
   Bank and the backup). Proved by `AppKoinModulesTest.theBackupChecksTheGraphsFreeSpaceAndReportsThroughTelemetryThatHonoursTheOptOut`;
   `sharedModule` singles are shared across tests in a JVM, so that test class now binds the shared `GuideKoinTestDao` leaves.
10. **Logger** now resolves its `CrashReporter` from Koin when Koin is started (the same platform reporter in the app), so a test can read
    what a log site sends without a mutable seam (the tech-debt audit refuses a stateful singleton).

From P7 (2026-11-04):
1. **NOT FINISHED: P10 "From P3" item 3, the password as a `CharArray`.** Decision taken in P7: take it as far as it goes without
   changing the crypto.
   - Done: the password now lives only in the open sheet's `remember` state. It left the Settings screen's state, so closing the
     sheet drops it. It is passed per call, and never held in a `BackupUiState`, a view-model field or saved state
     (`BackupUiStateLeakTest`).
   - Not done: it is still a `String` from the text field to the KDF, for three reasons.
     (a) Compose's `OutlinedTextField` holds its text as a `String` (and in its own buffers), so a `CharArray` made from it would copy a
     value that cannot be wiped.
     (b) `PayslipBackupService.backUp`/`prepare`, `openArchive`, `ArchiveKey.derive` and `CryptoHelper` all take a `String`.
     (c) A format 1–5 file keeps the password in `PasswordKey` while its preview is on screen. The key is closed afterwards but not wiped.
   - Closing it needs a password field whose text sits in a buffer that can be cleared (the Compose version used has none), and a
     `CharArray` path down through the crypto on both platforms. Android's `PBEKeySpec` takes a `CharArray`; iOS needs UTF-8 bytes it
     can zero.
   - **Owner to decide**: accept the `String`'s lifetime as a known limit (the KDF runs on the device, and the `String` is collected after
     the sheet closes), or schedule it as its own phase (a crypto API change, with the goldens unchanged).
   - **Closed in P10**: scheduled and built as its own phase; every golden restores unchanged and no wire byte changed (`GoldenBackupRestoreTest`, `BackupV6RoundTripTest`, the owner's 4 local backups).
2. **P10 "From P4" item 4 closed.** Every exit from a preview closes the `PreparedRestore` (the key is wiped) and deletes the picked
   file: Cancel, the sheet dismissed, `onCleared`, a commit that works and one that fails (`BackupRestoreViewModelExitsTest`, 5 tests).
   - Cancel, dismiss and `onCleared` release at once, on the main thread. The waiting restore then unwinds and closes its file
     (`FakeBackupFiles.openReads`).
   - New: `PreparedRestore.isClosed`, true only once the key is wiped (`PreparedRestoreCloseTest`: a closed restore opens no page).
   - A restore cut short by `onCleared` is not reported as a failure.
3. **P10 "From P6" item 8 closed.** `seedMockData` and `clearAllData` now put a fixed line (`AppStrings.errorSeedDataFailed` /
   `errorClearDataFailed`) into `uiState.error` (`PayslipViewModelDataErrorTest`).
   - Found, not changed (same kind, outside P7's scope): `PayslipViewModel.observePayslips` still writes
     `"Failed to load payslips: ${e.message}"` into `uiState.error`. Nothing on screen reads `uiState.error` today (grep), so no
     message reached the screen. **Owner to decide** (one line, same pattern). **Closed in P10** (owner chose it): a fixed `AppStrings.errorLoadPayslipsFailed` (`PayslipViewModelDataErrorTest.aPayslipReadThatBreaksShowsTheFixedLineNotTheExceptionsMessage`).
4. **P10 "From P6" item 4: a failed commit now reports the preview's format** (`BackupRestoreViewModelTelemetryTest`). A failed
   prepare still reports "unknown", since no preview exists yet. A backup reports `PortableBackup.CURRENT_VERSION`.
5. **Decision taken, owner to confirm: one view model per process (a Koin `single`)**, like `GuideViewModel` and the Bank.
   - Why: it must outlive the sheet, Settings leaving the screen and the app lock (the lock takes Settings out of composition).
   - Consequence: nothing in the app ever clears it, so `onCleared` runs only where an owner clears it (a `ViewModelStore`, as in the
     tests). In the app the process ends instead, which takes the key with it, and the picked file is swept at the next start.
   - Proof: `AppKoinModulesTest.theBackupViewModelIsOneInstanceSoWorkOutlivesTheSheetAndTheLock`.
6. **Decision taken, owner to confirm: the sheet opens again by itself.** When Settings is shown and the view model holds anything but
   Idle (work running, a preview or a result), the sheet reopens; it also reopens when the state changes while the officer has closed it.
   - Why: this is how "the result shows after unlock" is met, and it stops a preview (which holds the picked file and the key) from
     waiting unseen when the officer closed the sheet mid-restore.
   - A preview stays open across the app lock, until the officer answers it after unlocking. Test: `BackupSheetArrivalTest`.
   - P8 may keep or replace this when the sheet becomes non-dismissable while working.
   - **Closed in P8**: kept unchanged (owner, 2026-11-04). The sheet cannot be closed while working, so it cannot be left unseen; on the Pixel a preview sheet reopened by itself after a dark-mode recreation.
7. **For P8 (today's behaviour kept, as asked):**
   - A restore onto an empty device still commits Replace straight from the preview, with no question. D10 makes the preview always
     show; that is P8's card. **Closed in P8**: `BackupSheetHostUiTest.aRestoreOnAnEmptyDeviceWaitsAtThePreviewForTheOfficersAnswer`.
   - A commit shows `Working(RESTORE, PAGES, 0, 0)` until the first page is written, because `BackupStage` has no stage for writing
     rows. **Closed in P8**: while the total is 0 the line is the generic "Restoring…" (decision taken: no new stage, no ViewModel change).
   - There is no progress panel yet, and the buttons stay enabled while working; the view model ignores the second tap. **Closed in P8**: `BackupSheetUiTest.workingDisablesBothButtonsAndTheFieldsSoNothingStartsTwice`.
8. **Process death: no new code, both were already true.**
   - Mid-restore, nothing is applied: the commit is one Room transaction (`PayslipDao.replaceWithBackup`/`mergeBackup`), and pages are
     consumed inside it.
   - Mid-backup, the partial file is deleted at the next start: `BackupFiles.sweep()` runs in `PayslipApplication.onCreate` (Android)
     and in `ensureKoin` (iOS) (`BackupFilesSecurityContractTest.theSweepRunsAtLaunchOnBothPlatforms`).
9. **Tests rewritten, none deleted.**
   - Five PayslipViewModel test files moved to the new view model with their WHY (`git mv`): Files, RoundTrip, RestoreReport,
     Part2ItemCount and Telemetry.
   - Two cases whose path no longer exists (a view model built without backup wiring) now test their nearest intent: "a pick the view
     model refuses keeps no file", and "a device without Bank items answers 0".
   - Also rewritten:
     - `RestoreStatusTest` and `BackupStatusTest`: the status is now read from the state.
     - `BackupFilesSecurityContractTest`: it scans the new view model files.
     - `AppKoinModulesTest`: two tests read the new view model.
     - `SettingsDeveloperToolsUiTest`: its test Koin binds the backup view model.
10. **Not driven end to end: the opt-out on a failed commit.** The fake DAO is not transactional, so a failed REPLACE has already
    cleared the settings row before telemetry reads it (Room rolls it back). Every path reports through the same `BackupTelemetry`,
    whose opt-out is tested on the backup and prepare paths.

From P8 (2026-11-04):
1. **SCHEDULED, NOT BUILT: the password as a `CharArray` (owner chose option B, 2026-11-04).** Its own phase, after P8 and before P9, so the release gate runs once on the final crypto API. It closes P7 item 1 and P3 item 3.
   - Spike first: does Compose Multiplatform 1.10.3's `BasicSecureTextField` (`TextFieldState`) let the typed text be wiped, or does it copy into a `String`? The result decides how strong the field claim can be. The crypto path goes ahead either way.
   - Then: one `Secret` wrapper in `shared` (a `CharArray` with `use { }` and zeroing); `PayslipBackupService.backUp/prepare`, `openArchive`, `ArchiveKey.derive`, `CryptoHelper` and `PasswordKey` take it and zero it in `finally`. Android's `PBEKeySpec` already takes `char[]`; iOS passes UTF-8 bytes it zeroes. No wire change, goldens untouched.
   - Tests: a buffer is all zero after use (a mutation per call site), goldens still restore, R8 and the iOS timing test (`ParserUtilsIosPerfTest` pattern).
   - P8 prepared for it: both password entries go through one private composable (`BackupSecretField`), so the field type changes in one place.
   - **Closed in P10**: built as listed, and the field is now an `OutlinedSecureTextField` on a `TextFieldState` in that one composable. The spike result and its limits are under "From P10" item 2.
2. **Owner to confirm: Compose puts the raw typed password in `SemanticsProperties.InputText` on every text field.** It is an `AccessibilityKey` documented as "e.g. a real password". Android's accessibility delegate never reads it (0 references in `ui-android-1.10.5`), and the old password field did the same, so P8 adds no exposure. `BackupSheetUiTest.noPasswordReachesTheTextTheScreenReaderReads` pins what the app controls (`Text` and `EditableText` masked; mutation 17). Closing `InputText` needs a different field: item 1's spike. **Closed in P10** (owner: accept the limit and switch): the secure field plus an outer `semantics` that replaces `EditableText` and `InputText` with bullets closes it; the P8 test's `InputText` exemption is gone (`BackupSheetUiTest.noPasswordReachesAnyTextTheScreenReaderOrTheDisplayReads`, mutations C9 and C10).
3. **Owner to decide: the result card has no restored counts.** 14 U1 says "Restored 48 payslips, 12 orders, 120 pages". `RestoreReport` and `BackupSummary.Restored` carry only what was skipped, so the card says "Restore Complete!" and, only if rows were left out, which kinds. The preview just before it shows what the backup held, and a backup's card shows its counts. Adding restored counts needs `RestoreReport` (shared) and `BackupSummary.Restored` to carry them, tests first: a ViewModel change, so not done in a UI-only phase. **Closed in P10** (owner chose it): `RestoreReport.restored` (`RestoredRows`) and `BackupSummary.Restored` carry what the commit wrote, and the card says "Restored 48 payslips, 12 orders, 120 pages." (`GoldenBackupRestoreTest`, `BackupSkipCountTest`, `RestoreStatusTest`, `BackupSheetUiTest`). The new line is on the D13 list in this file's "From P10" item 3.
4. **NOT WALKED on the Pixel (D17 scope, nothing hidden).**
   - Walked on the owner's device with a debug build installed over the old one: Idle (with and without a last-backup line), confirm-password mismatch (Back Up disabled) and match, Back Up (spinner and the three stage lines, then the counts-only result), Restore through the system picker, the preview (date, format, counts), Merge (generic progress line, then "Restore Complete!"), Back and a swipe-down while working (the sheet stayed), a dark-mode recreation with a preview open (the sheet reopened), and the next-start sweep of a picked file.
   - Not walked: the D11 second confirm (no skipped-row file exists synthetically; covered by compose tests), the determinate bar (this vault holds 1 page, so the stage with a total is too brief to catch; covered by `workingShowsASpinnerUntilTheTotalIsKnownThenABarAndTheStageLine`), a Failed state on the device, and a 250 MB vault (14a P8's walk asked for one; the owner's vault is 8.5 MB). Colour in both schemes is proven by `skippedRowsAreNamedInTheWarningColourInDarkAndLight`, not by eye: some screenshots came out black (the capture was blank during the picker transition), so only the layout was checked on the device, by element bounds.
   - The iPhone walk waits for the all-features walk (D14).
5. **D13: new copy for the owner's review** (all in `BackupRestoreStrings`, plain and factual): "Confirm Password" · "Re-enter the password to Back Up. Not needed to Restore." · "The passwords don't match." · "No backup has been made on this device yet." · "Last backup: DD/MM/YYYY" · "This backup holds 34 payslips, 5 Orders Bank entries, 1 order, 1 page." · "Made on DD/MM/YYYY" · "Backup format 6" · "Could not be read, so not restored: 2 payslips, 1 note." · "Restore" (empty device) · "Cancel" · "Replace and lose these rows?" · "Replace deletes everything on this device and restores only what could be read. This will be lost: …" · "Replace anyway" · "Go back" · "Not restored: …" · progress: "Securing your backup…", "Encrypting your data…", "Checking the backup file…", "Backing up…", "Backing up pages: 3 of 9", "Opening the backup…", "Checking the backup…", "Restoring…", "Restoring pages: 34 of 120". The existing Replace/Merge question and its two buttons are unchanged.
6. **Decisions taken, owner to confirm.**
   - The preview's page check shows the spinner, no bar (closes P4 item 3).
   - D12 is one guard on `onDismissRequest` (Back and the scrim) plus `confirmValueChange` (swipe). No `BackHandler` (deprecated) and no `shouldDismissOnBackPress`: with both guards in, each hid a missing other (mutations 3 and 4 survived), so one was dropped and mutation 3 is now caught. The sheet state is what iOS swipe-down goes through too, but only Android was driven.
   - An empty device shows one "Restore" button, which does Replace (nothing to merge with, and Replace brings the settings along as P7 did). A device with data asks Replace or Merge. Replace asks a second time whenever rows were skipped (D11), on an empty device too.
   - The password and confirm entries live in the form, so they are dropped when a preview replaces it: after Cancel the officer types them again.
7. **Found in the walk and fixed: the sheet could sit lower than its content.** After the activity was recreated in dark mode the sheet's top was lower than the content needed, and Cancel was below the screen (header at y 1569, buttons at 2347-2400 on a 2424 px screen). The body now scrolls (`BackupPreviewUiTest.aSheetWithLittleRoomStillReachesCancelByScrolling`, RED first), which also covers large fonts and small phones. **Not isolated:** why the sheet's anchors did not follow the content after the recreation. After the fix the same flow fit (buttons at 2115, Cancel at 2262). If it comes back, look at `rememberBackupSheetState` and the sheet's anchor update.
8. **Tests rewritten, none deleted:** `RestoreStatusTest` (2 cases) now reads `restoreResultLines` and the per-kind wording; `BackupStatusTest.aBackupThatWorkedReadsAsItAlwaysDid` now pins `status()` as null for a finished backup, whose line comes from `backupDoneStatus` via `BackupResultCard`. `BackupGuardTest` and `BackupSheetArrivalTest` were not touched.

From P10 (2026-11-04):
Owner answers to the batch, asked before any code: candidates **a, b, c and d all in**; String APIs: **delete on the backup path only** (the device database's hex key keeps one entry point, `PasswordKey(String)`, over the same `Secret` code); field claim: **accept the limit and switch to the secure field**; field text: **keep until the sheet leaves** (a retry needs no retyping). "From P8" item 2 closed by the build (see there). "From P8" item 6 and "From P7" item 5 do not change P10, so they stay "owner to confirm".
1. **Built.**
   - `Secret` (shared, one place): a `CharArray` zeroed on `close()`. Whoever is handed one closes it. `reveal` and `withUtf8` only borrow it; `withUtf8` makes the UTF-8 bytes without a `String` and zeroes them.
   - Consumers: `PayslipBackupService.backUp/prepare/restore/export/exportTo`, `openArchive`, `newArchive`, `ArchiveKey.derive`, the format 1-5 `PasswordKey` (closed with the preview), `CryptoHelper` (Android `PBEKeySpec` cleared in a `finally`; iOS bytes zeroed), the view model (closes on any end, including a scope cancelled before the work started) and the sheet (`TypedPassword` copies the field into a `Secret` only when a call starts; Restore copies when the file arrives, so no copy waits on the picker).
   - Candidates: a (restored counts), b (iOS device key fails closed), c (`Logger.e` sweep), d (`observePayslips` line).
2. **Spike result (a controlled experiment, then deleted; pinned by `TypedPasswordReachabilityTest` and `BackupSecretFieldTest`).** Compose 1.10's `TextFieldState` does copy the typed text into a `String`: `state.text` is a `java.lang.String`, and the undo history holds more. Typed text sits in the gap buffer, the current value and the undo history. Deleting alone leaves it in the history, and `undo` brings it back. Delete plus `undoState.clearHistory()` leaves nothing reachable from the state holding it (an overwrite before the delete changed nothing the walk could see, so there is none). The `String` Compose made of each keystroke is then unreferenced garbage and **cannot be zeroed**, and the IME keeps its own buffers. So the claim is: the field is wiped and its history dropped when the form leaves; per-keystroke copies are collected, not zeroed. The secure field also puts the raw text in `EditableText` and `InputText`; an outer `Modifier.semantics` wins over the field's own and masks both (verified).
3. **Decisions taken, owner to confirm.**
   - iOS: Kotlin/Native's CommonCrypto binding declares the password as `String?`, which would have made an unwipeable `String`. `pbkdf2Sha256` (`IosPbkdf2.kt`) looks `CCKeyDerivationPBKDF` up with `dlsym` and calls it with the pinned bytes. No cinterop or build change. The published PBKDF2 vectors (non-ASCII and empty passwords too) pin it on the simulator. A non-zero status now throws; before, it was ignored and a zero key would have been used.
   - `Logger.e` lost its exception parameter, and with it the non-fatal it recorded. The three PdfParser sites duplicated the import flow's own report; the audit-repair and Gemma-build sites no longer reach Crashlytics as non-fatals.
   - `Secret` has no `isBlank`/`isEmpty`/`length`/`contentEquals` (nothing used them): `TypedPassword` does the blank check.
   - A Restore pressed and then a sheet wiped before the file arrives restores with an empty password and shows the wrong-password line (never a crash).
   - Restored counts: what the commit wrote. On MERGE a same-date payslip that overwrites counts as restored; PDFs, corrections, notes and settings are not counted, like the preview. **New D13 copy:** "Restored 48 payslips, 12 orders, 120 pages." (kinds with none are left out; no line when none of the four kinds was written).
   - A lone surrogate in a password: iOS gives U+FFFD as `encodeToByteArray` did (pinned); Android's provider is unchanged. Not reachable from a keyboard.
4. **Proof.** RED first (assertion failures, not compile errors): 18 for `Secret` and the call sites, 10 for restored counts. The view model, sheet and candidate b/d tests were written after their code; their RED is shown by mutation, not by a failing first run. Mutations: run 1 caught 34 of 41. The 7 not caught were 2 invalid mutations (a label error and a missing member); 3 redundant or equivalent guards (a verify copy, a `newArchive` finally, a `prepare` catch and an `open` `onFailure`), all removed; the overwrite-before-delete step (removed); and 3 test gaps (a mismatch UI test with no assertion, Restore with a blank password, a cleared view model for Restore), which now have tests. Re-run of everything affected: 10 of 10 caught; iOS 4 of 4. The tree was identical before and after every run.
5. **Found, not changed.**
   - Two `recordException(throwable)` callers remain outside `Logger`: the import flow's explicit report in `PayslipViewModel` (deliberate parser diagnostics, with its own sanitised keys) and `GemmaInstallTelemetry`. Android's reporter forwards the throwable's message. **Owner to decide** whether they should send the type only.
   - `PdfParser` still logs a sanitised file name at `Logger.w` and extracted column text at `Logger.d` (debug builds only). Both were there before.
   - The Pixel walk's "Replace" button was not found by exact text in the Replace/Merge question (never pressed). Not investigated; `BackupPreviewUiTest` covers the buttons.
6. **NOT WALKED (nothing hidden).**
   - iPhone: the iOS simulator build installs and launches (the field was not driven: iOS UI cannot be driven here) and the iOS tests pass. Fast typing on the secure field on iOS is unverified: the simulator does not reproduce the cursor race. D14: waits for the all-features walk.
   - Pixel (D17, Back Up and Merge only): walked and passed: typing, show/hide (the dump never held the password), the mismatch line with Back Up disabled, a fast typist (the whole string at once), Back Up with its 3 stage lines and the chooser closed unshared, Restore through the picker with a wrong password (the fixed line) and then the right one (preview, Merge, "Restore Complete!" and the counts line), the sheet closing wiping both fields, and no `.pcda` left after the next start. Not walked: Replace, the D11 second confirm, a Failed state other than a wrong password, dark mode, a 250 MB vault. Night mode and every setting were left as found; the debug build stays installed (built from `81daa515` plus this phase).
7. **For P9.** The release gate runs on this crypto API. Release notes: "restore backups on the latest version" (P1 item 3, P3 item 2), and that `Logger.e` no longer reports non-fatals. P9's release golden should carry a `fieldSource` value (P5 item 8) if the owner wants it.
8. **Unfinished in P10 scope: none.** The limits above (Compose's per-keystroke `String`s, the IME, the unwalked iPhone and the D13 copy review of the new "Restored" line) are recorded, not hidden.
