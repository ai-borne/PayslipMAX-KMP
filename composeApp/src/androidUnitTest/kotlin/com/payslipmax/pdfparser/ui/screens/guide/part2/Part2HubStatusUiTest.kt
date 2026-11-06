package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankFilter
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
import kotlin.test.assertTrue

/**
 * WHY: the Part II hub is the officer's first look at his file. With nothing saved it must say so and offer one next step (never a
 * claim of progress); once he has entries it must say how many casualties are noted and how many order copies are still missing, and
 * take him straight to those; and it must stay calm and short, with Learn first, then the Bank, then (only while empty) how it works.
 * Where the Bank is not available (flag off) the hub is exactly the two rows it was.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2HubStatusUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }
    private var upgradeTaps = 0

    private fun hub(canAdd: Boolean = true): GuideNavState =
        show(GuideNavState(listOf(Part2Destination.Hub)), part2Bank = { bank }, bankAccess = BankAccess(canAdd) { upgradeTaps++ })

    private fun seedMixed() =
        runBlocking {
            data.repository.saveOrder(SyntheticBank.order("o1"))
            data.repository.saveEntry(SyntheticBank.entry("waiting", code = "FIRSTCODE", from = CalendarDate(2026, 4, 1)))
            data.repository.saveEntry(SyntheticBank.entry("done", code = "SECONDCODE", orderId = "o1", from = CalendarDate(2025, 12, 5)))
        }

    private fun bounds(text: String): Rect = composeRule.onAllNodesWithText(text, substring = true)[0].fetchSemanticsNode().boundsInRoot

    @Test
    fun anEmptyFileSaysSoOffersOneNextStepAndPromisesPrivacyNearIt() {
        hub()

        see(OrdersBankStrings.hubEmptyTitle).assertIsDisplayed()
        see(OrdersBankStrings.hubEmptyBody).assertIsDisplayed()
        see(OrdersBankStrings.hubPrivacy).assertIsDisplayed()
        composeRule.onNodeWithText(OrdersBankStrings.hubAddFirst).assertIsDisplayed().assertIsEnabled()
        assertEquals(0, count(OrdersBankStrings.hubFileTitle).fetchSemanticsNodes().size, "an empty file claims no counts")
    }

    @Test
    fun anEmptyHubReadsTopToBottomHeroThenLearnThenTheBankThenHowItWorks() {
        hub()

        val order = listOf(OrdersBankStrings.hubEmptyTitle, LearnStrings.entryTitle, OrdersBankStrings.entryTitle, OrdersBankStrings.hubHowTitle).map { bounds(it).top }
        assertEquals(order.sorted(), order, "hero, Learn, Bank, How it works")
        see(OrdersBankStrings.hubStep(1)).assertIsDisplayed()
        see(OrdersBankStrings.hubStep(2)).assertIsDisplayed()
        see(OrdersBankStrings.hubStep(3)).assertIsDisplayed()
    }

    @Test
    fun addFirstCasualtyGoesTheWayTheBanksAddGoesToLearnsSearch() {
        val nav = hub()

        composeRule.onNodeWithText(OrdersBankStrings.hubAddFirst).performClickAndSettle()

        assertEquals(Part2Destination.Home, nav.current)
    }

    @Test
    fun aLapsedOfficerWithAnEmptyFileIsOfferedPremiumNotAnAddThatDoesNothing() {
        val nav = hub(canAdd = false)

        assertEquals(0, count(OrdersBankStrings.hubAddFirst).fetchSemanticsNodes().size)
        composeRule.onNodeWithText(OrdersBankStrings.upgrade).performClickAndSettle()

        assertEquals(1, upgradeTaps)
        assertEquals(Part2Destination.Hub, nav.current)
    }

    @Test
    fun aFileInProgressShowsCountsAndOneChipForTheCopiesStillMissing() {
        seedMixed()
        hub()

        see(OrdersBankStrings.hubFileTitle).assertIsDisplayed()
        see(OrdersBankStrings.hubNoted(2)).assertIsDisplayed()
        see(OrdersBankStrings.hubAttachedLine(attached = 1, total = 2)).assertIsDisplayed()
        see(OrdersBankStrings.hubNotAttached(1)).assertIsDisplayed().assertIsEnabled()
        assertEquals(0, count(OrdersBankStrings.hubEmptyTitle).fetchSemanticsNodes().size)
        see(OrdersBankStrings.hubHowTitle).assertIsDisplayed()
        val order = listOf(OrdersBankStrings.hubFileTitle, LearnStrings.entryTitle, OrdersBankStrings.entryTitle, OrdersBankStrings.hubHowTitle).map { bounds(it).top }
        assertEquals(order.sorted(), order, "hero, Learn, Bank, How it works: the steps stay once a casualty is saved")
    }

    @Test
    fun theMissingCopiesChipOpensTheBankFilteredToNotAttachedAndBackReturnsToTheHub() {
        seedMixed()
        val nav = hub()

        see(OrdersBankStrings.hubNotAttached(1)).performClickAndSettle()

        assertEquals(Part2Destination.Bank, nav.current)
        assertEquals(BankFilter.NOT_ATTACHED, bank.state.value.filter, "the list opens on the copies that are missing")
        back()
        assertEquals(Part2Destination.Hub, nav.current)
    }

    @Test
    fun withEveryCopyAttachedTheHubSaysSoAndOffersNoChip() {
        runBlocking {
            data.repository.saveOrder(SyntheticBank.order("o1"))
            data.repository.saveEntry(SyntheticBank.entry("done", orderId = "o1", from = CalendarDate(2026, 4, 1)))
        }
        hub()

        see(OrdersBankStrings.hubAttachedLine(attached = 1, total = 1)).assertIsDisplayed()
        assertTrue(count(" not attached").fetchSemanticsNodes().isEmpty(), "nothing is missing, so nothing is flagged")
    }

    @Test
    fun theBankCardAlwaysOpensTheWholeListEvenAfterTheChipLeftItFiltered() {
        seedMixed()
        val nav = hub()
        see(OrdersBankStrings.hubNotAttached(1)).performClickAndSettle()
        assertEquals(BankFilter.NOT_ATTACHED, bank.state.value.filter)
        back()

        tap(OrdersBankStrings.entryTitle)

        assertEquals(Part2Destination.Bank, nav.current)
        assertEquals(BankFilter.ALL, bank.state.value.filter, "a filter left by the chip must not hide the rest of the file next time")
    }

    @Test
    fun withoutTheBankTheHubIsJustTheTwoRowsItWas() {
        val nav = show(GuideNavState(listOf(Part2Destination.Hub)), part2Bank = null)

        see(LearnStrings.entryTitle).assertIsDisplayed()
        see(OrdersBankStrings.entryTitle).assertIsDisplayed()
        assertEquals(0, count(OrdersBankStrings.hubEmptyTitle).fetchSemanticsNodes().size)
        see(OrdersBankStrings.hubHowTitle).assertIsDisplayed()
        assertEquals(Part2Destination.Hub, nav.current)
    }

    @Test
    fun atTwiceTheFontSizeTheNextStepIsStillThereAndTappable() {
        fontScale = 2f
        hub()

        composeRule.onNodeWithText(OrdersBankStrings.hubAddFirst).assertIsDisplayed().assertIsEnabled()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performClickAndSettle() {
        performClick()
        settle()
    }
}
