package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.components.AllocationPieChart
import com.ssbmax.pdfparser.ui.theme.AppColors
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun AllocationChartCard(
    payslip: ParsedPayslip,
    modifier: Modifier = Modifier,
) {
    val gross = payslip.summary.grossPay.coerceAtLeast(1.0)
    val net = payslip.summary.netRemittance
    val dsop = payslip.deductions.dsopSubscription
    val tax = payslip.deductions.incomeTax + payslip.deductions.educationCess
    val other = (payslip.summary.totalDeductions - dsop - tax).coerceAtLeast(0.0)

    val values = listOf(net.toFloat(), dsop.toFloat(), tax.toFloat(), other.toFloat())
    val colors =
        listOf(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.tertiary,
            MaterialTheme.colorScheme.error,
            AppColors.Warning,
        )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = AppStrings.chartShareTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            AllocationPieChart(values = values, colors = colors)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            AllocationLegend(values = values, gross = gross, colors = colors)
        }
    }
}

@Composable
private fun AllocationLegend(
    values: List<Float>,
    gross: Double,
    colors: List<Color>,
) {
    val items =
        listOf(
            AppStrings.legendNetTakeHome,
            AppStrings.legendDsop,
            AppStrings.legendTax,
            AppStrings.legendOtherDeductions,
        )
    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSix)) {
        items.forEachIndexed { i, label ->
            val value = values[i]
            val pct = (value / gross) * 100.0
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(AppDimensions.SpacingTen).background(colors[i], RoundedCornerShape(AppDimensions.CornerRadiusSmall)),
                    )
                    Spacer(modifier = Modifier.width(AppDimensions.SpacingSmall))
                    Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "₹${formatAmount(value.toDouble())} (${pct.toString().take(4)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
