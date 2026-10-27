package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Which drafts still count (DECISIONS Round 25): one whose published order was cancelled in full is void, and a void draft sets no reminder and
 * supplies no figure (the year-end fill reads the leave used from the live ones). The planner and the fill read this one answer. Synthetic data.
 */
class CancelStatusLiveTest {
    private val day = CalendarDate(2026, 6, 20)
    private val order = VaultOrder("o1", "leave", "0123/26", day, 1, 1)

    private fun draft(
        id: String,
        vararg requests: OccurrenceRequest,
        printed: Boolean = true,
    ) = Part2Draft(id, LeaveEventInput(requests.toList()), emptyMap(), 1, 1, if (printed) PrintedSnapshot(5, emptyMap(), emptyList()) else null)

    private fun cancel(code: String) = OccurrenceRequest(code, mapOf("cancelled" to SlotValue.RefValue(OrderRef("0123/26", day))))

    private val leave = draft("leave", OccurrenceRequest("GTDAL"))

    @Test
    fun aDraftWhoseOrderWasCancelledInFullByAPrintedSlipIsVoidAndEveryOtherDraftStays() {
        val other = draft("other", OccurrenceRequest("GTDAL"))
        val cancelSlip = draft("cancel", cancel("CANCEL"))
        assertEquals(listOf(other, cancelSlip), CancelStatus.liveDrafts(listOf(leave, other, cancelSlip), listOf(order)))
    }

    @Test
    fun aCancelOfOneOccurrenceAnUnprintedCancelSlipOrNoOrderVoidsNothing() {
        val one = draft("one", cancel("CANCELS"))
        val unprinted = draft("unprinted", cancel("CANCEL"), printed = false)
        assertEquals(listOf(leave, one), CancelStatus.liveDrafts(listOf(leave, one), listOf(order)), "CANCELS covers one occurrence only")
        assertEquals(listOf(leave, unprinted), CancelStatus.liveDrafts(listOf(leave, unprinted), listOf(order)), "nothing was handed to the clerk")
        assertEquals(listOf(leave, draft("cancel", cancel("CANCEL"))), CancelStatus.liveDrafts(listOf(leave, draft("cancel", cancel("CANCEL"))), emptyList()), "no order of the draft is in the vault")
    }

    @Test
    fun theOrderOfTheDraftsIsKept() {
        val a = draft("a", OccurrenceRequest("GTDAL"))
        val b = draft("b", OccurrenceRequest("GTDAL"))
        assertEquals(listOf(b, a), CancelStatus.liveDrafts(listOf(b, a), listOf(order)))
    }
}
