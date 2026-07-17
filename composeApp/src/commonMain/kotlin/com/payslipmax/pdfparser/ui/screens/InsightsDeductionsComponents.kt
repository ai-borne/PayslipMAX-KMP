package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.ui.components.ChartLegend
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

@Composable
fun DeductionsBreakdownSection(
    history: List<LedgerRecordEntity>,
    selectedRecord: LedgerRecordEntity,
    modifier: Modifier = Modifier,
) {
    val bars = remember(history, selectedRecord) { buildDeductionBars(history, selectedRecord) }
    if (bars.isNotEmpty()) {
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
                        text = breakdownTitleFor(bars.size),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = breakdownRangeCaption(bars),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val chartColors = deductionsChartColors()
                DeductionsBarChart(bars = bars, colors = chartColors)
                ChartLegend(
                    listOf(
                        AppStrings.legendNetTakeHome to chartColors.net,
                        AppStrings.legendDsop to chartColors.dsop,
                        AppStrings.legendTax to chartColors.tax,
                        AppStrings.legendOtherDeductions to chartColors.other,
                    ),
                )
            }
        }
    }
}
