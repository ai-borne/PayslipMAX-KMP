package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.LearnStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: the officer could not tell a saved Add from nothing happening, and a delete had no sign at all (and no undo). Every save and delete
 * now says what happened, with the casualty's code only (an order's dates, notes and names never go into a message). A new entry is
 * waiting for its order copy, so its message offers the attach step at once; an edit or a delete has nothing to offer.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class Part2SaveNoticeUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    private fun open(vararg above: GuideDestination): GuideNavState {
        runBlocking { data.repository.saveEntry(SyntheticBank.entry("e1", code = "FIRSTCODE", from = CalendarDate(2026, 2, 1))) }
        return show(GuideNavState(above.toList()), part2Bank = { bank }, bankAccess = BankAccess(true) {})
    }

    private fun addFromTheCasualtyPage(nav: GuideNavState) {
        tap(LearnStrings.addToBank)
        val id = (nav.current as Part2Destination.Entry).entryId
        runBlocking {
            bank.openEditor(id)!!.apply {
                setFrom(CalendarDate(2026, 5, 1))
                setTo(CalendarDate(2026, 5, 9))
                setData(1, "Ravi")
            }
        }
        composeRule.onNodeWithText(OrdersBankStrings.add).performClick()
        settle()
    }

    private fun shown(text: String) = composeRule.onNodeWithText(text)

    @Test
    fun aSavedAddSaysSoAndOffersToAttachTheOrderCopy() {
        val nav = open(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))

        addFromTheCasualtyPage(nav)

        shown(OrdersBankStrings.noticeAdded("FIRSTCODE")).assertIsDisplayed()
        shown(OrdersBankStrings.noticeAttach).assertIsDisplayed()
        assertEquals(Part2Destination.Bank, nav.current)
    }

    @Test
    fun theAttachActionOpensTheAttachFlowForThatCasualty() {
        val nav = open(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))
        addFromTheCasualtyPage(nav)

        shown(OrdersBankStrings.noticeAttach).performClick()
        settle()

        val attach = nav.current as Part2Destination.Attach
        assertEquals(true, attach.entryId != null && runBlocking { data.repository.entry(attach.entryId!!) }?.code == "FIRSTCODE", "opened on the entry just added")
    }

    @Test
    fun theMessageLeavesByItselfAfterAboutFourSeconds() {
        val nav = open(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))
        addFromTheCasualtyPage(nav)
        shown(OrdersBankStrings.noticeAdded("FIRSTCODE")).assertIsDisplayed()

        // WHY: a notice that stays for ten seconds covers the foot of the list the officer is reading; four is enough to read it.
        composeRule.mainClock.advanceTimeBy(3_000)
        shown(OrdersBankStrings.noticeAdded("FIRSTCODE")).assertIsDisplayed()
        composeRule.mainClock.advanceTimeBy(2_000)
        assertEquals(0, composeRule.onAllNodesWithText(OrdersBankStrings.noticeAdded("FIRSTCODE")).fetchSemanticsNodes().size, "gone by 5 s")
    }

    @Test
    fun theMessageNeverCarriesWhatWasTyped() {
        val nav = open(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))
        addFromTheCasualtyPage(nav)

        shown(OrdersBankStrings.noticeAdded("FIRSTCODE")).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText("Ravi", substring = true).fetchSemanticsNodes().size, "the typed name is nowhere on screen")
        assertEquals("FIRSTCODE added. Order copy not attached yet", OrdersBankStrings.noticeAdded("FIRSTCODE"), "the message is the code and fixed words")
    }

    @Test
    fun aSavedEditSaysSavedWithNothingToOffer() {
        val nav = open(Part2Destination.Bank, Part2Destination.Entry("e1"))

        shown(OrdersBankStrings.save).performClick()
        settle()

        assertEquals(Part2Destination.Bank, nav.current)
        shown(OrdersBankStrings.noticeSaved("FIRSTCODE")).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText(OrdersBankStrings.noticeAttach).fetchSemanticsNodes().size)
    }

    @Test
    fun aDeleteSaysDeletedWithNothingToOffer() {
        val nav = open(Part2Destination.Bank, Part2Destination.Entry("e1"))

        tap(OrdersBankStrings.delete)
        composeRule.onAllNodesWithText(OrdersBankStrings.delete).onLast().performClick()
        settle()

        assertEquals(Part2Destination.Bank, nav.current)
        shown(OrdersBankStrings.noticeDeleted("FIRSTCODE")).assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodesWithText(OrdersBankStrings.noticeAttach).fetchSemanticsNodes().size)
    }

    @Test
    fun anIncompleteFormSaysNothingBecauseNothingWasSaved() {
        val nav = open(Part2Destination.Hub, Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))
        tap(LearnStrings.addToBank)

        composeRule.onNodeWithText(OrdersBankStrings.add).performClick()
        settle()

        assertEquals(0, composeRule.onAllNodesWithText(OrdersBankStrings.noticeAdded("FIRSTCODE")).fetchSemanticsNodes().size)
        assertEquals(true, nav.current is Part2Destination.Entry)
    }
}
