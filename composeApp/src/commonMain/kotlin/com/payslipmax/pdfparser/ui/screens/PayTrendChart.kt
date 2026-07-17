package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.parser.PayslipPatternConfig
import com.payslipmax.pdfparser.ui.components.ChartLegend
import com.payslipmax.pdfparser.ui.components.TrendLineChart
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings

data class PayTrendPoint(
    val label: String,
    val netPay: Double,
    val incomeTax: Double,
    val dateStr: String,
)

private fun trendMonthLabel(
    monthNum: Int,
    year: Int,
): String = "${PayslipPatternConfig.monthNames[monthNum].take(3)} '${year % 100}"

/** Trailing window ending at [selected] (not necessarily the latest record) — mirrors the selection-aware windowing already used for the deductions breakdown chart. */
fun buildPayTrendPoints(
    history: List<LedgerRecordEntity>,
    selected: LedgerRecordEntity,
    windowSize: Int = 6,
): List<PayTrendPoint> =
    history
        .filter { it.year < selected.year || (it.year == selected.year && it.monthNum <= selected.monthNum) }
        .sortedWith(compareBy({ it.year }, { it.monthNum }))
        .takeLast(windowSize)
        .map {
            PayTrendPoint(label = trendMonthLabel(it.monthNum, it.year), netPay = it.netPay, incomeTax = it.incomeTax, dateStr = it.dateStr)
        }

/** Net-pay (primary) + tax (secondary) trend line, with tappable month rows that highlight the matching chart point. */
@Composable
fun PayTrendChart(
    history: List<LedgerRecordEntity>,
    selected: LedgerRecordEntity,
    modifier: Modifier = Modifier,
) {
    val points = remember(history, selected) { buildPayTrendPoints(history, selected) }
    if (points.isEmpty()) return
    var highlightIndex by remember(points) {
        mutableStateOf(points.indexOfFirst { it.dateStr == selected.dateStr }.takeIf { it >= 0 })
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(text = InsightsStrings.payTrendChartTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            TrendLineChart(
                labels = points.map { it.label },
                lineData1 = points.map { it.netPay },
                lineData2 = points.map { it.incomeTax },
                label1 = InsightsStrings.payTrendLegendNetPay,
                label2 = InsightsStrings.payTrendLegendTax,
                highlightIndex = highlightIndex,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            ChartLegend(
                listOf(
                    InsightsStrings.payTrendLegendNetPay to MaterialTheme.colorScheme.primary,
                    InsightsStrings.payTrendLegendTax to MaterialTheme.colorScheme.secondary,
                ),
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
            points.forEachIndexed { index, point ->
                PayTrendMonthRow(point = point, isSelected = index == highlightIndex, onClick = { highlightIndex = index })
            }
        }
    }
}

@Composable
private fun PayTrendMonthRow(
    point: PayTrendPoint,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(background)
                .clickable(onClick = onClick)
                .padding(vertical = AppDimensions.SpacingTiny),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = point.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
            Text(text = formatCurrency(point.netPay), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = formatCurrency(point.incomeTax), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
        }
    }
}
