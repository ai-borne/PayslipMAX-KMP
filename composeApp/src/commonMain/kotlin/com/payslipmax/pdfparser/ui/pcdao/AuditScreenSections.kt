package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import com.payslipmax.pcdao.redressal.RedressalLetter

/**
 * LazyListScope section extensions for the PCDA(O) Audit Cockpit Screen.
 * Extracted per Rule 6 (300 lines principle) to keep PcdaoAuditScreen.kt concise and scalable.
 */
internal fun LazyListScope.auditContentItems(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)?,
    onHazardClick: () -> Unit,
) {
    auditBannerItems(uiState, viewModel, hasAccess, onUpgradeClick, onHazardClick)
    auditFeedAndMilestonesItems(uiState, viewModel, hasAccess, onUpgradeClick, onNavigateToRepresentation)
}

internal fun LazyListScope.auditBannerItems(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
    onHazardClick: () -> Unit,
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
            onHazardCardClick = onHazardClick,
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
                onLevelSelected = { from, to -> viewModel.setSandboxLevels(from, to) },
            )
        }
    }
}

internal fun LazyListScope.auditFeedAndMilestonesItems(
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
            activePresetId = uiState.activePresetId,
            onPresetSelected = { viewModel.applyMissionPreset(it) },
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
    auditDiscrepancyList(
        uiState = uiState,
        hasAccess = hasAccess,
        onUpgradeClick = onUpgradeClick,
        onDismissDiscrepancy = { viewModel.dismissDiscrepancy(it) },
    )
    if (uiState.careerMilestones.isNotEmpty()) {
        item {
            CareerMilestonesCard(
                milestones = uiState.careerMilestones,
            )
        }
    }
}

internal fun LazyListScope.auditDiscrepancyList(
    uiState: PcdaoAuditUiState,
    hasAccess: Boolean,
    onUpgradeClick: () -> Unit,
    onDismissDiscrepancy: (String) -> Unit,
) {
    if (uiState.filteredDiscrepancies.isEmpty()) {
        item { EmptyFindingsState() }
    } else {
        items(uiState.filteredDiscrepancies) { discrepancy ->
            AuditDiscrepancyCard(
                discrepancy = discrepancy,
                isUnlocked = hasAccess,
                onUpgradeClick = onUpgradeClick,
                onDismiss = { onDismissDiscrepancy(discrepancy.id) },
            )
        }
    }
}
