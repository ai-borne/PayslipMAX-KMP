package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.part2.domain.CancelState
import com.payslipmax.pdfparser.part2.domain.CancelStatus
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.Part2CancelStrings

/** Where cancelling an order stands, in words (DECISIONS Round 22): the one place that words `CancelStatus`, for the order's screen and for the rows that list it. */
internal object CancelNotes {
    /** The note for [order], or null when no slip has been handed over to cancel it. A print date is the day where the officer is ([localOffset]). */
    fun of(
        order: VaultOrder,
        drafts: List<Part2Draft>,
        orders: List<VaultOrder>,
        localOffset: (atMillis: Long) -> Long,
    ): String? =
        when (val state = CancelStatus.of(order, drafts, orders)) {
            null -> null
            is CancelState.SlipPrinted -> Part2CancelStrings.printedOn(state.scope, CivilCalendar.format(PreClaimDates.localDay(state.printedAt, localOffset)))
            is CancelState.CancelledBy -> Part2CancelStrings.cancelledBy(state.scope, state.order.orderNo, CivilCalendar.format(state.order.date))
        }
}
