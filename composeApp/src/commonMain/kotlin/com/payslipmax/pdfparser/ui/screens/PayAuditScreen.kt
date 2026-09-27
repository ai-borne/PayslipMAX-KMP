package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.ScreenBackHeader
import com.payslipmax.pdfparser.ui.components.detailScreenSafeArea
import com.payslipmax.pdfparser.ui.launchPurchaseFlow
import com.payslipmax.pdfparser.ui.rememberHasAccess
import com.payslipmax.pdfparser.ui.restorePurchases
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings

/**
 * Pay Audit screen (docs/Plan/09_PayAudit_PhasePlan.md Phase 4): the service timeline, this month's
 * explained pay-line changes, and findings, gated by [FeatureGate.ANOMALY_DETECTION] at the findings
 * level only — the timeline and finding count stay free.
 */
@Composable
fun PayAuditScreen(
    viewModel: PayslipViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val selected = uiState.selectedPayslip
    var showUpgradeSheet by remember { mutableStateOf(false) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .detailScreenSafeArea(),
    ) {
        ScreenBackHeader(
            title = PayAuditStrings.screenTitle,
            subtitle = PayAuditStrings.screenSubtitle,
            onBack = onBack,
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
        )
        if (selected == null) {
            PayAuditEmptyState()
        } else {
            val hasAnomalyDetection = viewModel.rememberHasAccess(FeatureGate.ANOMALY_DETECTION)
            PayAuditBody(
                selected = selected,
                payslips = uiState.payslips,
                hasAnomalyDetection = hasAnomalyDetection,
                onShowUpgradeSheet = { showUpgradeSheet = true },
            )
        }
    }

    if (showUpgradeSheet) {
        val premiumPrice by viewModel.premiumPriceState.collectAsState()
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
private fun PayAuditBody(
    selected: ParsedPayslip,
    payslips: List<ParsedPayslip>,
    hasAnomalyDetection: Boolean,
    onShowUpgradeSheet: () -> Unit,
) {
    val engineResult = rememberPayAuditEngineResult(selected, payslips)
    val findings =
        remember(engineResult, hasAnomalyDetection) {
            partitionPayAuditFindings(engineResult.anomalies, hasAnomalyDetection)
        }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(AppDimensions.PaddingMedium),
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
    ) {
        payAuditFindingsItems(display = findings, onUnlockClick = onShowUpgradeSheet)
        payAuditChangesItems(changes = engineResult.changeExplanations)
        payAuditTimelineItems(timeline = engineResult.timeline)
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
