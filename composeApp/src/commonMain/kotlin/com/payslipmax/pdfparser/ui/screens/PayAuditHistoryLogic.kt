package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.PostingKind
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import com.payslipmax.pdfparser.ui.theme.PayAuditStrings
import com.payslipmax.pdfparser.ui.theme.PayAuditVerdictStrings

/** One row of the History tab: a stretch of months on one level/stage, a DA step, or a posting. */
data class TimelineSpan(
    val from: PayMonth,
    val to: PayMonth,
    val title: String,
    val detail: String,
)

/** Collapses the per-month timeline into spans (docs/Plan Phase 2 U4), newest first. */
fun buildTimelineSpans(timeline: ServiceTimeline): List<TimelineSpan> {
    val months = timeline.months.sortedBy { it.month }
    val spans = levelSpans(months) + daSteps(months) + postingSpans(timeline)
    return spans.sortedWith(compareByDescending<TimelineSpan> { it.from }.thenByDescending { it.to })
}

private fun levelSpans(months: List<TimelineMonth>): List<TimelineSpan> {
    val spans = mutableListOf<TimelineSpan>()
    var run = mutableListOf<TimelineMonth>()

    fun flush() {
        if (run.isEmpty()) return
        val first = run.first()
        val level = first.level
        val stage = first.stage
        val title =
            if (level != null && stage != null) {
                "${PayAuditStrings.timelineLevelPrefix}${level.label}${PayAuditVerdictStrings.spanStageSeparator}$stage"
            } else {
                PayAuditStrings.timelineLevelUnresolved
            }
        spans += TimelineSpan(first.month, run.last().month, title, "${run.size}${PayAuditVerdictStrings.spanMonthsSuffix}")
        run = mutableListOf()
    }
    months.forEach { m ->
        val head = run.firstOrNull()
        if (head != null && (head.level != m.level || head.stage != m.stage)) flush()
        run += m
    }
    flush()
    return spans
}

private fun daSteps(months: List<TimelineMonth>): List<TimelineSpan> =
    months.zipWithNext().mapNotNull { (a, b) ->
        val from = a.daPercent
        val to = b.daPercent
        if (from == null || to == null || from == to) {
            null
        } else {
            TimelineSpan(b.month, b.month, "DA $from% → $to%", "${PayAuditVerdictStrings.spanEffectivePrefix}${formatPayMonth(b.month)}")
        }
    }

private fun postingSpans(timeline: ServiceTimeline): List<TimelineSpan> =
    timeline.postings.map {
        val title = if (it.kind == PostingKind.RISK_HARDSHIP) PayAuditVerdictStrings.spanRiskHardshipPosting else PayAuditVerdictStrings.spanFieldPosting
        TimelineSpan(it.first, it.last, title, "${formatPayMonth(it.first)} – ${formatPayMonth(it.last)}")
    }

/** Groups [items] (year to value) by year, newest year first, keeping each year's input order. */
fun <T> groupChangesByYear(items: List<Pair<Int, T>>): List<Pair<Int, List<T>>> =
    items.groupBy({ it.first }, { it.second }).toList().sortedByDescending { it.first }
