package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMonth
import com.payslipmax.pdfparser.insights.timeline.ServiceTimeline
import com.payslipmax.pdfparser.insights.timeline.TimelineEvent
import com.payslipmax.pdfparser.insights.timeline.TimelineEventType
import com.payslipmax.pdfparser.insights.timeline.TimelineMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PayAuditFixationCalculatorLogicTest {
    private fun month(
        year: Int,
        monthNum: Int,
        level: PayLevel?,
        stage: Int?,
    ) = TimelineMonth(PayMonth(year, monthNum), basicPay = 0.0, level = level, stage = stage, daPercent = null, tptaCity = null, occupiesQuarters = false)

    @Test
    fun readsFromLevelStageAndDniMonthOffTheTimeline() {
        val timeline =
            ServiceTimeline(
                months = listOf(month(2018, 1, PayLevel.L10, 7), month(2018, 7, PayLevel.L10, 8)),
                events =
                    listOf(
                        TimelineEvent(PayMonth(2018, 7), TimelineEventType.INCREMENT, PayLevel.L10, PayLevel.L10, 69000.0, 71100.0),
                    ),
                postings = emptyList(),
            )

        val result =
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 2026, promotionMonth = 3),
            )

        assertEquals(71500, result!!.comparison.option1.fixedPay)
        assertEquals(73600, result.comparison.option2.fixedPay)
        // An increment was recorded, so the DNI month is a fact, not an assumption.
        assertEquals(false, result.dniMonthAssumed)
    }

    @Test
    fun defaultsToJanuaryWhenNoIncrementHasBeenRecordedYet() {
        val timeline =
            ServiceTimeline(months = listOf(month(2018, 1, PayLevel.L10, 8)), events = emptyList(), postings = emptyList())

        val result =
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 2026, promotionMonth = 3),
            )

        // DNI defaults to the January cycle: next Jan on/after Mar 2026 is Jan 2027, so the first
        // post-DNI increment (6 months later, on cycle) lands in July 2027 — not Jan 2027, which a
        // July-default would have produced instead.
        assertEquals(PayMonth(2027, 7), result!!.comparison.option2.firstIncrementDate)
        // No increment on the timeline — this is an assumption, and the UI must say so (P7-07).
        assertEquals(true, result.dniMonthAssumed)
    }

    @Test
    fun returnsNullWhenTheLatestTimelineMonthIsUnplaced() {
        val timeline = ServiceTimeline(months = listOf(month(2018, 1, null, null)), events = emptyList(), postings = emptyList())

        val comparison =
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 2026, promotionMonth = 3),
            )

        assertNull(comparison)
    }

    @Test
    fun returnsNullWhenTheTargetLevelIsNotHigherThanTheCurrentOne() {
        val timeline = ServiceTimeline(months = listOf(month(2018, 1, PayLevel.L11, 8)), events = emptyList(), postings = emptyList())

        assertNull(
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L10, promotionYear = 2026, promotionMonth = 3),
            ),
        )
        assertNull(
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 2026, promotionMonth = 3),
            ),
        )
    }

    @Test
    fun returnsNullWhenThePromotionMonthIsNotAfterTheLatestTimelineMonth() {
        val timeline = ServiceTimeline(months = listOf(month(2018, 1, PayLevel.L10, 8)), events = emptyList(), postings = emptyList())

        // Same month as latest.
        assertNull(
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 2018, promotionMonth = 1),
            ),
        )
        // Before latest.
        assertNull(
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 1800, promotionMonth = 1),
            ),
        )
    }

    @Test
    fun returnsNullWhenThePromotionYearIsImplausiblyFarInTheFuture() {
        val timeline = ServiceTimeline(months = listOf(month(2018, 1, PayLevel.L10, 8)), events = emptyList(), postings = emptyList())

        assertNull(
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 9999, promotionMonth = 1),
            ),
        )
    }

    @Test
    fun returnsNullOnAnEmptyTimeline() {
        val timeline = ServiceTimeline(months = emptyList(), events = emptyList(), postings = emptyList())

        assertNull(
            resolveFixationComparison(
                timeline,
                FixationCalculatorInputs(toLevel = PayLevel.L11, promotionYear = 2026, promotionMonth = 3),
            ),
        )
    }
}
