package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Food on temporary duty is the pay level's daily lump sum (MoD letter 12630/Mov C/242/D(Mov)/2017), raised 25% for every 50% of
 * DA. Owner decision 2026-10-11: every day of a multi-day move is paid in full; the 30% / 70% / 100% share by time away applies to
 * a trip that starts and ends the same day. The 2014 Travel Regulations say nil under six hours; the 7th CPC letter replaced that
 * with 30% (register 23), so 30% is what the tests pin.
 */
class TdFoodCalculatorTest {
    private val figures = SyntheticGuideFigures.figures

    private fun profile(
        level: String? = "13A",
        da: Int? = 60,
    ) = PreClaimProfile(level = level, basicPay = 150000, daPercent = da, mspAmount = null, npaAmount = null)

    private fun food(
        days: Int,
        minutes: Int? = null,
        profile: PreClaimProfile = profile(),
    ) = TdFoodCalculator.estimate(figures, profile, days, minutes)

    private fun unavailable(issue: PreClaimIssue) = Unavailable(PreClaimElement.FOOD, issue)

    @Test
    fun theOwnersClaimAIsFiftySevenDaysAtTheUnescalatedLevelThirteenRate() {
        val estimate = assertIs<PerDayEstimate>(food(57, profile = profile(da = 46)))

        assertEquals(57000L, estimate.rupees, "57 x Rs 1000, as filed on 26-01-2024 at DA 46%")
        assertEquals(100_000L, estimate.ratePaise)
        assertEquals(100, estimate.sharePercent)
    }

    @Test
    fun atDaFiftyTheRateRisesATwentyFifthAndTheOwnersClaimBHasASixtyOneDayCeiling() {
        val estimate = assertIs<PerDayEstimate>(food(61))

        assertEquals(125_000L, estimate.ratePaise)
        assertEquals(76250L, estimate.rupees, "61 x Rs 1250; the owner claimed less on purpose, this is the most the rule allows")
    }

    @Test
    fun theLumpSumFollowsThePayLevelAndTheDaSteps() {
        assertEquals(112_500L, assertIs<PerDayEstimate>(food(1, 800, profile(level = "11"))).ratePaise, "Rs 900 x 1.25")
        assertEquals(150_000L, assertIs<PerDayEstimate>(food(2, null, profile(level = "14"))).ratePaise, "Rs 1200 x 1.25")
        assertEquals(180_000L, assertIs<PerDayEstimate>(food(2, null, profile(level = "14", da = 100))).ratePaise, "two steps: x 1.50")
        assertEquals(120_000L, assertIs<PerDayEstimate>(food(2, null, profile(level = "14", da = 49))).ratePaise, "no step below 50%")
    }

    @Test
    fun aMultiDayMoveIgnoresTheHoursAndPaysEveryDayInFull() {
        assertEquals(3750L, assertIs<PerDayEstimate>(food(3, minutes = null)).rupees)
        assertEquals(3750L, assertIs<PerDayEstimate>(food(3, minutes = 60)).rupees, "owner decision 3: the share is for a same-day trip only")
    }

    @Test
    fun aSameDayTripPaysThirtySeventyOrHundredPercentByTheTimeAway() {
        fun rupees(minutes: Int) = assertIs<PerDayEstimate>(food(1, minutes)).rupees

        assertEquals(375L, rupees(1), "30% of Rs 1250, not nil, under six hours")
        assertEquals(375L, rupees(359))
        assertEquals(875L, rupees(360), "exactly six hours is in the 6 to 12 share")
        assertEquals(875L, rupees(720), "exactly twelve hours is not over twelve")
        assertEquals(1250L, rupees(721), "twelve hours and a minute is over twelve")
        assertEquals(1250L, rupees(1440))
    }

    @Test
    fun aShareOfTheRateIsRoundedOnceOnTheTotal() {
        val thirty = assertIs<PerDayEstimate>(food(1, 100, profile(level = "11")))
        val seventy = assertIs<PerDayEstimate>(food(1, 400, profile(level = "11")))

        assertEquals(338L, thirty.rupees, "30% of Rs 1125 is 337.50")
        assertEquals(788L, seventy.rupees, "70% of Rs 1125 is 787.50")
        assertEquals(112_500L to 30, thirty.ratePaise to thirty.sharePercent, "the rate stays exact; only the total is rounded")
    }

    @Test
    fun aSameDayTripNeedsTheTimeAwayInRange() {
        assertEquals(unavailable(PreClaimIssue.MINUTES_MISSING), food(1, null))
        for (bad in listOf(0, -5, 1441)) assertEquals(unavailable(PreClaimIssue.MINUTES_OUT_OF_RANGE), food(1, bad), "$bad")
    }

    @Test
    fun daysMustBeFromOneToTheBound() {
        for (bad in listOf(0, -1, PreClaimLimits.MAX_DUTY_DAYS + 1)) {
            assertEquals(unavailable(PreClaimIssue.DAYS_OUT_OF_RANGE), food(bad, 60), "$bad")
        }
        assertIs<PerDayEstimate>(food(PreClaimLimits.MAX_DUTY_DAYS))
    }

    @Test
    fun missingOrUncoveredProfileValuesHideTheLineInsteadOfGuessing() {
        assertEquals(unavailable(PreClaimIssue.LEVEL_MISSING), food(3, profile = profile(level = null)))
        assertEquals(unavailable(PreClaimIssue.LEVEL_NOT_COVERED), food(3, profile = profile(level = "8")))
        assertEquals(unavailable(PreClaimIssue.DA_MISSING), food(3, profile = profile(da = null)))
        assertEquals(unavailable(PreClaimIssue.DA_OUT_OF_RANGE), food(3, profile = profile(da = PreClaimLimits.MAX_DA_PERCENT + 1)))
        assertEquals(unavailable(PreClaimIssue.DA_OUT_OF_RANGE), food(3, profile = profile(da = -1)))
    }

    @Test
    fun withoutTheFiguresTheLineIsUnavailableAndWithoutTheSharesOnlyASameDayTripIs() {
        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), TdFoodCalculator.estimate(null, profile(), 3, null))
        val noShares = figures.copy(figures = figures.figures - "td_food_hours")

        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), TdFoodCalculator.estimate(noShares, profile(), 1, 60))
        assertIs<PerDayEstimate>(TdFoodCalculator.estimate(noShares, profile(), 3, null), "a multi-day move needs no share")
    }

    @Test
    fun theLineNamesItsFigureAndTheDaItWasWorkedAt() {
        val source = assertIs<PerDayEstimate>(food(3)).source

        assertEquals("food_rate", source.figureId)
        assertEquals(60, source.daPercent)
        assertEquals("2017-07-01", source.effectiveFrom)
    }
}
