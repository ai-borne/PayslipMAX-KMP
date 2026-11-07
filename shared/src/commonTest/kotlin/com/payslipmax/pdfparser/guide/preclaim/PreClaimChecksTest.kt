package com.payslipmax.pdfparser.guide.preclaim

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: every pre-claim figure is a promise about rupees an officer will plan around. Bounds and rounding live only here, so a change to
 * either silently moves every calculator at once. Pinned at the edges: the first value in and out of each bound, and the half-rupee.
 */
class PreClaimChecksTest {
    @Test
    fun aRoadDistanceIsValidFromOneKmToTheBoundAndMissingIsItsOwnIssue() {
        assertEquals(PreClaimIssue.KM_MISSING, PreClaimChecks.kmIssue(null))
        assertEquals(PreClaimIssue.KM_OUT_OF_RANGE, PreClaimChecks.kmIssue(0))
        assertNull(PreClaimChecks.kmIssue(1))
        assertNull(PreClaimChecks.kmIssue(PreClaimLimits.MAX_ROAD_KM))
        assertEquals(PreClaimIssue.KM_OUT_OF_RANGE, PreClaimChecks.kmIssue(PreClaimLimits.MAX_ROAD_KM + 1))
    }

    @Test
    fun daysOfDutyAreValidFromOneToTheBound() {
        assertEquals(PreClaimIssue.DAYS_OUT_OF_RANGE, PreClaimChecks.daysIssue(0))
        assertNull(PreClaimChecks.daysIssue(1))
        assertNull(PreClaimChecks.daysIssue(PreClaimLimits.MAX_DUTY_DAYS))
        assertEquals(PreClaimIssue.DAYS_OUT_OF_RANGE, PreClaimChecks.daysIssue(PreClaimLimits.MAX_DUTY_DAYS + 1))
    }

    @Test
    fun aTotalRoundsHalfARupeeUp() {
        assertEquals(0L, PreClaimChecks.rupees(49L))
        assertEquals(1L, PreClaimChecks.rupees(50L))
        assertEquals(1L, PreClaimChecks.rupees(149L))
        assertEquals(2L, PreClaimChecks.rupees(150L))
    }

    @Test
    fun aShareOfATotalIsRoundedOnceOnTheShareNotOnTheTotal() {
        assertEquals(0L, PreClaimChecks.rupees(99L, 50), "49.5 paise is under half a rupee")
        assertEquals(1L, PreClaimChecks.rupees(100L, 50), "exactly 50 paise rounds up")
        assertEquals(5L, PreClaimChecks.rupees(1001L, 50), "500.5 paise is 5.005 rupees")
    }

    @Test
    fun aFullShareEqualsTheWholeTotal() {
        listOf(0L, 49L, 50L, 12_345L, 99_999L).forEach {
            assertEquals(PreClaimChecks.rupees(it), PreClaimChecks.rupees(it, PreClaimChecks.FULL_SHARE_PERCENT), "paise=$it")
        }
        assertEquals(100L, PreClaimChecks.RUPEE_IN_PAISE)
    }
}
