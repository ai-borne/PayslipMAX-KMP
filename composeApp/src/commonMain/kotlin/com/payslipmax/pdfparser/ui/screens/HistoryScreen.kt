package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.ui.*

@Composable
fun HistoryScreen(
    viewModel: PayslipViewModel,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    onNavigateToInsights: () -> Unit,
    onSharePayslip: (ParsedPayslip) -> Unit = {},
    onOpenPayslipDetail: (ParsedPayslip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val ledgerRecords by viewModel.ledgerRecords.collectAsState()
    var activeActionPayslip by remember { mutableStateOf<ParsedPayslip?>(null) }
    var pendingDeletePayslip by remember { mutableStateOf<ParsedPayslip?>(null) }

    HistoryContent(
        uiState = uiState,
        ledgerRecords = ledgerRecords,
        activeActionPayslip = activeActionPayslip,
        pendingDeletePayslip = pendingDeletePayslip,
        onSelectDetail = onOpenPayslipDetail,
        onOpenOriginal = { payslip ->
            viewModel.getPayslipPdf(payslip.dateStr) { bytes ->
                if (bytes != null) onOpenPdf(bytes, payslip.file)
            }
        },
        onLongPress = { activeActionPayslip = it },
        onSwipeDelete = { pendingDeletePayslip = it },
        onSharePayslip = onSharePayslip,
        onDismissAction = { activeActionPayslip = null },
        onDeleteRequest = {
            pendingDeletePayslip = activeActionPayslip
            activeActionPayslip = null
        },
        onConfirmDelete = { payslip ->
            viewModel.deletePayslip(payslip.dateStr)
            pendingDeletePayslip = null
        },
        onDismissDelete = { pendingDeletePayslip = null },
        viewModel = viewModel,
        onNavigateToInsights = onNavigateToInsights,
        modifier = modifier,
    )
}

@Composable
private fun HistoryMainView(
    uiState: PayslipUiState,
    ledgerRecords: List<LedgerRecordEntity>,
    activeActionPayslip: ParsedPayslip?,
    pendingDeletePayslip: ParsedPayslip?,
    onSelectDetail: (ParsedPayslip) -> Unit,
    onOpenOriginal: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onSharePayslip: (ParsedPayslip) -> Unit,
    onDismissAction: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: (ParsedPayslip) -> Unit,
    onDismissDelete: () -> Unit,
    onNavigateToInsights: () -> Unit,
    expandedYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    initialScrollIndex: Int,
    initialScrollOffset: Int,
    onScrollPositionChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    HistoryListContainer(
        payslips = uiState.payslips,
        ledgerRecords = ledgerRecords,
        isLoading = uiState.isLoading,
        onPayslipClick = onSelectDetail,
        onLongPress = onLongPress,
        onSwipeDelete = onSwipeDelete,
        onNavigateToInsights = onNavigateToInsights,
        expandedYears = expandedYears,
        onToggleYear = onToggleYear,
        initialScrollIndex = initialScrollIndex,
        initialScrollOffset = initialScrollOffset,
        onScrollPositionChanged = onScrollPositionChanged,
        modifier = modifier,
    )
    HistoryOverlays(
        activeActionPayslip = activeActionPayslip,
        pendingDeletePayslip = pendingDeletePayslip,
        onDismissAction = onDismissAction,
        onViewReplica = { activeActionPayslip?.let(onSelectDetail) },
        onViewOriginal = { activeActionPayslip?.let(onOpenOriginal) },
        onShareSummary = { activeActionPayslip?.let(onSharePayslip) },
        onDeleteRequest = onDeleteRequest,
        onConfirmDelete = { pendingDeletePayslip?.let(onConfirmDelete) },
        onDismissDelete = onDismissDelete,
    )
}

@Composable
private fun HistoryContent(
    uiState: PayslipUiState,
    ledgerRecords: List<LedgerRecordEntity>,
    activeActionPayslip: ParsedPayslip?,
    pendingDeletePayslip: ParsedPayslip?,
    onSelectDetail: (ParsedPayslip) -> Unit,
    onOpenOriginal: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onSharePayslip: (ParsedPayslip) -> Unit,
    onDismissAction: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: (ParsedPayslip) -> Unit,
    onDismissDelete: () -> Unit,
    viewModel: PayslipViewModel,
    onNavigateToInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HistoryMainView(
        uiState = uiState,
        ledgerRecords = ledgerRecords,
        activeActionPayslip = activeActionPayslip,
        pendingDeletePayslip = pendingDeletePayslip,
        onSelectDetail = onSelectDetail,
        onOpenOriginal = onOpenOriginal,
        onLongPress = onLongPress,
        onSwipeDelete = onSwipeDelete,
        onSharePayslip = onSharePayslip,
        onDismissAction = onDismissAction,
        onDeleteRequest = onDeleteRequest,
        onConfirmDelete = onConfirmDelete,
        onDismissDelete = onDismissDelete,
        onNavigateToInsights = onNavigateToInsights,
        expandedYears = uiState.expandedHistoryYears,
        onToggleYear = viewModel::toggleHistoryYearExpanded,
        initialScrollIndex = uiState.historyScrollIndex,
        initialScrollOffset = uiState.historyScrollOffset,
        onScrollPositionChanged = viewModel::saveHistoryScrollPosition,
        modifier = modifier,
    )
}

@Composable
private fun HistoryOverlays(
    activeActionPayslip: ParsedPayslip?,
    pendingDeletePayslip: ParsedPayslip?,
    onDismissAction: () -> Unit,
    onViewReplica: () -> Unit,
    onViewOriginal: () -> Unit,
    onShareSummary: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
) {
    activeActionPayslip?.let { payslip ->
        HistoryActionBottomSheet(
            payslip = payslip,
            onDismissRequest = onDismissAction,
            onViewReplica = onViewReplica,
            onViewOriginal = onViewOriginal,
            onShareSummary = onShareSummary,
            onDelete = onDeleteRequest,
        )
    }

    pendingDeletePayslip?.let { payslip ->
        HistoryDeleteConfirmationDialog(
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDelete,
        )
    }
}
