package com.payslipmax.pdfparser.database

import com.payslipmax.pdfparser.part2.data.Part2DraftEntity
import com.payslipmax.pdfparser.part2.data.Part2OrderEntity
import com.payslipmax.pdfparser.part2.data.Part2PageEntity

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
    val guideNotes: List<GuideNoteEntity> = emptyList(),
    val part2Drafts: List<Part2DraftEntity> = emptyList(),
    val part2Orders: List<Part2OrderEntity> = emptyList(),
    /** A sequence, not a list: a restore re-encrypts one page at a time inside the transaction, so no second copy of the vault is held. */
    val part2Pages: Sequence<Part2PageEntity> = emptySequence(),
)
