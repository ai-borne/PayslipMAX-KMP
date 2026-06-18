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
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    val isLoading = uiState.isLoading

    if (isLoading && selected == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

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
        onNavigateTo = onNavigateTo,
        modifier = modifier,
    )

    InsightsOverlayDialogs(
        showUpgradeSheet = showUpgradeSheet,
        showTransparencyDialog = showTransparencyDialog,
        selected = selected,
        viewModel = viewModel,
        onDismissUpgrade = { showUpgradeSheet = false },
        onDismissTransparency = { showTransparencyDialog = false }
    )
}

@Composable
private fun InsightsOverlayDialogs(
    showUpgradeSheet: Boolean,
    showTransparencyDialog: Boolean,
    selected: ParsedPayslip,
    viewModel: PayslipViewModel,
    onDismissUpgrade: () -> Unit,
    onDismissTransparency: () -> Unit,
) {
    if (showUpgradeSheet) {
        PremiumUpgradeBottomSheet(
            onDismissRequest = onDismissUpgrade,
            onUnlockClick = { viewModel.setPremiumEnabled(true) },
        )
    }

    if (showTransparencyDialog) {
        com.ssbmax.pdfparser.ui.components.TransparencyDialog(
            payslip = selected,
            onConfirm = {
                onDismissTransparency()
                viewModel.generateAiInsights(selected)
            },
            onDismiss = onDismissTransparency,
        )
    }
}

@Composable
private fun rememberInsights(
    selected: ParsedPayslip,
    payslips: List<ParsedPayslip>,
): List<FinancialInsight> {
    val previous =
        remember(selected, payslips) {
            val index = payslips.indexOfFirst { it.dateStr == selected.dateStr }
            if (index > 0) payslips[index - 1] else null
        }
    return remember(selected, previous) { FinancialInsightsGenerator.generate(selected, previous) }
}

@Composable
private fun InsightsContent(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    selected: ParsedPayslip,
    onShowUpgradeSheet: () -> Unit,
    onShowTransparency: () -> Unit,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val insights = rememberInsights(selected, uiState.payslips)

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
        item {
            PremiumToolsSection(
                isPremiumEnabled = uiState.isPremiumEnabled,
                onNavigateTo = onNavigateTo,
                onUpgradeClick = onShowUpgradeSheet,
            )
        }
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

@Composable
private fun PremiumToolsSection(
    isPremiumEnabled: Boolean,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = AppStrings.premiumToolsTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            PremiumButtonsRow(
                isPremiumEnabled = isPremiumEnabled,
                onNavigateTo = onNavigateTo,
                onUpgradeClick = onUpgradeClick,
            )
        }
    }
}

@Composable
private fun PremiumButtonsRow(
    isPremiumEnabled: Boolean,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        val onClick = { target: com.ssbmax.pdfparser.Screen ->
            if (isPremiumEnabled) onNavigateTo(target) else onUpgradeClick()
        }
        OutlinedButton(
            onClick = { onClick(com.ssbmax.pdfparser.Screen.Representation) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.premiumToolsDraftClaims, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { onClick(com.ssbmax.pdfparser.Screen.TaxPlanning) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.premiumToolsTaxPlanner, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
        OutlinedButton(
            onClick = { onClick(com.ssbmax.pdfparser.Screen.RetirementPlanning) },
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.premiumToolsDsopSimulator, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
