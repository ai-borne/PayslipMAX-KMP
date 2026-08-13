package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.hasAccess
import com.payslipmax.pdfparser.ui.launchPurchaseFlow
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.ui.saveInsightsScrollPosition
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

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

    InsightsContent(
        viewModel = viewModel,
        uiState = uiState,
        selected = selected,
        onShowUpgradeSheet = { showUpgradeSheet = true },
        onNavigateTo = onNavigateTo,
        modifier = modifier,
    )

    InsightsOverlayDialogs(
        showUpgradeSheet = showUpgradeSheet,
        viewModel = viewModel,
        onDismissUpgrade = { showUpgradeSheet = false },
    )
}

@Composable
private fun InsightsOverlayDialogs(
    showUpgradeSheet: Boolean,
    viewModel: PayslipViewModel,
    onDismissUpgrade: () -> Unit,
) {
    if (showUpgradeSheet) {
        val premiumPrice by viewModel.premiumPriceState.collectAsState()
        PremiumUpgradeBottomSheet(
            onDismissRequest = onDismissUpgrade,
            onUnlockClick = { onResult -> viewModel.launchPurchaseFlow(onResult) },
            price = premiumPrice,
        )
    }
}

@Composable
private fun InsightsContent(
    viewModel: PayslipViewModel,
    uiState: PayslipUiState,
    selected: ParsedPayslip,
    onShowUpgradeSheet: () -> Unit,
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
        )
        InsightsLazyBody(
            state = state,
            uiState = uiState,
            viewModel = viewModel,
            wellnessExpanded = wellnessExpanded,
            onWellnessExpandClick = { wellnessExpanded = !wellnessExpanded },
            onShowUpgradeSheet = onShowUpgradeSheet,
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
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
) {
    val access = rememberInsightsFeatureAccess(viewModel)
    val premiumPrice by viewModel.premiumPriceState.collectAsState()
    val smartInsights = remember(state) { buildSmartInsights(state) }
    var toolsExpanded by remember(access.hasPremiumIntelligence) { mutableStateOf(access.hasPremiumIntelligence) }
    val listState = rememberInsightsListState(uiState, viewModel)
    LazyColumn(
        state = listState,
        modifier = modifier.background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        insightsPrimaryItems(
            state = state,
            smartInsights = smartInsights,
            wellnessExpanded = wellnessExpanded,
            onWellnessExpandClick = onWellnessExpandClick,
            hasWealthOptimization = access.hasWealthOptimization,
            hasAccess = { gate -> viewModel.hasAccess(gate) },
            onShowUpgradeSheet = onShowUpgradeSheet,
            onNavigateTo = onNavigateTo,
        )
        insightsPremiumItems(
            state = state,
            smartInsights = smartInsights,
            isPremium = access.hasPremiumIntelligence,
            hasAnomalyDetection = access.hasAnomalyDetection,
            toolsExpanded = toolsExpanded,
            onToolsExpandClick = { toolsExpanded = !toolsExpanded },
            onShowUpgradeSheet = onShowUpgradeSheet,
            onNavigateTo = onNavigateTo,
            price = premiumPrice,
        )
    }
}

private data class InsightsFeatureAccess(
    val hasPremiumIntelligence: Boolean,
    val hasWealthOptimization: Boolean,
    val hasAnomalyDetection: Boolean,
)

@Composable
private fun rememberInsightsFeatureAccess(viewModel: PayslipViewModel): InsightsFeatureAccess =
    InsightsFeatureAccess(
        hasPremiumIntelligence = viewModel.rememberHasAccess(FeatureGate.PREMIUM_INTELLIGENCE),
        hasWealthOptimization = viewModel.rememberHasAccess(FeatureGate.WEALTH_OPTIMIZATION),
        hasAnomalyDetection = viewModel.rememberHasAccess(FeatureGate.ANOMALY_DETECTION),
    )

@Composable
private fun rememberInsightsListState(
    uiState: PayslipUiState,
    viewModel: PayslipViewModel,
): LazyListState {
    val listState = rememberLazyListState(uiState.insightsScrollIndex, uiState.insightsScrollOffset)
    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveInsightsScrollPosition(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
    }
    return listState
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
