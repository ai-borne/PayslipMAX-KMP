package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.part2.domain.BankRow
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.testing.SyntheticBank
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: on a Bank card the coloured status line is the casualty's order, so tapping it does the order job in one tap: open the order it
 * is on, or start attaching one to it. A lapsed officer may not attach (the Bank's rule is view, delete, back up), so for him a waiting
 * line does nothing of its own and the tap is the card's: open the entry.
 */
class BankStatusTapTest {
    private fun row(
        status: BankStatus,
        orderId: String? = null,
    ) = BankRow(SyntheticBank.entry("e1", orderId = orderId), status, null)

    @Test
    fun anAttachedLineOpensTheOrderItIsOn() {
        assertEquals(BankStatusTap.OpenOrder("o1"), BankStatusTap.of(row(BankStatus.ATTACHED, "o1"), canAdd = true))
        assertEquals(BankStatusTap.OpenOrder("o1"), BankStatusTap.of(row(BankStatus.ATTACHED, "o1"), canAdd = false), "a lapsed officer may still view")
    }

    @Test
    fun aWaitingLineStartsAttachingAnOrderToThatCasualty() {
        assertEquals(BankStatusTap.AttachTo("e1"), BankStatusTap.of(row(BankStatus.NOT_ATTACHED), canAdd = true))
    }

    @Test
    fun aLapsedOfficersWaitingLineIsNotATapOfItsOwn() {
        assertNull(BankStatusTap.of(row(BankStatus.NOT_ATTACHED), canAdd = false))
    }
}
