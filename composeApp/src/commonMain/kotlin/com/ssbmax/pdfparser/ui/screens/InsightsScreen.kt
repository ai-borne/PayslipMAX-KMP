package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.Screen
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipUiState
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.clearAiInsights
import com.ssbmax.pdfparser.ui.components.TransparencyDialog
import com.ssbmax.pdfparser.ui.generateAiInsights
import com.ssbmax.pdfparser.ui.setPremiumEnabled
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun InsightsScreen(
    viewModel: PayslipViewModel,
    onNavigateTo: (Screen) -> Unit,
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
    var showInsightsSheet by remember { mutableStateOf(false) }

    InsightsContent(
        viewModel = viewModel,
        uiState = uiState,
        selected = selected,
        onShowUpgradeSheet = { showUpgradeSheet = true },
        onShowTransparency = { showTransparencyDialog = true },
        onViewInsightsClick = { showInsightsSheet = true },
        onNavigateTo = onNavigateTo,
        modifier = modifier,
    )

    InsightsOverlayDialogs(
        showUpgradeSheet = showUpgradeSheet,
        showTransparencyDialog = showTransparencyDialog,
        showInsightsSheet = showInsightsSheet,
        aiInsights = uiState.aiInsights,
        selected = selected,
        viewModel = viewModel,
        onDismissUpgrade = { showUpgradeSheet = false },
        onDismissTransparency = { showTransparencyDialog = false },
        onDismissInsights = { showInsightsSheet = false },
    )
}

@Composable
private fun InsightsOverlayDialogs(
    showUpgradeSheet: Boolean,
    showTransparencyDialog: Boolean,
    showInsightsSheet: Boolean,
    aiInsights: String?,
    selected: ParsedPayslip,
    viewModel: PayslipViewModel,
    onDismissUpgrade: () -> Unit,
    onDismissTransparency: () -> Unit,
    onDismissInsights: () -> Unit,
) {
    if (showUpgradeSheet) {
        PremiumUpgradeBottomSheet(
            onDismissRequest = onDismissUpgrade,
            onUnlockClick = { viewModel.setPremiumEnabled(true) },
        )
    }

    if (showTransparencyDialog) {
        TransparencyDialog(
            payslip = selected,
            onConfirm = {
                onDismissTransparency()
                viewModel.generateAiInsights(selected)
            },
            onDismiss = onDismissTransparency,
        )
    }

    if (showInsightsSheet && aiInsights != null) {
        AiInsightsBottomSheet(
            aiInsights = aiInsights,
            onDismissRequest = onDismissInsights,
            onRegenerateClick = {
                onDismissInsights()
                viewModel.generateAiInsights(selected)
            },
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
    onViewInsightsClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ledgerRecords by viewModel.ledgerRecords.collectAsState()
    val state = rememberInsightsState(selected, ledgerRecords)
    var wellnessExpanded by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxSize()) {
        InsightsTopBar(
            payslips = uiState.payslips,
            selected = selected,
            onSelectPayslip = { viewModel.selectPayslip(it) },
            healthScore = state.engineResult.healthScore,
            wellnessExpanded = wellnessExpanded,
            onWellnessExpandClick = { wellnessExpanded = !wellnessExpanded },
        )
        InsightsLazyBody(
            state = state,
            uiState = uiState,
            viewModel = viewModel,
            wellnessExpanded = wellnessExpanded,
            onWellnessExpandClick = { wellnessExpanded = !wellnessExpanded },
            onShowUpgradeSheet = onShowUpgradeSheet,
            onShowTransparency = onShowTransparency,
            onViewInsightsClick = onViewInsightsClick,
            onNavigateTo = onNavigateTo,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InsightsLazyBody(
    state: InsightsState,
    uiState: PayslipUiState,
    viewModel: PayslipViewModel,
    wellnessExpanded: Boolean,
    onWellnessExpandClick: () -> Unit,
    onShowUpgradeSheet: () -> Unit,
    onShowTransparency: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        item {
            InsightsHealthKpiCardItem(
                state = state,
                isPremiumEnabled = uiState.isPremiumEnabled,
                wellnessExpanded = wellnessExpanded,
                onWellnessExpandClick = onWellnessExpandClick,
                onShowUpgradeSheet = onShowUpgradeSheet,
                onNavigateTo = onNavigateTo,
            )
        }
        item { ExecutiveSummaryCard(current = state.currentRecord, previous = state.previousRecord) }
        item { DeductionsBreakdownSection(history = state.historySorted, selectedRecord = state.currentRecord) }
        item { KeyFindingsSection(state = state) }
        item { AiHighlightsSection(state = state) }
        item {
            InsightsPremiumIntelligenceItem(
                state = state,
                uiState = uiState,
                viewModel = viewModel,
                onShowUpgradeSheet = onShowUpgradeSheet,
                onShowTransparency = onShowTransparency,
                onViewInsightsClick = onViewInsightsClick,
                onNavigateTo = onNavigateTo,
            )
        }
    }
}

@Composable
private fun InsightsHealthKpiCardItem(
    state: InsightsState,
    isPremiumEnabled: Boolean,
    wellnessExpanded: Boolean,
    onWellnessExpandClick: () -> Unit,
    onShowUpgradeSheet: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    HealthKpiCard(
        score = state.engineResult.healthScore,
        delta = state.scoreDelta,
        previousMonthLabel = state.previousMonthLabel,
        expanded = wellnessExpanded,
        onExpandClick = onWellnessExpandClick,
        drivers = breakdownWellnessDrivers(state.engineResult),
        opportunityAmount = state.optimizationResult.totalPotentialTaxSaving,
        onSeeHowClick = {
            if (isPremiumEnabled) onNavigateTo(Screen.TaxPlanning) else onShowUpgradeSheet()
        },
    )
}

@Composable
private fun InsightsPremiumIntelligenceItem(
    state: InsightsState,
    uiState: PayslipUiState,
    viewModel: PayslipViewModel,
    onShowUpgradeSheet: () -> Unit,
    onShowTransparency: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
) {
    PremiumIntelligenceCard(
        isPremiumEnabled = uiState.isPremiumEnabled,
        state = state,
        onUpgradeClick = onShowUpgradeSheet,
        onNavigateTo = onNavigateTo,
        aiSectionContent = {
            GeminiAiInsightsSection(
                aiInsights = uiState.aiInsights,
                isAiLoading = uiState.isAiLoading,
                aiError = uiState.aiError,
                onGenerateClick = onShowTransparency,
                onViewInsightsClick = onViewInsightsClick,
                onClearClick = { viewModel.clearAiInsights() },
            )
        },
    )
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
