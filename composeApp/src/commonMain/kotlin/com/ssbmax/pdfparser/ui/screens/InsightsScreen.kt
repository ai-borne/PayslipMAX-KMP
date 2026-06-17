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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.FinancialInsight
import com.ssbmax.pdfparser.insights.FinancialInsightsGenerator
import com.ssbmax.pdfparser.ui.*
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

    var showUpgradeSheet by remember { mutableStateOf(false) }
    var showTransparencyDialog by remember { mutableStateOf(false) }

    InsightsContent(
        viewModel = viewModel,
        uiState = uiState,
        selected = selected,
        onShowUpgradeSheet = { showUpgradeSheet = true },
        onShowTransparency = { showTransparencyDialog = true },
        modifier = modifier,
    )

    if (showUpgradeSheet) {
        PremiumUpgradeBottomSheet(
            onDismissRequest = { showUpgradeSheet = false },
            onUnlockClick = { viewModel.setPremiumEnabled(true) },
        )
    }

    if (showTransparencyDialog) {
        com.ssbmax.pdfparser.ui.components.TransparencyDialog(
            payslip = selected,
            onConfirm = {
                showTransparencyDialog = false
                viewModel.generateAiInsights(selected)
            },
            onDismiss = { showTransparencyDialog = false }
        )
    }
}

@Composable
private fun InsightsContent(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    selected: ParsedPayslip,
    onShowUpgradeSheet: () -> Unit,
    onShowTransparency: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val previous =
        remember(selected, uiState.payslips) {
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
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
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
                onGenerateClick = onShowTransparency,
                onClearClick = { viewModel.clearAiInsights() },
                onUpgradeClick = onShowUpgradeSheet,
            )
        }
        item { DsopSimulatorSection(initialContribution = selected.deductions.dsopSubscription) }
        items(items = insights) { insight ->
            InsightCard(insight = insight)
        }
    }
}

@Composable
private fun EmptyInsightsView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppStrings.insightsEmptyState,
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
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = AppStrings.insightsSubheader,
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
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
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
    Column(modifier = Modifier.weight(1f).padding(end = AppDimensions.SpacingLarge)) {
        Text(
            text = AppStrings.insightsSavingsRateTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = AppStrings.insightsSavingsRateTarget + ratePercent.toString().take(4) + AppStrings.insightsSavingsRateSuffix,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WellnessIndicator(progress: Float) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(AppDimensions.IconSizeHuge)) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.secondary,
            strokeWidth = AppDimensions.SpacingSix,
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
        border = BorderStroke(AppDimensions.BorderThin, color.copy(alpha = 0.2f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalAlignment = Alignment.Top,
        ) {
            Text(text = insight.icon, fontSize = AppDimensions.TextSizeExtraLarge, modifier = Modifier.padding(end = AppDimensions.SpacingMedium))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}
