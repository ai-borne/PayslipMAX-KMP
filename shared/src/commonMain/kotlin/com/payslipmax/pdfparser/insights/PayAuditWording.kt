package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.parser.PayslipPatternConfig
import kotlin.math.abs
import kotlin.math.round

/**
 * SSOT for how finding descriptions and change reasons write rupees, percentages and months, so every
 * auditor reads the same way: "₹1,23,100" (Indian grouping, via [TaxLedgerAggregator.formatIndianCurrency]),
 * "58% to 60%", "July to August 2018" — never "₹123100", "58%->60%" or "7/2018".
 */
object PayAuditWording {
    fun rupees(amount: Double): String {
        val sign = if (round(amount) < 0.0) "-" else ""
        return "$sign₹" + TaxLedgerAggregator.formatIndianCurrency(abs(amount))
    }

    fun percentChange(
        from: Int,
        to: Int,
    ): String = "$from% to $to%"

    fun monthYear(
        month: Int,
        year: Int,
    ): String = "${PayslipPatternConfig.monthNames[month]} $year"

    /** "July 2018" for one month, "July to August 2018" for several (both ends in the same [year]). */
    fun monthSpan(
        first: Int,
        last: Int,
        year: Int,
    ): String =
        if (first == last) {
            monthYear(first, year)
        } else {
            "${PayslipPatternConfig.monthNames[first]} to ${PayslipPatternConfig.monthNames[last]} $year"
        }
}
