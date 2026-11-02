package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.testing.FakePageShare
import com.payslipmax.pdfparser.testing.SyntheticBank
import com.payslipmax.pdfparser.ui.screens.guide.BankAccess
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import com.payslipmax.pdfparser.ui.screens.guide.part2.bank.BankTestBase
import com.payslipmax.pdfparser.ui.screens.guide.part2.orders.PageImageDecoder
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the officer scans an order to keep it and, sooner or later, to hand it on (the unit clerk, a gallery backup, a printout). Share or
 * save must hand the stored page itself to the share sheet, and only when he taps it, and say so when it cannot. The Orders Bank shortcut
 * must take him from the entry, attach and order screens straight to the Bank, so he is not made to back out of every screen he opened.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2ShareAndShortcutUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }
    private val share = FakePageShare()
    private val decoder = PageImageDecoder { android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888).asImageBitmap() }
    private val pageOne = SyntheticBank.jpeg(40, 1)

    private fun open(vararg above: GuideDestination): GuideNavState =
        show(GuideNavState(above.toList()), part2Bank = { bank }, bankAccess = BankAccess(true) { unlockTaps++ }, decoder = decoder, pageShare = share)

    private fun seedOrder() =
        runBlocking {
            data.repository.saveOrder(VaultOrder("o1", "SYN/10", CalendarDate(2026, 3, 20), 1L, 2L, 1, null))
            data.repository.addPage("o1", pageOne)
            data.repository.saveEntry(SyntheticBank.entry("e1", code = "FIRSTCODE", orderId = "o1"))
        }

    private fun tapExact(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    @Test
    fun shareOrSaveHandsTheStoredPageToTheShareSheetAndNothingIsSharedBeforeTheTap() {
        seedOrder()
        open(Part2Destination.Bank, Part2Destination.Order("o1"))
        assertTrue(share.shared.isEmpty(), "opening an order must send nothing anywhere")

        tapExact(OrderStrings.sharePage)

        assertEquals(1, share.shared.size)
        assertContentEquals(pageOne, share.shared.single(), "the officer gets the very picture he scanned")
    }

    @Test
    fun aPageThatCannotBeSharedSaysSoInWords() {
        seedOrder()
        share.fails = true
        open(Part2Destination.Bank, Part2Destination.Order("o1"))

        tapExact(OrderStrings.sharePage)

        see(OrderStrings.shareFailed).assertIsDisplayed()
        assertTrue(share.shared.isEmpty())
    }

    @Test
    fun theOrderScreenShortcutGoesStraightToTheBank() {
        seedOrder()
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Entry("e1"), Part2Destination.Order("o1"))

        tapExact(OrdersBankStrings.toBank)

        assertEquals(Part2Destination.Bank, nav.current)
    }

    @Test
    fun theEntryScreenShortcutGoesStraightToTheBank() {
        seedOrder()
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Entry("e1"))

        tapExact(OrdersBankStrings.toBank)

        assertEquals(Part2Destination.Bank, nav.current)
    }

    @Test
    fun theAttachScreenShortcutGoesStraightToTheBank() {
        seedOrder()
        val nav = open(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Entry("e1"), Part2Destination.Attach("e1"))

        tapExact(OrdersBankStrings.toBank)

        assertEquals(Part2Destination.Bank, nav.current)
    }
}
