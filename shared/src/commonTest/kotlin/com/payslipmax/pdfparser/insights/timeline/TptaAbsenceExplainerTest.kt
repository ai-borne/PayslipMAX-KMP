package com.payslipmax.pdfparser.insights.timeline

import com.payslipmax.pdfparser.insights.payAuditMonths
import com.payslipmax.pdfparser.insights.payAuditPayslip
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** P7-17 (docs/Plan/09_PayAudit_PhasePlan.md): [TptaAbsenceExplainer.isPendingFutureData] on its own. */
class TptaAbsenceExplainerTest {
    @Test
    fun aRelocationGapWithNoFutureSampleYetIsPending() {
        // Jan-Feb at OTHER, Mar has no TPTA. Truncated here: the HIGHER-city sample that would confirm or
        // rule out a relocation (Apr onward) has not been imported yet.
        val history =
            payAuditMonths(2018, 1, 3) { y, m ->
                payAuditPayslip(y, m, 85300.0, tpta = if (m == 3) 0.0 else 3600.0 * 1.17)
            }
        val timeline = ServiceTimelineBuilder.build(history)
        assertFalse(TptaAbsenceExplainer.explains(timeline, PayMonth(2018, 3)))
        assertTrue(TptaAbsenceExplainer.isPendingFutureData(timeline, PayMonth(2018, 3)))
    }

    @Test
    fun onceTheFutureMonthArrivesTheGapIsSettledNotPending() {
        val history =
            payAuditMonths(2018, 1, 5) { y, m ->
                val tpta =
                    if (m < 3) {
                        3600.0 * 1.17
                    } else if (m == 3) {
                        0.0
                    } else {
                        7200.0 * 1.17
                    }
                payAuditPayslip(y, m, 85300.0, tpta = tpta)
            }
        val timeline = ServiceTimelineBuilder.build(history)
        assertTrue(TptaAbsenceExplainer.explains(timeline, PayMonth(2018, 3)))
        assertFalse(TptaAbsenceExplainer.isPendingFutureData(timeline, PayMonth(2018, 3)))
    }

    @Test
    fun withNoPriorCitySampleAtAllThereIsNothingToHoldPendingOn() {
        val history = payAuditMonths(2018, 1, 1) { y, m -> payAuditPayslip(y, m, 85300.0, tpta = 0.0) }
        val timeline = ServiceTimelineBuilder.build(history)
        assertFalse(TptaAbsenceExplainer.isPendingFutureData(timeline, PayMonth(2018, 1)))
    }
}
