package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import com.payslipmax.pdfparser.ui.theme.PreClaimStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: on the Pixel, adding the second and third casualty of one order cost about nine actions, one of them a casualty page the officer
 * had no need to read (he knew what he was adding). From "Add a casualty" on the attach form, Learn is a picker: a tap on a casualty,
 * in search or in a ribbon, goes straight to its Add form. Everywhere else Learn still teaches (the casualty page).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2AttachPickerUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    private fun open(
        vararg above: com.payslipmax.pdfparser.ui.screens.guide.GuideDestination,
        canAdd: Boolean = true,
    ) = show(GuideNavState(above.toList()), part2Bank = { bank }, bankAccess = BankAccess(canAdd) { unlockTaps++ })

    @Test
    fun aSearchResultFromTheAttachFormGoesStraightToTheAddForm() {
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach(), Part2Destination.Home)
        type("FIRSTCODE")

        tap(FIRST_NAME)

        assertTrue(nav.current is Part2Destination.Entry, "no casualty page in between")
        see(OrdersBankStrings.newTitle).assertIsDisplayed()
    }

    @Test
    fun theAddFormFromThePickerOpensOnTheDateOfTheCasualtyAlreadyOnThisOrder() {
        runBlocking { data.repository.saveEntry(SyntheticBank.entry("w1", code = "SECONDCODE", from = CalendarDate(2026, 3, 1))) }
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach("w1"))
        nav.push(Part2Destination.Home)
        settle()
        type("FIRSTCODE")
        tap(FIRST_NAME)

        composeRule.onNodeWithText(OrdersBankStrings.fromLabel, substring = true).performClick()
        composeRule.onNodeWithText(PreClaimStrings.dateSet).performClick()
        settle()

        see(PreClaimDates.format(CalendarDate(2026, 3, 1))).assertIsDisplayed()
    }

    @Test
    fun aRibbonRowFromTheAttachFormAlsoGoesStraightToTheAddForm() {
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach(), Part2Destination.Home, Part2Destination.Ribbon("leave"))

        tap(FIRST_NAME)

        assertTrue(nav.current is Part2Destination.Entry)
    }

    @Test
    fun learnOpenedFromTheHubStillShowsTheCasualtyPage() {
        val nav = open(Part2Destination.Hub, Part2Destination.Home)
        type("FIRSTCODE")

        tap(FIRST_NAME)

        assertEquals(Part2Destination.Code("FIRSTCODE"), nav.current)
    }

    @Test
    fun theAttachPickerSaysWhatTappingDoes() {
        open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach(), Part2Destination.Home)

        see(LearnStrings.pickTitle).assertIsDisplayed()
        see(LearnStrings.pickSubtitle).assertIsDisplayed()
    }

    @Test
    fun aLapsedOfficerStillGetsTheCasualtyPageNotAnAddForm() {
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Home, canAdd = false)
        type("FIRSTCODE")

        tap(FIRST_NAME)

        assertEquals(Part2Destination.Code("FIRSTCODE"), nav.current)
    }

    private companion object {
        /** The invented casualty's name in the synthetic bundle: the search result row shows it, the search box does not. */
        const val FIRST_NAME = "Granted the first thing"
    }
}
