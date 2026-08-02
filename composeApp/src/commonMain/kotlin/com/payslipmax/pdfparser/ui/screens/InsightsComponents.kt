package com.payslipmax.pdfparser.ui.screens

fun formatCurrency(amount: Double): String {
    val longVal = amount.toLong()
    val str = longVal.toString()
    if (str.length <= 3) return "₹$str"
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
    return "₹$builder,$lastThree"
}
