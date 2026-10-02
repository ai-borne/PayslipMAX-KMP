package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.parser.PayslipPatternConfig
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings

/** "Aug 2026": month names come from [PayslipPatternConfig.monthNames], the SSOT for month spelling. */
fun formatPayMonth(month: PayMonth): String {
    val name = PayslipPatternConfig.monthNames.getOrNull(month.month).orEmpty()
    return "${name.take(SHORT_MONTH_LENGTH)} ${month.year}"
}

/** Plain-words name for a pay-line field key; unknown keys fall back to their camelCase split into words. */
fun payLineLabel(field: String): String =
    PayAuditVerdictStrings.payLineLabels[field]
        ?: buildString {
            field.forEachIndexed { index, c ->
                when {
                    index == 0 -> append(c.uppercaseChar())
                    c.isUpperCase() -> append(' ').append(c)
                    else -> append(c)
                }
            }
        }

fun formatChangeAmounts(
    from: Double,
    to: Double,
): String = "${formatCurrency(from)} → ${formatCurrency(to)}"

private const val SHORT_MONTH_LENGTH = 3
