package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideBundleContract
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.part2.domain.FillNote
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Rules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.YearCheck
import com.payslipmax.pdfparser.part2.leavebank.CrossCheck
import com.payslipmax.pdfparser.part2.leavebank.LeaveBankSource
import com.payslipmax.pdfparser.testing.FakeLeaveBankSource
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.FieldValue
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.LeaveDraftViewModel
import com.payslipmax.pdfparser.ui.theme.Part2LeaveBankStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The year-end forms filled from the Leave Bank, on the shipped templates and rules and a synthetic statement (plan, phase 8b; owner,
 * Round 33): the button, what it fills, the cap note, a figure overridden, the refill question, the break-up box with its missing years
 * named and the check against the statement on the slip. A lapsed officer never sees the form. All figures are synthetic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2LeaveBankFillUiTest : Part2DraftUiBase() {
    private val real: GuideBundle = runBlocking { GuideBundleContract.parseShippedBundle(GuideBundleContract.readShippedBundleText()) }
    private val statement = listOf(accrued("2008-12-31", 7, 7), accrued("2010-12-31", 5, 12), accrued("2023-12-31", 30, 282), ltcEncashed("2024-04-26", 10), accrued("2024-12-31", 19, 291))
    private var source: LeaveBankSource? = FakeLeaveBankSource(statement)

    override fun bundle(): GuideBundle = real

    override fun rules(): Part2Rules = Part2Rules.shipped()

    override fun leaveBank(): LeaveBankSource? = source

    private fun openYearEnd(row: Int): GuideNavState {
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap("Leave")
        tap("Year-end leave and encashment")
        draftRow(row)
        return nav
    }

    private fun LeaveDraftViewModel.field(slot: String) = state.value.sections[0].fields.first { it.slotId == slot }.value

    private fun LeaveDraftViewModel.fillSection() = state.value.sections[0].fill

    @Test
    fun theFormOffersTheFillAndAPressWithoutTheLeaveYearSaysToTypeItAndFillsNothing() {
        val nav = openYearEnd(row = 0)
        see(Part2LeaveBankStrings.fillButton).assertIsDisplayed()
        tap(Part2LeaveBankStrings.fillButton)
        see(Part2LeaveBankStrings.needsYear).assertIsDisplayed()
        assertEquals(FieldValue.Digits(""), model(nav).field("prev_total"))
    }

    @Test
    fun aFillAfterTheYearPutsTheFourFiguresInTheirBoxesAndStatesTheCapAndTheButtonBecomesRefill() {
        val nav = openYearEnd(row = 0)
        typeInto("Leave year", "2024")
        tap(Part2LeaveBankStrings.fillButton)
        val form = model(nav)
        assertEquals(FieldValue.Digits("282"), form.field("prev_total"))
        assertEquals(FieldValue.Digits("10"), form.field("ltc_days"))
        assertEquals(FieldValue.Digits("0"), form.field("al_used"))
        assertEquals(FieldValue.Digits("30"), form.field("accrued"))
        see(Part2LeaveBankStrings.filled(4)).assertIsDisplayed()
        see(Part2LeaveBankStrings.note(FillNote.AccrualLimit(used = 0, allowed = 30))).assertIsDisplayed()
        see(Part2LeaveBankStrings.refillButton).assertIsDisplayed()
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.fillButton).assertCountEquals(0)
    }

    @Test
    fun aFigureCanBeOverriddenByHandAndARefillAsksBeforeItReplacesAndKeepMineChangesNothing() {
        val nav = openYearEnd(row = 0)
        typeInto("Leave year", "2024")
        tap(Part2LeaveBankStrings.fillButton)
        val form = model(nav)
        form.setNumber(0, "ltc_days", "7")
        settle()
        assertEquals(FieldValue.Digits("7"), form.field("ltc_days"), "the officer's figure stands")
        see(Part2LeaveBankStrings.refillButton).assertIsDisplayed()
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.refillTitle).assertCountEquals(0)

        tap(Part2LeaveBankStrings.refillButton)
        see(Part2LeaveBankStrings.refillTitle).assertIsDisplayed()
        assertEquals(FieldValue.Digits("7"), form.field("ltc_days"), "nothing is replaced by merely asking")
        tap(Part2LeaveBankStrings.refillKeep)
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.refillTitle).assertCountEquals(0)
        assertEquals(FieldValue.Digits("7"), form.field("ltc_days"))

        tap(Part2LeaveBankStrings.refillButton)
        tap(Part2LeaveBankStrings.refillConfirm)
        assertEquals(FieldValue.Digits("10"), form.field("ltc_days"), "the confirmed refill replaces it with the statement's")
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.refillTitle).assertCountEquals(0)
    }

    @Test
    fun theSlipChecksTheDraftedTotalAgainstTheStatementShowingBothFiguresOnADifferenceAndChangingNothing() {
        val nav = openYearEnd(row = 0)
        typeInto("Leave year", "2024")
        tap(Part2LeaveBankStrings.fillButton)
        val form = model(nav)
        form.setDate(0, "as_on", CalendarDate(2024, 12, 31))
        settle()
        tap(Part2Strings.seeSlip)
        val different = Part2LeaveBankStrings.crossCheck(YearCheck(2024, CrossCheck.Differs(drafted = 302, statement = 291)))
        see(Part2LeaveBankStrings.crossCheckHeading).assertIsDisplayed()
        see(different).assertIsDisplayed()
        assertEquals(302, form.state.value.slip!!.orders.single().rows.single().data[1].toInt(), "the slip carries what the officer drafted, not the statement's figure")

        tap(Part2Strings.editDraft)
        form.setNumber(0, "accrued", "19")
        settle()
        tap(Part2Strings.seeSlip)
        see(Part2LeaveBankStrings.crossCheck(YearCheck(2024, CrossCheck.Matches(291)))).assertIsDisplayed()
    }

    @Test
    fun aYearTheStatementHasNoRowForIsNotCheckedAndSaysSo() {
        val nav = openYearEnd(row = 0)
        val form = model(nav)
        form.setNumber(0, "year", "2030")
        form.setDate(0, "as_on", CalendarDate(2030, 12, 31))
        for (slot in listOf("prev_total", "ltc_days", "accrued")) form.setNumber(0, slot, "1")
        settle()
        tap(Part2Strings.seeSlip)
        see(Part2LeaveBankStrings.crossCheck(YearCheck(2030, CrossCheck.NoStatement))).assertIsDisplayed()
    }

    @Test
    fun theEncashmentIsFilledFromTheStatementWithItsMissingYearsAndDifferenceNamedAndAFollowingTypedLineClearsAName() {
        val nav = openYearEnd(row = 4)
        assertEquals(listOf("Year-by-year accumulation"), model(nav).state.value.missing, "an empty break-up is still needed")
        tap(Part2LeaveBankStrings.fillButton)
        val form = model(nav)
        assertEquals(emptyList(), form.state.value.missing)
        assertEquals(FieldValue.Years("2008 7\n2010 5\n2023 30\n2024 19"), form.field("breakup"))
        val missing = (2009..2009).toList() + (2011..2022).toList()
        see(Part2LeaveBankStrings.note(FillNote.MissingYears(missing))).assertIsDisplayed()
        see(Part2LeaveBankStrings.note(FillNote.StatementTotal(statement = 291, breakup = 61))).assertIsDisplayed()

        form.setYears(0, "breakup", "2008 7\n2009 3\n2010 5\n2023 30\n2024 19")
        settle()
        see(Part2LeaveBankStrings.note(FillNote.MissingYears((2011..2022).toList()))).assertIsDisplayed()
    }

    @Test
    fun aStatementOfSixtyYearsFillsTheBreakUpWholeNotCutAtTheOrdinaryTextLimit() {
        // why: an ordinary text answer stops at 200 characters; sixty lines of a career are about 480, and a break-up cut mid-line would be refused.
        source = FakeLeaveBankSource((1966..2025).map { accrued("$it-12-31", 30, (it - 1965) * 30) })
        val nav = openYearEnd(row = 4)
        tap(Part2LeaveBankStrings.fillButton)
        val text = (model(nav).field("breakup") as FieldValue.Years).text
        assertEquals(60, text.lines().size)
        assertEquals((1966..2025).joinToString("\n") { "$it 30" }, text)
    }

    @Test
    fun aLongBreakUpIsKeptWholeWhileOrdinaryTextStillStopsAtItsOwnLimit() {
        val nav = openYearEnd(row = 4)
        val form = model(nav)
        val rows = (2000 until 2060).joinToString("\n") { "$it 30" }
        form.setYears(0, "breakup", rows)
        settle()
        assertEquals(FieldValue.Years(rows), form.field("breakup"))
    }

    @Test
    fun aBreakUpTypedIntoTheBoxItselfKeepsEveryLineAndHelpNoLongerTellsToUseSemicolons() {
        // why: the box must hand a long list to the year-list path, not to the ordinary text path that stops at 200 characters.
        val nav = openYearEnd(row = 4)
        val rows = (1990..2025).joinToString("\n") { "$it 30" }
        typeInto("Year-by-year accumulation", rows)
        assertEquals(FieldValue.Years(rows), model(nav).field("breakup"))
        composeRule.onAllNodesWithText("semicolon", substring = true).assertCountEquals(0)
    }

    @Test
    fun withoutTheLeaveBankNoFillIsOfferedAndTheFormStillWorksByHand() {
        source = null
        val nav = openYearEnd(row = 0)
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.fillButton).assertCountEquals(0)
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.refillButton).assertCountEquals(0)
        assertNull(model(nav).fillSection())
        typeInto("Leave year", "2024")
        assertEquals(FieldValue.Digits("2024"), model(nav).field("year"))
    }

    @Test
    fun aLapsedOfficerOnTheSlipSeesNeitherTheFormNorAFillButton() {
        val values =
            mapOf(
                "year" to SlotValue.NumberValue(2024),
                "as_on" to SlotValue.DateValue(CalendarDate(2024, 12, 31)),
                "prev_total" to SlotValue.NumberValue(282),
                "ltc_days" to SlotValue.NumberValue(10),
                "accrued" to SlotValue.NumberValue(19),
            )
        runBlocking { repository.saveDraft(Part2Draft("lapsed", LeaveEventInput(listOf(OccurrenceRequest("ACCLVE", values))), emptyMap(), 1L, 2L)) }
        show(GuideNavState(listOf(Part2Destination.Home, Part2Destination.Slip("lapsed"))), part2Unlocked = false)
        see(Part2Strings.viewOnlyNote).assertIsDisplayed()
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.fillButton).assertCountEquals(0)
        composeRule.onAllNodesWithText(Part2LeaveBankStrings.refillButton).assertCountEquals(0)
        composeRule.onAllNodesWithText(Part2Strings.editDraft).assertCountEquals(0)
    }
}
