package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.components.AllocationPieChart
import com.ssbmax.pdfparser.ui.components.TrendLineChart
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun DashboardScreen(
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val payslips = uiState.payslips
    val selected = uiState.selectedPayslip

    if (payslips.isEmpty()) {
        EmptyStateScreen()
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.PaddingMedium),
    ) {
        selected?.let { payslip ->
            OfficerInfoBar(payslip = payslip)
            Spacer(modifier = Modifier.height(12.dp))
        }

        YearMonthPickerRow(
            viewModel = viewModel,
            selected = selected,
        )

        selected?.let {
            Spacer(modifier = Modifier.height(16.dp))
            StatsGridSection(payslip = it)

            Spacer(modifier = Modifier.height(16.dp))
            TrendChartCard(payslips = payslips)

            Spacer(modifier = Modifier.height(16.dp))
            AllocationChartCard(payslip = it)
        }
    }
}


@Composable
private fun StatsGridSection(payslip: ParsedPayslip) {
    val net = payslip.summary.netRemittance
    val basic = payslip.earnings.basicPay
    val dsop = payslip.taxAndSavings?.dsopFund?.closingBalance ?: 0.0
    val tax = payslip.deductions.incomeTax + payslip.deductions.educationCess
    val taxRate = if (payslip.summary.grossPay > 0) (tax / payslip.summary.grossPay) * 100 else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = AppStrings.cardNetTitle,
                value = "₹${formatAmount(net)}",
                subtitle = AppStrings.cardNetUnit,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = AppStrings.cardBpTitle,
                value = "₹${formatAmount(basic)}",
                subtitle = AppStrings.cardBpDesc,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = AppStrings.cardDsopTitle,
                value = "₹${formatAmount(dsop)}",
                subtitle = AppStrings.cardDsopDesc,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = AppStrings.cardTaxTitle,
                value = "${taxRate.toString().take(4)}%",
                subtitle = AppStrings.cardTaxDesc,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TrendChartCard(payslips: List<ParsedPayslip>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
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
                        modifier = Modifier
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
    }
}

@Composable
private fun AllocationChartCard(payslip: ParsedPayslip) {
    val gross = payslip.summary.grossPay.coerceAtLeast(1.0)
    val net = payslip.summary.netRemittance
    val dsop = payslip.deductions.dsopSubscription
    val tax = payslip.deductions.incomeTax + payslip.deductions.educationCess
    val other = (payslip.summary.totalDeductions - dsop - tax).coerceAtLeast(0.0)

    val values = listOf(net.toFloat(), dsop.toFloat(), tax.toFloat(), other.toFloat())
    val colors = listOf(Color(0xFF10B981), Color(0xFF8B5CF6), Color(0xFFEF4444), Color(0xFFF59E0B))

    Card(
        modifier = Modifier.fillMaxWidth(),
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

