package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = AppStrings.chartIncomeTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            val recent = payslips.takeLast(6)
            TrendLineChart(
                labels = recent.map { "${it.monthName.take(3)} '${it.year.toString().takeLast(2)}" },
                lineData1 = recent.map { it.summary.grossPay },
                lineData2 = recent.map { it.summary.netRemittance },
                label1 = "Gross Pay",
                label2 = "Net Remittance",
            )
            Spacer(modifier = Modifier.height(8.dp))
            TrendChartLegend()
        }
    }
}

@Composable
private fun TrendChartLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp, 4.dp)
                        .background(Color(0xFF3B82F6), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Gross Pay",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp, 4.dp)
                        .background(Color(0xFF10B981), RoundedCornerShape(2.dp)),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Net Remittance",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
