package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.part2.data.Part2RowBackup
import com.payslipmax.pdfparser.repository.PortableBackup
import com.payslipmax.pdfparser.repository.backup.PayslipBackupRow
import kotlinx.serialization.json.Json

/** `.pcda` archives made by hand for tests that need rows a real export would never write (unreadable ones). No pages. */
object SyntheticPart2Backups {
    /**
     * A current-version archive holding [unreadableEntries] and [unreadableOrders] rows that cannot be decoded, sealed under [password],
     * beside one readable synthetic payslip: a file in which every row is unreadable is damaged, not a restore with skips (14a P5).
     */
    fun withUnreadableRows(
        password: String,
        unreadableEntries: Int,
        unreadableOrders: Int,
    ): ByteArray {
        val backup =
            PortableBackup(
                PortableBackup.CURRENT_VERSION,
                listOf(readablePayslip(password)),
                emptyList(),
                null,
                part2Entries = (1..unreadableEntries).map { Part2RowBackup("junk-entry-$it", "!!") },
                part2Orders = (1..unreadableOrders).map { Part2RowBackup("junk-order-$it", "!!") },
            )
        return CryptoHelper.encrypt(Json.encodeToString(PortableBackup.serializer(), backup).encodeToByteArray(), password).getOrThrow()
    }

    private fun readablePayslip(password: String): PayslipBackupRow {
        val payslip =
            ParsedPayslip(
                "synthetic.pdf", 2024, 8, "August", "08/2024", Officer("Synthetic", "0", "0"), Earnings(), Deductions(), LedgerBalances(),
                PayslipSummary(0.0, 0.0, 0.0), null,
            )
        val sealed = payslip.toEncryptedEntity(password)
        return PayslipBackupRow(sealed.dateStr, sealed.year, sealed.monthNum, sealed.monthName, sealed.ciphertext)
    }
}
