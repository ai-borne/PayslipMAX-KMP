package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.launchPurchaseFlow
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.ui.restorePurchases
import com.payslipmax.pdfparser.ui.screens.PremiumUpgradeBottomSheet
import com.payslipmax.pdfparser.ui.theme.AppDimensions

@Composable
fun PcdaoAuditScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)? = null,
) {
    val auditViewModel = remember { PcdaoAuditViewModel(viewModel.repository) }
    val hasAccess = viewModel.rememberHasAccess(FeatureGate.PAYSLIPMAX_AI)
    val premiumPrice by viewModel.premiumPriceState.collectAsState()
    var showUpgradeSheet by remember { mutableStateOf(false) }

    PcdaoAuditScreen(
        viewModel = auditViewModel,
        onBack = onBack,
        modifier = modifier,
        hasAccess = hasAccess,
        onUpgradeClick = { showUpgradeSheet = true },
        onNavigateToRepresentation = onNavigateToRepresentation,
    )

    if (showUpgradeSheet) {
        PremiumUpgradeBottomSheet(
            onDismissRequest = { showUpgradeSheet = false },
            onUnlockClick = { onResult -> viewModel.launchPurchaseFlow(onResult) },
            onRestoreClick = { onResult -> viewModel.restorePurchases(onResult) },
            price = premiumPrice,
            onPresented = viewModel::refreshPremiumPrice,
        )
    }
}

@Composable
fun PcdaoAuditScreen(
    payslipRepository: PayslipRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hasAccess: Boolean = true,
    onUpgradeClick: () -> Unit = {},
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)? = null,
) {
    val viewModel = remember { PcdaoAuditViewModel(payslipRepository) }
    PcdaoAuditScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier,
        hasAccess = hasAccess,
        onUpgradeClick = onUpgradeClick,
        onNavigateToRepresentation = onNavigateToRepresentation,
    )
}

@Composable
fun PcdaoAuditScreen(
    viewModel: PcdaoAuditViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hasAccess: Boolean = true,
    onUpgradeClick: () -> Unit = {},
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    PcdaoAuditDialogs(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToRepresentation = onNavigateToRepresentation,
    )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea()
                .padding(AppDimensions.PaddingMedium),
    ) {
        AuditScreenBody(
            uiState = uiState,
            viewModel = viewModel,
            onBack = onBack,
            hasAccess = hasAccess,
            onUpgradeClick = onUpgradeClick,
            onNavigateToRepresentation = onNavigateToRepresentation,
        )
    }
}

@Composable
private fun AuditScreenBody(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    onBack: () -> Unit,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)?,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { TopNavBar(onBack = onBack) }
        item {
            MonthSelectorRow(
                availablePayslips = uiState.availablePayslips,
                selectedPayslip = uiState.selectedPayslip,
                onSelect = { viewModel.selectPayslip(it) },
            )
        }
        if (uiState.availablePayslips.isEmpty() && !uiState.isLoading) {
            item { EmptyVaultState() }
        } else {
            auditContentItems(uiState, viewModel, hasAccess, onUpgradeClick, onNavigateToRepresentation)
        }
    }
}

private fun LazyListScope.auditContentItems(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)?,
) {
    auditBannerItems(uiState, viewModel, hasAccess, onUpgradeClick)
    auditFeedAndMilestonesItems(uiState, viewModel, hasAccess, onUpgradeClick, onNavigateToRepresentation)
}

private fun LazyListScope.auditBannerItems(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
) {
    if (!hasAccess && uiState.filteredDiscrepancies.isNotEmpty()) {
        item {
            AuditTeaserBanner(
                claimsCount = uiState.filteredDiscrepancies.size,
                unclaimedTotal = uiState.unclaimedTotal,
                onUpgradeClick = onUpgradeClick,
            )
        }
    }
    item {
        ImpactCountersStrip(
            unclaimedAmount = uiState.unclaimedTotal,
            hazardAmount = uiState.hazardTotal,
            criticalAlarmsCount = uiState.alarmsCount,
        )
    }
    if (uiState.hasCumulativeArrears && uiState.cumulativeRollup != null) {
        item {
            CumulativeArrearsBanner(
                rollup = uiState.cumulativeRollup,
                isCumulativeActive = uiState.isCumulativeViewActive,
                isUnlocked = hasAccess,
                onToggleView = { viewModel.toggleCumulativeView() },
                onUpgradeClick = onUpgradeClick,
            )
        }
    }
    uiState.payFixationResult?.let { fixation ->
        item {
            PayFixationCard(
                result = fixation,
                isUnlocked = hasAccess,
                onUpgradeClick = onUpgradeClick,
            )
        }
    }
}

private fun LazyListScope.auditFeedAndMilestonesItems(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)?,
) {
    item {
        SituationalTileMatrix(
            selectedCategory = uiState.selectedCategory,
            activeContext = uiState.activeContext,
            autoInferredTileIds = uiState.autoInferredTileIds,
            onCategorySelected = { viewModel.selectCategory(it) },
            onToggleTile = { viewModel.toggleTile(it) },
            onOpenAddFactorSheet = { viewModel.setAddFactorSheetVisible(true) },
        )
    }
    item {
        FeedHeaderSection(
            selectedFilter = uiState.selectedFilter,
            onFilterSelected = { viewModel.setFilter(it) },
            onRedressalClick = {
                if (hasAccess) {
                    viewModel.generateRedressalLetter()
                } else {
                    onUpgradeClick()
                }
            },
            isUnlocked = hasAccess,
        )
    }
    auditDiscrepancyList(uiState, hasAccess, onUpgradeClick)
    if (uiState.careerMilestones.isNotEmpty()) {
        item {
            CareerMilestonesCard(
                milestones = uiState.careerMilestones,
            )
        }
    }
}

private fun LazyListScope.auditDiscrepancyList(
    uiState: PcdaoAuditUiState,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
) {
    if (uiState.filteredDiscrepancies.isEmpty()) {
        item { EmptyFindingsState() }
    } else {
        items(uiState.filteredDiscrepancies) { discrepancy ->
            AuditDiscrepancyCard(
                discrepancy = discrepancy,
                isUnlocked = hasAccess,
                onUpgradeClick = onUpgradeClick,
            )
        }
    }
}
