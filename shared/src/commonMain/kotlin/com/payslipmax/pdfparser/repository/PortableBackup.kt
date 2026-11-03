package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.part2.data.Part2LegacyDraft
import com.payslipmax.pdfparser.part2.data.Part2RowBackup
import com.payslipmax.pdfparser.repository.backup.CorrectionBackupRow
import com.payslipmax.pdfparser.repository.backup.DismissedDraftBackupRow
import com.payslipmax.pdfparser.repository.backup.DraftBackupRow
import com.payslipmax.pdfparser.repository.backup.GuideNoteBackupRow
import com.payslipmax.pdfparser.repository.backup.PayslipBackupRow
import com.payslipmax.pdfparser.repository.backup.PdfBackupRow
import com.payslipmax.pdfparser.repository.backup.SettingsBackup
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The decrypted contents of a `.pcda` archive. It holds only what the user created or imported: payslips,
 * their PDFs, settings, letters (with edits), deleted-letter records, per-field corrections and private Claim Guide notes. The
 * ledger and insights are derived from payslips and are rebuilt after a restore, never stored.
 *
 * It holds backup rows only (`repository/backup`), never Room entities, so the database schema is not the file format (F1); every key is
 * pinned with @SerialName (BackupWireKeysTest).
 *
 * Versions: 1 = payslips under the legacy key; 2 = payslips under the backup password; 3 = adds
 * [drafts], [dismissedDrafts] and [corrections]; 4 = adds [guideNotes]; 5 = adds the Orders Bank entries and orders (scanned pages travel
 * beside this document, see `PageArchive`); 6 = the same document and rows inside a versioned envelope (`BackupEnvelope`): one key derived
 * per archive seals the document, every row's ciphertext and every page, in place of the password per row. Added fields default to empty so
 * older files still decode: a v4 file restores with an empty Bank.
 */
@Serializable
data class PortableBackup(
    @SerialName("version") val version: Int = 1,
    @SerialName("encryptedPayslips") val encryptedPayslips: List<PayslipBackupRow>,
    @SerialName("pdfs") val pdfs: List<PdfBackupRow>,
    @SerialName("settings") val settings: SettingsBackup?,
    @SerialName("drafts") val drafts: List<DraftBackupRow> = emptyList(),
    @SerialName("dismissedDrafts") val dismissedDrafts: List<DismissedDraftBackupRow> = emptyList(),
    /** Each row's ciphertext is sealed with the backup's key (the password, per row, up to format 5; the archive key from format 6), never the device key. */
    @SerialName("corrections") val corrections: List<CorrectionBackupRow> = emptyList(),
    /** Private Guide notes; like [corrections], each row's ciphertext is sealed with the backup's key, never the device key. */
    @SerialName("guideNotes") val guideNotes: List<GuideNoteBackupRow> = emptyList(),
    /**
     * Orders Bank entries and vault orders (v5); each row's ciphertext is sealed with the backup's key, as Base64 text. Scanned pages
     * are not here: they would be a copy of the vault inside one JSON string. When the vault has pages the archive is a `PageArchive`
     * (this document, then one binary record per page); see there.
     */
    @SerialName("part2Entries") val part2Entries: List<Part2RowBackup> = emptyList(),
    @SerialName("part2Orders") val part2Orders: List<Part2RowBackup> = emptyList(),
    /** Drafts written by the frozen feature/part2-orders branch's v5 files: counted as skipped entries on restore, never decoded and never written. */
    @SerialName("part2Drafts") val part2Drafts: List<Part2LegacyDraft> = emptyList(),
    /** When the backup was made (epoch millis), shown in the restore preview; written from format 6, null in older files (D15). */
    @SerialName("exportedAt") val exportedAt: Long? = null,
) {
    companion object {
        const val CURRENT_VERSION = 6
    }
}

/**
 * How a restore reconciles a backup against payslips already on the device.
 *
 * - [REPLACE] wipes the device's existing payslips/PDFs first, then restores the backup — the target
 *   ends up as an exact copy of the backup (a fresh install effectively always behaves this way), except the
 *   device-only settings (lock, PIN hash, telemetry consent, Premium), which a backup never carries (D5).
 * - [MERGE] keeps the device's existing payslips and adds the backup's on top; a payslip present in
 *   both (same `dateStr`) is overwritten by the backup's copy. The device's own settings/entitlement
 *   are left untouched.
 */
enum class RestoreMode {
    REPLACE,
    MERGE,
}

/** What a restore did besides succeed: the rows it left out, per kind (unreadable, oversized, surplus, orphaned or a draft), so the officer can be told. */
class RestoreReport(
    val skipped: SkippedRows,
)

/**
 * Thrown when a backup was written by a newer app than this one: a format, KDF or envelope this app does not know ([backupVersion] is the
 * format version, or the envelope version when the envelope itself is newer). Decided before anything is decrypted when the file has a
 * header (`BackupEnvelope`). Fixed message; the screen shows the NEWER_VERSION line by type (`BackupFailure`), never this text.
 */
class UnsupportedBackupVersionException(
    val backupVersion: Int,
) : Exception("This backup was made by a newer version of PayslipMax. Update the app, then restore again.")

/** An archive that opened with the right password but whose rows cannot be read. Fixed message: no path, no content. */
class BackupDamagedException : Exception("This backup file is damaged and cannot be restored.")
