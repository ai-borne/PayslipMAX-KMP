package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Lodging is the hotel bill, capped at the ceiling for the pay level (MoD letter 12630/Mov C/242/D(Mov)/2017, raised by the DA
 * step). The handbook makes the bill mandatory, so the officer types the nightly rate they expect to pay and the app never shows
 * more than the ceiling. At DA 50 to 99% the level 9 to 11 ceiling is Rs 2812.50, a half rupee: it stays exact in paise.
 */
class TdLodgingCalculatorTest {
    private val figures = SyntheticGuideFigures.figures

    private fun profile(
        level: String? = "13A",
        da: Int? = 60,
    ) = PreClaimProfile(level = level, basicPay = 150000, daPercent = da, mspAmount = null, npaAmount = null)

    private fun lodging(
        nights: Int?,
        nightly: Long?,
        days: Int = 100,
        profile: PreClaimProfile = profile(),
    ) = TdLodgingCalculator.estimate(figures, profile, days, nights, nightly)

    private fun unavailable(issue: PreClaimIssue) = Unavailable(PreClaimElement.LODGING, issue)

    @Test
    fun aBillUnderTheCeilingIsClaimedAsTyped() {
        val estimate = assertIs<LodgingEstimate>(lodging(nights = 5, nightly = 3000))

        assertEquals(15000L, estimate.rupees)
        assertFalse(estimate.capped)
        assertEquals(562_500L, estimate.ceilingPaise, "level 13A: Rs 4500 x 1.25 = Rs 5625 a night")
    }

    @Test
    fun aBillOverTheCeilingIsCappedAtTheCeilingNeverAboveIt() {
        val estimate = assertIs<LodgingEstimate>(lodging(nights = 5, nightly = 7000))

        assertEquals(28125L, estimate.rupees, "5 x Rs 5625, not 5 x Rs 7000")
        assertTrue(estimate.capped)
        assertEquals(7000L, estimate.claimedPerNight, "the typed bill stays on the line so the screen can say what was capped")
    }

    @Test
    fun theLevelNineToElevenCeilingIsTwoThousandEightHundredTwelveAndAHalfNotRoundedUp() {
        val one = assertIs<LodgingEstimate>(lodging(nights = 1, nightly = 3000, profile = profile(level = "11")))
        val two = assertIs<LodgingEstimate>(lodging(nights = 2, nightly = 3000, profile = profile(level = "11")))

        assertEquals(281_250L, one.ceilingPaise, "Rs 2812.50 exactly; rounding to 2813 would overstate the ceiling")
        assertEquals(2813L, one.rupees, "only the total is rounded, half up: 2812.50 becomes 2813")
        assertEquals(5625L, two.rupees, "two nights is a whole number, so the half rupee cannot have been rounded early")
    }

    @Test
    fun theCeilingMovesWithTheLevelAndTheDaStep() {
        fun ceiling(
            level: String,
            da: Int,
        ) = assertIs<LodgingEstimate>(lodging(1, 1, profile = profile(level, da))).ceilingPaise

        assertEquals(937_500L, ceiling("14", 60), "7500 x 1.25")
        assertEquals(450_000L, ceiling("12", 49), "no step below 50%")
        assertEquals(675_000L, ceiling("12", 100), "two steps: x 1.50")
    }

    @Test
    fun theOwnersTwoClaimsComeOutAsFiled() {
        val a = assertIs<LodgingEstimate>(lodging(57, 4500, days = 57, profile = profile(da = 46)))
        val b = assertIs<LodgingEstimate>(lodging(61, 1500, days = 61, profile = profile(da = 50)))

        assertEquals(256500L, a.rupees, "57 x Rs 4500, the level 12 to 13B ceiling before the step, not capped")
        assertFalse(a.capped)
        assertEquals(91500L, b.rupees, "61 x Rs 1500, well under the ceiling")
    }

    @Test
    fun noBillTypedWhateverTheRateIsNeverReachesPastTheCeiling() {
        val ceiling = assertIs<LodgingEstimate>(lodging(1, 1)).ceilingPaise
        for (rate in listOf(1L, 999L, 5624L, 5625L, 5626L, 10_000L, PreClaimLimits.MAX_NIGHTLY_RATE)) {
            val estimate = assertIs<LodgingEstimate>(lodging(nights = 7, nightly = rate))
            assertTrue(estimate.rupees <= (ceiling * 7 + 50) / 100, "rate $rate gave ${estimate.rupees}")
        }
    }

    @Test
    fun zeroNightsIsAValidClaimOfNothing() {
        assertEquals(0L, assertIs<LodgingEstimate>(lodging(nights = 0, nightly = 3000)).rupees)
    }

    @Test
    fun nightsAndTheRateMustBeInRange() {
        assertEquals(unavailable(PreClaimIssue.NIGHTS_MISSING), lodging(null, 3000))
        for (bad in listOf(-1, 101)) assertEquals(unavailable(PreClaimIssue.NIGHTS_OUT_OF_RANGE), lodging(bad, 3000, days = 100), "$bad nights in 100 days")
        assertEquals(unavailable(PreClaimIssue.NIGHTLY_RATE_MISSING), lodging(2, null))
        for (bad in listOf(0L, -1L, PreClaimLimits.MAX_NIGHTLY_RATE + 1)) {
            assertEquals(unavailable(PreClaimIssue.NIGHTLY_RATE_OUT_OF_RANGE), lodging(2, bad), "$bad")
        }
    }

    @Test
    fun missingOrUncoveredProfileValuesAndFiguresHideTheLine() {
        assertEquals(unavailable(PreClaimIssue.LEVEL_MISSING), lodging(2, 3000, profile = profile(level = null)))
        assertEquals(unavailable(PreClaimIssue.LEVEL_NOT_COVERED), lodging(2, 3000, profile = profile(level = "8")))
        assertEquals(unavailable(PreClaimIssue.DA_MISSING), lodging(2, 3000, profile = profile(da = null)))
        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), TdLodgingCalculator.estimate(null, profile(), 5, 2, 3000))
        assertEquals(unavailable(PreClaimIssue.DAYS_OUT_OF_RANGE), lodging(1, 3000, days = 0))
    }
}
