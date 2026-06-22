package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.database.AiInsightReportEntity
import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.ui.*
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
        modifier = modifier,
    )
}

@Composable
private fun HistoryReplicaView(
    payslip: ParsedPayslip, onBack: () -> Unit, onOpenOriginal: (ParsedPayslip) -> Unit,
    viewModel: PayslipViewModel, modifier: Modifier = Modifier,
) {
    PayslipReplicaScreen(
        payslip = payslip,
        onBackClick = onBack,
        onViewPdfClick = { viewModel.getPayslipPdf(it) { bytes -> if (bytes != null) onOpenOriginal(payslip) } },
        modifier = modifier,
    )
}

@Composable
private fun HistoryMainView(
    uiState: PayslipUiState, aiReports: List<AiInsightReportEntity>, ledgerRecords: List<LedgerRecordEntity>,
    activeActionPayslip: ParsedPayslip?, pendingDeletePayslip: ParsedPayslip?, selectedAiReport: AiInsightReportEntity?,
    onSelectDetail: (ParsedPayslip?) -> Unit, onOpenOriginal: (ParsedPayslip) -> Unit, onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit, onAiReportClick: (AiInsightReportEntity) -> Unit, onSharePayslip: (ParsedPayslip) -> Unit,
    onDismissAction: () -> Unit, onDeleteRequest: () -> Unit, onConfirmDelete: (ParsedPayslip) -> Unit,
    onDismissDelete: () -> Unit, onDismissAiReport: () -> Unit, modifier: Modifier = Modifier,
) {
    HistoryListContainer(
        payslips = uiState.payslips, aiReports = aiReports, ledgerRecords = ledgerRecords,
        isLoading = uiState.isLoading, onPayslipClick = onSelectDetail, onLongPress = onLongPress,
        onSwipeDelete = onSwipeDelete, onAiReportClick = onAiReportClick, modifier = modifier,
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
    uiState: PayslipUiState, aiReports: List<AiInsightReportEntity>, ledgerRecords: List<LedgerRecordEntity>,
    selectedDetailPayslip: ParsedPayslip?, activeActionPayslip: ParsedPayslip?, pendingDeletePayslip: ParsedPayslip?,
    selectedAiReport: AiInsightReportEntity?, onSelectDetail: (ParsedPayslip?) -> Unit, onOpenOriginal: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit, onSwipeDelete: (ParsedPayslip) -> Unit, onAiReportClick: (AiInsightReportEntity) -> Unit,
    onSharePayslip: (ParsedPayslip) -> Unit, onDismissAction: () -> Unit, onDeleteRequest: () -> Unit,
    onConfirmDelete: (ParsedPayslip) -> Unit, onDismissDelete: () -> Unit, onDismissAiReport: () -> Unit,
    viewModel: PayslipViewModel, modifier: Modifier = Modifier,
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

@Composable
private fun HistoryListContainer(
    payslips: List<ParsedPayslip>,
    aiReports: List<AiInsightReportEntity>,
    ledgerRecords: List<LedgerRecordEntity>,
    isLoading: Boolean,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onAiReportClick: (AiInsightReportEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableStateOf(HistoryTab.STATEMENTS) }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(AppDimensions.PaddingMedium),
    ) {
        HistoryHeader()
        Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

        HistoryTabSelector(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingLarge))

        HistoryActiveList(
            selectedTab = selectedTab,
            isLoading = isLoading,
            payslips = payslips,
            aiReports = aiReports,
            ledgerRecords = ledgerRecords,
            onPayslipClick = onPayslipClick,
            onLongPress = onLongPress,
            onSwipeDelete = onSwipeDelete,
            onAiReportClick = onAiReportClick,
        )
    }
}

@Composable
private fun HistoryActiveList(
    selectedTab: HistoryTab,
    isLoading: Boolean,
    payslips: List<ParsedPayslip>,
    aiReports: List<AiInsightReportEntity>,
    ledgerRecords: List<LedgerRecordEntity>,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onAiReportClick: (AiInsightReportEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (selectedTab == HistoryTab.STATEMENTS) {
        if (isLoading && payslips.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (payslips.isEmpty()) {
            EmptyHistoryView()
        } else {
            HistoryLazyList(
                payslips = payslips,
                ledgerRecords = ledgerRecords,
                onPayslipClick = onPayslipClick,
                onLongPress = onLongPress,
                onSwipeDelete = onSwipeDelete,
            )
        }
    } else {
        if (aiReports.isEmpty()) {
            EmptyAiReportsView()
        } else {
            AiReportsLazyList(
                aiReports = aiReports,
                onAiReportClick = onAiReportClick,
            )
        }
    }
}


