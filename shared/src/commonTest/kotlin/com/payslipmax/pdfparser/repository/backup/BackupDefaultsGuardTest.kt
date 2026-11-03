package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.database.GuideNotePayload
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SingleCorrection
import com.payslipmax.pdfparser.part2.data.EntryPayload
import com.payslipmax.pdfparser.part2.data.OrderPayload
import com.payslipmax.pdfparser.part2.data.PageTrailer
import com.payslipmax.pdfparser.repository.PortableBackup
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (invariant 5, F2): an older app reading a newer backup, or this app reading a backup made before a field existed, can only cope
 * with a field it may find missing if that field has a default. The fields that have none today are frozen here (owner decision, P2:
 * they are identities and values every shipped writer has always written, and a missing `summary` must fail rather than become zeros).
 * A new field without a default, or an old field that loses its default, fails this test: give the field a default instead.
 * Checked through the serializer descriptors (`isElementOptional`), the same walk as BackupWireKeysTest, so no reflection.
 */
class BackupDefaultsGuardTest {
    @Test
    fun noFieldABackupOrADevicePayloadCarriesIsRequiredExceptTheOnesEveryShippedWriterAlwaysWrote() {
        val roots =
            listOf(
                PortableBackup.serializer().descriptor,
                ParsedPayslip.serializer().descriptor,
                SingleCorrection.serializer().descriptor,
                GuideNotePayload.serializer().descriptor,
                EntryPayload.serializer().descriptor,
                OrderPayload.serializer().descriptor,
                PageTrailer.serializer().descriptor,
            )

        val required = roots.map(::requiredKeys).reduce { all, next -> all + next }.filterValues { it.isNotEmpty() }

        assertEquals(REQUIRED_TODAY, required)
    }

    private companion object {
        const val DOMAIN = "com.payslipmax.pdfparser.domain"
        const val PART2 = "com.payslipmax.pdfparser.part2.data"
        const val BACKUP = "com.payslipmax.pdfparser.repository.backup"

        val REQUIRED_TODAY =
            mapOf(
                "com.payslipmax.pdfparser.repository.PortableBackup" to setOf("encryptedPayslips", "pdfs", "settings"),
                "$BACKUP.PayslipBackupRow" to setOf("dateStr", "year", "monthNum", "monthName", "ciphertext"),
                "$BACKUP.PdfBackupRow" to setOf("dateStr", "pdfData"),
                "$BACKUP.DraftBackupRow" to setOf("id", "disputeMonth", "disputeType", "recipient", "subject", "bodyText", "createdAt"),
                "$BACKUP.DismissedDraftBackupRow" to setOf("disputeMonth", "disputeType"),
                "$BACKUP.CorrectionBackupRow" to setOf("dateStr", "ciphertext"),
                "$BACKUP.GuideNoteBackupRow" to setOf("cardId", "ciphertext"),
                "$PART2.Part2RowBackup" to setOf("id", "data"),
                "$DOMAIN.ParsedPayslip" to
                    setOf(
                        "file", "year", "monthNum", "monthName", "dateStr", "officer", "earnings", "deductions", "ledgerBalances",
                        "summary", "taxAndSavings",
                    ),
                "$DOMAIN.Officer" to setOf("name", "accountNo", "pan"),
                "$DOMAIN.PayslipSummary" to setOf("grossPay", "totalDeductions", "netRemittance"),
                "$DOMAIN.DiagnosticSuggestion" to setOf("fieldKey", "reason"),
                "$DOMAIN.LeaveAccumulation" to setOf("entries"),
                "$DOMAIN.LeaveEntry" to setOf("casDate", "casCode", "days", "orderNo", "orderDate"),
                "$DOMAIN.SingleCorrection" to
                    setOf("fieldKey", "codeHead", "amount", "category", "type", "originalAmount", "originalCodeHead", "timestamp"),
                "com.payslipmax.pdfparser.database.GuideNotePayload" to setOf("cardId", "text", "cardRev", "updatedAt"),
                "$PART2.EntryPayload" to setOf("v", "id", "code", "from", "createdAt", "updatedAt"),
                "$PART2.DatePayload" to setOf("y", "m", "d"),
                "$PART2.OrderPayload" to setOf("v", "id", "createdAt", "updatedAt"),
                "$PART2.PageTrailer" to setOf("pages"),
            )
    }
}
