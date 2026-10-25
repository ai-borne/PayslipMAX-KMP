package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * How a cancel slip and its re-draft start (Part II plan, phase 6c; DECISIONS Round 22). Para 11 (p.8): an order cannot be amended, so
 * the wrong one is cancelled and its occurrences notified afresh. The re-draft must carry what the officer already typed, and nothing
 * about the old print or the old last-order line, which belong to the old slip. Synthetic orders only.
 */
class CancelDraftsTest {
    private val order = OrderRef("0123/26", CalendarDate(2026, 6, 20))

    private val leave =
        LeaveEventInput(
            requests =
                listOf(
                    OccurrenceRequest(
                        "GTDAL",
                        mapOf("leave_from" to SlotValue.DateValue(CalendarDate(2026, 7, 6)), "year" to SlotValue.NumberValue(2026), "grant" to SlotValue.RefValue(order)),
                        setOf("on_ration", "rh:GTDRH12"),
                    ),
                    OccurrenceRequest("GTDCL", mapOf("remark" to SlotValue.TextValue("kept"))),
                ),
            lastOrder = OrderRef("0001/26", CalendarDate(2026, 1, 2)),
        )

    @Test
    fun aCancelEventIsOneCancelRequestQuotingTheOrderAndNothingElse() {
        val event = CancelDrafts.cancelEvent(order)

        assertEquals(listOf(OccurrenceRequest("CANCEL", mapOf("cancelled" to SlotValue.RefValue(order)))), event.requests)
        assertNull(event.lastOrder, "the vault's last order is read live, never copied in")
    }

    @Test
    fun aRedraftCarriesEveryRequestWithItsAnswersInOrderAndNothingOfTheOldLastOrder() {
        val event = CancelDrafts.redraftEvent(leave)

        assertEquals(leave.requests, event.requests)
        assertNull(event.lastOrder)
    }

    @Test
    fun aRedraftOfNothingIsNothing() {
        assertEquals(emptyList(), CancelDrafts.redraftEvent(LeaveEventInput(emptyList())).requests)
    }

    @Test
    fun aSlipIsACancelSlipWhenAnyRequestIsOneOfTheTwoCancelCodes() {
        assertTrue(CancelDrafts.isCancel(CancelDrafts.cancelEvent(order)))
        assertTrue(CancelDrafts.isCancel(LeaveEventInput(listOf(OccurrenceRequest("CANCELS")))))
        assertTrue(CancelDrafts.isCancel(LeaveEventInput(listOf(OccurrenceRequest("GTDAL"), OccurrenceRequest("CANCEL")))))
        assertFalse(CancelDrafts.isCancel(leave))
        assertFalse(CancelDrafts.isCancel(LeaveEventInput(emptyList())))
    }
}
