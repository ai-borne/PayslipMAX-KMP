package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Baggage by road is Rs 50 a km for pay levels 10 and above, raised 25% for every 50% of DA (Handbook on Travelling
 * Allowances 2023 with MoD letter 12630/Mov C/242/D(Mov)/2017 para 2(iv); owner decision F2). At DA 50-99% that is Rs 62.50 a
 * km, which is a half rupee: the rate is kept exact in paise and only the total is rounded to a rupee.
 */
class RoadBaggageCalculatorTest {
    private val figures = SyntheticGuideFigures.figures

    private fun profile(
        level: String? = "14",
        da: Int? = 60,
    ) = PreClaimProfile(level = level, basicPay = 149000, daPercent = da, mspAmount = null, npaAmount = null)

    private fun baggage(
        km: Int?,
        profile: PreClaimProfile = profile(),
    ) = RoadBaggageCalculator.estimate(figures, profile, km)

    private fun unavailable(issue: PreClaimIssue) = Unavailable(PreClaimElement.BAGGAGE_ROAD, issue)

    @Test
    fun atDa60TheRateIsSixtyTwoFiftyAKmAndTheTotalIsRoundedOnce() {
        val estimate = assertIs<PerKmEstimate>(baggage(2881))

        assertEquals(6250L, estimate.ratePaise, "Rs 62.50, not rounded to 62 or 63")
        assertEquals(180063L, estimate.rupees, "2881 x 62.50 = 180062.50 rounds half up")
        assertEquals(2881, estimate.km)
    }

    @Test
    fun theRateStepsExactlyWhenDaReachesEachMultipleOf50() {
        assertEquals(5000L, assertIs<PerKmEstimate>(baggage(100, profile(da = 49))).ratePaise, "below 50% the base applies")
        assertEquals(6250L, assertIs<PerKmEstimate>(baggage(100, profile(da = 50))).ratePaise)
        assertEquals(6250L, assertIs<PerKmEstimate>(baggage(100, profile(da = 99))).ratePaise)
        assertEquals(7500L, assertIs<PerKmEstimate>(baggage(100, profile(da = 100))).ratePaise, "two steps: base x 1.50")
        assertEquals(5000L, assertIs<PerKmEstimate>(baggage(100, profile(da = 0))).rupees, "100 km at Rs 50")
    }

    @Test
    fun everyLevelFromTenUpIsCoveredAndLevelNineIsNot() {
        for (level in listOf("10", "10A", "10B", "11", "12", "13B", "14", "18")) {
            assertIs<PerKmEstimate>(baggage(10, profile(level = level)), level)
        }
        assertEquals(unavailable(PreClaimIssue.LEVEL_NOT_COVERED), baggage(10, profile(level = "9")))
    }

    @Test
    fun theKmMustBeAWholeNumberFromOneToTheBound() {
        for (bad in listOf(0, -1, -2881, PreClaimLimits.MAX_ROAD_KM + 1)) {
            assertEquals(unavailable(PreClaimIssue.KM_OUT_OF_RANGE), baggage(bad), "$bad")
        }
        assertEquals(unavailable(PreClaimIssue.KM_MISSING), baggage(null))
        assertEquals(6250L, assertIs<PerKmEstimate>(baggage(1)).ratePaise)
        assertEquals(625_000L, assertIs<PerKmEstimate>(baggage(PreClaimLimits.MAX_ROAD_KM)).rupees)
    }

    @Test
    fun missingOrImpossibleLevelAndDaGiveNoEstimate() {
        assertEquals(unavailable(PreClaimIssue.LEVEL_MISSING), baggage(10, profile(level = null)))
        assertEquals(unavailable(PreClaimIssue.DA_MISSING), baggage(10, profile(da = null)))
        assertEquals(unavailable(PreClaimIssue.DA_OUT_OF_RANGE), baggage(10, profile(da = -1)))
        assertEquals(unavailable(PreClaimIssue.DA_OUT_OF_RANGE), baggage(10, profile(da = PreClaimLimits.MAX_DA_PERCENT + 1)))
    }

    @Test
    fun noFiguresMeansNoEstimate() {
        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), RoadBaggageCalculator.estimate(null, profile(), 10))
        val without = figures.copy(figures = figures.figures - "baggage_road")
        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), RoadBaggageCalculator.estimate(without, profile(), 10))
    }

    @Test
    fun theEstimateNamesTheFigureTheDateAndTheDaItUsed() {
        val estimate = assertIs<PerKmEstimate>(baggage(100))

        assertEquals(PreClaimElement.BAGGAGE_ROAD, estimate.element)
        assertEquals("baggage_road", estimate.source.figureId)
        assertEquals("2017-07-01", estimate.source.effectiveFrom)
        assertEquals(60, estimate.source.daPercent)
        assertEquals(figures.figures.getValue("baggage_road").assumption, estimate.source.assumption)
    }
}
