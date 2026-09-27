package com.payslipmax.pdfparser.insights.timeline

/**
 * One pay-line field whose rupee value moved between two consecutive stored payslips, and the reason a
 * structural rule attributes it to (see [PayLineChangeExplainer]). [reason] is null when nothing in the
 * officer's [ServiceTimeline] accounts for the move — an honest gap, not a guess.
 */
data class ChangeExplanation(
    val month: PayMonth,
    val field: String,
    val from: Double,
    val to: Double,
    val reason: String?,
)
