package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.payslipmax.pcdao.redressal.RedressalLetter
import com.payslipmax.pdfparser.onboarding.OnboardingManager
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
    onboardingManager: OnboardingManager = remember { OnboardingManager() },
) {
    val selectedPayslip = viewModel.uiState.collectAsState().value.selectedPayslip
    val auditViewModel = remember { PcdaoAuditViewModel(viewModel.repository, initialSelectedPayslip = selectedPayslip) }
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
        onboardingManager = onboardingManager,
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
    onboardingManager: OnboardingManager = remember { OnboardingManager() },
) {
    val viewModel = remember { PcdaoAuditViewModel(payslipRepository) }
    PcdaoAuditScreen(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier,
        hasAccess = hasAccess,
        onUpgradeClick = onUpgradeClick,
        onNavigateToRepresentation = onNavigateToRepresentation,
        onboardingManager = onboardingManager,
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
    onboardingManager: OnboardingManager = remember { OnboardingManager() },
) {
    val uiState by viewModel.uiState.collectAsState()
    var showOnboardingSheet by rememberSaveable {
        mutableStateOf(onboardingManager.shouldShowPcdaoAuditIntro())
    }
    var showHazardExplainer by rememberSaveable { mutableStateOf(false) }

    PcdaoAuditDialogs(
        uiState = uiState,
        viewModel = viewModel,
        onNavigateToRepresentation = onNavigateToRepresentation,
        showHazardExplainer = showHazardExplainer,
        onDismissHazardExplainer = { showHazardExplainer = false },
    )

    if (showOnboardingSheet) {
        PcdaoOnboardingSheet(
            onDismiss = {
                showOnboardingSheet = false
                onboardingManager.onPcdaoAuditIntroDismissed()
            },
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
            hasAccess = hasAccess,
            onUpgradeClick = onUpgradeClick,
            onNavigateToRepresentation = onNavigateToRepresentation,
            onGuideClick = { showOnboardingSheet = true },
            onHazardClick = { showHazardExplainer = true },
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
    onGuideClick: () -> Unit,
    onHazardClick: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { TopNavBar(onBack = onBack, onGuideClick = onGuideClick) }
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
            auditContentItems(uiState, viewModel, hasAccess, onUpgradeClick, onNavigateToRepresentation, onHazardClick)
        }
    }
}
