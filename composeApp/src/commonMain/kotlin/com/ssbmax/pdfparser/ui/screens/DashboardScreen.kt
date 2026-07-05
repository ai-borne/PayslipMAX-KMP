package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipUiState
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.clearError
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings
import kotlin.math.round

@Composable
fun DashboardScreen(
    viewModel: PayslipViewModel,
    onPickPdfTrigger: (password: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val payslips = uiState.payslips
    val selected = uiState.selectedPayslip

    var showUploadDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading && payslips.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .testTag("dashboard_loading"),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (payslips.isEmpty()) {
            EmptyDashboardPlaceholder(modifier.testTag("dashboard_empty"))
        } else {
            PopulatedDashboard(payslips, selected, viewModel, modifier.testTag("dashboard_populated"))
        }
        UploadFab(onClick = { showUploadDialog = true }, modifier = Modifier.align(Alignment.BottomEnd))
    }

    if (showUploadDialog) {
        UploadDialog(uiState, onPickPdfTrigger, viewModel, onDismiss = { showUploadDialog = false })
    }
}

@Composable
private fun EmptyDashboardPlaceholder(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(AppDimensions.PaddingLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "📄",
            fontSize = AppDimensions.FontSizeEmoji,
            modifier = Modifier.padding(bottom = AppDimensions.SpacingLarge),
        )
        Text(
            text = AppStrings.dashboardEmptyStateTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingSmall))
        Text(
            text = AppStrings.dashboardEmptyStateDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = AppDimensions.PaddingLarge),
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingHuge))
        Text(
            text = AppStrings.dashboardEmptyStateLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun UploadFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.padding(AppDimensions.PaddingMedium),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(Icons.Default.Add, contentDescription = "Import Payslip")
    }
}

@Composable
private fun UploadDialog(
    uiState: PayslipUiState,
    onPickPdfTrigger: (password: String) -> Unit,
    viewModel: PayslipViewModel,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        UploadWidget(
            isLoading = uiState.isLoading,
            error = uiState.importError,
            success = uiState.importSuccess,
            onPickPdfTrigger = onPickPdfTrigger,
            onClearError = { viewModel.clearError() },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun PopulatedDashboard(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip?,
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.PaddingMedium),
    ) {
        selected?.let { payslip ->
            OfficerInfoBar(
                payslip = payslip,
                profileName = uiState.profileName,
                profileCda = uiState.profileCdaNumber,
                profilePan = uiState.profilePanNumber,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingMedium))
        }

        YearMonthPickerRow(
            viewModel = viewModel,
            selected = selected,
        )

        selected?.let {
            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            StatsGridSection(payslip = it)

            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            TrendChartCard(payslips = payslips)

            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            AllocationChartCard(payslip = it)
        }
    }
}

@Composable
private fun StatsGridSection(payslip: ParsedPayslip) {
    val net = payslip.summary.netRemittance
    val basic = payslip.earnings.basicPay
    val dsop = payslip.taxAndSavings?.dsopFund?.closingBalance ?: 0.0
    val tax = payslip.deductions.incomeTax + payslip.deductions.educationCess
    val taxRateRaw = if (payslip.summary.grossPay > 0) (tax / payslip.summary.grossPay) * 100 else 0.0
    val taxRate = round(taxRateRaw * 10) / 10.0

    Column(verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
            StatCard(
                title = AppStrings.cardNetTitle,
                value = "₹${formatAmount(net)}",
                subtitle = AppStrings.cardNetUnit,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = AppStrings.cardBpTitle,
                value = "₹${formatAmount(basic)}",
                subtitle = AppStrings.cardBpDesc,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium)) {
            StatCard(
                title = AppStrings.cardDsopTitle,
                value = "₹${formatAmount(dsop)}",
                subtitle = AppStrings.cardDsopDesc,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = AppStrings.cardTaxTitle,
                value = "$taxRate%",
                subtitle = AppStrings.cardTaxDesc,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppDimensions.SpacingTwo))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
