package com.payslipmax.pdfparser.ui.screens.guide.preclaim

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.NilReason
import com.payslipmax.pdfparser.guide.preclaim.PreClaimIssue
import com.payslipmax.pdfparser.guide.preclaim.PreClaimNote
import com.payslipmax.pdfparser.ui.theme.PreClaimIssueStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Plan 22, P7: the temporary-duty worksheet on the owner's claim A (level 13A, DA 46%, 1 Feb to 28 Mar 2024 = 57 days, Rs 4,500 a
 * night): food Rs 57,000 and hotel Rs 2,56,500, the Rule 124 nils, the own-car question, a short trip's time away, the date hint and
 * the 180-day note. Dates here are synthetic; inputs are numbers only.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "en-rUS-w360dp-h6000dp")
class PreClaimTemporaryUiTest : PreClaimUiBase() {
    private fun openTemporary() {
        show()
        openFromHome("Temporary duty")
    }

    private fun chooseRule(label: String) {
        tap(PreClaimStrings.trRule + ": " + PreClaimStrings.trRuleChoose)
        tap(label)
    }

    private fun enterClaimA() {
        chooseRule("Rule 119")
        setDates(CalendarDate(2024, 2, 1), CalendarDate(2024, 3, 28))
        type(PreClaimStrings.nightlyRate, "4500")
    }

    @Test
    fun claimAGivesFoodAndHotelAndEachCopyButtonHoldsTheValueForThatBoxOnly() {
        openTemporary()
        enterClaimA()
        composeRule.onNodeWithText("${PreClaimStrings.daysAway}: 57 days").assertIsDisplayed()
        composeRule.onNodeWithText("₹57,000").assertIsDisplayed()
        composeRule.onNodeWithText("₹2,56,500").assertIsDisplayed()
        composeRule.onNodeWithText("57 days at ₹1,000 a day").assertIsDisplayed()
        tapDescribed("Copy Food")
        tapDescribed("Copy Accommodation")
        tapDescribed("Copy Total absence")
        assertEquals(listOf("57000", "256500", "57"), copied)
    }

    @Test
    fun theLocalTaxiForThisLevelIsWordsWithNoRupeeAmountOnTheRow() {
        openTemporary()
        enterClaimA()
        composeRule.onNodeWithText("Local taxi").assertIsDisplayed()
        composeRule.onNodeWithText("No rupee amount in the rule. AC taxi, up to 50 km a day within the city", substring = true).assertIsDisplayed()
    }

    @Test
    fun theRulePickerListsAShortLineUnderEachRuleSoTheyCanBeToldApart() {
        openTemporary()
        tap(PreClaimStrings.trRule + ": " + PreClaimStrings.trRuleChoose)
        composeRule.onNodeWithText("Ordinary duty trip").assertIsDisplayed()
        composeRule.onNodeWithText("Course or exam").assertIsDisplayed()
    }

    @Test
    fun rule124MakesFoodHotelAndTaxiNilAndTheirCopyValueIsZero() {
        openTemporary()
        chooseRule("Rule 124 (no DA)")
        setDates(CalendarDate(2024, 2, 1), CalendarDate(2024, 3, 28))
        composeRule.onAllNodesWithText(PreClaimStrings.nilAmount).assertCountEquals(NIL_UNDER_RULE_124)
        tapDescribed("Copy Food")
        assertEquals(listOf("0"), copied)
    }

    @Test
    fun withoutARuleChosenTheDailyLinesAskForItOnceTheDatesAreIn() {
        openTemporary()
        composeRule.onAllNodesWithText(PreClaimIssueStrings.of(PreClaimIssue.DATES_MISSING)).assertCountEquals(DAILY_LINES)
        setDates(CalendarDate(2024, 2, 1), CalendarDate(2024, 3, 28))
        composeRule.onAllNodesWithText(PreClaimIssueStrings.of(PreClaimIssue.TR_RULE_MISSING)).assertCountEquals(DAILY_LINES)
    }

    @Test
    fun ownCarNeedsTheSanctionAnswerThenAKilometreAndAStateAndNoSanctionIsNil() {
        openTemporary()
        composeRule.onNodeWithText(PreClaimIssueStrings.of(PreClaimIssue.SANCTION_UNANSWERED)).assertIsDisplayed()
        tap(PreClaimStrings.no)
        composeRule.onNodeWithText(PreClaimStrings.nilReason(NilReason.NO_RULE_40_SANCTION)).assertIsDisplayed()
        tap(PreClaimStrings.yes)
        type(PreClaimStrings.roadKm, "100")
        tap(PreClaimStrings.originState + ": " + PreClaimStrings.stateChoose)
        tap("Assam")
        composeRule.onNodeWithText("₹3,000").assertIsDisplayed()
        composeRule.onNodeWithText("100 km at ₹30 a km").assertIsDisplayed()
    }

    @Test
    fun aTripWithinOneDayAsksHowLongAwayAndPaysAShareOfTheDay() {
        openTemporary()
        chooseRule("Rule 119")
        setDates(CalendarDate(2024, 2, 1), null)
        composeRule.onAllNodesWithText(PreClaimStrings.awayHours).assertCountEquals(0)
        setDates(CalendarDate(2024, 2, 1), CalendarDate(2024, 2, 1))
        type(PreClaimStrings.awayHours, "5")
        composeRule.onNodeWithText("₹300").assertIsDisplayed()
        composeRule.onNodeWithText("1 day at ₹1,000 a day, 30% of the day's rate for a short trip").assertIsDisplayed()
    }

    @Test
    fun anImpossibleDateGetsAMessageNotASilentChange() {
        openTemporary()
        setDates(CalendarDate(2024, 4, 31), CalendarDate(2024, 5, 5))
        composeRule.onAllNodesWithText(PreClaimIssueStrings.of(PreClaimIssue.DATE_INVALID)).assertCountEquals(DAILY_LINES)
    }

    @Test
    fun theCalendarPicksADayClearsItAndCancelLeavesItAlone() {
        openTemporary()
        val from = PreClaimStrings.fromDate
        composeRule.onNodeWithText("$from: ${PreClaimStrings.dateChoose}").assertIsDisplayed()
        setDates(CalendarDate(2024, 2, 1), null)
        composeRule.onNodeWithText("$from: 01/02/2024").assertIsDisplayed()
        tap("$from: 01/02/2024")
        tap("Tuesday, February 20, 2024")
        tap(PreClaimStrings.dateSet)
        composeRule.onNodeWithText("$from: 20/02/2024").assertIsDisplayed()
        assertEquals(CalendarDate(2024, 2, 20), worksheets.single().typed.value.from)
        tap("$from: 20/02/2024")
        tap("Monday, February 5, 2024")
        tap(PreClaimStrings.dateCancel)
        assertEquals(CalendarDate(2024, 2, 20), worksheets.single().typed.value.from, "cancel keeps the date")
        tap("$from: 20/02/2024")
        tap(PreClaimStrings.dateClear)
        assertNull(worksheets.single().typed.value.from)
        composeRule.onNodeWithText("$from: ${PreClaimStrings.dateChoose}").assertIsDisplayed()
    }

    @Test
    fun aStayOver180DaysAddsTheDailyAllowanceNote() {
        openTemporary()
        chooseRule("Rule 119")
        setDates(CalendarDate(2024, 1, 1), CalendarDate(2024, 9, 30))
        composeRule.onNodeWithText(PreClaimStrings.note(PreClaimNote.OVER_180_DAYS)).assertIsDisplayed()
    }

    @Test
    fun theTypedRuleLineSaysWhenToUseTheChosenRule() {
        openTemporary()
        chooseRule("Rule 119")
        composeRule.onNodeWithText("Ordinary temporary duty.").assertIsDisplayed()
    }

    private companion object {
        const val DAILY_LINES = 3
        const val NIL_UNDER_RULE_124 = 3
    }
}
