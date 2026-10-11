package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Own-car conveyance on a move is the distance times the Road Mileage Allowance of the state the journey starts in (PCDA(O)
 * Pune Wing Circular no. 10, 2024-09-03; owner instruction 2026-10-11). The state is spelt as the circular spells it, so a
 * picker built from the bundle's own table can never miss; a free-typed near miss ("Gujarat") must not silently price at nil.
 */
class CarConveyanceCalculatorTest {
    private val figures = SyntheticGuideFigures.figures

    private fun conveyance(
        state: String?,
        km: Int?,
    ) = CarConveyanceCalculator.estimate(figures, state, km)

    private fun unavailable(issue: PreClaimIssue) = Unavailable(PreClaimElement.CAR_CONVEYANCE, issue)

    @Test
    fun theAmountIsTheKmTimesTheRateOfTheOriginatingState() {
        assertEquals(1200L, assertIs<PerKmEstimate>(conveyance("Punjab", 100)).rupees, "Punjab Rs 12 a km")
        assertEquals(57620L, assertIs<PerKmEstimate>(conveyance("Maharashtra", 2881)).rupees, "Maharashtra Rs 20 a km")
        assertEquals(86430L, assertIs<PerKmEstimate>(conveyance("Assam", 2881)).rupees, "Assam Rs 30 a km, the top of the table")
    }

    @Test
    fun theRateIsKeptInPaiseSoItMatchesTheBaggageUnit() {
        val estimate = assertIs<PerKmEstimate>(conveyance("Gujrat", 10))

        assertEquals(1800L, estimate.ratePaise)
        assertEquals(PreClaimElement.CAR_CONVEYANCE, estimate.element)
    }

    @Test
    fun aStateNotInTheCircularIsRejectedNotPricedAtNil() {
        assertEquals(unavailable(PreClaimIssue.STATE_NOT_LISTED), conveyance("Gujarat", 10), "the circular spells it Gujrat")
        assertEquals(unavailable(PreClaimIssue.STATE_NOT_LISTED), conveyance("Lakshadweep", 10), "no Army presence, no rate")
        assertEquals(unavailable(PreClaimIssue.STATE_NOT_LISTED), conveyance("punjab", 10), "matched as spelt")
    }

    @Test
    fun noStateOrABlankOneIsMissing() {
        assertEquals(unavailable(PreClaimIssue.STATE_MISSING), conveyance(null, 10))
        assertEquals(unavailable(PreClaimIssue.STATE_MISSING), conveyance("  ", 10))
    }

    @Test
    fun theKmFollowsTheSameBoundsAsBaggage() {
        for (bad in listOf(0, -1, PreClaimLimits.MAX_ROAD_KM + 1)) {
            assertEquals(unavailable(PreClaimIssue.KM_OUT_OF_RANGE), conveyance("Assam", bad), "$bad")
        }
        assertEquals(unavailable(PreClaimIssue.KM_MISSING), conveyance("Assam", null))
    }

    @Test
    fun theEstimateNamesTheCircularFigureAndItsDate() {
        val estimate = assertIs<PerKmEstimate>(conveyance("Goa", 50))

        assertEquals("rma", estimate.source.figureId)
        assertEquals("2024-09-03", estimate.source.effectiveFrom)
        assertEquals(null, estimate.source.daPercent, "the circular's rate does not move with DA here")
    }

    @Test
    fun noFiguresMeansNoEstimate() {
        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), CarConveyanceCalculator.estimate(null, "Goa", 50))
        val without = figures.copy(figures = figures.figures - "rma")
        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), CarConveyanceCalculator.estimate(without, "Goa", 50))
    }
}
