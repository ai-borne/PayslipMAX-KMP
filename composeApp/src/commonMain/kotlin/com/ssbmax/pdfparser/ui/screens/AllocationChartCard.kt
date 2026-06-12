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
import com.ssbmax.pdfparser.ui.components.AllocationPieChart
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
    val colors = listOf(Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFFF59E0B))

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = AppStrings.chartShareTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(16.dp))
            AllocationPieChart(values = values, colors = colors)
            Spacer(modifier = Modifier.height(16.dp))
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
    val items = listOf("Net Take-Home", "Provident Fund (DSOP)", "Taxes & Cess", "Other Deductions")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                        modifier = Modifier.size(10.dp).background(colors[i], RoundedCornerShape(2.dp)),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
