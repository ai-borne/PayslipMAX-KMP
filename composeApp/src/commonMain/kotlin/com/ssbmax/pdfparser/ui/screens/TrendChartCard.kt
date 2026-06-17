package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.components.TrendLineChart
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun TrendChartCard(
    payslips: List<ParsedPayslip>,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = AppStrings.chartIncomeTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            val recent = payslips.takeLast(6)
            TrendLineChart(
                labels = recent.map { "${it.monthName.take(3)} '${it.year.toString().takeLast(2)}" },
                lineData1 = recent.map { it.summary.grossPay },
                lineData2 = recent.map { it.summary.netRemittance },
                label1 = AppStrings.legendGrossPay,
                label2 = AppStrings.legendNetRemittance,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
            TrendChartLegend()
        }
    }
}

@Composable
private fun TrendChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(AppDimensions.SpacingMedium, AppDimensions.SpacingTiny)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(AppDimensions.CornerRadiusSmall)),
            )
            Spacer(modifier = Modifier.width(AppDimensions.SpacingSix))
            Text(
                text = AppStrings.legendGrossPay,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(AppDimensions.SpacingMedium, AppDimensions.SpacingTiny)
                        .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(AppDimensions.CornerRadiusSmall)),
            )
            Spacer(modifier = Modifier.width(AppDimensions.SpacingSix))
            Text(
                text = AppStrings.legendNetRemittance,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
