package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.testing.SyntheticGuideFigures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Temporary-duty estimates end to end: food, lodging cap, local conveyance cap and own-car RMA, each independent, never summed
 * (the portal sets the advance, plan 21 F1). The two golden cases are the owner's filed claims reduced to numbers (level 13A,
 * 57 and 61 days); the rules around them are TR Rule 124 (no DA), the Rule 40 Note 2 sanction for RMA, and the 180-day note.
 */
class TemporaryDutyCalculatorTest {
    private val figures = SyntheticGuideFigures.figures

    private fun profile(
        level: String? = "13A",
        da: Int? = 46,
    ) = PreClaimProfile(level = level, basicPay = 150000, daPercent = da, mspAmount = null, npaAmount = null)

    private fun date(
        month: Int,
        day: Int,
        year: Int = 2024,
    ) = CalendarDate(year, month, day)

    private fun claimA(
        rule: String? = "tr119",
        sanction: Boolean? = false,
    ) = TemporaryDutyRequest(
        profile = profile(),
        trRuleId = rule,
        from = date(2, 1),
        to = date(3, 28),
        minutesAway = null,
        nights = 57,
        nightlyRateRupees = 4500,
        holdsRmaSanction = sanction,
        roadKm = 1000,
        originState = "Assam",
    )

    private fun result(request: TemporaryDutyRequest = claimA()) = PreClaimCalculator.temporaryDuty(figures, request)

    @Test
    fun thereAreSixElementsInAllAndTemporaryDutyShowsFourInOrderWithNoTotalAndNoAdvance() {
        assertEquals(
            listOf(
                PreClaimElement.CTG,
                PreClaimElement.BAGGAGE_ROAD,
                PreClaimElement.CAR_CONVEYANCE,
                PreClaimElement.FOOD,
                PreClaimElement.LODGING,
                PreClaimElement.LOCAL_TAXI,
            ),
            PreClaimElement.entries,
            "adding a total or an advance line needs the owner (plan 21, F1: the portal sets and adjusts the advance)",
        )
        assertEquals(
            listOf(PreClaimElement.FOOD, PreClaimElement.LODGING, PreClaimElement.LOCAL_TAXI, PreClaimElement.CAR_CONVEYANCE),
            result().outcomes.map { it.element },
        )
    }

    @Test
    fun theOwnersClaimAComesOutAsFiled() {
        val outcome = result()
        val (food, lodging, taxi, rma) = outcome.outcomes

        assertEquals(57, outcome.days)
        assertEquals(57000L, assertIs<PerDayEstimate>(food).rupees, "57 x Rs 1000")
        assertEquals(256500L, assertIs<LodgingEstimate>(lodging).rupees, "57 x Rs 4500")
        assertEquals("AC taxi, up to 50 km a day within the city", assertIs<LimitNote>(taxi).text, "level 13A has no rupee cap")
        assertEquals(NotPayable(PreClaimElement.CAR_CONVEYANCE, NilReason.NO_RULE_40_SANCTION), rma, "claim A filed RMA as nil")
        assertTrue(outcome.notes.isEmpty())
    }

    @Test
    fun theOwnersClaimBLodgingIsSixtyOneNightsAtFifteenHundred() {
        val request =
            claimA().copy(profile = profile(da = 50), from = date(4, 1), to = date(5, 31), nights = 61, nightlyRateRupees = 1500)

        val (food, lodging) = result(request).outcomes

        assertEquals(76250L, assertIs<PerDayEstimate>(food).rupees, "the most the rule allows; the owner filed less")
        assertEquals(91500L, assertIs<LodgingEstimate>(lodging).rupees)
    }

    @Test
    fun ruleOneTwentyFourPaysNoFoodNoLodgingAndNoLocalTaxi() {
        val (food, lodging, taxi, rma) = result(claimA(rule = "tr124", sanction = true)).outcomes

        for ((element, outcome) in listOf(PreClaimElement.FOOD to food, PreClaimElement.LODGING to lodging, PreClaimElement.LOCAL_TAXI to taxi)) {
            assertEquals(NotPayable(element, NilReason.RULE_124_NO_DA), outcome, "$element")
        }
        assertIs<PerKmEstimate>(rma, "Rule 124 grants conveyance; the RMA still follows its own sanction rule")
    }

    @Test
    fun theRuleMustBeChosenBeforeAnyDailyAllowanceIsShown() {
        for (blank in listOf(null, "", "  ")) {
            val (food, lodging, taxi, rma) = result(claimA(rule = blank, sanction = true)).outcomes

            assertEquals(List(3) { PreClaimIssue.TR_RULE_MISSING }, listOf(food, lodging, taxi).map { assertIs<Unavailable>(it).issue }, "$blank")
            assertIs<PerKmEstimate>(rma, "the RMA does not depend on the DA rule")
        }
    }

    @Test
    fun rmaIsNilWithoutTheRuleFortySanctionAndKmTimesTheStateRateWithIt() {
        val without = result(claimA(sanction = false)).outcomes[3]
        val with = assertIs<PerKmEstimate>(result(claimA(sanction = true)).outcomes[3])

        assertEquals(NotPayable(PreClaimElement.CAR_CONVEYANCE, NilReason.NO_RULE_40_SANCTION), without)
        assertEquals(30_000L, with.rupees, "1000 km x Assam Rs 30")
        assertEquals(PreClaimElement.CAR_CONVEYANCE, with.element)
    }

    @Test
    fun theSanctionQuestionMustBeAnsweredAndTheKmMustBeGivenWhenOneIsHeld() {
        assertEquals(Unavailable(PreClaimElement.CAR_CONVEYANCE, PreClaimIssue.SANCTION_UNANSWERED), result(claimA(sanction = null)).outcomes[3])
        assertEquals(
            Unavailable(PreClaimElement.CAR_CONVEYANCE, PreClaimIssue.KM_MISSING),
            result(claimA(sanction = true).copy(roadKm = null)).outcomes[3],
        )
        assertIs<NotPayable>(result(claimA(sanction = false).copy(roadKm = null, originState = null)).outcomes[3], "nil needs no km and no state")
    }

    @Test
    fun overOneHundredEightyDaysRaisesTheCapNoteAndTheLimitItselfIsNotOver() {
        val atLimit = result(claimA().copy(from = date(1, 1), to = date(6, 28), nights = 0))
        val over = result(claimA().copy(from = date(1, 1), to = date(6, 29), nights = 0))

        assertEquals(180, atLimit.days)
        assertTrue(atLimit.notes.isEmpty())
        assertEquals(181, over.days)
        assertEquals(setOf(PreClaimNote.OVER_180_DAYS), over.notes)
        assertEquals(setOf(PreClaimNote.OVER_180_DAYS), result(claimA(rule = "tr124").copy(from = date(1, 1), to = date(12, 31), nights = 0)).notes)
    }

    @Test
    fun aSameDayTripUsesTheTimeAwayAndNeedsNoLodging() {
        val request = claimA().copy(from = date(3, 1), to = date(3, 1), minutesAway = 300, nights = 0)

        val outcome = result(request)

        assertEquals(1, outcome.days)
        assertEquals(300L, assertIs<PerDayEstimate>(outcome.outcomes[0]).rupees, "30% of Rs 1000 (no step at DA 46)")
        assertEquals(0L, assertIs<LodgingEstimate>(outcome.outcomes[1]).rupees)
    }

    @Test
    fun badDatesHideEveryDailyElementButNotTheRma() {
        for ((from, to, issue) in listOf(
            Triple(null, date(3, 1), PreClaimIssue.DATES_MISSING),
            Triple(date(3, 2), date(3, 1), PreClaimIssue.DATES_REVERSED),
            Triple(date(2, 30), date(3, 1), PreClaimIssue.DATE_INVALID),
        )) {
            val outcome = result(claimA(sanction = true).copy(from = from, to = to))

            assertEquals(null, outcome.days)
            assertEquals(List(3) { issue }, outcome.outcomes.take(3).map { assertIs<Unavailable>(it).issue })
            assertIs<PerKmEstimate>(outcome.outcomes[3])
            assertFalse(outcome.notes.isNotEmpty())
        }
    }

    @Test
    fun noFiguresMeansFourUnavailableElementsAndNoCrash() {
        val outcome = PreClaimCalculator.temporaryDuty(null, claimA(sanction = true))

        assertEquals(List(4) { PreClaimIssue.FIGURE_MISSING }, outcome.outcomes.map { assertIs<Unavailable>(it).issue })
    }
}
