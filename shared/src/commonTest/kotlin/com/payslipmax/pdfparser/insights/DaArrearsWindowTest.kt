package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY (907b42ac): a DA rise is effective on 1 Jan or 1 Jul but is paid on whichever payslip follows the order, so arrears can cover
 * the months since ANY of the last three effective dates. The audit used to assume the payslip's own half-year, which flagged correctly
 * paid arrears as short. These tests pin the window and the "nearest amount decides" rule that fixed it.
 */
class DaArrearsWindowTest {
    @Test
    fun theMonthAfterAnEffectiveDateCanBePayingOneSevenOrThirteenMonths() {
        assertEquals(listOf(1, 7, 13), DaArrearsWindow.candidateMonths(2))
        assertEquals(listOf(1, 7, 13), DaArrearsWindow.candidateMonths(8))
    }

    @Test
    fun theEffectiveMonthItselfOwesNothingSoThatCandidateIsLeftOut() {
        assertEquals(listOf(6, 12), DaArrearsWindow.candidateMonths(1))
        assertEquals(listOf(6, 12), DaArrearsWindow.candidateMonths(7))
    }

    @Test
    fun theLastMonthOfAHalfYearOwesFiveElevenOrSeventeen() {
        assertEquals(listOf(5, 11, 17), DaArrearsWindow.candidateMonths(6))
        assertEquals(listOf(5, 11, 17), DaArrearsWindow.candidateMonths(12))
    }

    @Test
    fun minimumIsTheFewestMonthsOwed() {
        assertEquals(1, DaArrearsWindow.minimumMonths(2))
        assertEquals(6, DaArrearsWindow.minimumMonths(1))
    }

    @Test
    fun theAmountPaidPicksTheEffectiveDateEvenWhenItIsInAnEarlierHalfYear() {
        // February paying seven months of 1,000 is arrears from the previous July, not a mis-count of one month.
        assertEquals(7, DaArrearsWindow.closestMonths(2, actual = 7_000.0, perMonth = 1_000.0))
        assertEquals(13, DaArrearsWindow.closestMonths(2, actual = 12_800.0, perMonth = 1_000.0))
        assertEquals(1, DaArrearsWindow.closestMonths(2, actual = 1_100.0, perMonth = 1_000.0))
    }

    @Test
    fun anExactTieGoesToTheFewerMonthsSoAShortfallIsNeverOverstated() {
        // 4,000 is equally far from 1 month (1,000) and 7 months (7,000).
        assertEquals(1, DaArrearsWindow.closestMonths(2, actual = 4_000.0, perMonth = 1_000.0))
    }

    @Test
    fun noArrearsPaidReadsAsTheFewestMonths() {
        assertEquals(1, DaArrearsWindow.closestMonths(2, actual = 0.0, perMonth = 1_000.0))
    }
}
