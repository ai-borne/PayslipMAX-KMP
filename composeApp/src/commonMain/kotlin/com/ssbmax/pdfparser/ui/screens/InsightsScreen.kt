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
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.DeterministicIntelligenceEngine
import com.ssbmax.pdfparser.insights.EngineResult
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
            }
        )
    }
}

data class InsightsState(
    val currentRecord: LedgerRecordEntity,
    val previousRecord: LedgerRecordEntity?,
    val historySorted: List<LedgerRecordEntity>,
    val engineResult: EngineResult,
    val scoreDelta: Int?
)

@Composable
private fun rememberInsightsState(
    selected: ParsedPayslip,
    ledgerRecords: List<LedgerRecordEntity>
): InsightsState {
    val currentRecord = remember(ledgerRecords, selected) {
        ledgerRecords.find { it.dateStr == selected.dateStr } ?: LedgerRecordEntity(
            dateStr = selected.dateStr, year = selected.year, monthNum = selected.monthNum,
            basicPay = selected.earnings.basicPay, dearnessAllowance = selected.earnings.dearnessAllowance,
            militaryServicePay = selected.earnings.militaryServicePay, transportAllowance = selected.earnings.transportAllowance,
            transportAllowanceDa = selected.earnings.transportAllowanceDa, houseRentAllowance = selected.earnings.houseRentAllowance,
            grossPay = selected.summary.grossPay, dsopSubscription = selected.deductions.dsopSubscription,
            incomeTax = selected.deductions.incomeTax, netPay = selected.summary.netRemittance
        )
    }
    val previousRecord = remember(ledgerRecords, currentRecord) {
        ledgerRecords.find {
            val isPrevYear = it.year == currentRecord.year && it.monthNum == currentRecord.monthNum - 1
            val isDecToJan = it.year == currentRecord.year - 1 && it.monthNum == 12 && currentRecord.monthNum == 1
            isPrevYear || isDecToJan
        }
    }
    val historySorted = remember(ledgerRecords) {
        ledgerRecords.sortedWith(compareBy<LedgerRecordEntity> { it.year }.thenBy { it.monthNum })
    }
    val engineResult = remember(currentRecord, previousRecord, historySorted) {
        DeterministicIntelligenceEngine.analyze(currentRecord, previousRecord, historySorted)
    }
    val previousScore = remember(previousRecord, ledgerRecords) {
        previousRecord?.let { prev ->
            val prevPrev = ledgerRecords.find {
                val isPrevYear = it.year == prev.year && it.monthNum == prev.monthNum - 1
                val isDecToJan = it.year == prev.year - 1 && it.monthNum == 12 && prev.monthNum == 1
                isPrevYear || isDecToJan
            }
            val prevHistory = ledgerRecords.filter { it.year < prev.year || (it.year == prev.year && it.monthNum <= prev.monthNum) }
            DeterministicIntelligenceEngine.analyze(prev, prevPrev, prevHistory).healthScore
        }
    }
    val scoreDelta = remember(engineResult.healthScore, previousScore) {
        previousScore?.let { engineResult.healthScore - it }
    }
    return remember(currentRecord, previousRecord, historySorted, engineResult, scoreDelta) {
        InsightsState(currentRecord, previousRecord, historySorted, engineResult, scoreDelta)
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
    ) {
        item { InsightsHeader() }
        item { SalaryHealthScoreDial(score = state.engineResult.healthScore, delta = state.scoreDelta) }
        item { ExecutiveSummaryCard(current = state.currentRecord, previous = state.previousRecord) }
        item { CriticalAlertsQueue(anomalies = state.engineResult.anomalies) }
        item { MomChangesGrid(current = state.currentRecord, previous = state.previousRecord) }
        item {
            GeminiAiInsightsSection(
                payslip = selected, isPremiumEnabled = uiState.isPremiumEnabled,
                hasInsights = uiState.aiInsights != null, isAiLoading = uiState.isAiLoading, aiError = uiState.aiError,
                onGenerateClick = onShowTransparency, onViewInsightsClick = onViewInsightsClick,
                onClearClick = { viewModel.clearAiInsights() }, onUpgradeClick = onShowUpgradeSheet,
            )
        }
        item { TrendsSparklinesSection(history = state.historySorted) }
        item {
            PremiumToolsSection(
                isPremiumEnabled = uiState.isPremiumEnabled,
                onNavigateTo = onNavigateTo,
                onUpgradeClick = onShowUpgradeSheet,
            )
        }
        item {
            AuditsArchiveGrid(
                payslips = uiState.payslips,
                selected = selected,
                onSelect = { viewModel.selectPayslip(it) }
            )
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


