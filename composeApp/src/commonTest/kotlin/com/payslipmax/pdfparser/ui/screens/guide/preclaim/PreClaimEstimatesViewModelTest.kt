package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import com.payslipmax.pdfparser.guide.preclaim.CtgCase
import com.payslipmax.pdfparser.guide.preclaim.CtgEstimate
import com.payslipmax.pdfparser.guide.preclaim.LimitNote
import com.payslipmax.pdfparser.guide.preclaim.LodgingEstimate
import com.payslipmax.pdfparser.guide.preclaim.NilReason
import com.payslipmax.pdfparser.guide.preclaim.NotPayable
import com.payslipmax.pdfparser.guide.preclaim.PerDayEstimate
import com.payslipmax.pdfparser.guide.preclaim.PerKmEstimate
import com.payslipmax.pdfparser.guide.preclaim.PreClaimElement
import com.payslipmax.pdfparser.guide.preclaim.PreClaimIssue
import com.payslipmax.pdfparser.guide.preclaim.PreClaimNote
import com.payslipmax.pdfparser.guide.preclaim.Unavailable
import com.payslipmax.pdfparser.testing.SyntheticGuidePreClaim
import com.payslipmax.pdfparser.testing.SyntheticPreClaimWorksheet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The estimates the worksheet shows, reached only through the ViewModel: the owner's claim A and voucher as golden cases, and the
 * owner's 2026-10-11 decisions (island pre-tick, nights = days, spouse in Army is a note). The rate maths is in the calculators'
 * own tests; each case here fails if the flow feeds a calculator the wrong answer.
 */
class PreClaimEstimatesViewModelTest : PreClaimViewModelTestBase() {
    @Test
    fun theVoucherSampleThroughTheFlowGivesCtgBaggageAndConveyance() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }

            val frame =
                model.edit {
                    setRoadKm("2881")
                    chooseOriginState("Assam")
                }

            assertEquals(119200L, frame.outcome<CtgEstimate>(PreClaimElement.CTG).rupees, "80% of the basic, MSP and NPA left out")
            assertEquals(86430L, frame.outcome<PerKmEstimate>(PreClaimElement.CAR_CONVEYANCE).rupees)
            assertEquals(listOf(PreClaimElement.CTG, PreClaimElement.BAGGAGE_ROAD, PreClaimElement.CAR_CONVEYANCE), frame.estimates.map { it.element }, "no total, no advance line (plan 21, F1)")
        }

    @Test
    fun theAndamanNicobarTickPreSelectsTheIslandCaseUntilTheOfficerPicksOne() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }

            val ticked = model.edit { setTick("andaman_nicobar", true) }
            assertEquals(CtgCase.ISLAND, ticked.form.ctgCase)
            assertEquals(149000L, ticked.outcome<CtgEstimate>(PreClaimElement.CTG).rupees, "island is 100% of the basic")

            assertEquals(CtgCase.NORMAL, model.edit { setTick("andaman_nicobar", false) }.form.ctgCase, "unticking undoes what the tick chose")

            model.edit { chooseCtgCase(CtgCase.LOCAL_MOVE) }
            val after = model.edit { setTick("andaman_nicobar", true) }
            assertEquals(CtgCase.LOCAL_MOVE, after.form.ctgCase, "owner decision 2: the officer's own pick is never overridden")
            assertEquals(39733L, after.outcome<CtgEstimate>(PreClaimElement.CTG).rupees, "a local move is one-third of 80%")
        }

    @Test
    fun withBothSpousesServingTheCtgIsOnlyANote() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }

            val frame = model.edit { setTick("spouse_in_army", true) }

            assertEquals(PreClaimIssue.BOTH_SPOUSES_SERVING, frame.outcome<Unavailable>(PreClaimElement.CTG).issue)
            assertTrue(model.edit { setTick("spouse_in_army", false) }.estimates.first() is CtgEstimate)
        }

    private fun claimA(model: PreClaimViewModel) =
        model.edit {
            chooseMove(PreClaimMoveType.TEMPORARY_DUTY)
            chooseTrRule("tr119")
            setFromDate(date(2024, 2, 1))
            setToDate(date(2024, 3, 28))
            setNightlyRate("4500")
        }

    @Test
    fun theOwnersClaimAGivesFiftySevenDaysOfFoodAndLodgingWithNightsDefaultingToTheDays() =
        test { model ->
            val frame = claimA(model)

            assertEquals(57, frame.days)
            assertEquals("57", frame.nightsText, "owner decision 2026-10-11: nights default to the day count")
            assertEquals(57_000L, frame.outcome<PerDayEstimate>(PreClaimElement.FOOD).rupees)
            assertEquals(256_500L, frame.outcome<LodgingEstimate>(PreClaimElement.LODGING).rupees)
            assertFalse(PreClaimNote.OVER_180_DAYS in frame.notes, "57 days is under the 180-day cap")
        }

    @Test
    fun typingNightsOverridesTheDefaultAndClearingTheBoxReturnsToTheDays() =
        test { model ->
            claimA(model)

            val ten = model.edit { setNights("10") }
            assertEquals(45_000L, ten.outcome<LodgingEstimate>(PreClaimElement.LODGING).rupees)
            assertEquals("10", ten.nightsText)

            val back = model.edit { setNights("") }
            assertEquals("57", back.nightsText)
            assertEquals(256_500L, back.outcome<LodgingEstimate>(PreClaimElement.LODGING).rupees)
        }

    @Test
    fun rule124MakesFoodLodgingAndTaxiNilAndTheCopyBoxesSayZero() =
        test { model ->
            claimA(model)

            val frame = model.edit { chooseTrRule("tr124") }

            for (element in listOf(PreClaimElement.FOOD, PreClaimElement.LODGING, PreClaimElement.LOCAL_TAXI)) {
                assertEquals(NilReason.RULE_124_NO_DA, frame.outcome<NotPayable>(element).reason)
            }
            assertEquals("0", frame.copyValues["food_charges"])
            assertEquals("0", frame.copyValues["accommodation"])
        }

    @Test
    fun aSameDayTripPaysTheTimeShareOfTheDayAndALongerMoveIgnoresTheHours() =
        test { model ->
            model.edit {
                chooseMove(PreClaimMoveType.TEMPORARY_DUTY)
                chooseTrRule("tr119")
                setFromDate(date(2026, 10, 11))
                setToDate(date(2026, 10, 11))
                setAwayHours("7")
            }
            assertEquals(700L, model.worksheet().outcome<PerDayEstimate>(PreClaimElement.FOOD).rupees, "6 to 12 hours is 70% of Rs 1,000")

            val short =
                model.edit {
                    setAwayHours("5")
                    setAwayMinutes("59")
                }
            assertEquals(300L, short.outcome<PerDayEstimate>(PreClaimElement.FOOD).rupees, "359 minutes is still under 6 hours")

            val multi = model.edit { setToDate(date(2026, 10, 12)) }
            assertEquals(2000L, multi.outcome<PerDayEstimate>(PreClaimElement.FOOD).rupees, "owner decision 3: two full days")
        }

    @Test
    fun theTaxiOfLevelThirteenIsAWordsOnlyLimitNote() =
        test { model ->
            val frame = claimA(model)

            assertTrue(frame.outcome<LimitNote>(PreClaimElement.LOCAL_TAXI).text.isNotEmpty(), "no rupee cap exists for level 12 and above")
        }

    @Test
    fun ownCarRmaNeedsTheSanctionAnswerAndIsNilWithoutIt() =
        test { model ->
            claimA(model)
            model.edit {
                setRoadKm("2881")
                chooseOriginState("Assam")
            }
            assertEquals(PreClaimIssue.SANCTION_UNANSWERED, model.worksheet().outcome<Unavailable>(PreClaimElement.CAR_CONVEYANCE).issue)

            val no = model.edit { setHoldsRmaSanction(false) }
            assertEquals(NilReason.NO_RULE_40_SANCTION, no.outcome<NotPayable>(PreClaimElement.CAR_CONVEYANCE).reason)
            assertEquals("0", no.copyValues["rma"])

            val yes = model.edit { setHoldsRmaSanction(true) }
            assertEquals(86_430L, yes.outcome<PerKmEstimate>(PreClaimElement.CAR_CONVEYANCE).rupees)
            assertEquals("86430", yes.copyValues["rma"])
        }

    @Test
    fun copyValuesAreOnePlainValuePerKnownBoxAndNeverForTicksDatesOrChoices() =
        test { model ->
            val frame = claimA(model)

            assertEquals(
                mapOf("basic_pay" to "149000", "npa" to "0", "msp" to "15500", "total_absence" to "57", "food_charges" to "57000", "accommodation" to "256500"),
                frame.copyValues,
                "the RMA box has no value until the sanction question is answered; ticks, dates and the rule choice carry none",
            )
        }

    @Test
    fun aMaritalStatusTheOfficerTypedIsCopyableAndABlankOneIsNot() =
        test { model ->
            model.edit { chooseMove(PreClaimMoveType.TEMPORARY_DUTY) }
            assertFalse("marital_status" in model.worksheet().copyValues)

            assertEquals("Married", model.edit { setMaritalStatus("Married") }.copyValues["marital_status"])
        }

    @Test
    fun leaveTravelComputesNothingAndLinksTheChosenRuleToItsCard() =
        test { model ->
            val start = model.edit { chooseMove(PreClaimMoveType.LEAVE_TRAVEL) }
            assertEquals(emptyList(), start.estimates)
            assertNull(start.ruleCardId)

            val picked = model.edit { chooseTrRule("ltc_home") }
            assertEquals(SyntheticGuidePreClaim.PICKER_CARD, picked.ruleCardId, "every picker option links to a card (P3)")

            val ignored = model.edit { chooseTrRule("tr119") }
            assertEquals("ltc_home", ignored.form.trRuleId, "an option from the temporary-duty list is ignored on a leave-travel move")
        }

    @Test
    fun withNoFiguresEveryEstimateIsUnavailableInsteadOfBlank() =
        test(withBundle(SyntheticPreClaimWorksheet.bundle().copy(figures = null))) { model ->
            val frame = model.edit { chooseMove(PreClaimMoveType.PERMANENT_DUTY) }

            assertTrue(frame.estimates.all { it is Unavailable && it.issue == PreClaimIssue.FIGURE_MISSING })
            assertEquals(emptyList(), frame.levelChoices)
        }
}
