package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.DismissedDraftEntity
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.GuideNoteEntity
import com.payslipmax.pdfparser.database.PayslipCorrectionEntity
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import kotlinx.serialization.Serializable

/**
 * The decrypted contents of a `.pcda` archive. It holds only what the user created or imported: payslips,
 * their PDFs, settings, letters (with edits), deleted-letter records, per-field corrections and private Claim Guide notes. The
 * ledger and insights are derived from payslips and are rebuilt after a restore, never stored.
 *
 * Versions: 1 = payslips under the legacy key; 2 = payslips under the backup password; 3 = adds
 * [drafts], [dismissedDrafts] and [corrections]; 4 = adds [guideNotes]. Added fields default to empty so older files still decode.
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
) {
    companion object {
        const val CURRENT_VERSION = 4
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
