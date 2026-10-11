package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideFigure
import com.payslipmax.pdfparser.guide.model.GuideTimeShare
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The food shares of a same-day trip decide a rupee amount on a pre-claim, so a bad table must stop at load, not become a wrong
 * estimate: a gap at the start would leave some durations with no share, a falling share would pay less for a longer trip.
 * `figures.py` enforced the same shape when the bundle was built.
 */
class GuideTimeShareValidationTest {
    private val valid: GuideBundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle
    private val food: GuideFigure = SyntheticGuideFigures.figures.figures.getValue("td_food_hours")

    private fun problems(shares: List<GuideTimeShare>): List<String> {
        val figures = SyntheticGuideFigures.figures
        return GuideBundleValidator.validate(valid.copy(figures = figures.copy(figures = mapOf("td_food_hours" to food.copy(timeShares = shares)))))
    }

    private fun share(
        from: Int,
        percent: Int,
    ) = GuideTimeShare(from, percent)

    @Test
    fun theShippedSharesAreThirtySeventyAndHundredPercentAtSixAndOverTwelveHours() {
        assertEquals(listOf(0 to 30, 360 to 70, 721 to 100), food.timeShares.map { it.fromMinutes to it.percent })
        assertEquals(emptyList(), problems(food.timeShares))
    }

    @Test
    fun aTableThatDoesNotStartAtZeroLeavesAGapAndIsRejected() {
        val found = problems(listOf(share(30, 30), share(360, 70), share(721, 100)))

        assertTrue(found.any { "figure td_food_hours" in it && "from_minutes 0" in it }, "$found")
    }

    @Test
    fun startsThatDoNotStrictlyAscendAreRejected() {
        for (bad in listOf(listOf(share(0, 30), share(0, 70), share(721, 100)), listOf(share(0, 30), share(721, 70), share(360, 100)))) {
            assertTrue(problems(bad).any { "ascend" in it }, "$bad")
        }
    }

    @Test
    fun aShareThatFallsAsTheTimeGrowsIsRejected() {
        assertTrue(problems(listOf(share(0, 70), share(360, 30), share(721, 100))).any { "must not fall" in it })
    }

    @Test
    fun theLastShareMustBeTheFullDay() {
        assertTrue(problems(listOf(share(0, 30), share(360, 70))).any { "full day" in it })
    }

    @Test
    fun aPercentOutsideOneToHundredOrANegativeStartIsRejected() {
        for (bad in listOf(listOf(share(0, 0), share(721, 100)), listOf(share(0, 101)), listOf(share(-1, 30), share(721, 100)))) {
            assertTrue(problems(bad).any { "figure td_food_hours" in it }, "$bad")
        }
    }

    @Test
    fun aTimeShareFigureWithNoSharesIsRejected() {
        assertTrue(problems(emptyList()).any { "no time_shares" in it })
    }
}
