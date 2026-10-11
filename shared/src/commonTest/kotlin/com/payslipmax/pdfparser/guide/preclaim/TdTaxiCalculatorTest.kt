package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Local conveyance on temporary duty is capped per day: a rupee cap for pay levels 9 to 11 (Rs 338, Rs 422.50 at DA 50 to 99%),
 * and for 12 and above only a rule in words (a 50 km a day limit, or "at actual"), with no rupee cap. The app must therefore show
 * a number only for levels 9 to 11 and never invent one for the rest (register 23, owner approved 2026-10-11).
 */
class TdTaxiCalculatorTest {
    private val figures = SyntheticGuideFigures.figures

    private fun profile(
        level: String? = "11",
        da: Int? = 60,
    ) = PreClaimProfile(level = level, basicPay = 90000, daPercent = da, mspAmount = null, npaAmount = null)

    private fun taxi(
        days: Int,
        profile: PreClaimProfile = profile(),
    ) = TdTaxiCalculator.estimate(figures, profile, days)

    @Test
    fun theLevelNineToElevenCapAtDaFiftyIsFourHundredTwentyTwoFiftyAndStaysExact() {
        val estimate = assertIs<PerDayEstimate>(taxi(1))

        assertEquals(42_250L, estimate.ratePaise, "Rs 422.50, not 422 or 423")
        assertEquals(423L, estimate.rupees, "one day's cap, the total rounded half up")
        assertEquals(PreClaimElement.LOCAL_TAXI, estimate.element)
    }

    @Test
    fun theCapIsPerDayAndTheTotalIsRoundedOnceOverTheDays() {
        assertEquals(1268L, assertIs<PerDayEstimate>(taxi(3)).rupees, "3 x 422.50 = 1267.50")
        assertEquals(4225L, assertIs<PerDayEstimate>(taxi(10)).rupees)
        assertEquals(33_800L, assertIs<PerDayEstimate>(taxi(1, profile(da = 40))).ratePaise, "no step below DA 50%")
    }

    @Test
    fun levelsTwelveAndAboveGetTheRuleInWordsAndNoRupeeFigure() {
        val twelve = assertIs<LimitNote>(taxi(5, profile(level = "12A")))
        val fourteen = assertIs<LimitNote>(taxi(5, profile(level = "14")))

        assertEquals("AC taxi, up to 50 km a day within the city", twelve.text)
        assertEquals("AC taxi at actual cost, in line with official engagements, within the city", fourteen.text)
        assertEquals(PreClaimElement.LOCAL_TAXI, twelve.element)
        assertEquals("td_taxi", twelve.source.figureId)
    }

    @Test
    fun aLevelTheFigureDoesNotCoverAndMissingInputsHideTheLine() {
        fun unavailable(issue: PreClaimIssue) = Unavailable(PreClaimElement.LOCAL_TAXI, issue)

        assertEquals(unavailable(PreClaimIssue.LEVEL_NOT_COVERED), taxi(2, profile(level = "8")))
        assertEquals(unavailable(PreClaimIssue.LEVEL_MISSING), taxi(2, profile(level = null)))
        assertEquals(unavailable(PreClaimIssue.DA_MISSING), taxi(2, profile(da = null)))
        assertEquals(unavailable(PreClaimIssue.DAYS_OUT_OF_RANGE), taxi(0))
        assertEquals(unavailable(PreClaimIssue.FIGURE_MISSING), TdTaxiCalculator.estimate(null, profile(), 2))
    }
}
