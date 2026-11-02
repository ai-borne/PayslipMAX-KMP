package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the officer added a casualty and was put back on the casualty page, which looked unchanged and still offered Add, so he tapped it
 * again and got a second blank form. A saved Add now lands on the Orders Bank with the new row in view (the Bank is sorted by From, so a
 * new row can be far down a long list), and Back from there is the hub, not the Learn trail.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class Part2AddLandsOnBankUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }
    private val oldFrom = CalendarDate(2015, 3, 1)

    private fun open(vararg above: GuideDestination): GuideNavState {
        runBlocking { repeat(12) { data.repository.saveEntry(SyntheticBank.entry("e$it", code = "SECONDCODE", from = CalendarDate(2026, 1 + it % 9, 1 + it))) } }
        return show(GuideNavState(above.toList()), part2Bank = { bank }, bankAccess = BankAccess(true) {})
    }

    private fun addNewEntryFromTheCasualtyPage(nav: GuideNavState) {
        tap(LearnStrings.addToBank)
        val id = (nav.current as Part2Destination.Entry).entryId
        runBlocking {
            bank.openEditor(id)!!.apply {
                setFrom(oldFrom)
                setTo(CalendarDate(2015, 3, 9))
                setData(1, "Ravi")
            }
        }
        composeRule.onNodeWithText(OrdersBankStrings.add).performClick()
        settle()
    }

    @Test
    fun addingFromTheHubTrailLandsOnTheBankAndBackIsTheHub() {
        val nav = open(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Ribbon("leave"), Part2Destination.Code("FIRSTCODE"))

        addNewEntryFromTheCasualtyPage(nav)

        assertEquals(listOf(GuideDestination.Home, Part2Destination.Hub, Part2Destination.Bank), nav.stack)
        back()
        assertEquals(Part2Destination.Hub, nav.current)
    }

    @Test
    fun addingFromTheBanksOwnAddReturnsToThatBank() {
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))

        addNewEntryFromTheCasualtyPage(nav)

        assertEquals(listOf(GuideDestination.Home, Part2Destination.Hub, Part2Destination.Bank), nav.stack)
    }

    @Test
    fun theNewRowIsOnScreenEvenWhenItsFromDateSortsItFarDownAPhoneSizedList() {
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))

        addNewEntryFromTheCasualtyPage(nav)

        composeRule.onNodeWithText(PreClaimDates.format(oldFrom), substring = true).assertIsDisplayed()
    }

    @Test
    fun tappingTheRowJustAddedOpensItsEditFormWithDeleteNotTheAddFormAgain() {
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))
        addNewEntryFromTheCasualtyPage(nav)

        tap(PreClaimDates.format(oldFrom))

        assertEquals(true, nav.current is Part2Destination.Entry)
        see(OrdersBankStrings.editTitle).assertIsDisplayed()
        see(OrdersBankStrings.delete).assertIsDisplayed()
        assertEquals(0, count(OrdersBankStrings.newTitle).fetchSemanticsNodes().size, "not the Add to the Orders Bank form")
    }
}
