package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.data.Part2DraftBackup
import com.payslipmax.pdfparser.part2.data.Part2OrderBackup
import com.payslipmax.pdfparser.repository.PortableBackup
import kotlinx.serialization.json.Json

/** `.pcda` archives made by hand for tests that need rows a real export would never write (unreadable ones). No payslips, no pages. */
object SyntheticPart2Backups {
    /** A current-version archive whose only content is [unreadableDrafts] and [unreadableOrders] rows that cannot be decoded, sealed under [password]. */
    fun withUnreadableRows(
        password: String,
        unreadableDrafts: Int,
        unreadableOrders: Int,
    ): ByteArray {
        val backup =
            PortableBackup(
                PortableBackup.CURRENT_VERSION,
                emptyList(),
                emptyList(),
                null,
                part2Drafts = (1..unreadableDrafts).map { Part2DraftBackup("junk-draft-$it", "!!") },
                part2Orders = (1..unreadableOrders).map { Part2OrderBackup("junk-order-$it", "!!") },
            )
        return CryptoHelper.encrypt(Json.encodeToString(PortableBackup.serializer(), backup).encodeToByteArray(), password).getOrThrow()
    }
}
