package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
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
    onSharePayslip: (ParsedPayslip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val payslips = uiState.payslips
    var selectedDetailPayslip by remember { mutableStateOf<ParsedPayslip?>(null) }
    var activeActionPayslip by remember { mutableStateOf<ParsedPayslip?>(null) }
    var pendingDeletePayslip by remember { mutableStateOf<ParsedPayslip?>(null) }

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
            onLongPress = { activeActionPayslip = it },
            onSwipeDelete = { pendingDeletePayslip = it },
            modifier = modifier,
        )

        activeActionPayslip?.let { payslip ->
            HistoryActionBottomSheet(
                payslip = payslip,
                onDismissRequest = { activeActionPayslip = null },
                onViewReplica = { selectedDetailPayslip = payslip },
                onViewOriginal = {
                    viewModel.getPayslipPdf(payslip.dateStr) { bytes ->
                        if (bytes != null) {
                            onOpenPdf(bytes, payslip.file)
                        }
                    }
                },
                onShareSummary = { onSharePayslip(payslip) },
                onDelete = {
                    pendingDeletePayslip = payslip
                    activeActionPayslip = null
                },
            )
        }

        pendingDeletePayslip?.let { payslip ->
            HistoryDeleteConfirmationDialog(
                onConfirm = {
                    viewModel.deletePayslip(payslip.dateStr)
                    pendingDeletePayslip = null
                },
                onDismiss = { pendingDeletePayslip = null },
            )
        }
    }
}

@Composable
private fun HistoryListContainer(
    payslips: List<ParsedPayslip>,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
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
            HistoryLazyList(
                payslips = payslips,
                onPayslipClick = onPayslipClick,
                onLongPress = onLongPress,
                onSwipeDelete = onSwipeDelete,
            )
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
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
) {
    val grouped =
        remember(payslips) {
            payslips.groupBy { it.year }.toList().sortedByDescending { it.first }
        }
    val latestYear = remember(grouped) { grouped.firstOrNull()?.first }
    var expandedYears by remember {
        mutableStateOf(if (latestYear != null) setOf(latestYear) else emptySet())
    }

    androidx.compose.foundation.lazy.LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        grouped.forEach { (year, yearPayslips) ->
            val isExpanded = expandedYears.contains(year)
            historyYearGroup(
                year = year,
                yearPayslips = yearPayslips,
                allPayslips = payslips,
                isExpanded = isExpanded,
                onToggleExpand = {
                    expandedYears =
                        if (isExpanded) {
                            expandedYears - year
                        } else {
                            expandedYears + year
                        }
                },
                onPayslipClick = onPayslipClick,
                onLongPress = onLongPress,
                onSwipeDelete = onSwipeDelete,
            )
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.historyYearGroup(
    year: Int,
    yearPayslips: List<ParsedPayslip>,
    allPayslips: List<ParsedPayslip>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
) {
    item(key = year) {
        HistoryYearHeader(
            year = year,
            payslips = yearPayslips,
            isExpanded = isExpanded,
            onToggleExpand = onToggleExpand,
        )
    }
    if (isExpanded) {
        items(
            items = yearPayslips.sortedByDescending { it.monthNum },
            key = { it.dateStr },
        ) { payslip ->
            HistoryCard(
                payslip = payslip,
                onViewReplica = { onPayslipClick(payslip) },
                onLongPress = { onLongPress(payslip) },
                onSwipeDelete = { onSwipeDelete(payslip) },
            ) {
                HistoryCardContent(
                    payslip = payslip,
                    allPayslips = allPayslips,
                )
            }
        }
    }
}
