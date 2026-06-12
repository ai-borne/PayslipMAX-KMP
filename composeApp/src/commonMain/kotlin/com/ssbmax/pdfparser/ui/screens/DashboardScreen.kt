package com.ssbmax.pdfparser.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipUiState
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

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
        if (payslips.isEmpty()) {
            EmptyDashboardPlaceholder(modifier)
        } else {
            PopulatedDashboard(payslips, selected, viewModel, modifier)
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
            fontSize = 64.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = "No Payslips Imported",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Import your monthly payslips to unlock digital replicas, historical tracking, financial insights, and tax audits.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Tap the + button below to get started",
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
        modifier = modifier.padding(16.dp),
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
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        text = {
            UploadWidget(
                isLoading = uiState.isLoading,
                error = uiState.error,
                success = uiState.importSuccess,
                onPickPdfTrigger = onPickPdfTrigger,
                onClearError = { viewModel.clearError() },
            )
        },
    )
}

@Composable
private fun PopulatedDashboard(
    payslips: List<ParsedPayslip>,
    selected: ParsedPayslip?,
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(AppDimensions.PaddingMedium),
    ) {
        selected?.let { payslip ->
            OfficerInfoBar(payslip = payslip)
            Spacer(modifier = Modifier.height(12.dp))
        }

        YearMonthPickerRow(
            viewModel = viewModel,
            selected = selected,
        )

        selected?.let {
            Spacer(modifier = Modifier.height(16.dp))
            StatsGridSection(payslip = it)

            Spacer(modifier = Modifier.height(16.dp))
            TrendChartCard(payslips = payslips)

            Spacer(modifier = Modifier.height(16.dp))
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
    val taxRate = if (payslip.summary.grossPay > 0) (tax / payslip.summary.grossPay) * 100 else 0.0

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard(
                title = AppStrings.cardDsopTitle,
                value = "₹${formatAmount(dsop)}",
                subtitle = AppStrings.cardDsopDesc,
                modifier = Modifier.weight(1f),
            )
            StatCard(
                title = AppStrings.cardTaxTitle,
                value = "${taxRate.toString().take(4)}%",
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
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingMedium)) {
            Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
