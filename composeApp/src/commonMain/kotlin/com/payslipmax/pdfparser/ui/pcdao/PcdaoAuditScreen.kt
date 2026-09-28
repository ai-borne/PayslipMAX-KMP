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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.theme.AppDimensions

@Composable
fun PcdaoAuditScreen(
    payslipRepository: PayslipRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)? = null,
) {
    val viewModel = remember { PcdaoAuditViewModel(payslipRepository) }
    PcdaoAuditScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier,
        onNavigateToRepresentation = onNavigateToRepresentation,
    )
}

@Composable
fun PcdaoAuditScreen(
    viewModel: PcdaoAuditViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)? = null,
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isAddFactorSheetVisible) {
        AddFactorBottomSheet(
            activeFactors = uiState.activeContext.activeSpecializedFactors,
            onToggleFactor = { viewModel.toggleSpecializedFactor(it) },
            onDismiss = { viewModel.setAddFactorSheetVisible(false) },
        )
    }

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
            onNavigateToRepresentation = onNavigateToRepresentation,
        )
    }
}

@Composable
private fun AuditScreenBody(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    onBack: () -> Unit,
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
            auditContentItems(uiState, viewModel, onNavigateToRepresentation)
        }
    }
}

private fun LazyListScope.auditContentItems(
    uiState: PcdaoAuditUiState,
    viewModel: PcdaoAuditViewModel,
    onNavigateToRepresentation: ((RedressalLetter) -> Unit)?,
) {
    item {
        ImpactCountersStrip(
            unclaimedAmount = uiState.unclaimedTotal,
            hazardAmount = uiState.hazardTotal,
            criticalAlarmsCount = uiState.alarmsCount,
        )
    }
    uiState.payFixationResult?.let { fixation ->
        item { PayFixationCard(result = fixation) }
    }
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
                val letter = viewModel.generateRedressalLetter()
                if (letter != null) onNavigateToRepresentation?.invoke(letter)
            },
        )
    }
    if (uiState.filteredDiscrepancies.isEmpty()) {
        item { EmptyFindingsState() }
    } else {
        items(uiState.filteredDiscrepancies) { discrepancy ->
            AuditDiscrepancyCard(discrepancy = discrepancy)
        }
    }
}
