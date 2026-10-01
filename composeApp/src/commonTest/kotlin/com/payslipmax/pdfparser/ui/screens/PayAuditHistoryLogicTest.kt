package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.PostingKind
import com.payslipmax.pdfparser.insights.timeline.PostingSpan
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** U4: History must read as a handful of spans (stage, DA step, posting), not one row per month. */
class PayAuditHistoryLogicTest {
    private fun month(
        y: Int,
        m: Int,
        stage: Int?,
        da: Int?,
        level: PayLevel? = PayLevel.L12A,
    ) =
        TimelineMonth(PayMonth(y, m), 100000.0, level, stage, da, null, false)

    @Test
    fun consecutiveMonthsOnTheSameStageCollapseIntoOneSpan() {
        val timeline =
            ServiceTimeline(
                months = listOf(month(2025, 11, 7, 55), month(2025, 12, 7, 55), month(2026, 1, 8, 55), month(2026, 2, 8, 55)),
                events = emptyList(),
                postings = emptyList(),
            )
        val spans = buildTimelineSpans(timeline).filter { it.title.startsWith("Level") }
        assertEquals(listOf("Level 12A · Stage 8", "Level 12A · Stage 7"), spans.map { it.title })
        assertEquals(PayMonth(2026, 1), spans[0].from)
        assertEquals(PayMonth(2026, 2), spans[0].to)
    }

    @Test
    fun aDaStepBecomesItsOwnDatedEntryAndIssuesWithinNeitherSpanAreLost() {
        val timeline =
            ServiceTimeline(
                months = listOf(month(2026, 3, 8, 58), month(2026, 4, 8, 60)),
                events = emptyList(),
                postings = emptyList(),
            )
        val step = buildTimelineSpans(timeline).single { it.title.startsWith("DA") }
        assertEquals("DA 58% → 60%", step.title)
        assertEquals(PayMonth(2026, 4), step.from)
        // A DA rise is effective 1 Jan / 1 Jul; the payslip month is only when it first showed up (real device, Apr 2026).
        assertEquals("First paid on the Apr 2026 payslip", step.detail)
    }

    @Test
    fun postingSpansAreIncludedAndEverythingIsNewestFirst() {
        val timeline =
            ServiceTimeline(
                months = listOf(month(2024, 10, 6, 50), month(2024, 11, 6, 50)),
                events = emptyList(),
                postings = listOf(PostingSpan(PostingKind.RISK_HARDSHIP, PayMonth(2024, 11), PayMonth(2024, 11))),
            )
        val spans = buildTimelineSpans(timeline)
        assertTrue(spans.any { it.title.contains("Risk & Hardship") })
        assertEquals(spans.sortedByDescending { it.from.index }.map { it.title }, spans.map { it.title })
    }

    @Test
    fun unresolvedLevelIsNamedHonestlyInsteadOfGuessed() {
        val timeline = ServiceTimeline(listOf(month(2026, 1, null, 58, level = null)), emptyList(), emptyList())
        assertEquals("Level unresolved", buildTimelineSpans(timeline).single().title)
    }

    @Test
    fun changesGroupByYearNewestFirst() {
        val years = groupChangesByYear(listOf(2025 to "a", 2026 to "b", 2025 to "c"))
        assertEquals(listOf(2026, 2025), years.map { it.first })
        assertEquals(listOf("a", "c"), years[1].second)
    }
}
