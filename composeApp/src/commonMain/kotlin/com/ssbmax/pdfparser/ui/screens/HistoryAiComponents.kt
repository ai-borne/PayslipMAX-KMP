package com.ssbmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.ssbmax.pdfparser.database.AiInsightReportEntity
import com.ssbmax.pdfparser.ui.theme.AppDimensions
import com.ssbmax.pdfparser.ui.theme.AppStrings

enum class HistoryTab {
    STATEMENTS,
    AI_REPORTS,
}

@Composable
fun HistoryTabSelector(
    selectedTab: HistoryTab,
    onTabSelected: (HistoryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium))
                .padding(AppDimensions.SpacingTiny),
        horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
    ) {
        HistoryTab.values().forEach { tab ->
            val isSelected = tab == selectedTab
            val containerColor = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
            val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Button(
                onClick = { onTabSelected(tab) },
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = containerColor,
                        contentColor = contentColor,
                    ),
                shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = AppDimensions.SpacingSmall),
            ) {
                Text(
                    text =
                        when (tab) {
                            HistoryTab.STATEMENTS -> AppStrings.historyTabStatements
                            HistoryTab.AI_REPORTS -> AppStrings.historyTabAiReports
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun EmptyAiReportsView(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = AppStrings.historyEmptyAiReports,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AiReportsLazyList(
    aiReports: List<AiInsightReportEntity>,
    onAiReportClick: (AiInsightReportEntity) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sortedReports =
        remember(aiReports) {
            aiReports.sortedByDescending { it.generatedDate }
        }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingLarge),
        modifier = modifier.fillMaxSize(),
    ) {
        items(
            items = sortedReports,
            key = { it.id },
        ) { report ->
            AiReportCard(
                report = report,
                onClick = { onAiReportClick(report) },
            )
        }
    }
}

@Composable
private fun AiReportCard(
    report: AiInsightReportEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = AppDimensions.BorderThin),
    ) {
        AiReportCardContent(report = report)
    }
}

@Composable
private fun AiReportCardContent(
    report: AiInsightReportEntity,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(AppDimensions.PaddingMedium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppDimensions.SpacingMedium),
        ) {
            Text(
                text = "🤖",
                style = MaterialTheme.typography.titleLarge,
            )
            Column {
                Text(
                    text = getReadableMonth(report.payslipMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(AppDimensions.SpacingTiny))
                Text(
                    text = AppStrings.historyPremiumIntelligenceNarrative,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = "➔",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

internal fun getReadableMonth(monthStr: String): String {
    val parts = monthStr.split("/")
    if (parts.size != 2) return monthStr
    val monthNum = parts[0].toIntOrNull() ?: return monthStr
    val year = parts[1]
    val monthName =
        when (monthNum) {
            1 -> "January"
            2 -> "February"
            3 -> "March"
            4 -> "April"
            5 -> "May"
            6 -> "June"
            7 -> "July"
            8 -> "August"
            9 -> "September"
            10 -> "October"
            11 -> "November"
            12 -> "December"
            else -> monthStr
        }
    return "$monthName $year"
}
