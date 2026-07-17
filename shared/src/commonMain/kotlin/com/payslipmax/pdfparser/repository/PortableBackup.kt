package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.database.EncryptedPayslipEntity
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import kotlinx.serialization.Serializable

@Serializable
data class PortableBackup(
    val version: Int = 1,
    val encryptedPayslips: List<EncryptedPayslipEntity>,
    val pdfs: List<PayslipPdfEntity>,
    val settings: AppSettingsEntity?,
)

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
