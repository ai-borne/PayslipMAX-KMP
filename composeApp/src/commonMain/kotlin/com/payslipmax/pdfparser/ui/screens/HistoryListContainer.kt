package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.database.AiInsightReportEntity
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.theme.AppDimensions

@Composable
internal fun HistoryListContainer(
    payslips: List<ParsedPayslip>,
    aiReports: List<AiInsightReportEntity>,
    ledgerRecords: List<LedgerRecordEntity>,
    isLoading: Boolean,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onAiReportClick: (AiInsightReportEntity) -> Unit,
    onNavigateToInsights: () -> Unit,
    expandedYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    initialScrollIndex: Int,
    initialScrollOffset: Int,
    onScrollPositionChanged: (Int, Int) -> Unit,
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
            selectedTab = selectedTab, isLoading = isLoading, payslips = payslips,
            aiReports = aiReports, ledgerRecords = ledgerRecords, onPayslipClick = onPayslipClick,
            onLongPress = onLongPress, onSwipeDelete = onSwipeDelete, onAiReportClick = onAiReportClick,
            onNavigateToInsights = onNavigateToInsights, expandedYears = expandedYears,
            onToggleYear = onToggleYear, initialScrollIndex = initialScrollIndex,
            initialScrollOffset = initialScrollOffset, onScrollPositionChanged = onScrollPositionChanged,
        )
    }
}

@Composable
internal fun HistoryActiveList(
    selectedTab: HistoryTab,
    isLoading: Boolean,
    payslips: List<ParsedPayslip>,
    aiReports: List<AiInsightReportEntity>,
    ledgerRecords: List<LedgerRecordEntity>,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    onAiReportClick: (AiInsightReportEntity) -> Unit,
    onNavigateToInsights: () -> Unit,
    expandedYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    initialScrollIndex: Int,
    initialScrollOffset: Int,
    onScrollPositionChanged: (Int, Int) -> Unit,
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
                expandedYears = expandedYears,
                onToggleYear = onToggleYear,
                initialScrollIndex = initialScrollIndex,
                initialScrollOffset = initialScrollOffset,
                onScrollPositionChanged = onScrollPositionChanged,
            )
        }
    } else {
        if (aiReports.isEmpty()) {
            EmptyAiReportsView(onNavigateToInsights = onNavigateToInsights)
        } else {
            AiReportsLazyList(
                aiReports = aiReports,
                onAiReportClick = onAiReportClick,
            )
        }
    }
}
