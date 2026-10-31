package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.WaitingAge
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the Bank is what the officer opens on the phone after an event. He must see an honest empty state, then his entries newest first
 * under year headings, each in the owner's two status words with the waiting age, filter them with the chips, open one, add a casualty
 * from its page, and delete one only after confirming. A lapsed officer sees everything and can delete, but is offered Premium instead of
 * Add, and the form cannot be edited.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2BankUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }
    private var upgradeTaps = 0

    private fun open(
        vararg above: com.payslipmax.pdfparser.ui.screens.guide.GuideDestination,
        canAdd: Boolean = true,
    ): GuideNavState = show(GuideNavState(above.toList()), part2Bank = { bank }, bankAccess = BankAccess(canAdd) { upgradeTaps++ })

    private fun exact(text: String) = composeRule.onNodeWithText(text)

    private fun seed() =
        runBlocking {
            data.repository.saveOrder(SyntheticBank.order("o1"))
            data.repository.saveEntry(SyntheticBank.entry("waiting", code = "FIRSTCODE", from = CalendarDate(2026, 4, 1)))
            data.repository.saveEntry(SyntheticBank.entry("done", code = "SECONDCODE", orderId = "o1", from = CalendarDate(2025, 12, 5)))
        }

    @Test
    fun anEmptyBankSaysWhatItIsForAndOffersOneAddButton() {
        open(Part2Destination.Bank)

        see(OrdersBankStrings.emptyTitle).assertIsDisplayed()
        see(OrdersBankStrings.emptyBody).assertIsDisplayed()
        see(OrdersBankStrings.addButton).assertIsDisplayed().assertIsEnabled()
        count(OrdersBankStrings.addButton).let { assertEquals(1, it.fetchSemanticsNodes().size, "one Add on the list") }
        see(OrdersBankStrings.backupNever).assertIsDisplayed()
    }

    @Test
    fun entriesShowNewestFirstUnderYearsWithTheOwnersStatusWordsAndTheWaitingAge() {
        seed()
        open(Part2Destination.Bank)

        exact("2026").assertIsDisplayed()
        exact("2025").assertIsDisplayed()
        see(OrdersBankStrings.status(com.payslipmax.pdfparser.part2.domain.BankStatus.NOT_ATTACHED)).assertIsDisplayed()
        assertEquals("Part II Order Copy not Attached", OrdersBankStrings.status(com.payslipmax.pdfparser.part2.domain.BankStatus.NOT_ATTACHED))
        assertEquals("Part II Order Copy Attached", OrdersBankStrings.status(com.payslipmax.pdfparser.part2.domain.BankStatus.ATTACHED))
        see(OrdersBankStrings.waiting(WaitingAge.Months(6))).assertIsDisplayed()
        see("Granted the first thing").assertIsDisplayed()
    }

    @Test
    fun theChipsFilterTheListAndShowTheirCounts() {
        seed()
        open(Part2Destination.Bank)
        see(OrdersBankStrings.chipWithCount(BankFilter.ALL, 2)).assertIsDisplayed()

        tap(OrdersBankStrings.chipWithCount(BankFilter.ATTACHED, 1))
        count("Granted the first thing").let { assertEquals(0, it.fetchSemanticsNodes().size) }
        see("Reported for the second thing").assertIsDisplayed()

        tap(OrdersBankStrings.chipWithCount(BankFilter.NOT_ATTACHED, 1))
        see("Granted the first thing").assertIsDisplayed()
    }

    @Test
    fun tappingARowOpensItsFormAndBackReturnsToTheList() {
        seed()
        val nav = open(Part2Destination.Bank)

        tap("Granted the first thing")

        assertEquals(Part2Destination.Entry("waiting"), nav.current)
        see(OrdersBankStrings.editTitle).assertIsDisplayed()
        see(OrdersBankStrings.delete).assertIsDisplayed()
        back()
        assertEquals(Part2Destination.Bank, nav.current)
    }

    @Test
    fun addFromACasualtyPageOpensABlankFormAndAddingItPutsItInTheBank() {
        val nav = open(Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))

        tap(LearnStrings.addToBank)

        val entry = nav.current as Part2Destination.Entry
        see(OrdersBankStrings.newTitle).assertIsDisplayed()
        exact(OrdersBankStrings.add).assertIsDisplayed()
        runBlocking {
            bank.openEditor(entry.entryId)!!.apply {
                setFrom(CalendarDate(2026, 3, 1))
                setData(1, "Ravi")
            }
        }
        exact(OrdersBankStrings.add).performClick().also { settle() }

        assertEquals(Part2Destination.Code("FIRSTCODE"), nav.current, "back on the casualty page it was added from")
        assertEquals("FIRSTCODE", runBlocking { data.repository.entry(entry.entryId)!!.code })
    }

    @Test
    fun anIncompleteFormShowsWhatIsMissingAndWritesNothing() {
        val nav = open(Part2Destination.Home, Part2Destination.Code("FIRSTCODE"))
        tap(LearnStrings.addToBank)
        val id = (nav.current as Part2Destination.Entry).entryId

        exact(OrdersBankStrings.add).performClick().also { settle() }

        assertTrue(count(OrdersBankStrings.problem(com.payslipmax.pdfparser.part2.domain.EntryProblem.MISSING)).fetchSemanticsNodes().isNotEmpty(), "each missing field says so")
        assertNull(runBlocking { data.repository.entry(id) })
        assertEquals(Part2Destination.Entry(id), nav.current)
    }

    @Test
    fun deletingNeedsAConfirmationAndCancelKeepsTheEntry() {
        seed()
        val nav = open(Part2Destination.Bank, Part2Destination.Entry("waiting"))

        tap(OrdersBankStrings.delete)
        see(OrdersBankStrings.deleteTitle).assertIsDisplayed()
        exact(OrdersBankStrings.cancel).performClick().also { settle() }
        assertEquals(Part2Destination.Entry("waiting"), nav.current)
        assertEquals("waiting", runBlocking { data.repository.entry("waiting")?.id })

        tap(OrdersBankStrings.delete)
        composeRule.onAllNodesWithText(OrdersBankStrings.delete).onLast().performClick().also { settle() }
        assertNull(runBlocking { data.repository.entry("waiting") })
    }

    @Test
    fun aLapsedOfficerSeesEverythingIsOfferedPremiumNotAddAndCannotEditButCanDelete() {
        seed()
        val nav = open(Part2Destination.Bank, canAdd = false)
        see(OrdersBankStrings.lockedNote).assertIsDisplayed()
        see("Granted the first thing").assertIsDisplayed()

        tap(OrdersBankStrings.upgrade)
        assertEquals(1, upgradeTaps)
        assertEquals(Part2Destination.Bank, nav.current)

        tap("Granted the first thing")
        see(OrdersBankStrings.viewTitle).assertIsDisplayed()
        count(OrdersBankStrings.save).let { assertEquals(0, it.fetchSemanticsNodes().size, "no Save for a lapsed officer") }
        see(OrdersBankStrings.delete).assertIsDisplayed()
    }

    @Test
    fun anAddFromTheCasualtyPageOffersPremiumToALapsedOfficerAndOpensNoForm() {
        val nav = open(Part2Destination.Home, Part2Destination.Code("FIRSTCODE"), canAdd = false)

        tap(LearnStrings.addToBank)

        assertEquals(1, upgradeTaps)
        assertEquals(Part2Destination.Code("FIRSTCODE"), nav.current)
    }

    @Test
    fun anEntryThatIsNoLongerThereClosesItsLevelInsteadOfShowingABlankForm() {
        val nav = open(Part2Destination.Bank, Part2Destination.Entry("gone"))

        assertEquals(Part2Destination.Bank, nav.current)
    }

    @Test
    fun theBankStaysUsableAtTwiceTheFontSize() {
        seed()
        fontScale = 2f
        open(Part2Destination.Bank)

        see(OrdersBankStrings.addButton).assertIsDisplayed()
        see(OrdersBankStrings.chipWithCount(BankFilter.ALL, 2)).assertIsDisplayed()
    }

    @Test
    fun theBankStaysClosedWhileThePartTwoFlagIsOffBecauseNoModelIsMade() {
        val nav = show(GuideNavState(listOf(Part2Destination.Bank)), part2 = null, part2Bank = null)

        assertEquals(com.payslipmax.pdfparser.ui.screens.guide.GuideDestination.Home, nav.current, "the level is cut, not drawn")
    }

    @Test
    fun theGuideHomeOffersTheBankRowAfterTheLearnRowAndItOpens() {
        val nav = show(GuideNavState(), part2Bank = { bank }, bankAccess = BankAccess(true) {})

        see(OrdersBankStrings.entryTitle).assertIsDisplayed()
        tap(OrdersBankStrings.entryTitle)

        assertEquals(Part2Destination.Bank, nav.current)
        see(OrdersBankStrings.title).assertIsDisplayed()
    }
}
