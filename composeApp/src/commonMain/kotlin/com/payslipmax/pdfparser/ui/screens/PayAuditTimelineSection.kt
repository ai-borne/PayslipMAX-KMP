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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.payslipmax.pdfparser.insights.timeline.ChangeExplanation
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings as S

/**
 * "What changed from last month" (U3): every row names its pay line, then the move and the reason in
 * plain words. Only explained moves are shown; an unexplained one is an honest gap (Phase 3), not a row.
 */
fun LazyListScope.payAuditChangesItems(changes: List<ChangeExplanation>) {
    val explained = changes.filter { it.reason != null }
    item(key = "pay_audit_changes_header", contentType = "section_header") {
        Text(text = S.changesTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    if (explained.isEmpty()) {
        item(key = "pay_audit_changes_empty", contentType = "empty_state") {
            Text(text = S.changesEmpty, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        items(explained, key = { "change_${it.field}_${it.month.index}" }, contentType = { "change_row" }) { PayAuditChangeRow(it, showMonth = false) }
    }
}

/** History tab: the timeline as spans, then every explained change collapsed by year (U4). */
fun LazyListScope.payAuditHistoryItems(
    timeline: ServiceTimeline,
    allChanges: List<ChangeExplanation>,
    currentMonth: PayMonth?,
) {
    item(key = "pay_audit_spans_header", contentType = "section_header") {
        Text(text = S.spansTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    val spans = buildTimelineSpans(timeline)
    if (spans.isEmpty()) {
        item(key = "pay_audit_spans_empty", contentType = "empty_state") {
            Text(text = PayAuditStrings.timelineEmptyState, style = MaterialTheme.typography.bodyMedium)
        }
    } else {
        items(spans, key = { "span_${it.from.index}_${it.to.index}_${it.title}" }, contentType = { "span_row" }) { PayAuditSpanRow(it) }
    }
    item(key = "pay_audit_by_year_header", contentType = "section_header") {
        Text(text = S.everyChangeTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    val byYear =
        groupChangesByYear(
            allChanges.filter { it.reason != null }.sortedByDescending { it.month }.map { it.month.year to it },
        )
    items(byYear, key = { "year_${it.first}" }, contentType = { "year_group" }) { (year, changes) ->
        PayAuditYearGroup(year = year, changes = changes, startExpanded = currentMonth?.year == year)
    }
}

@Composable
private fun PayAuditYearGroup(
    year: Int,
    changes: List<ChangeExplanation>,
    startExpanded: Boolean,
) {
    var expanded by rememberSaveable(year) { mutableStateOf(startExpanded) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingSmall), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingSmall)) {
            TextButton(onClick = { expanded = !expanded }) {
                Text("$year · ${changes.size}${if (changes.size == 1) S.changesCountSingular else S.changesCountPlural}", fontWeight = FontWeight.Bold)
            }
            if (expanded) changes.forEach { PayAuditChangeRow(it, showMonth = true) }
        }
    }
}

@Composable
private fun PayAuditChangeRow(
    change: ChangeExplanation,
    showMonth: Boolean,
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo)) {
        val name = payLineLabel(change.field)
        Text(text = if (showMonth) "${formatPayMonth(change.month)} · $name" else name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Text(text = formatChangeAmounts(change.from, change.to), style = MaterialTheme.typography.bodyMedium)
        Text(text = change.reason.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PayAuditSpanRow(span: TimelineSpan) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppDimensions.CornerRadiusMedium),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
        border = BorderStroke(AppDimensions.BorderHairline, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(modifier = Modifier.padding(AppDimensions.PaddingSmall), verticalArrangement = Arrangement.spacedBy(AppDimensions.SpacingTwo)) {
            val range = if (span.from == span.to) formatPayMonth(span.from) else "${formatPayMonth(span.from)} – ${formatPayMonth(span.to)}"
            Text(text = range, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = span.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(text = span.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
