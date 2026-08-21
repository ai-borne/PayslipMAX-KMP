package com.payslipmax.pdfparser.ui.screens

import kotlin.math.abs

fun formatCurrency(amount: Double): String {
    val isNegative = amount < 0.0
    val absVal = abs(amount.toLong())
    val sign = if (isNegative && absVal != 0L) "-" else ""
    val str = absVal.toString()
    if (str.length <= 3) return "$sign₹$str"
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
    return "$sign₹$builder,$lastThree"
}
