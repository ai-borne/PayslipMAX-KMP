package com.payslipmax.pdfparser.ui.screens.guide.part2

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.VaultOrder
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
import kotlin.test.assertTrue

/**
 * WHY: the officer came to the Bank to see an order or to attach one, and had to open the entry first and then the order (Bank, entry,
 * order). The status line on a card now goes straight there; the rest of the card still opens the entry to edit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h6000dp")
class Part2BankStatusTapUiTest : Part2UiBase() {
    private val data = BankTestBase()
    private val bank by lazy { data.bank() }

    private fun open(
        attached: Boolean,
        canAdd: Boolean = true,
    ): GuideNavState {
        runBlocking {
            if (attached) data.repository.saveOrder(VaultOrder("o1", "SYN/10", CalendarDate(2026, 3, 20), 1L, 2L, 1, null))
            data.repository.saveEntry(SyntheticBank.entry("e1", code = "FIRSTCODE", orderId = "o1".takeIf { attached }))
        }
        return show(GuideNavState(listOf(Part2Destination.Hub, Part2Destination.Bank)), part2Bank = { bank }, bankAccess = BankAccess(canAdd) {})
    }

    private fun tapStatus(status: BankStatus) {
        composeRule.onNodeWithText(OrdersBankStrings.status(status)).performClick()
        settle()
    }

    @Test
    fun theAttachedLineOpensTheOrderStraightFromTheBank() {
        val nav = open(attached = true)

        tapStatus(BankStatus.ATTACHED)

        assertEquals(listOf(Part2Destination.Hub, Part2Destination.Bank, Part2Destination.Order("o1")), nav.stack.drop(1))
    }

    @Test
    fun theWaitingLineOpensAttachAnOrderWithThatCasualtyTicked() {
        val nav = open(attached = false)

        tapStatus(BankStatus.NOT_ATTACHED)

        assertEquals(Part2Destination.Attach("e1"), nav.current)
        assertTrue("e1" in bank.attach.state.value.ticked)
    }

    @Test
    fun theRestOfTheCardStillOpensTheEntry() {
        val nav = open(attached = true)

        tap("FIRSTCODE")

        assertEquals(Part2Destination.Entry("e1"), nav.current)
    }

    @Test
    fun aLapsedOfficersWaitingLineOpensTheEntryLikeTheRestOfTheCard() {
        val nav = open(attached = false, canAdd = false)

        tapStatus(BankStatus.NOT_ATTACHED)

        assertEquals(Part2Destination.Entry("e1"), nav.current)
    }
}
