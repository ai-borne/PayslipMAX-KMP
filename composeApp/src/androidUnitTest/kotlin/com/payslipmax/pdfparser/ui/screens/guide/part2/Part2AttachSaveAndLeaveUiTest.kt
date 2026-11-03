package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * WHY: found on the Pixel after a real scan. With three casualties ticked, "Save order" was below the screen, so the officer scanned and
 * then had to hunt for the one button that keeps it. It stays on screen under the form. And a held scan is real work: Back, the Orders
 * Bank shortcut or the system back deleted it with one stray tap, so each asks first, but only while a scan is held.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h640dp")
class Part2AttachSaveAndLeaveUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }
    private val scanner = FakePageScanner()

    private fun openAttach(waiting: Int = 6): GuideNavState {
        runBlocking { repeat(waiting) { data.repository.saveEntry(SyntheticBank.entry("w$it", code = "FIRSTCODE")) } }
        scanner.next = PageScan.Pages(listOf(FakeScannedPage(byteArrayOf(1, 2, 3))))
        return show(GuideNavState(listOf(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Attach())), part2Bank = { bank }, bankAccess = BankAccess(true) {}, scanner = scanner)
    }

    @Test
    fun saveOrderIsOnScreenWithoutScrollingEvenWithManyCasualtiesAndAHeldScan() {
        openAttach()
        tap(OrderStrings.scanButton)

        composeRule.onNodeWithText(OrderStrings.saveOrder).assertIsDisplayed()
        assertEquals(1, count(OrderStrings.saveOrder).fetchSemanticsNodes().size, "one Save, pinned, not a second at the end of the list")
    }

    @Test
    fun backWithAHeldScanAsksFirstAndKeepingTheScanChangesNothing() {
        val nav = openAttach()
        tap(OrderStrings.scanButton)

        back()

        see(OrderStrings.leaveScanTitle).assertIsDisplayed()
        tapExact(OrderStrings.leaveScanKeep)
        assertEquals(Part2Destination.Attach(), nav.current)
        see(OrderStrings.pagesReady(1)).assertIsDisplayed()
    }

    @Test
    fun confirmingTheQuestionLeavesAndTheBankDropsTheScan() {
        val nav = openAttach()
        tap(OrderStrings.scanButton)
        back()

        tapExact(OrderStrings.leaveScanConfirm)

        assertEquals(Part2Destination.Bank, nav.current)
    }

    @Test
    fun theOrdersBankShortcutAndTheSystemBackAskToo() {
        val nav = openAttach()
        tap(OrderStrings.scanButton)

        tapExact(OrdersBankStrings.toBank)
        see(OrderStrings.leaveScanTitle).assertIsDisplayed()
        tapExact(OrderStrings.leaveScanKeep)
        systemBack()

        see(OrderStrings.leaveScanTitle).assertIsDisplayed()
        assertEquals(Part2Destination.Attach(), nav.current, "nothing left until the officer says so")
    }

    @Test
    fun withNothingScannedBackLeavesAtOnceWithNoQuestion() {
        val nav = openAttach()

        back()

        assertEquals(Part2Destination.Bank, nav.current)
        assertEquals(0, count(OrderStrings.leaveScanTitle).fetchSemanticsNodes().size)
    }
}
