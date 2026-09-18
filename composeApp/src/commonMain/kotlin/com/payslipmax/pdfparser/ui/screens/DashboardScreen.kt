package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.SalaryCountdownCalculator
import com.payslipmax.pdfparser.onboarding.OnboardingManager
import com.payslipmax.pdfparser.ui.ImportUiState
import com.payslipmax.pdfparser.ui.PayslipUiState
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.components.DashboardUploadArea
import com.payslipmax.pdfparser.ui.maybePromptForRating
import com.payslipmax.pdfparser.ui.onDismissImport
import com.payslipmax.pdfparser.ui.onFilePicked
import com.payslipmax.pdfparser.ui.onImportPasswordChanged
import com.payslipmax.pdfparser.ui.onSubmitImportPassword
import com.payslipmax.pdfparser.ui.onToggleImportPasswordVisibility
import com.payslipmax.pdfparser.ui.saveDashboardScrollPosition
import com.payslipmax.pdfparser.ui.screens.importflow.ImportPayslipDialog
import com.payslipmax.pdfparser.ui.startImport
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlin.math.round

@Composable
fun DashboardScreen(
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    onboardingManager: OnboardingManager = OnboardingManager(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val payslips = uiState.payslips
    val selected = uiState.selectedPayslip

    var showUploadDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.importUiState) {
        val importState = uiState.importUiState
        if (importState is ImportUiState.Success) {
            kotlinx.coroutines.delay(600)
            viewModel.maybePromptForRating(importState.payslip)
            kotlinx.coroutines.delay(600)
            showUploadDialog = false
            viewModel.startImport()
        }
    }

    DashboardContent(
        uiState = uiState,
        payslips = payslips,
        selected = selected,
        viewModel = viewModel,
        onUploadClick = {
            viewModel.startImport()
            showUploadDialog = true
        },
        showUploadDialog = showUploadDialog,
        onboardingManager = onboardingManager,
        modifier = modifier,
    )

    if (showUploadDialog) {
        DashboardImportDialog(
            importUiState = uiState.importUiState,
            viewModel = viewModel,
            onPickPdf = onPickPdf,
            onDismiss = {
                viewModel.onDismissImport()
                showUploadDialog = false
            },
        )
    }
}

@Composable
private fun DashboardContent(
    uiState: PayslipUiState,
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip?,
    viewModel: PayslipViewModel,
    onUploadClick: () -> Unit,
    showUploadDialog: Boolean,
    onboardingManager: OnboardingManager,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
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
            EmptyDashboardPlaceholder(Modifier.testTag("dashboard_empty"))
        } else {
            PopulatedDashboard(payslips, selected, viewModel, Modifier.testTag("dashboard_populated"))
        }
        DashboardUploadArea(
            onboardingManager = onboardingManager,
            onUploadClick = onUploadClick,
            coachmarkEnabled = !showUploadDialog,
        )
    }
}

@Composable
private fun DashboardImportDialog(
    importUiState: ImportUiState,
    viewModel: PayslipViewModel,
    onPickPdf: (onResult: (ByteArray, String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
) {
    ImportPayslipDialog(
        importUiState = importUiState,
        onSelectFileClick = {
            onPickPdf { bytes, name -> viewModel.onFilePicked(bytes, name) }
        },
        onPasswordChanged = { viewModel.onImportPasswordChanged(it) },
        onTogglePasswordVisibility = { viewModel.onToggleImportPasswordVisibility() },
        onSubmitPassword = { viewModel.onSubmitImportPassword() },
        onChooseDifferentFile = {
            onPickPdf { bytes, name -> viewModel.onFilePicked(bytes, name) }
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun PopulatedDashboard(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip?,
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberDashboardScrollState(uiState, viewModel)

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
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

            val countdown = remember { SalaryCountdownCalculator.getCurrentCountdown() }
            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            SalaryCountdownRibbon(countdown = countdown)

            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            TrendChartCard(payslips = payslips)

            Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))
            AllocationChartCard(payslip = it, modifier = Modifier.testTag("allocation_chart_card"))
        }

        Spacer(modifier = Modifier.height(AppDimensions.FabClearanceHeight))
    }
}

@Composable
private fun rememberDashboardScrollState(
    uiState: PayslipUiState,
    viewModel: PayslipViewModel,
): ScrollState {
    val scrollState = rememberScrollState(uiState.dashboardScrollValue)
    DisposableEffect(Unit) {
        onDispose { viewModel.saveDashboardScrollPosition(scrollState.value) }
    }
    return scrollState
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
