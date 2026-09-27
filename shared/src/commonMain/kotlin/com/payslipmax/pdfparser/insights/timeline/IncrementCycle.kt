package com.payslipmax.pdfparser.insights.timeline

/** [this] advanced by [months] calendar months. */
fun PayMonth.plusMonths(months: Int): PayMonth {
    val totalIndex = index + months
    return PayMonth(totalIndex / 12, totalIndex % 12 + 1)
}

/**
 * The next 1 January or 1 July on or after [this] (Army Officers Pay Rules 2017, Rule 10 & 11: annual
 * increments fall only on those two dates). Used to round a "N months from now" date up to the officer's
 * actual next increment date.
 */
fun PayMonth.nextIncrementCycle(): PayMonth =
    when {
        month == 1 || month == 7 -> this
        month in 2..6 -> PayMonth(year, 7)
        else -> PayMonth(year + 1, 1)
    }
