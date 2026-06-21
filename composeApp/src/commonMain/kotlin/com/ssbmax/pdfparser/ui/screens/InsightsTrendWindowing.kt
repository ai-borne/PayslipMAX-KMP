package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.ui.theme.InsightsStrings

data class DeductionBar(
    val label: String,
    val net: Float,
    val dsop: Float,
    val tax: Float,
    val other: Float,
    val isSelected: Boolean,
)

private val monthAbbreviations =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun monthLabel(
    monthNum: Int,
    year: Int,
): String = "${monthAbbreviations[monthNum - 1]} '${year % 100}"

private fun trailingRecords(
    history: List<LedgerRecordEntity>,
    selected: LedgerRecordEntity,
    windowSize: Int,
): List<LedgerRecordEntity> =
    history
        .filter { it.year < selected.year || (it.year == selected.year && it.monthNum <= selected.monthNum) }
        .sortedWith(compareBy({ it.year }, { it.monthNum }))
        .takeLast(windowSize)

fun buildDeductionBars(
    history: List<LedgerRecordEntity>,
    selected: LedgerRecordEntity,
    windowSize: Int = 6,
): List<DeductionBar> =
    trailingRecords(history, selected, windowSize).map {
        val other = (it.grossPay - it.netPay - it.incomeTax - it.dsopSubscription).coerceAtLeast(0.0)
        DeductionBar(
            label = monthLabel(it.monthNum, it.year),
            net = it.netPay.toFloat(),
            dsop = it.dsopSubscription.toFloat(),
            tax = it.incomeTax.toFloat(),
            other = other.toFloat(),
            isSelected = it.dateStr == selected.dateStr,
        )
    }

fun breakdownRangeCaption(bars: List<DeductionBar>): String {
    if (bars.isEmpty()) return ""
    if (bars.size == 1) return bars.first().label
    return "${bars.first().label}${InsightsStrings.dateRangeSeparator}${bars.last().label}"
}

fun breakdownTitleFor(barCount: Int): String =
    if (barCount == 6) InsightsStrings.sixMonthBreakdownTitle else "$barCount${InsightsStrings.monthBreakdownTitleSuffix}"
