package com.payslipmax.pdfparser.database

/**
 * Everything a restore writes, already decoded and re-encrypted for this device, so the database
 * transaction that applies it can only fail on the write itself.
 */
data class BackupRows(
    val payslips: List<EncryptedPayslipEntity>,
    val pdfs: List<PayslipPdfEntity>,
    val drafts: List<RepresentationDraftEntity> = emptyList(),
    val dismissedDrafts: List<DismissedDraftEntity> = emptyList(),
    val corrections: List<PayslipCorrectionEntity> = emptyList(),
)
