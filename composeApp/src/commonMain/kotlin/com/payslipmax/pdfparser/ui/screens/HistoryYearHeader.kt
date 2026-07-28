package com.payslipmax.pdfparser.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.payslipmax.pdfparser.database.LedgerRecordEntity
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.AppStrings

internal data class YearlyStats(
    val totalNet: Double,
    val totalDsop: Double,
)

internal fun calculateYearlyStats(payslips: List<ParsedPayslip>): YearlyStats {
    val totalNet = payslips.sumOf { it.summary.netRemittance }
    val totalDsop = payslips.sumOf { it.deductions.dsopSubscription }
    return YearlyStats(totalNet, totalDsop)
}

@Composable
fun HistoryYearHeader(
    year: Int,
    payslips: List<ParsedPayslip>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = remember(payslips) { calculateYearlyStats(payslips) }
    val rotationState by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f)

    FlatBorderedCard(modifier = modifier.clickable { onToggleExpand() }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = year.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
                YearlyStatsRow(stats = stats)
            }
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.rotate(rotationState),
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = AppStrings.historyChevronContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun YearlyStatsRow(stats: YearlyStats) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${AppStrings.historyNetTakeHomeLabel}: ₹${formatAmount(stats.totalNet)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "${AppStrings.historyDsopLabel}: ₹${formatAmount(stats.totalDsop)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun HistoryHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = AppStrings.navigationHistory,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
        Text(
            text = AppStrings.historyHeaderDescription,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun EmptyHistoryView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppStrings.historyEmptyState,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

fun LazyListScope.historyYearGroup(
    year: Int,
    yearPayslips: List<ParsedPayslip>,
    allPayslips: List<ParsedPayslip>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    trendMap: Map<String, TrendInfo>? = null,
) {
    item(key = "header_$year", contentType = "history_year_header") {
        HistoryYearHeader(
            year = year,
            payslips = yearPayslips,
            isExpanded = isExpanded,
            onToggleExpand = onToggleExpand,
        )
    }
    if (isExpanded) {
        items(
            items = yearPayslips,
            key = { it.dateStr },
            contentType = { "history_card" },
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
                    trendMap = trendMap,
                )
            }
        }
    }
}

@Composable
fun HistoryLazyList(
    payslips: List<ParsedPayslip>,
    ledgerRecords: List<LedgerRecordEntity>,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    expandedYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    initialScrollIndex: Int,
    initialScrollOffset: Int,
    onScrollPositionChanged: (Int, Int) -> Unit,
) {
    val trendMap = remember(payslips) { precomputeTrends(payslips) }
    val grouped =
        remember(payslips) {
            payslips.groupBy { it.year }
                .mapValues { (_, list) -> list.sortedByDescending { it.monthNum } }
                .toList()
                .sortedByDescending { it.first }
        }
    val sortedLedger =
        remember(ledgerRecords) {
            ledgerRecords.sortedWith(compareByDescending<LedgerRecordEntity> { it.year }.thenByDescending { it.monthNum })
        }
    val listState = rememberLazyListState(initialScrollIndex, initialScrollOffset)

    DisposableEffect(Unit) {
        onDispose {
            onScrollPositionChanged(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset)
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
        modifier = Modifier.fillMaxSize(),
    ) {
        historyLazyListContent(
            sortedLedger = sortedLedger,
            grouped = grouped,
            payslips = payslips,
            expandedYears = expandedYears,
            onToggleYear = onToggleYear,
            onPayslipClick = onPayslipClick,
            onLongPress = onLongPress,
            onSwipeDelete = onSwipeDelete,
            trendMap = trendMap,
        )
    }
}

private fun LazyListScope.historyLazyListContent(
    sortedLedger: List<LedgerRecordEntity>,
    grouped: List<Pair<Int, List<ParsedPayslip>>>,
    payslips: List<ParsedPayslip>,
    expandedYears: Set<Int>,
    onToggleYear: (Int) -> Unit,
    onPayslipClick: (ParsedPayslip) -> Unit,
    onLongPress: (ParsedPayslip) -> Unit,
    onSwipeDelete: (ParsedPayslip) -> Unit,
    trendMap: Map<String, TrendInfo>,
) {
    if (sortedLedger.isNotEmpty()) {
        item(key = "historical_ledger", contentType = "historical_ledger") {
            HistoricalLedgerCard(ledgerRecords = sortedLedger)
        }
    }
    grouped.forEach { (year, yearPayslips) ->
        historyYearGroup(
            year = year,
            yearPayslips = yearPayslips,
            allPayslips = payslips,
            isExpanded = expandedYears.contains(year),
            onToggleExpand = { onToggleYear(year) },
            onPayslipClick = onPayslipClick,
            onLongPress = onLongPress,
            onSwipeDelete = onSwipeDelete,
            trendMap = trendMap,
        )
    }
}
