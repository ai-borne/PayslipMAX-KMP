package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.ui.components.TrendChartLegend
import com.ssbmax.pdfparser.ui.components.TrendLineChart
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun TrendsSparklinesSection(
    history: List<LedgerRecordEntity>,
    selectedRecord: LedgerRecordEntity,
    modifier: Modifier = Modifier,
) {
    val points = remember(history, selectedRecord) { buildTrailingWindow(history, selectedRecord) }
    if (points.size >= 2) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppDimensions.CornerRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        ) {
            Column(
                modifier = Modifier.padding(AppDimensions.PaddingMedium),
                verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Text(
                        text = trendTitleFor(points.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = trendRangeCaption(points),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TrendLineChart(
                    labels = points.map { it.label },
                    lineData1 = points.map { it.gross.toDouble() },
                    lineData2 = points.map { it.net.toDouble() },
                    label1 = AppStrings.legendGrossPay,
                    label2 = AppStrings.legendNetRemittance,
                    highlightIndex = points.indexOfFirst { it.isSelected }.takeIf { it >= 0 },
                )
                TrendChartLegend(label1 = AppStrings.legendGrossPay, label2 = AppStrings.legendNetRemittance)
            }
        }
    }
}
