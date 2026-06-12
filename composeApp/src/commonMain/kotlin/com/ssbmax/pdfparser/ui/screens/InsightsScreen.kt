package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.FinancialInsight
import com.ssbmax.pdfparser.insights.FinancialInsightsGenerator
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun InsightsScreen(
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip

    if (selected == null) {
        EmptyInsightsView()
        return
    }

    val previous = remember(selected, uiState.payslips) {
        val index = uiState.payslips.indexOfFirst { it.dateStr == selected.dateStr }
        if (index > 0) uiState.payslips[index - 1] else null
    }

    val insights = remember(selected, previous) { FinancialInsightsGenerator.generate(selected, previous) }

    LazyColumn(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { InsightsHeader() }
        item { WellnessMeterSection(payslip = selected) }
        item {
            GeminiAiInsightsSection(
                payslip = selected,
                isPremiumEnabled = uiState.isPremiumEnabled,
                aiInsights = uiState.aiInsights,
                isAiLoading = uiState.isAiLoading,
                aiError = uiState.aiError,
                onGenerateClick = { viewModel.generateAiInsights(selected) },
                onClearClick = { viewModel.clearAiInsights() }
            )
        }
        item { DsopSimulatorSection(initialContribution = selected.deductions.dsopSubscription) }
        items(items = insights) { insight ->
            InsightCard(insight = insight)
        }
    }
}

@Composable
private fun GeminiAiInsightsSection(
    payslip: ParsedPayslip,
    isPremiumEnabled: Boolean,
    aiInsights: String?,
    isAiLoading: Boolean,
    aiError: String?,
    onGenerateClick: () -> Unit,
    onClearClick: () -> Unit,
) {
    if (!isPremiumEnabled) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 22.sp, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = "AI Chartered Accountant Audit",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (aiInsights != null) {
                    IconButton(onClick = onClearClick, modifier = Modifier.size(24.dp)) {
                        Text("🔄", fontSize = 14.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when {
                isAiLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                aiError != null -> {
                    Text(
                        text = aiError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                aiInsights != null -> {
                    Text(
                        text = aiInsights,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                else -> {
                    Text(
                        text = "Generate professional tax saving suggestions, investment recommendations, and error audits using Gemini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onGenerateClick,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Analyze Payslip with Gemini AI")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInsightsView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "Please import or select a payslip to unlock financial insights.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InsightsHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = AppStrings.navigationInsights,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Personalized financial wellness and savings audits",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WellnessMeterSection(payslip: ParsedPayslip) {
    val gross = payslip.summary.grossPay
    val savings = payslip.deductions.dsopSubscription + payslip.deductions.agif
    val rate = if (gross > 0) (savings / gross) else 0.0
    val ratePercent = (rate * 100.0).coerceIn(0.0, 100.0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WellnessTextColumn(ratePercent)
            WellnessIndicator(ratePercent.toFloat() / 100f)
        }
    }
}

@Composable
private fun RowScope.WellnessTextColumn(ratePercent: Double) {
    Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
        Text(
            text = "Monthly Savings Rate",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Target: 20%+. You save ${ratePercent.toString().take(4)}% of your gross pay in DSOP and AGIF.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WellnessIndicator(progress: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.secondary,
            strokeWidth = 6.dp,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        )
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DsopSimulatorSection(
    initialContribution: Double,
    modifier: Modifier = Modifier
) {
    var monthlyContribution by remember { androidx.compose.runtime.mutableStateOf(initialContribution.coerceIn(6000.0, 100000.0).toFloat()) }
    val annualContribution = monthlyContribution * 12
    val exceedsLimit = annualContribution > 500000f

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(
                text = "DSOP Compound Simulator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Monthly Subscription",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "₹${formatAmount(monthlyContribution.toDouble())}/mo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                )
            }
            
            Slider(
                value = monthlyContribution,
                onValueChange = { monthlyContribution = it },
                valueRange = 6000f..100000f,
                steps = 94,
                colors = SliderDefaults.colors(
                    thumbColor = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    activeTrackColor = if (exceedsLimit) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            )

            Spacer(modifier = Modifier.height(4.dp))
            
            if (exceedsLimit) {
                TaxWarningTooltip()
            } else {
                TaxSafeTooltip()
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            ProjectionRow(monthlyContribution.toDouble())
        }
    }
}

@Composable
private fun TaxWarningTooltip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚠️", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = "Tax Warning: Annual DSOP contributions above ₹5 Lakhs (₹41,666/mo) attract income tax on interest earned. Stay below ₹41,666/mo to keep gains 100% tax-free.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun TaxSafeTooltip() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("✅", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
            Text(
                text = "Tax optimized: Annual contribution is below ₹5 Lakhs. All DSOP interest remains 100% tax-free under Section 10(11) of the Income Tax Act.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun ProjectionRow(monthly: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val calculated5 = remember(monthly) { calculateProjectedSavings(monthly, 5) }
        val calculated10 = remember(monthly) { calculateProjectedSavings(monthly, 10) }
        val calculated15 = remember(monthly) { calculateProjectedSavings(monthly, 15) }

        ProjectionCard("5 Years", calculated5, Modifier.weight(1f))
        ProjectionCard("10 Years", calculated10, Modifier.weight(1f))
        ProjectionCard("15 Years", calculated15, Modifier.weight(1f))
    }
}

@Composable
private fun ProjectionCard(label: String, value: Double, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "₹${formatShortAmount(value)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun InsightCard(insight: FinancialInsight) {
    val color =
        when (insight.type) {
            "success" -> MaterialTheme.colorScheme.secondary
            "warning" -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.tertiary
        }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.Top,
        ) {
            Text(text = insight.icon, fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

private fun calculateProjectedSavings(monthly: Double, years: Int, rate: Double = 0.071): Double {
    var balance = 0.0
    val annualContribution = monthly * 12.0
    for (i in 1..years) {
        balance = (balance + annualContribution) * (1.0 + rate)
    }
    return balance
}

private fun formatShortAmount(value: Double): String {
    return when {
        value >= 10000000.0 -> "${(value / 10000000.0).toString().take(4)} Cr"
        value >= 100000.0 -> "${(value / 100000.0).toString().take(4)} L"
        else -> value.toLong().toString()
    }
}


