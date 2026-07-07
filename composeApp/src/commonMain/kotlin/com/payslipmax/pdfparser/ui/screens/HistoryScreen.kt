package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.database.AiInsightReportEntity
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.ui.*

@Composable
fun HistoryScreen(
    viewModel: PayslipViewModel,
    onOpenPdf: (pdfBytes: ByteArray, filename: String) -> Unit,
    onNavigateToInsights: () -> Unit,
    onSharePayslip: (ParsedPayslip) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val aiReports by viewModel.aiInsightReports.collectAsState()
    val ledgerRecords by viewModel.ledgerRecords.collectAsState()
    var selectedDetailPayslip by remember { mutableStateOf<ParsedPayslip?>(null) }
    var activeActionPayslip by remember { mutableStateOf<ParsedPayslip?>(null) }
    var pendingDeletePayslip by remember { mutableStateOf<ParsedPayslip?>(null) }
    var selectedAiReport by remember { mutableStateOf<AiInsightReportEntity?>(null) }

    HistoryContent(
        uiState = uiState,
        aiReports = aiReports,
        ledgerRecords = ledgerRecords,
        selectedDetailPayslip = selectedDetailPayslip,
        activeActionPayslip = activeActionPayslip,
        pendingDeletePayslip = pendingDeletePayslip,
        selectedAiReport = selectedAiReport,
        onSelectDetail = { selectedDetailPayslip = it },
        onOpenOriginal = { payslip ->
            viewModel.getPayslipPdf(payslip.dateStr) { bytes ->
                if (bytes != null) onOpenPdf(bytes, payslip.file)
            }
        },
        onLongPress = { activeActionPayslip = it },
        onSwipeDelete = { pendingDeletePayslip = it },
        onAiReportClick = { selectedAiReport = it },
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
        onDismissAiReport = { selectedAiReport = null },
        viewModel = viewModel,
        onNavigateToInsights = onNavigateToInsights,
        modifier = modifier,
    )
}

@Composable
private fun HistoryReplicaView(
    payslip: ParsedPayslip,
    onBack: () -> Unit,
    onOpenOriginal: (ParsedPayslip) -> Unit,
    viewModel: PayslipViewModel,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    PayslipReplicaScreen(
        payslip = payslip,
        onBackClick = onBack,
        onViewPdfClick = { viewModel.getPayslipPdf(it) { bytes -> if (bytes != null) onOpenOriginal(payslip) } },
        modifier = modifier,
        onCorrectField = { fieldKey, newValue -> viewModel.applyCorrection(payslip.dateStr, fieldKey, newValue) },
        isEditModeActive = uiState.isEditModeActive,
        draftCorrections = uiState.draftCorrections,
        onStartEditing = { viewModel.startEditingSession(payslip.dateStr) },
        onUpdateDraft = { viewModel.updateDraftCorrection(it) },
        onDeleteDraft = { fieldKey, codeHead, category, originalAmount ->
            viewModel.deleteDraftCorrection(fieldKey, codeHead, category, originalAmount)
        },
        onSaveSession = { viewModel.saveEditingSession(payslip.dateStr) },
        onCancelSession = { viewModel.cancelEditingSession() },
    )
}

@Composable
private fun HistoryMainView(
    uiState: PayslipUiState,
    aiReports: List<AiInsightReportEntity>,
    ledgerRecords: List<LedgerRecordEntity>,
    activeActionPayslip: ParsedPayslip?,
    pendingDeletePayslip: ParsedPayslip?,
    selectedAiReport: AiInsightReportEntity?,
    onSelectDetail: (ParsedPayslip?) -> Unit,
    onOpenOriginal: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onAiReportClick: (AiInsightReportEntity) -> Unit,
    onSharePayslip: (ParsedPayslip) -> Unit,
    onDismissAction: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: (ParsedPayslip) -> Unit,
    onDismissDelete: () -> Unit,
    onDismissAiReport: () -> Unit,
    onNavigateToInsights: () -> Unit,
    expandedYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    initialScrollIndex: Int,
    initialScrollOffset: Int,
    onScrollPositionChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    HistoryListContainer(
        payslips = uiState.payslips, aiReports = aiReports, ledgerRecords = ledgerRecords,
        isLoading = uiState.isLoading, onPayslipClick = onSelectDetail, onLongPress = onLongPress,
        onSwipeDelete = onSwipeDelete, onAiReportClick = onAiReportClick,
        onNavigateToInsights = onNavigateToInsights, expandedYears = expandedYears,
        onToggleYear = onToggleYear, initialScrollIndex = initialScrollIndex,
        initialScrollOffset = initialScrollOffset, onScrollPositionChanged = onScrollPositionChanged,
        modifier = modifier,
    )
    HistoryOverlays(
        activeActionPayslip = activeActionPayslip, pendingDeletePayslip = pendingDeletePayslip,
        selectedAiReport = selectedAiReport, onDismissAction = onDismissAction,
        onViewReplica = { onSelectDetail(activeActionPayslip) },
        onViewOriginal = { activeActionPayslip?.let(onOpenOriginal) },
        onShareSummary = { activeActionPayslip?.let(onSharePayslip) },
        onDeleteRequest = onDeleteRequest,
        onConfirmDelete = { pendingDeletePayslip?.let(onConfirmDelete) },
        onDismissDelete = onDismissDelete,
        onDismissAiReport = onDismissAiReport,
    )
}

@Composable
private fun HistoryContent(
    uiState: PayslipUiState,
    aiReports: List<AiInsightReportEntity>,
    ledgerRecords: List<LedgerRecordEntity>,
    selectedDetailPayslip: ParsedPayslip?,
    activeActionPayslip: ParsedPayslip?,
    pendingDeletePayslip: ParsedPayslip?,
    selectedAiReport: AiInsightReportEntity?,
    onSelectDetail: (ParsedPayslip?) -> Unit,
    onOpenOriginal: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onAiReportClick: (AiInsightReportEntity) -> Unit,
    onSharePayslip: (ParsedPayslip) -> Unit,
    onDismissAction: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: (ParsedPayslip) -> Unit,
    onDismissDelete: () -> Unit,
    onDismissAiReport: () -> Unit,
    viewModel: PayslipViewModel,
    onNavigateToInsights: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedDetailPayslip != null) {
        HistoryReplicaView(
            payslip = selectedDetailPayslip,
            onBack = { onSelectDetail(null) },
            onOpenOriginal = onOpenOriginal,
            viewModel = viewModel,
            modifier = modifier,
        )
    } else {
        HistoryMainView(
            uiState = uiState, aiReports = aiReports, ledgerRecords = ledgerRecords,
            activeActionPayslip = activeActionPayslip, pendingDeletePayslip = pendingDeletePayslip,
            selectedAiReport = selectedAiReport, onSelectDetail = onSelectDetail,
            onOpenOriginal = onOpenOriginal, onLongPress = onLongPress,
            onSwipeDelete = onSwipeDelete, onAiReportClick = onAiReportClick,
            onSharePayslip = onSharePayslip, onDismissAction = onDismissAction,
            onDeleteRequest = onDeleteRequest, onConfirmDelete = onConfirmDelete,
            onDismissDelete = onDismissDelete, onDismissAiReport = onDismissAiReport,
            onNavigateToInsights = onNavigateToInsights, expandedYears = uiState.expandedHistoryYears,
            onToggleYear = viewModel::toggleHistoryYearExpanded, initialScrollIndex = uiState.historyScrollIndex,
            initialScrollOffset = uiState.historyScrollOffset, onScrollPositionChanged = viewModel::saveHistoryScrollPosition,
            modifier = modifier,
        )
    }
}

@Composable
private fun HistoryOverlays(
    activeActionPayslip: ParsedPayslip?,
    pendingDeletePayslip: ParsedPayslip?,
    selectedAiReport: AiInsightReportEntity?,
    onDismissAction: () -> Unit,
    onViewReplica: () -> Unit,
    onViewOriginal: () -> Unit,
    onShareSummary: () -> Unit,
    onDeleteRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onDismissAiReport: () -> Unit,
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

    selectedAiReport?.let { report ->
        AiInsightsBottomSheet(
            aiInsights = report.reportJSON,
            onDismissRequest = onDismissAiReport,
            onRegenerateClick = null,
        )
    }
}
