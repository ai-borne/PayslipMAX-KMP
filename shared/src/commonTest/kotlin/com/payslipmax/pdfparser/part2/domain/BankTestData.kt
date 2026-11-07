package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate

/** Invented entries for the Orders Bank logic tests; no real date, order or casualty of the officer is used. */
internal fun date(
    y: Int,
    m: Int,
    d: Int,
) = CalendarDate(y, m, d)

internal fun entry(
    id: String,
    from: CalendarDate,
    orderId: String? = null,
    created: Long = 0,
    code: String = "FIRSTCODE",
) = BankEntry(id, code, from, null, listOf("", "", "", ""), "", orderId, created, created)
