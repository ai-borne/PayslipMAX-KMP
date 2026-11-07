package com.payslipmax.pdfparser.guide.summary

import com.payslipmax.pdfparser.guide.model.GuideFigures
import com.payslipmax.pdfparser.guide.model.GuideTimeShare
import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * The TD summary card's numbers (plan 24, P2). Why they matter: every officer reads a hotel, taxi, food and CEA amount off this card
 * and may claim it, so each must be the approved base raised by the one DA-step rule, exact to the paisa, and must move on when DA
 * crosses a multiple of 50, not before and not after. Amounts are paise: Rs 2,812.50 is 281250.
 */
class TdSummaryBuilderTest {
    private val figures: GuideFigures = SyntheticGuideFigures.figures

    private fun at(da: Int): TdSummary = assertNotNull(TdSummaryBuilder.build(figures, da), "a summary at DA $da")

    private fun TdSummary.group(level: String) = assertNotNull(groupFor(level), "a group for level $level")

    @Test
    fun atDa60EveryBandShowsTheRatesPcdaoPrints() {
        val low = at(60).group("11")
        val mid = at(60).group("12A")
        val high = at(60).group("14")
        // Hotel ceiling a night: base 2250 / 4500 / 7500 raised 25%.
        assertEquals(listOf(281250L, 562500L, 937500L), listOf(low, mid, high).map { it.hotelPaise })
        // Food a day, the FAQ's own figures: 1125 / 1250 / 1500.
        assertEquals(listOf(112500L, 125000L, 150000L), listOf(low, mid, high).map { it.foodPaise })
        // Non-AC taxi for levels 9 to 11: 338 raised 25% is Rs 422.50.
        assertEquals(TdTaxi.Amount(42250L), low.taxi)
    }

    @Test
    fun taxiAboveLevel11IsAWordedLimitNeverAnInventedAmount() {
        for (level in listOf("12", "13B", "14", "18")) {
            assertIs<TdTaxi.Limit>(at(60).group(level).taxi, "level $level has a limit text, not a rupee cap")
        }
    }

    @Test
    fun groupsRunFromTheLowestLevelUpAndKeepEveryPrintedLevel() {
        val groups = at(60).levelGroups
        assertEquals(listOf("9", "12", "14"), groups.map { it.levels.first() })
        assertEquals(listOf("14", "15", "16", "17", "18"), groups.last().levels)
        assertEquals(listOf("12", "12A", "12B", "13", "13A", "13B"), groups[1].levels)
    }

    @Test
    fun foodByTimeAwayIsTheSharesOfTheDaysAmount() {
        val summary = at(60)
        assertEquals(listOf(0 to 30, 360 to 70, 721 to 100), summary.foodTimings.map { it.fromMinutes to it.percent })
        // Level 12A: 1250 a day, so 30% / 70% / 100% is 375 / 875 / 1250.
        assertEquals(listOf(37500L, 87500L, 125000L), summary.group("12A").foodByTimingPaise)
    }

    @Test
    fun theStepMovesExactlyAtEachMultipleOfFiftyOfDa() {
        // Hotel for levels 9 to 11, base Rs 2,250: x1.00 below DA 50, x1.25 from 50 to 99, x1.50 from 100, x1.75 from 150.
        val hotelAt = { da: Int -> at(da).group("10").hotelPaise }
        assertEquals(225000L, hotelAt(0))
        assertEquals(225000L, hotelAt(49))
        assertEquals(281250L, hotelAt(50))
        assertEquals(281250L, hotelAt(63))
        assertEquals(281250L, hotelAt(99))
        assertEquals(337500L, hotelAt(100))
        assertEquals(393750L, hotelAt(150))
    }

    @Test
    fun theStepAppliesToFoodAndTaxiAlike() {
        val s = at(100)
        assertEquals(135000L, s.group("11").foodPaise)
        assertEquals(TdTaxi.Amount(50700L), s.group("11").taxi)
    }

    @Test
    fun theDaBandNamesTheStepTheTableIsDrawnAt() {
        // The card says "DA 50 to 99%", never a DA number that may not match the officer's payslip yet.
        assertEquals(50 to 99, at(60).let { it.daBandFrom to it.daBandTo })
        assertEquals(50 to 99, at(63).let { it.daBandFrom to it.daBandTo })
        assertEquals(0 to 49, at(49).let { it.daBandFrom to it.daBandTo })
        assertEquals(100 to 149, at(100).let { it.daBandFrom to it.daBandTo })
        assertEquals(25, at(60).stepPercent)
    }

    @Test
    fun aLevelWithNoBandHasNoGroupSoNoRowIsHighlightedByGuessing() {
        assertNull(at(60).groupFor("5"))
        assertNull(at(60).groupFor(""))
        assertNull(at(60).groupFor("99"))
    }

    @Test
    fun noFiguresOrANegativeDaShowsNothingRatherThanAGuess() {
        assertNull(TdSummaryBuilder.build(null, 60))
        assertNull(TdSummaryBuilder.build(figures, -1))
    }

    @Test
    fun aMissingFigureHidesOnlyItsOwnCell() {
        val withoutFood = figures.copy(figures = figures.figures - "food_rate")
        val summary = assertNotNull(TdSummaryBuilder.build(withoutFood, 60))
        assertNull(summary.group("11").foodPaise)
        assertEquals(emptyList(), summary.group("11").foodByTimingPaise)
        assertEquals(281250L, summary.group("11").hotelPaise)

        val withoutHotel = figures.copy(figures = figures.figures - "td_hotel")
        assertNull(TdSummaryBuilder.build(withoutHotel, 60), "the groups come from the hotel bands, so without them there is no table")
    }

    @Test
    fun timingSharesAreReadFromTheDataNotHardCoded() {
        val shares = listOf(GuideTimeShare(0, 50), GuideTimeShare(300, 100))
        val edited = figures.copy(figures = figures.figures + ("td_food_hours" to figures.figures.getValue("td_food_hours").copy(timeShares = shares)))
        val summary = assertNotNull(TdSummaryBuilder.build(edited, 60))
        assertEquals(listOf(50, 100), summary.foodTimings.map { it.percent })
        assertEquals(listOf(62500L, 125000L), summary.group("12A").foodByTimingPaise)
    }

    @Test
    fun withoutAPayslipDaTheTableIsDrawnAtTheBundlesTier() {
        val summary = assertNotNull(TdSummaryBuilder.buildFor(figures, null))
        assertEquals(50 to 99, summary.daBandFrom to summary.daBandTo)
        assertEquals(281250L, summary.group("10").hotelPaise)
    }

    @Test
    fun aPayslipDaDecidesTheStepWhenTheOfficerHasOne() {
        // An officer already past DA 100% must not be shown the DA 50 to 99% table.
        val summary = assertNotNull(TdSummaryBuilder.buildFor(figures, 100))
        assertEquals(100 to 149, summary.daBandFrom to summary.daBandTo)
        assertEquals(337500L, summary.group("10").hotelPaise)
        assertEquals(50 to 99, assertNotNull(TdSummaryBuilder.buildFor(figures, 63)).let { it.daBandFrom to it.daBandTo })
    }

    @Test
    fun noPayslipDaAndNoShippedTierShowsNothingRatherThanTheWrongStep() {
        val noTier = figures.copy(daStep = figures.daStep.copy(tableDaPercent = 0))
        assertNull(TdSummaryBuilder.buildFor(noTier, null))
        assertNotNull(TdSummaryBuilder.buildFor(noTier, 60), "a payslip DA is enough on its own")
        assertNull(TdSummaryBuilder.buildFor(null, 60))
    }
}
