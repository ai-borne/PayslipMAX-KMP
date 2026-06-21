package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
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
import com.ssbmax.pdfparser.domain.ParsedPayslip
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
        com.ssbmax.pdfparser.ui.components.TransparencyDialog(
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
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ledgerRecords by viewModel.ledgerRecords.collectAsState()
    val state = rememberInsightsState(selected, ledgerRecords)
    var showWellnessDrivers by remember { mutableStateOf(false) }
    val drivers = remember(state.engineResult) { breakdownWellnessDrivers(state.engineResult) }
    Column(modifier = modifier.fillMaxSize()) {
        InsightsTopBar(
            payslips = uiState.payslips,
            selected = selected,
            score = state.engineResult.healthScore,
            delta = state.scoreDelta,
            expanded = showWellnessDrivers,
            onExpandClick = { showWellnessDrivers = !showWellnessDrivers },
            onSelectPayslip = { viewModel.selectPayslip(it) },
        )
        InsightsLazyBody(
            state = state, uiState = uiState,
            showWellnessDrivers = showWellnessDrivers, drivers = drivers,
            viewModel = viewModel, onShowUpgradeSheet = onShowUpgradeSheet,
            onShowTransparency = onShowTransparency, onViewInsightsClick = onViewInsightsClick,
            onNavigateTo = onNavigateTo,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun InsightsLazyBody(
    state: InsightsState,
    uiState: PayslipUiState,
    showWellnessDrivers: Boolean,
    drivers: List<WellnessDriver>,
    viewModel: PayslipViewModel,
    onShowUpgradeSheet: () -> Unit,
    onShowTransparency: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        item { InsightsHeroItem(state, uiState, onNavigateTo, onShowUpgradeSheet) }
        if (state.engineResult.anomalies.size > 1) {
            item { CriticalAlertsQueue(anomalies = state.engineResult.anomalies) }
        }
        if (uiState.isPremiumEnabled) {
            premiumContentItems(uiState, showWellnessDrivers, drivers, state, onShowTransparency, onViewInsightsClick) { viewModel.clearAiInsights() }
        }
        item { ExecutiveSummaryCard(current = state.currentRecord, previous = state.previousRecord) }
        if (state.momChanges.isNotEmpty()) {
            item { MomChangesGrid(current = state.currentRecord, previous = state.previousRecord) }
        }
        item { TrendsSparklinesSection(history = state.historySorted) }
        bottomTierItem(uiState.isPremiumEnabled, onNavigateTo, onShowUpgradeSheet)
    }
}

private fun LazyListScope.premiumContentItems(
    uiState: PayslipUiState,
    showWellnessDrivers: Boolean,
    drivers: List<WellnessDriver>,
    state: InsightsState,
    onShowTransparency: () -> Unit,
    onViewInsightsClick: () -> Unit,
    onClearAiInsights: () -> Unit,
) {
    item {
        GeminiAiInsightsSection(
            aiInsights = uiState.aiInsights,
            isAiLoading = uiState.isAiLoading,
            aiError = uiState.aiError,
            onGenerateClick = onShowTransparency,
            onViewInsightsClick = onViewInsightsClick,
            onClearClick = onClearAiInsights,
        )
    }
    if (showWellnessDrivers) {
        item { WellnessDriversSection(score = state.engineResult.healthScore, drivers = drivers) }
    }
}

private fun LazyListScope.bottomTierItem(
    isPremiumEnabled: Boolean,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    if (isPremiumEnabled) {
        item { PremiumToolsSection(onNavigateTo = onNavigateTo) }
    } else {
        item { ProFeaturesTeaser(onUpgradeClick = onUpgradeClick) }
    }
}

@Composable
private fun InsightsHeroItem(
    state: InsightsState,
    uiState: PayslipUiState,
    onNavigateTo: (com.ssbmax.pdfparser.Screen) -> Unit,
    onUpgradeClick: () -> Unit,
) {
    AdaptiveHeroCard(
        engineResult = state.engineResult,
        optimizationResult = state.optimizationResult,
        isPremiumEnabled = uiState.isPremiumEnabled,
        onNavigateTo = onNavigateTo,
        onUpgradeClick = onUpgradeClick,
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
