package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.DismissedDraftEntity
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.PayslipCorrectionEntity
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.part2.data.Part2DraftBackup
import com.payslipmax.pdfparser.part2.data.Part2OrderBackup
import kotlinx.serialization.Serializable

/**
 * The decrypted contents of a `.pcda` archive. It holds only what the user created or imported: payslips,
 * their PDFs, settings, letters (with edits), deleted-letter records, per-field corrections and private Claim Guide notes. The
 * ledger and insights are derived from payslips and are rebuilt after a restore, never stored.
 *
 * Versions: 1 = payslips under the legacy key; 2 = payslips under the backup password; 3 = adds
 * [drafts], [dismissedDrafts] and [corrections]; 4 = adds [guideNotes]; 5 = adds the Part II drafts and orders (pages travel beside this document, see `PageArchive`). Added fields default to empty so older files still decode.
 */
@Serializable
data class PortableBackup(
    val version: Int = 1,
    val encryptedPayslips: List<EncryptedPayslipEntity>,
    val pdfs: List<PayslipPdfEntity>,
    val settings: AppSettingsEntity?,
    val drafts: List<RepresentationDraftEntity> = emptyList(),
    val dismissedDrafts: List<DismissedDraftEntity> = emptyList(),
    /** Each row's ciphertext is encrypted with the backup password, not the device key. */
    val corrections: List<PayslipCorrectionEntity> = emptyList(),
    /** Private Guide notes; like [corrections], each row's ciphertext is encrypted with the backup password, not the device key. */
    val guideNotes: List<GuideNoteEntity> = emptyList(),
    /**
     * Part II drafts and vault orders (v5); each row's ciphertext is encrypted with the backup password, as Base64 text. Scanned pages are
     * not here: they would be a copy of the vault inside one JSON string. When the vault has pages the archive is a `PageArchive`
     * (this document, then one binary record per page); see there.
     */
    val part2Drafts: List<Part2DraftBackup> = emptyList(),
    val part2Orders: List<Part2OrderBackup> = emptyList(),
) {
    companion object {
        const val CURRENT_VERSION = 5
    }
}

/**
 * How a restore reconciles a backup against payslips already on the device.
 *
 * - [REPLACE] wipes the device's existing payslips/PDFs first, then restores the backup — the target
 *   ends up as an exact copy of the backup (a fresh install effectively always behaves this way).
 * - [MERGE] keeps the device's existing payslips and adds the backup's on top; a payslip present in
 *   both (same `dateStr`) is overwritten by the backup's copy. The device's own settings/entitlement
 *   are left untouched.
 */
enum class RestoreMode {
    REPLACE,
    MERGE,
}

/**
 * Thrown when a backup was written by a newer app than this one. Its message is shown to the user as the
 * restore error, so it says what to do.
 */
class UnsupportedBackupVersionException(
    val backupVersion: Int,
) : Exception("This backup was made by a newer version of PayslipMax. Update the app, then restore again.")
