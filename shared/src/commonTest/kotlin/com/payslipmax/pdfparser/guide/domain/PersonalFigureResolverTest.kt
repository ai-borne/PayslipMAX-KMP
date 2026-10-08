package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * The "your figure" line states a rupee amount to an officer who may quote it in a claim, so a wrong formula is a
 * wrong claim. Each rule below is stated from the letter it implements (expected amounts are the oracle; production
 * code holds none). Missing or unlisted data must hide the line, never guess: the card is complete without it.
 */
class PersonalFigureResolverTest {
    private val figures = SyntheticGuideFigures.figures

    private fun profile(
        level: String? = "14",
        basic: Double? = 144200.0,
        da: Int? = 60,
        tpta: String? = null,
        hra: Double? = null,
    ) = GuideProfile(2026, 6, level, basic, da, tpta, hra)

    private fun resolve(
        card: String,
        profile: GuideProfile?,
    ) = PersonalFigureResolver.resolve(card, figures, profile)

    // Food rate: base by level, +25% each time DA rises by 50 (MoD 15-09-2017, para 2(iv)); the FAQ prints 1,125 / 1,250 / 1,500.
    private fun food(
        level: String,
        da: Int?,
    ) = (resolve(SyntheticGuideFigures.FOOD_CARD, profile(level = level, da = da)) as? PersonalFigure.FoodRate)?.amount

    @Test
    fun foodRateAtDa60MatchesTheFaqFiguresForEveryListedLevel() {
        assertEquals(1125L, food("11", 60))
        assertEquals(1125L, food("10B", 60))
        assertEquals(1250L, food("12A", 60))
        assertEquals(1250L, food("13A", 60))
        assertEquals(1500L, food("14", 60))
        assertEquals(1500L, food("18", 60))
    }

    @Test
    fun foodRateStepsExactlyWhenDaReachesEachMultipleOf50() {
        assertEquals(1200L, food("14", 49), "below 50% the base applies")
        assertEquals(1500L, food("14", 50), "the step lands at 50%, as it did on 01-01-2024")
        assertEquals(1500L, food("14", 58), "58% is still one step")
        assertEquals(1500L, food("14", 99))
        assertEquals(1800L, food("14", 100), "two steps: base x 1.50")
    }

    @Test
    fun foodRateShowsNothingBelowTheListedLevelsOrWithoutLevelOrDa() {
        assertNull(food("8", 60), "the card lists no rate below level 9")
        assertNull(resolve(SyntheticGuideFigures.FOOD_CARD, profile(level = null)))
        assertNull(food("14", null), "no payslip DA means no line")
    }

    @Test
    fun foodRateCarriesItsBasisSoTheLineCanSayWhereItCameFrom() {
        val figure = assertIs<PersonalFigure.FoodRate>(resolve(SyntheticGuideFigures.FOOD_CARD, profile(level = "12A", da = 58)))
        assertEquals(1000L, figure.base)
        assertEquals(25, figure.stepPercent)
        assertEquals("2017-07-01", figure.basis.effectiveFrom)
        assertEquals(58, figure.basis.daPercent)
        assertEquals(2026 to 6, figure.basis.year to figure.basis.month)
        assertEquals("full day (over 12 hours away), before taxes", figure.basis.assumption)
    }

    // CTG: 80% of the last month's basic pay (MoD 15-09-2017, para 3(ii)(a)); needs no DA.
    @Test
    fun ctgIsEightyPercentOfBasicAndNeedsNoDa() {
        val figure = assertIs<PersonalFigure.Ctg>(resolve(SyntheticGuideFigures.CTG_CARD, profile(basic = 144200.0, da = null)))
        assertEquals(115360L, figure.amount)
        assertEquals(80, figure.percent)
    }

    @Test
    fun ctgIsHiddenWithoutABasicPay() {
        assertNull(resolve(SyntheticGuideFigures.CTG_CARD, profile(basic = null)))
        assertNull(resolve(SyntheticGuideFigures.CTG_CARD, profile(basic = 0.0)))
    }

    @Test
    fun ctgRoundsToTheNearestRupee() {
        val figure = assertIs<PersonalFigure.Ctg>(resolve(SyntheticGuideFigures.CTG_CARD, profile(basic = 121203.0)))
        assertEquals(96962L, figure.amount, "80% of 121,203 is 96,962.4")
    }

    // Transport allowance: base plus DA on it, no 25% step (same shape as TptaEntitlementAuditor).
    private fun transport(
        level: String?,
        tpta: String?,
        da: Int? = 60,
    ) = (resolve(SyntheticGuideFigures.TRANSPORT_CARD, profile(level = level, tpta = tpta, da = da)) as? PersonalFigure.Transport)?.amount

    @Test
    fun transportIsBasePlusDaOnTheBaseForTheOfficersCityClass() {
        assertEquals(11520L, transport("12A", "HIGHER"), "7,200 + 60% DA")
        assertEquals(5760L, transport("12A", "OTHER"), "3,600 + 60% DA")
        assertEquals(7200L, transport("11", "HIGHER", da = 0))
        assertEquals(11520L, transport("13A", "HIGHER", da = 60))
    }

    @Test
    fun transportAtLevel14AndAboveIsFlatWhateverTheCity() {
        assertEquals(25200L, transport("14", null), "15,750 + 60% DA, no class needed")
        assertEquals(25200L, transport("16", "OTHER"))
    }

    @Test
    fun transportIsHiddenWhenTheClassOrDaIsUnknown() {
        assertNull(transport("12A", null), "levels 10-13A need the HIGHER / OTHER class")
        assertNull(transport("12A", "HIGHER", da = null))
        assertNull(transport(null, "HIGHER"))
    }

    // HRA: X 24/27/30, Y 16/18/20, Z 8/9/10 by DA step; the class is read from the payslip's HRA / basic ratio.
    private fun hra(
        hra: Double?,
        da: Int?,
        basic: Double? = 100000.0,
    ) = resolve(SyntheticGuideFigures.HRA_CARD, profile(basic = basic, da = da, hra = hra)) as? PersonalFigure.Hra

    @Test
    fun hraClassIsInferredFromTheRatioAtTheCurrentDaStep() {
        assertEquals("X" to 30, hra(30000.0, 60)?.let { it.hraClass to it.percent })
        assertEquals("Y" to 20, hra(20000.0, 60)?.let { it.hraClass to it.percent })
        assertEquals("Z" to 10, hra(10000.0, 60)?.let { it.hraClass to it.percent })
    }

    @Test
    fun hraFollowsTheRateStepsAndShowsTheDateOfTheStepInForce() {
        assertEquals(24 to "2017-07-01", hra(24000.0, 20)?.let { it.percent to it.basis.effectiveFrom })
        assertEquals(27 to "2021-07-01", hra(27000.0, 28)?.let { it.percent to it.basis.effectiveFrom })
        assertEquals(30 to "2024-01-01", hra(30000.0, 50)?.let { it.percent to it.basis.effectiveFrom }, "the 30% step is at DA 50, inclusive")
        assertEquals(27, hra(27000.0, 49)?.percent)
    }

    @Test
    fun hraIsHiddenWhenTheRatioFitsNoClass() {
        assertNull(hra(25000.0, 60), "25% is no HRA rate")
        assertNull(hra(0.0, 60))
        assertNull(hra(null, 60), "no HRA on the payslip (Government quarters)")
    }

    @Test
    fun hraIsHiddenWhenTheRateIsNotTheRateForThatClassAtThePayslipDa() {
        // 24% is a valid X rate, but at DA 60% class X is 30%. That mismatch is Pay Audit's finding, not the Guide's.
        assertNull(hra(24000.0, 60))
        assertNull(hra(30000.0, 20), "30% is not yet in force at DA 20%")
    }

    @Test
    fun hraAllowsForRupeeRoundingOnARealBasic() {
        assertEquals("X", hra(36930.0, 60, basic = 123100.0)?.hraClass)
        assertEquals("X", hra(36931.0, 60, basic = 123100.0)?.hraClass, "a rupee of rounding is within tolerance")
    }

    @Test
    fun hraIsHiddenWithoutDaOrBasic() {
        assertNull(hra(30000.0, null))
        assertNull(hra(30000.0, 60, basic = null))
    }

    // Shared behaviour.
    @Test
    fun noProfileNoFiguresOrAnUnknownCardShowsNothing() {
        assertNull(resolve(SyntheticGuideFigures.FOOD_CARD, null))
        assertNull(PersonalFigureResolver.resolve(SyntheticGuideFigures.FOOD_CARD, null, profile()))
        assertNull(resolve("RB-NOT-A-FIGURE-CARD", profile()))
    }

    @Test
    fun aModeThisAppVersionDoesNotKnowIsHiddenNotGuessed() {
        val future = figures.copy(figures = figures.figures.mapValues { (_, f) -> f.copy(mode = "compound_da") })
        assertNull(PersonalFigureResolver.resolve(SyntheticGuideFigures.FOOD_CARD, future, profile()))
    }
}
