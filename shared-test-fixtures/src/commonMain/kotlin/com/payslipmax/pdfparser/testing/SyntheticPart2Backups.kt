package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.data.Part2RowBackup
import com.payslipmax.pdfparser.repository.PortableBackup
import kotlinx.serialization.json.Json

/** `.pcda` archives made by hand for tests that need rows a real export would never write (unreadable ones). No payslips, no pages. */
object SyntheticPart2Backups {
    /** A current-version archive whose only content is [unreadableEntries] and [unreadableOrders] rows that cannot be decoded, sealed under [password]. */
    fun withUnreadableRows(
        password: String,
        unreadableEntries: Int,
        unreadableOrders: Int,
    ): ByteArray {
        val backup =
            PortableBackup(
                PortableBackup.CURRENT_VERSION,
                emptyList(),
                emptyList(),
                null,
                part2Entries = (1..unreadableEntries).map { Part2RowBackup("junk-entry-$it", "!!") },
                part2Orders = (1..unreadableOrders).map { Part2RowBackup("junk-order-$it", "!!") },
            )
        return CryptoHelper.encrypt(Json.encodeToString(PortableBackup.serializer(), backup).encodeToByteArray(), password).getOrThrow()
    }
}
