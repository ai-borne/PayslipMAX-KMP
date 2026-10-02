package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.InsightsStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings
import org.koin.compose.koinInject

/**
 * Free-visible entry point into the Pay Audit screen (docs/Plan/09_PayAudit_PhasePlan.md Phase 4):
 * "Free tier: timeline and finding count." Renders nothing until the service timeline has at least
 * one month — matching [PayAuditScreen]'s own empty state, so nothing is promised before there is data.
 */
@Composable
fun PayAuditEntryCard(
    timelineMonths: Int,
    findingsCount: Int,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (timelineMonths == 0) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadius),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(AppDimensions.BorderThin, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingMedium),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall),
        ) {
            Text(
                text = PayAuditStrings.screenTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = PayAuditStrings.entryCardSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (findingsCount > 0) "$findingsCount ${findingsCountLabel(findingsCount)}" else PayAuditStrings.entryCardNoFindings,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Button(onClick = onOpen) { Text(InsightsStrings.premiumToolsOpenLabel) }
            }
        }
    }
}

private fun findingsCountLabel(count: Int): String = if (count == 1) PayAuditStrings.findingsCountSingular else PayAuditStrings.findingsCountPlural

/**
 * Insights-tab host for [PayAuditEntryCard]: runs the same [PayAuditViewModel] pipeline as the Pay Audit
 * screen so the teaser's issue count always matches what the screen shows (waiting and verified arrears
 * are not issues). Gating is irrelevant to a count, so it analyses as unlocked.
 */
@Composable
fun PayAuditEntryHost(
    selected: ParsedPayslip,
    payslips: List<ParsedPayslip>,
    onOpen: () -> Unit,
    viewModel: PayAuditViewModel = koinInject(),
) {
    DisposableEffect(viewModel) { onDispose { viewModel.dispose() } }
    LaunchedEffect(selected, payslips) { viewModel.setInputs(payslips, hasAccess = true, requestedMonth = PayMonth(selected.year, selected.monthNum)) }
    val state by viewModel.uiState.collectAsState()
    val issues =
        when (val verdict = state.verdict) {
            is PayAuditVerdict.Issue -> verdict.count
            is PayAuditVerdict.LockedIssue -> verdict.count
            else -> 0
        }
    PayAuditEntryCard(timelineMonths = state.timeline.months.size, findingsCount = issues, onOpen = onOpen)
}
