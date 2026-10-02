package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.insights.payAuditMonths
import com.payslipmax.pdfparser.insights.payAuditPayslip
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NextIncrementPredictorTest {
    private fun basic(stage: Int) = PayMatrix.payAt(PayLevel.L11, stage)!!.toDouble()

    @Test
    fun predictsTwelveMonthsAfterTheLastIncrement() {
        // Level 11: stage 7 until Jun 2018, stage 8 from the Jul 2018 DNI — same shape as IncrementAuditorTest.
        val months =
            payAuditMonths(2018, 1, 19) { y, m ->
                payAuditPayslip(y, m, if (y == 2018 && m < 7) basic(7) else basic(8))
            }
        val timeline = ServiceTimelineBuilder.build(months)

        val prediction = NextIncrementPredictor.predict(timeline)!!

        assertEquals(PayMonth(2019, 7), prediction.date)
        assertEquals(basic(9), prediction.predictedBasicPay)
        assertEquals(PayLevel.L11, prediction.level)
        assertEquals(8, prediction.currentStage)
        // The predicted date is the latest payslip's own month, and that payslip doesn't show the raise —
        // due exactly now with no confirmation yet counts as overdue, same as IncrementAuditor's exact
        // "dueAgain" condition (P7-05).
        assertEquals(true, prediction.isOverdue)
    }

    @Test
    fun flagsOverdueWhenTheDueDateIsWellBeforeTheLatestPayslip() {
        // Last increment Jul 2018; several more months pass at the same basic pay with no further
        // increment event, so the Jul 2019 DNI is overdue well before the Dec 2019 latest payslip.
        val months =
            payAuditMonths(2018, 1, 24) { y, m ->
                payAuditPayslip(y, m, if (y == 2018 && m < 7) basic(7) else basic(8))
            }
        val timeline = ServiceTimelineBuilder.build(months)

        val prediction = NextIncrementPredictor.predict(timeline)!!

        assertEquals(PayMonth(2019, 7), prediction.date)
        assertEquals(true, prediction.isOverdue)
    }

    @Test
    fun predictsSixMonthsAfterAPromotionRoundedToTheNextCycle() {
        // A promotion to Level 12A in May 2019: first increment there follows the next cycle at/after +6 months.
        val months =
            payAuditMonths(2018, 1, 15) { y, m ->
                payAuditPayslip(y, m, if (y == 2018 && m < 7) basic(7) else basic(8))
            } +
                payAuditMonths(2019, 5, 2) { y, m -> payAuditPayslip(y, m, PayMatrix.payAt(PayLevel.L12A, 1)!!.toDouble()) }
        val timeline = ServiceTimelineBuilder.build(months)

        val prediction = NextIncrementPredictor.predict(timeline)!!

        // Promotion month May 2019 + 6 months = Nov 2019; next cycle on/after that is 1 Jan 2020.
        assertEquals(PayMonth(2020, 1), prediction.date)
        assertEquals(PayMatrix.payAt(PayLevel.L12A, 2)!!.toDouble(), prediction.predictedBasicPay)
        // Latest payslip is Jun 2019, well before the Jan 2020 due date — genuinely upcoming, not overdue.
        assertEquals(false, prediction.isOverdue)
    }

    @Test
    fun returnsNullWhenTheLatestMonthIsUnplaced() {
        val months = payAuditMonths(2018, 1, 3) { y, m -> payAuditPayslip(y, m, 99999.0) }
        val timeline = ServiceTimelineBuilder.build(months)

        assertNull(NextIncrementPredictor.predict(timeline))
    }

    @Test
    fun returnsNullAtTheTopStageOfALevel() {
        // Promoted to Level 17, which has only one pay-matrix stage — no further increment is possible.
        val months =
            payAuditMonths(2018, 1, 12) { y, m -> payAuditPayslip(y, m, PayMatrix.payAt(PayLevel.L16, 4)!!.toDouble()) } +
                payAuditMonths(2019, 1, 2) { y, m -> payAuditPayslip(y, m, PayMatrix.payAt(PayLevel.L17, 1)!!.toDouble()) }
        val timeline = ServiceTimelineBuilder.build(months)

        assertNull(NextIncrementPredictor.predict(timeline))
    }
}
