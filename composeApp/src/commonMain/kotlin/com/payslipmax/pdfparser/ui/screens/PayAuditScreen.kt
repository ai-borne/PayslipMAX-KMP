package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.launchPurchaseFlow
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.ui.restorePurchases
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings
import org.koin.compose.koinInject

/**
 * Pay Audit screen (docs/Plan Phase 2 redesign): a month picker and a one-glance verdict on top, then three
 * tabs. All derived state comes from [PayAuditViewModel]; this composable only renders it. Premium gating
 * ([FeatureGate.ANOMALY_DETECTION]) stays at the findings-evidence level only.
 */
@Composable
fun PayAuditScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    onNavigateTo: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    payAuditViewModel: PayAuditViewModel = koinInject(),
    onboardingManager: OnboardingManager = koinInject(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    val hasAnomalyDetection = viewModel.rememberHasAccess(FeatureGate.ANOMALY_DETECTION)
    var showUpgradeSheet by remember { mutableStateOf(false) }
    val requested = selected?.let { PayMonth(it.year, it.monthNum) }
    // Survives the system recreating the app; a fresh entry (new composition) starts empty and follows `requested`.
    var saved by rememberSaveable(stateSaver = PayAuditSavedState.Saver) { mutableStateOf(PayAuditSavedState()) }
    // Only the first open follows the app-wide selection (or the restored month); after that the on-screen picker owns it.
    LaunchedEffect(uiState.payslips, hasAnomalyDetection) {
        val firstOpen = payAuditViewModel.uiState.value.selectedMonth == null
        payAuditViewModel.setInputs(uiState.payslips, hasAnomalyDetection, requestedMonth = if (firstOpen) saved.month ?: requested else null)
        if (firstOpen && uiState.payslips.isNotEmpty()) payAuditViewModel.selectTab(saved.tab)
    }
    val audit by payAuditViewModel.uiState.collectAsState()
    LaunchedEffect(audit.selectedMonth, audit.tab) {
        if (audit.selectedMonth != null) saved = PayAuditSavedState(audit.selectedMonth, audit.tab)
    }
    DisposableEffect(payAuditViewModel) { onDispose { payAuditViewModel.dispose() } }
    if (uiState.payslips.isNotEmpty()) PayAuditIntroGate(onboardingManager)

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).detailScreenSafeArea()) {
        ScreenBackHeader(
            title = PayAuditStrings.screenTitle,
            subtitle = PayAuditStrings.screenSubtitle,
            onBack = onBack,
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
        )
        if (uiState.payslips.isEmpty()) {
            PayAuditEmptyState()
        } else {
            PayAuditBody(payAuditViewModel, onShowUpgradeSheet = { showUpgradeSheet = true }, onDraftLetter = { onNavigateTo(Screen.Representation) })
        }
    }
    if (showUpgradeSheet) PayAuditUpgradeSheet(viewModel, onDismiss = { showUpgradeSheet = false })
}

@Composable
private fun PayAuditUpgradeSheet(
    viewModel: PayslipViewModel,
    onDismiss: () -> Unit,
) {
    val premiumPrice by viewModel.premiumPriceState.collectAsState()
    PremiumUpgradeBottomSheet(
        onDismissRequest = onDismiss,
        onUnlockClick = { onResult -> viewModel.launchPurchaseFlow(onResult) },
        onRestoreClick = { onResult -> viewModel.restorePurchases(onResult) },
        price = premiumPrice,
        onPresented = viewModel::refreshPremiumPrice,
    )
}

@Composable
private fun PayAuditBody(
    vm: PayAuditViewModel,
    onShowUpgradeSheet: () -> Unit,
    onDraftLetter: () -> Unit,
) {
    val state by vm.uiState.collectAsState()
    PayAuditContent(state, vm::selectMonth, vm::selectTab, onShowUpgradeSheet, onDraftLetter)
}

/** Stateless render of [PayAuditUiState]: the month bar, the verdict card, the tab row and the selected tab. */
@Composable
internal fun PayAuditContent(
    state: PayAuditUiState,
    onSelectMonth: (PayMonth) -> Unit,
    onSelectTab: (PayAuditTab) -> Unit,
    onShowUpgradeSheet: () -> Unit,
    onDraftLetter: () -> Unit,
) {
    var sheet by remember { mutableStateOf<PayAuditSheet?>(null) }
    val month = state.selectedMonth
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        item(key = "month_bar", contentType = "month_bar") {
            PayAuditMonthBar(month, state.availableMonths, onSelectMonth, { sheet = PayAuditSheet.MONTH }, { sheet = PayAuditSheet.GLOSSARY })
        }
        if (month != null) {
            item(key = "verdict", contentType = "verdict") {
                PayAuditVerdictCard(
                    copy = payAuditVerdictCopy(state.verdict, month),
                    historyLine = if (state.history.monthsAudited > 0) payAuditHistoryLine(state.history) else null,
                    onAction = { verdictAction(state.verdict, onShowUpgradeSheet, onDraftLetter) { onSelectTab(PayAuditTab.THIS_MONTH) } },
                )
            }
        }
        item(key = "tabs", contentType = "tabs") { PayAuditTabs(state.tab, onSelectTab) }
        payAuditTabContent(state, onShowUpgradeSheet, onDraftLetter)
    }
    when (sheet) {
        PayAuditSheet.MONTH ->
            PayAuditMonthPickerSheet(month, state.availableMonths, onSelect = {
                onSelectMonth(it)
                sheet = null
            }, onDismiss = { sheet = null })
        PayAuditSheet.GLOSSARY -> PayAuditGlossarySheet(onDismiss = { sheet = null }, onShowOrientation = { sheet = PayAuditSheet.ORIENTATION })
        PayAuditSheet.ORIENTATION -> PayAuditOrientationSheet(onDismiss = { sheet = null })
        null -> Unit
    }
}

private fun verdictAction(
    verdict: PayAuditVerdict,
    onUnlock: () -> Unit,
    onDraftLetter: () -> Unit,
    onShowEvidence: () -> Unit,
) = when {
    verdict is PayAuditVerdict.LockedIssue -> onUnlock()
    verdict is PayAuditVerdict.Issue && verdict.canDraftLetter -> onDraftLetter()
    else -> onShowEvidence()
}

private enum class PayAuditSheet { MONTH, GLOSSARY, ORIENTATION }

private fun LazyListScope.payAuditTabContent(
    state: PayAuditUiState,
    onShowUpgradeSheet: () -> Unit,
    onDraftLetter: () -> Unit,
) {
    when (state.tab) {
        PayAuditTab.THIS_MONTH -> {
            payAuditFindingsItems(state.findings, state.hiddenFindingCount, onShowUpgradeSheet, onDraftLetter)
            payAuditChangesItems(state.changes)
        }
        PayAuditTab.HISTORY -> payAuditHistoryItems(state.timeline, state.allChanges, state.selectedMonth)
        PayAuditTab.PLAN_AHEAD -> {
            payAuditPredictionsItems(state.incrementPrediction, state.dsopRoom)
            payAuditFixationCalculatorItems(state.timeline)
        }
    }
}

@Composable
private fun PayAuditTabs(
    selected: PayAuditTab,
    onSelect: (PayAuditTab) -> Unit,
) {
    val labels =
        mapOf(
            PayAuditTab.THIS_MONTH to PayAuditVerdictStrings.tabThisMonth,
            PayAuditTab.HISTORY to PayAuditVerdictStrings.tabHistory,
            PayAuditTab.PLAN_AHEAD to PayAuditVerdictStrings.tabPlanAhead,
        )
    PrimaryTabRow(selectedTabIndex = selected.ordinal) {
        PayAuditTab.entries.forEach { tab ->
            Tab(selected = tab == selected, onClick = { onSelect(tab) }, text = { Text(labels.getValue(tab)) })
        }
    }
}

@Composable
private fun PayAuditEmptyState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = PayAuditStrings.emptyState,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
