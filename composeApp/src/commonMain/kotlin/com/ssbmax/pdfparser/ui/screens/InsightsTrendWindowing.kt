package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.ui.theme.InsightsStrings

data class TrendPoint(
    val label: String,
    val gross: Float,
    val net: Float,
    val isSelected: Boolean,
)

private val monthAbbreviations =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun monthLabel(
    monthNum: Int,
    year: Int,
): String = "${monthAbbreviations[monthNum - 1]} '${year % 100}"

fun buildTrailingWindow(
    history: List<LedgerRecordEntity>,
    selected: LedgerRecordEntity,
    windowSize: Int = 6,
): List<TrendPoint> {
    val uptoSelected =
        history
            .filter { it.year < selected.year || (it.year == selected.year && it.monthNum <= selected.monthNum) }
            .sortedWith(compareBy({ it.year }, { it.monthNum }))
            .takeLast(windowSize)
    return uptoSelected.map {
        TrendPoint(
            label = monthLabel(it.monthNum, it.year),
            gross = it.grossPay.toFloat(),
            net = it.netPay.toFloat(),
            isSelected = it.dateStr == selected.dateStr,
        )
    }
}

fun trendRangeCaption(points: List<TrendPoint>): String {
    if (points.isEmpty()) return ""
    return "${points.first().label}${InsightsStrings.trendDateRangeSeparator}${points.last().label}"
}

fun trendTitleFor(pointCount: Int): String =
    if (pointCount == 6) InsightsStrings.sixMonthTrendTitle else "$pointCount${InsightsStrings.monthTrendTitleSuffix}"
