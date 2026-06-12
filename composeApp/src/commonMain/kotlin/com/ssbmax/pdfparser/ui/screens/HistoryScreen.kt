package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.PayslipViewModel
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

@Composable
fun HistoryScreen(
    viewModel: PayslipViewModel,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val payslips = uiState.payslips
    var selectedDetailPayslip by remember { mutableStateOf<ParsedPayslip?>(null) }

    if (selectedDetailPayslip != null) {
        PayslipReplicaScreen(
            payslip = selectedDetailPayslip!!,
            onBackClick = { selectedDetailPayslip = null },
            onViewPdfClick = { dateStr ->
                viewModel.getPayslipPdf(dateStr) { bytes ->
                    if (bytes != null) {
                        onOpenPdf(bytes, selectedDetailPayslip!!.file)
                    }
                }
            },
            modifier = modifier,
        )
    } else {
        HistoryListContainer(
            payslips = payslips,
            onPayslipClick = { selectedDetailPayslip = it },
            modifier = modifier,
        )
    }
}

@Composable
private fun HistoryListContainer(
    payslips: List<ParsedPayslip>,
    onPayslipClick: (ParsedPayslip) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(AppDimensions.PaddingMedium),
    ) {
        HistoryHeader()
        Spacer(modifier = Modifier.height(16.dp))
        if (payslips.isEmpty()) {
            EmptyHistoryView()
        } else {
            HistoryLazyList(payslips = payslips, onPayslipClick = onPayslipClick)
        }
    }
}

@Composable
private fun HistoryHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = AppStrings.navigationHistory,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Browse and read your historical military payslip statements",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyHistoryView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "No payslips in history. Go to Dashboard to import one.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HistoryLazyList(
    payslips: List<ParsedPayslip>,
    onPayslipClick: (ParsedPayslip) -> Unit,
) {
    val grouped =
        remember(payslips) {
            payslips.groupBy { it.year }.toList().sortedByDescending { it.first }
        }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        grouped.forEach { (year, yearPayslips) ->
            item {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            items(
                items = yearPayslips.sortedByDescending { it.monthNum },
                key = { it.dateStr },
            ) { payslip ->
                HistoryCard(payslip = payslip, onClick = { onPayslipClick(payslip) })
            }
        }
    }
}

@Composable
private fun HistoryCard(
    payslip: ParsedPayslip,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
    ) {
        Row(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = payslip.monthName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Gross: ₹${formatAmount(payslip.summary.grossPay)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹${formatAmount(payslip.summary.netRemittance)}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = "Net Take-Home",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
