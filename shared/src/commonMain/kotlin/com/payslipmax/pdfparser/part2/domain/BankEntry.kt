package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate

/**
 * One casualty the officer noted: the code, the From date, an optional To, the Data columns the casualty uses as typed, a one-line note
 * and, once a scanned order covers it, that order's id. Never rendered text. Whether the entry is waiting is not stored: see [BankStatus].
 * [toString] hides everything, like every Part II item.
 */
data class BankEntry(
    val id: String,
    val code: String,
    val from: CalendarDate,
    val to: CalendarDate?,
    val data: List<String>,
    val note: String,
    val orderId: String?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    override fun toString(): String = "BankEntry(${if (orderId == null) "waiting" else "attached"})"
}
