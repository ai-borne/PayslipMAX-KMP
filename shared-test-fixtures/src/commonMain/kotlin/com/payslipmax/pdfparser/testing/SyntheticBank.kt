package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.VaultOrder

/**
 * Invented Orders Bank data for tests: codes, notes and order numbers are made up and carry markers, so a test can scan a stored row,
 * a log line or an error for them. Nothing here is a real order, casualty or person.
 */
object SyntheticBank {
    const val MARKER_NOTE = "SYN-NOTE-Zq7"
    const val MARKER_DATA = "SYN-DATA-Kx3"
    const val MARKER_ORDER_NO = "SYN/0001/2026"
    const val CODE = "SYNCODE1"

    fun entry(
        id: String = "entry-1",
        updatedAt: Long = 1_000L,
        orderId: String? = null,
        code: String = CODE,
        from: CalendarDate = CalendarDate(2026, 3, 1),
    ): BankEntry = BankEntry(id, code, from, CalendarDate(2026, 3, 9), listOf("", MARKER_DATA, "", ""), MARKER_NOTE, orderId, updatedAt - 500, updatedAt)

    fun order(
        id: String = "order-1",
        orderNo: String? = MARKER_ORDER_NO,
        date: CalendarDate? = CalendarDate(2026, 3, 20),
        updatedAt: Long = 2_000L,
    ): VaultOrder = VaultOrder(id, orderNo, date, updatedAt - 1000, updatedAt)

    /** A page-sized byte string that starts like a JPEG; the stored bytes are never decoded as an image. */
    fun jpeg(
        size: Int,
        seed: Int = 1,
    ): ByteArray = ByteArray(size) { i -> if (i < 2) (if (i == 0) 0xFF else 0xD8).toByte() else ((i * 31 + seed) and 0xFF).toByte() }
}
