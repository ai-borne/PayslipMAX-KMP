package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.timeline.PayLevel
import com.payslipmax.pdfparser.insights.timeline.PayMatrix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IncrementAuditorTest {
    private val auditor = IncrementAuditor()

    private fun basic(stage: Int) = PayMatrix.payAt(PayLevel.L11, stage)!!.toDouble()

    // Level 11: stage 7 until Jun 2018, stage 8 from the Jul 2018 DNI.
    private fun history(
        untilYear: Int,
        untilMonth: Int,
        stageAt: (year: Int, month: Int) -> Int,
    ) = payAuditMonths(2018, 1, (untilYear * 12 + untilMonth) - (2018 * 12 + 1) + 1) { y, m -> payAuditPayslip(y, m, basic(stageAt(y, m))) }

    private fun run(months: List<ParsedPayslip>) = auditor.audit(months.last(), months.getOrNull(months.size - 2), months.dropLast(1))

    @Test
    fun aMissedIncrementTwelveMonthsAfterTheLastIsFlaggedForTheStageDifference() {
        val months = history(2019, 7) { y, m -> if (y == 2018 && m < 7) 7 else 8 }
        val finding = run(months).single()
        assertEquals("SALARY_LOSS", finding.type)
        assertEquals(basic(9), finding.expected)
        assertEquals(basic(8), finding.actual)
        assertEquals(basic(9) - basic(8), finding.amount)
        assertEquals(PayAuthorities.ANNUAL_INCREMENT, finding.authority)
    }

    @Test
    fun anAppliedIncrementIsNotFlagged() {
        val months =
            history(2019, 7) { y, m ->
                if (y == 2018 && m < 7) {
                    7
                } else if (y == 2019 && m >= 7) {
                    9
                } else {
                    8
                }
            }
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun theMonthsBeforeTheAnniversaryAreNotFlagged() {
        val months = history(2019, 6) { y, m -> if (y == 2018 && m < 7) 7 else 8 }
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun aPromotionSinceTheLastIncrementResetsTheDate() {
        val months =
            history(2019, 7) { y, m -> if (y == 2018 && m < 7) 7 else 8 }.dropLast(3) +
                payAuditMonths(2019, 5, 3) { y, m -> payAuditPayslip(y, m, PayMatrix.payAt(PayLevel.L12A, 1)!!.toDouble()) }
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun anIncrementFirstSeenOutsideJanuaryAndJulyIsNotUsedAsTheDate() {
        // A late catch-up in September must not make the following September look overdue.
        val months = payAuditMonths(2018, 1, 21) { y, m -> payAuditPayslip(y, m, basic(if (y * 12 + m < 2018 * 12 + 9) 7 else 8)) }
        assertTrue(run(months).isEmpty())
    }

    @Test
    fun theLastStageOfALevelHasNoNextCell() {
        val top = PayMatrix.payAt(PayLevel.L11, 38)!!.toDouble()
        val prev = PayMatrix.payAt(PayLevel.L11, 37)!!.toDouble()
        val months = payAuditMonths(2018, 1, 19) { y, m -> payAuditPayslip(y, m, if (y * 12 + m < 2018 * 12 + 7) prev else top) }
        assertTrue(run(months).isEmpty())
    }
}
