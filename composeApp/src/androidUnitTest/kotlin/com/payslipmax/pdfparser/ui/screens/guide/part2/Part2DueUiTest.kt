package com.payslipmax.pdfparser.ui.screens.guide.part2

import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: reminders come from the officer's printed slips, so the Due list must show exactly what the planner says is due, open the right
 * slip, follow the vault live, and be an entitled officer's alone (reminders are Premium, Round 10), while the draft and vault screens keep
 * serving someone whose Premium ended. Run on the shipped bundle; every draft and order is synthetic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class Part2DueUiTest : Part2DueUiBase() {
    private val rejoining = Part2DueStrings.action(ReminderKind.RETURN, null)

    @Test
    fun theHomeShowsADueRowWithTheCountForAnEntitledOfficerWithSomethingDue() {
        hold("d1")
        show()
        tap(Part2Strings.entryTitle)
        assertEquals(1, dueTitles())
        see(Part2DueStrings.rowDue(1)).assertExists()
    }

    @Test
    fun theRowSaysNothingIsDueWhenOnlyComingUpRemainsAndIsAbsentWhenNothingIsPlanned() {
        hold("d1", lastDay = 20)
        show()
        tap(Part2Strings.entryTitle)
        see(Part2DueStrings.rowNothingDue).assertExists()
    }

    @Test
    fun noRowAtAllWhenNothingIsPlanned() {
        show()
        tap(Part2Strings.entryTitle)
        assertEquals(0, dueTitles())
    }

    @Test
    fun noRowForAnOfficerWhosePremiumEndedEvenWithPrintedSlips() {
        hold("d1")
        show(part2Unlocked = false)
        tap(Part2Strings.entryTitle)
        assertEquals(0, dueTitles())
        assertEquals(0, count(Part2DueStrings.rowDue(1)).fetchSemanticsNodes().size)
    }

    @Test
    fun tappingTheRowOpensTheListWithTheOverdueItemInItsWords() {
        hold("d1")
        val nav = show()
        tap(Part2Strings.entryTitle)
        tap(Part2DueStrings.rowDue(1))
        assertEquals(Part2Destination.Due, nav.current)
        see(Part2DueStrings.dueHeading).assertExists()
        see(rejoining).assertExists()
        see(Part2DueStrings.dueLine("10/10/2026", overdue = true)).assertExists()
    }

    @Test
    fun anItemOpensItsDraftsSlip() {
        hold("d1")
        val nav = show(atDue())
        tap(rejoining)
        assertEquals(Part2Destination.Slip("d1"), nav.current)
    }

    @Test
    fun backFromTheListGoesToThePartTwoHome() {
        hold("d1")
        val nav = show(atDue())
        back()
        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun comingUpIsListedUnderItsOwnHeadingBeneathWhatIsDue() {
        hold("d1")
        hold("d2", lastDay = 20, firstDay = 14)
        show(atDue())
        see(Part2DueStrings.dueHeading).assertExists()
        see(Part2DueStrings.upcomingHeading).assertExists()
        see(Part2DueStrings.dueLine("21/10/2026", overdue = false)).assertExists()
    }

    @Test
    fun everyItemOffersDismissAndDismissingOneRemovesItForGood() {
        hold("d1")
        hold("d2", lastDay = 20, firstDay = 14)
        show(atDue())
        assertEquals(2, count(Part2DueStrings.dismiss).fetchSemanticsNodes().size, "one for the item due now and one for the item coming up")

        tap(Part2DueStrings.dismiss)

        assertEquals(setOf("return"), runBlocking { repository.draft("d1")!!.dismissed })
        assertEquals(emptySet(), runBlocking { repository.draft("d2")!!.dismissed })
        assertEquals(0, count(Part2DueStrings.dueHeading).fetchSemanticsNodes().size)
        assertEquals(1, count(Part2DueStrings.dismiss).fetchSemanticsNodes().size)
    }

    @Test
    fun anItemComingUpCanBeDismissedAndTheLastOneLeavesTheEmptyMessage() {
        hold("d1", lastDay = 20)
        show(atDue())

        tap(Part2DueStrings.dismiss)

        see(Part2DueStrings.empty).assertExists()
        assertEquals(setOf("return"), runBlocking { repository.draft("d1")!!.dismissed })
    }

    @Test
    fun anEmptyListSaysSoInsteadOfShowingNothing() {
        show(atDue())
        see(Part2DueStrings.empty).assertExists()
        assertEquals(0, count(Part2DueStrings.dueHeading).fetchSemanticsNodes().size)
        assertEquals(0, count(Part2DueStrings.upcomingHeading).fetchSemanticsNodes().size)
    }

    @Test
    fun aListWithOnlyComingUpHasNoDueHeadingAndNoEmptyMessage() {
        hold("d1", lastDay = 20)
        show(atDue())
        see(Part2DueStrings.upcomingHeading).assertExists()
        assertEquals(0, count(Part2DueStrings.dueHeading).fetchSemanticsNodes().size)
        assertEquals(0, count(Part2DueStrings.empty).fetchSemanticsNodes().size)
    }

    @Test
    fun aNumberedOrderLinkedWhileTheListIsOpenTakesItsAddTheOrderItemAway() {
        hold("d1", published = false)
        show(atDue())
        see(Part2DueStrings.action(ReminderKind.ADD_ORDER, null)).assertExists()
        runBlocking { repository.saveOrder(SyntheticPart2Drafts.order("o1", "d1", "A/12", com.payslipmax.pdfparser.calendar.CalendarDate(2026, 6, 20))) }
        settle()
        assertEquals(0, count(Part2DueStrings.action(ReminderKind.ADD_ORDER, null)).fetchSemanticsNodes().size)
        see(rejoining).assertExists()
    }

    @Test
    fun aRestoredListForAnOfficerWhosePremiumEndedShowsTheUpgradePanelAndNoItem() {
        hold("d1")
        show(atDue(), part2Unlocked = false)
        see(Part2Strings.lockedTitle).assertExists()
        assertEquals(0, count(rejoining).fetchSemanticsNodes().size)
        tap(GuideStrings.unlock)
        assertEquals(1, unlockTaps, "the upgrade button on the panel opens the upgrade sheet")
    }

    @Test
    fun aRestoredListForAFreeOfficerShowsTheUpgradePanel() {
        show(atDue(), part2Unlocked = false)
        see(Part2Strings.lockedTitle).assertExists()
    }

    @Test
    fun withTheFeatureOffARestoredListIsCutBackToTheGuideHome() {
        val nav = show(atDue(), part2 = null)
        assertEquals(GuideDestination.Home, nav.current)
    }

    @Test
    fun anUnreadableItemIsSaidNotHidden() {
        hold("d1")
        repository.unreadableOrders = 1
        runBlocking { repository.saveOrder(SyntheticPart2Drafts.order("o9", null, "B/1", com.payslipmax.pdfparser.calendar.CalendarDate(2026, 6, 21))) }
        show(atDue())
        see(Part2DueStrings.unreadable).assertExists()
    }
}
