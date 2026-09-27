package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.timeline.ChangeExplanation
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import com.payslipmax.pdfparser.insights.timeline.TptaCityClass
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings

/**
 * "What changed this month" + Service Timeline sections of [PayAuditScreen] (docs/Plan/09_PayAudit_PhasePlan.md
 * Phase 4). Both render for free users — only [payAuditFindingsItems] is gated. [changes] is filtered
 * to entries with a non-null [ChangeExplanation.reason]; an unexplained move is an honest gap
 * (Phase 3), not something worth surfacing as an empty "no reason" row.
 */
fun LazyListScope.payAuditChangesItems(changes: List<ChangeExplanation>) {
    val explained = changes.filter { it.reason != null }
    item(key = "pay_audit_changes_header", contentType = "section_header") {
        Text(text = PayAuditStrings.changesSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (explained.isEmpty()) {
        item(key = "pay_audit_changes_empty", contentType = "empty_state") {
            Text(text = PayAuditStrings.changesEmptyState, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        items(explained, key = { "${it.field}_${it.month.index}" }, contentType = { "change_row" }) { change ->
            PayAuditChangeRow(change = change)
        }
    }
}

/**
 * Every explained change across the whole stored history (docs/Plan/09_PayAudit_PhasePlan.md Phase 8
 * P7-12), excluding [currentMonth] since that transition is already shown by [payAuditChangesItems]
 * above — avoids duplicate `LazyColumn` keys and a duplicated row for the same month.
 */
fun LazyListScope.payAuditAllChangesItems(
    changes: List<ChangeExplanation>,
    currentMonth: PayMonth,
) {
    val explained = changes.filter { it.reason != null && it.month != currentMonth }.sortedByDescending { it.month }
    item(key = "pay_audit_all_changes_header", contentType = "section_header") {
        Text(text = PayAuditStrings.allChangesSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (explained.isEmpty()) {
        item(key = "pay_audit_all_changes_empty", contentType = "empty_state") {
            Text(text = PayAuditStrings.allChangesEmptyState, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        items(explained, key = { "all_${it.field}_${it.month.index}" }, contentType = { "change_row" }) { change ->
            PayAuditChangeRow(change = change)
        }
    }
}

fun LazyListScope.payAuditTimelineItems(timeline: ServiceTimeline) {
    item(key = "pay_audit_timeline_header", contentType = "section_header") {
        Text(text = PayAuditStrings.timelineSectionTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (timeline.months.isEmpty()) {
        item(key = "pay_audit_timeline_empty", contentType = "empty_state") {
            Text(text = PayAuditStrings.timelineEmptyState, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        val monthsNewestFirst = timeline.months.sortedByDescending { it.month }
        items(monthsNewestFirst, key = { it.month.index }, contentType = { "timeline_month_row" }) { month ->
            PayAuditTimelineMonthRow(month = month)
        }
    }
}

@Composable
private fun PayAuditChangeRow(change: ChangeExplanation) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo),
        ) {
            Text(
                text = "${change.month.month}/${change.month.year} — ₹${change.from.toInt()} → ₹${change.to.toInt()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(text = change.reason.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PayAuditTimelineMonthRow(month: TimelineMonth) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        border = BorderStroke(AppDimensions.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(AppDimensions.PaddingSmall),
            verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo),
        ) {
            Text(text = "${month.month.month}/${month.month.year}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = timelineMonthDetail(month), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun timelineMonthDetail(month: TimelineMonth): String =
    buildString {
        val level = month.level
        val stage = month.stage
        if (level != null && stage != null) {
            append(PayAuditStrings.timelineLevelPrefix)
            append(level.label)
            append(PayAuditStrings.timelineStageSeparator)
            append(stage)
        } else {
            append(PayAuditStrings.timelineLevelUnresolved)
        }
        month.daPercent?.let {
            append(" · ")
            append(it)
            append(PayAuditStrings.timelineDaSuffix)
        }
        if (month.tptaCity == TptaCityClass.HIGHER) append(PayAuditStrings.timelineTptaHigherLabel)
        if (month.occupiesQuarters) append(PayAuditStrings.timelineQuartersLabel)
    }
