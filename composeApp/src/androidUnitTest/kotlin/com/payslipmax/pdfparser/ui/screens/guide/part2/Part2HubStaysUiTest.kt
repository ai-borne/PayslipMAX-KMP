package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * WHY: a hospital stay with no regularising Part II order can cost the officer pay, so the hub says so once, quietly ("noted", never
 * "missing" or an alarm), and only when a stay really needs a look. It takes him to exactly those entries, says when the delay is over
 * two years, and disappears when he has nothing to check or no Bank is available. The Bank's own chip row shows the stays chip only
 * while there is something in it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2HubStaysUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    private fun open(vararg above: com.payslipmax.pdfparser.ui.screens.guide.GuideDestination): GuideNavState =
        show(GuideNavState(above.toList()), part2Bank = { bank }, bankAccess = BankAccess(true) {})

    private fun seedStay(year: Int) =
        runBlocking {
            data.repository.saveEntry(SyntheticBank.entry("adm$year", code = "HOSADM", from = CalendarDate(year, 3, 1)))
            data.repository.saveEntry(SyntheticBank.entry("dis$year", code = "HOSDISCH", from = CalendarDate(year, 3, 20)))
        }

    @Test
    fun aStayWithNoRegularisingEntryShowsOneQuietChipOnTheHub() {
        seedStay(2026)
        open(Part2Destination.Hub)

        see(OrdersBankStrings.hubStays(1, 0)).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun theWordingIsQuietNotAnAlarm() {
        val text = listOf(OrdersBankStrings.hubStays(1, 0), OrdersBankStrings.hubStays(3, 0), OrdersBankStrings.hubStays(2, 1)).joinToString(" ").lowercase()
        assertTrue("noted" in text, text)
        assertFalse(listOf("missing", "error", "warning", "urgent", "!").any { it in text }, text)
    }

    @Test
    fun aStayOverTwoYearsOldSaysSoDifferently() {
        seedStay(2024)
        open(Part2Destination.Hub)

        assertNotEquals(OrdersBankStrings.hubStays(1, 0), OrdersBankStrings.hubStays(1, 1))
        see(OrdersBankStrings.hubStays(1, 1)).assertIsDisplayed()
    }

    @Test
    fun tappingTheChipOpensTheBankOnThoseStaysAndBackReturnsToTheHub() {
        seedStay(2026)
        val nav = open(Part2Destination.Hub)

        see(OrdersBankStrings.hubStays(1, 0)).performClickAndSettle()

        assertEquals(Part2Destination.Bank, nav.current)
        assertEquals(BankFilter.UNREGULARISED, bank.state.value.filter)
        back()
        assertEquals(Part2Destination.Hub, nav.current)
    }

    @Test
    fun aRegularisedStayOrNoStayShowsNoChip() {
        runBlocking {
            data.repository.saveEntry(SyntheticBank.entry("adm", code = "HOSADM", from = CalendarDate(2026, 3, 1)))
            data.repository.saveEntry(SyntheticBank.entry("dis", code = "HOSDISCH", from = CalendarDate(2026, 3, 20)))
            data.repository.saveEntry(SyntheticBank.entry("slc", code = "SLC", from = CalendarDate(2026, 3, 1)))
        }
        open(Part2Destination.Hub)

        assertTrue(count("hospital stay").fetchSemanticsNodes().isEmpty(), "nothing to check, so nothing is said")
    }

    @Test
    fun withoutTheBankTheHubSaysNothingAboutStays() {
        seedStay(2026)
        show(GuideNavState(listOf(Part2Destination.Hub)), part2Bank = null)

        assertTrue(count("hospital stay").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun theBanksChipRowShowsTheStaysChipOnlyWhileItHoldsSomething() {
        seedStay(2026)
        open(Part2Destination.Bank)
        see(OrdersBankStrings.chipWithCount(BankFilter.UNREGULARISED, 1)).assertIsDisplayed()
    }

    @Test
    fun theBanksChipRowHidesTheStaysChipAtZero() {
        runBlocking { data.repository.saveEntry(SyntheticBank.entry("x", code = "FIRSTCODE", from = CalendarDate(2026, 3, 1))) }
        open(Part2Destination.Bank)
        assertTrue(count(OrdersBankStrings.chip(BankFilter.UNREGULARISED)).fetchSemanticsNodes().isEmpty())
        see(OrdersBankStrings.chipWithCount(BankFilter.ALL, 1)).assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.performClickAndSettle() {
        performClick()
        settle()
    }
}
