package com.payslipmax.pdfparser.utils

import kotlin.math.round

/** SSOT for Indian-style (Lakh/Crore) numeric display formatting, shared across insight text and stat cards. */
object FormatUtils {
    /** Full comma-grouped form, e.g. 1234567.0 -> "12,34,567". */
    fun formatIndianGrouped(amount: Double): String {
        val longVal = amount.toLong()
        val str = longVal.toString()
        if (str.length <= 3) return str
        val lastThree = str.substring(str.length - 3)
        val remaining = str.substring(0, str.length - 3)
        val builder = StringBuilder()
        var i = remaining.length
        while (i > 0) {
            if (i >= 2) {
                builder.insert(0, remaining.substring(i - 2, i))
                if (i - 2 > 0) builder.insert(0, ",")
                i -= 2
            } else {
                builder.insert(0, remaining.substring(0, 1))
                i -= 1
            }
        }
        return "$builder,$lastThree"
    }

    /** Lakh-abbreviated form for amounts >= 1,00,000, e.g. 1234567.0 -> "12.3L"; falls back to [formatIndianGrouped] below that. */
    fun formatIndianCompact(amount: Double): String {
        val longVal = amount.toLong()
        if (longVal >= 100000) {
            val lakhs = round(longVal / 100000.0 * 10) / 10.0
            return "${lakhs}L"
        }
        return formatIndianGrouped(amount)
    }
}
