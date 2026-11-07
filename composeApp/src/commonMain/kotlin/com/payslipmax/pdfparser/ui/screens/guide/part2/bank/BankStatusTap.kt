package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.part2.domain.BankRow
import com.payslipmax.pdfparser.part2.domain.BankStatus

/** What a tap on a Bank card's status line does: the line is the casualty's order, so it opens that order or starts attaching one. */
internal sealed interface BankStatusTap {
    data class OpenOrder(val orderId: String) : BankStatusTap

    data class AttachTo(val entryId: String) : BankStatusTap

    companion object {
        /** Null when the line does nothing of its own (a lapsed officer cannot attach), so the tap is the card's: open the entry. */
        fun of(
            row: BankRow,
            canAdd: Boolean,
        ): BankStatusTap? =
            when (row.status) {
                BankStatus.ATTACHED -> row.entry.orderId?.let(::OpenOrder)
                BankStatus.NOT_ATTACHED -> if (canAdd) AttachTo(row.entry.id) else null
            }
    }
}
