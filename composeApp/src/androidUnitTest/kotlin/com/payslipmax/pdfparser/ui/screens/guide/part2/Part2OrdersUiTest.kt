package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.OrderCompleteness
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.ScanFailure
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.PageImageDecoder
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: this is what the officer does with the paper: scan it, say which casualties it covers, and later find it again from the shelf. He
 * must see each way a scan can end in words, be refused a save that covers nobody, read the "personal reference copy" label and the chain
 * flag on the order, delete an order without losing his casualties, and, once lapsed, keep what he has without being able to add.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2OrdersUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }
    private val scanner = FakePageScanner()
    private val decoder = PageImageDecoder { android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap() }

    private fun open(
        vararg above: com.payslipmax.pdfparser.ui.screens.guide.GuideDestination,
        canAdd: Boolean = true,
    ): GuideNavState = show(GuideNavState(above.toList()), part2Bank = { bank }, bankAccess = BankAccess(canAdd) { unlockTaps++ }, scanner = scanner, decoder = decoder)

    private fun seedOrder(
        pages: Int = 2,
        expected: Int? = 5,
        last: String? = "SYN/gone",
    ) = runBlocking {
        data.repository.saveOrder(VaultOrder("o1", "SYN/10", CalendarDate(2026, 3, 20), 1L, 2L, expected, last))
        repeat(pages) { data.repository.addPage("o1", SyntheticBank.jpeg(40, it + 1)) }
        data.repository.saveEntry(SyntheticBank.entry("e1", code = "FIRSTCODE", orderId = "o1"))
        data.repository.saveEntry(SyntheticBank.entry("e2", code = "SECONDCODE", orderId = "o1"))
    }

    @Test
    fun theBankOffersAttachToAPremiumOfficerAndNotToALapsedOne() {
        open(Part2Destination.Bank)
        see(OrderStrings.attachTitle).assertIsDisplayed()

        assertEquals(1, count(OrderStrings.attachTitle).fetchSemanticsNodes().size)
    }

    @Test
    fun aLapsedOfficerIsOfferedPremiumInsteadOfTheAttachForm() {
        open(Part2Destination.Bank, Part2Destination.Attach(), canAdd = false)

        see(OrderStrings.upgradeToAttach).assertIsDisplayed()
        assertEquals(0, count(OrderStrings.scanButton).fetchSemanticsNodes().size)
    }

    @Test
    fun savingWithNobodyTickedSaysSoAndWritesNothing() {
        runBlocking { data.repository.saveEntry(SyntheticBank.entry("w1", code = "FIRSTCODE")) }
        scanner.next = PageScan.Pages(listOf(FakeScannedPage(byteArrayOf(1, 2, 3))))
        open(Part2Destination.Bank, Part2Destination.Attach())

        tap(OrderStrings.scanButton)
        tap(OrderStrings.saveOrder)

        see(OrderStrings.needCasualty).assertIsDisplayed()
        assertEquals(0, runBlocking { data.repository.pages("o1").size })
    }

    @Test
    fun aScanThatCouldNotStartAndOneThatFailedEachSayTheirOwnThing() {
        scanner.next = PageScan.Failed(ScanFailure.UNAVAILABLE)
        open(Part2Destination.Bank, Part2Destination.Attach())
        tap(OrderStrings.scanButton)
        see(OrderStrings.scanUnavailable).assertIsDisplayed()

        scanner.next = PageScan.Failed(ScanFailure.FAILED)
        tap(OrderStrings.scanButton)
        see(OrderStrings.scanFailed).assertIsDisplayed()
    }

    @Test
    fun scanTickAndSaveLandsOnTheOrderWithOneScanCoveringTheTickedCasualties() {
        runBlocking {
            data.repository.saveEntry(SyntheticBank.entry("w1", code = "FIRSTCODE"))
            data.repository.saveEntry(SyntheticBank.entry("w2", code = "SECONDCODE"))
        }
        scanner.next = PageScan.Pages(listOf(FakeScannedPage(byteArrayOf(1, 2, 3)), FakeScannedPage(byteArrayOf(4, 5, 6))))
        val nav = open(Part2Destination.Bank, Part2Destination.Attach())

        tap(OrderStrings.scanButton)
        see(OrderStrings.pagesReady(2)).assertIsDisplayed()
        tap("FIRSTCODE")
        tap("SECONDCODE")
        tap(OrderStrings.saveOrder)

        see(OrderStrings.referenceCopy).assertIsDisplayed()
        assertTrue(nav.current is Part2Destination.Order)
        val orderId = (nav.current as Part2Destination.Order).orderId
        assertEquals(2, runBlocking { data.repository.pages(orderId).size })
        assertEquals(listOf(orderId, orderId), runBlocking { listOf("w1", "w2").map { data.repository.entry(it)!!.orderId } })
    }

    @Test
    fun theOrderShowsItsLabelCounterChainFlagAndCasualties() {
        seedOrder()
        open(Part2Destination.Bank, Part2Destination.Order("o1"))

        see(OrderStrings.referenceCopy).assertIsDisplayed()
        see(OrderStrings.counter(OrderCompleteness(2, 5))).assertIsDisplayed()
        see(OrderStrings.chainMissing("SYN/gone")).assertIsDisplayed()
        see("FIRSTCODE").assertIsDisplayed()
        see("SECONDCODE").assertIsDisplayed()
    }

    @Test
    fun anOrderWithNoQuoteShowsNoChainFlagAndAnUnknownCountShowsOnlyTheEnteredNumber() {
        seedOrder(expected = null, last = null)
        open(Part2Destination.Bank, Part2Destination.Order("o1"))

        assertEquals(0, count(OrderStrings.chainMissing("")).fetchSemanticsNodes().size)
        see(OrderStrings.counter(OrderCompleteness(2, null))).assertIsDisplayed()
    }

    @Test
    fun aPageIsShownOnlyWhileAskedForAndHiddenAgain() {
        seedOrder(pages = 1)
        open(Part2Destination.Bank, Part2Destination.Order("o1"))
        assertEquals(0, count(OrderStrings.hidePage).fetchSemanticsNodes().size)

        tap(OrderStrings.showPage)
        composeRule.onNodeWithContentDescription(OrderStrings.pageNumber(1)).assertIsDisplayed()

        tap(OrderStrings.hidePage)
        assertEquals(0, composeRule.onAllNodesWithContentDescription(OrderStrings.pageNumber(1)).fetchSemanticsNodes().size)
    }

    @Test
    fun deletingTheOrderNeedsConfirmationThenReturnsAndLeavesTheCasualtiesNotAttached() {
        seedOrder()
        val nav = open(Part2Destination.Bank, Part2Destination.Order("o1"))

        tap(OrderStrings.deleteOrder)
        assertNotNull(runBlocking { data.repository.order("o1") }, "the dialog alone deletes nothing")
        tap(OrderStrings.confirm)

        assertNull(runBlocking { data.repository.order("o1") })
        assertEquals(Part2Destination.Bank, nav.current)
        assertEquals("o1", runBlocking { data.repository.entry("e1")!!.orderId }, "entries are not rewritten; their status is derived")
    }

    @Test
    fun aLapsedOfficerSeesTheOrderAndCanDeleteItsPagesAndItButNotAddPagesOrRemoveCasualties() {
        seedOrder()
        open(Part2Destination.Bank, Part2Destination.Order("o1"), canAdd = false)

        see(OrderStrings.referenceCopy).assertIsDisplayed()
        see(OrderStrings.deleteOrder).assertIsDisplayed()
        assertEquals(0, count(OrderStrings.addPages).fetchSemanticsNodes().size)
        assertEquals(0, count(OrderStrings.unlink).fetchSemanticsNodes().size)
        assertEquals(2, count(OrderStrings.deletePage).fetchSemanticsNodes().size, "the Bank rule is view, delete and back up: a page can be deleted")
    }

    @Test
    fun aLapsedOfficerCanDeleteOnePageAfterConfirmingAndTheOrderStays() {
        seedOrder()
        open(Part2Destination.Bank, Part2Destination.Order("o1"), canAdd = false)

        tap(OrderStrings.deletePage)
        assertEquals(2, runBlocking { data.repository.pages("o1").size }, "the dialog alone deletes nothing")
        tap(OrderStrings.confirm)

        assertEquals(1, runBlocking { data.repository.pages("o1").size })
        assertNotNull(runBlocking { data.repository.order("o1") })
    }

    @Test
    fun theShelfGroupsByYearShowsTheCounterAndOpensAnOrder() {
        seedOrder()
        val nav = open(Part2Destination.Bank, Part2Destination.Shelf)

        composeRule.onNodeWithText("2026").assertIsDisplayed()
        see("SYN/10").assertIsDisplayed()
        see(OrderStrings.checkChain).assertIsDisplayed()
        tap("SYN/10")
        assertEquals(Part2Destination.Order("o1"), nav.current)
    }

    @Test
    fun theLensSwitchTakesTheBankToTheShelfAndBack() {
        seedOrder()
        val nav = open(Part2Destination.Bank)

        tap(OrderStrings.lensOrders)
        assertEquals(Part2Destination.Shelf, nav.current)
        tap(OrderStrings.lensEntries)
        assertEquals(Part2Destination.Bank, nav.current)
    }

    @Test
    fun aWaitingCasualtyOffersToAttachAnOrderAndStartsTheFlowWithItTicked() {
        runBlocking { data.repository.saveEntry(SyntheticBank.entry("w1", code = "FIRSTCODE")) }
        val nav = open(Part2Destination.Bank, Part2Destination.Entry("w1"))

        assertEquals(0, count(OrderStrings.viewOrder).fetchSemanticsNodes().size)
        tap(OrderStrings.attachThisCasualty)

        assertEquals(Part2Destination.Attach("w1"), nav.current)
    }

    @Test
    fun anAttachedCasualtyOffersToViewItsOrderAndNotToAttachAgain() {
        seedOrder()
        val nav = open(Part2Destination.Bank, Part2Destination.Entry("e1"))

        assertEquals(0, count(OrderStrings.attachThisCasualty).fetchSemanticsNodes().size)
        tap(OrderStrings.viewOrder)

        assertEquals(Part2Destination.Order("o1"), nav.current)
    }

    @Test
    fun aLapsedOfficerCanViewTheOrderOfACasualtyButIsNeverOfferedToAttach() {
        seedOrder()
        open(Part2Destination.Bank, Part2Destination.Entry("e1"), canAdd = false)
        see(OrderStrings.viewOrder).assertIsDisplayed()
    }

    @Test
    fun aLapsedOfficerIsNotOfferedToAttachAWaitingCasualty() {
        runBlocking { data.repository.saveEntry(SyntheticBank.entry("w1", code = "FIRSTCODE")) }
        open(Part2Destination.Bank, Part2Destination.Entry("w1"), canAdd = false)

        assertEquals(0, count(OrderStrings.attachThisCasualty).fetchSemanticsNodes().size)
    }

    @Test
    fun aCasualtyOnTheOrderScreenOpensItsEntry() {
        seedOrder()
        val nav = open(Part2Destination.Bank, Part2Destination.Order("o1"))

        tap("FIRSTCODE")

        assertEquals(Part2Destination.Entry("e1"), nav.current)
    }

    @Test
    fun openingTheBankSweepsAnythingTheScannerLeftBehind() {
        open(Part2Destination.Bank)

        assertTrue(scanner.leftoversDiscarded >= 1, "the Bank empties the scanner's cache whenever it is the level on top")
    }

    @Test
    fun aRestoredOrderLevelSweepsTheScannersCache() {
        seedOrder()
        open(Part2Destination.Order("o1"))

        assertTrue(scanner.leftoversDiscarded >= 1, "an order level restored after process death may sit over a scan the dead process left")
    }

    @Test
    fun anOrderLevelDoesNotSweepAScanTheAttachFlowIsStillHolding() {
        seedOrder()
        val held = FakeScannedPage(byteArrayOf(1, 2, 3))
        scanner.next = PageScan.Pages(listOf(held))
        bank.attach.begin()
        runBlocking { bank.attach.scan(scanner) }

        open(Part2Destination.Order("o1"))

        assertEquals(0, scanner.leftoversDiscarded)
        assertTrue(!held.discarded && bank.attach.holdsScan())
    }
}
