package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.AreaRules
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.RationRules
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder

/**
 * Invented drafts, orders and pages for storage tests: invented codes, dates and order numbers only, never anything from a real order.
 * [fullDraft] carries one value of every kind (date, number, text, order reference), both kinds of answer and a last order, so a
 * round trip that loses any of them fails.
 */
object SyntheticPart2Drafts {
    const val MARKER_TEXT = "synthetic-remark-zq9"
    const val MARKER_ORDER_NO = "SYN/4417/2026"

    fun fullDraft(
        id: String = "draft-1",
        updatedAt: Long = 1_700_000_000_000L,
    ): Part2Draft =
        Part2Draft(
            id = id,
            event =
                LeaveEventInput(
                    requests =
                        listOf(
                            OccurrenceRequest(
                                code = "SYNCODE1",
                                values =
                                    mapOf(
                                        "leave_from" to SlotValue.DateValue(CalendarDate(2026, 7, 5)),
                                        "leave_to" to SlotValue.DateValue(CalendarDate(2026, 9, 4)),
                                        "year" to SlotValue.NumberValue(2026),
                                        "remark" to SlotValue.TextValue(MARKER_TEXT),
                                        "grant" to SlotValue.RefValue(OrderRef(MARKER_ORDER_NO, CalendarDate(2026, 6, 30))),
                                    ),
                                answers = setOf(RationRules.ANSWER_ON_RATION, AreaRules.ANSWER_RH_PREFIX + "SYNRH12"),
                            ),
                            OccurrenceRequest(code = "SYNCODE2"),
                        ),
                    lastOrder = OrderRef("SYN/0001/2026", CalendarDate(2026, 1, 2)),
                ),
            templateRevs = mapOf("SYNCODE1" to "a1b2c3d4", "SYNCODE2" to "e5f6a7b8"),
            createdAt = updatedAt - 1000,
            updatedAt = updatedAt,
        )

    fun emptyDraft(
        id: String = "draft-empty",
        updatedAt: Long = 5L,
    ): Part2Draft = Part2Draft(id, LeaveEventInput(emptyList()), emptyMap(), updatedAt, updatedAt)

    fun order(
        id: String = "order-1",
        draftId: String? = "draft-1",
        orderNo: String? = MARKER_ORDER_NO,
        date: CalendarDate? = CalendarDate(2026, 7, 1),
        updatedAt: Long = 1_700_000_000_000L,
    ): VaultOrder = VaultOrder(id, draftId, orderNo, date, updatedAt - 1000, updatedAt)

    /** A page-sized byte string that starts like a JPEG; the stored bytes are never decoded as an image. */
    fun jpeg(
        size: Int,
        seed: Int = 1,
    ): ByteArray = ByteArray(size) { i -> if (i < 2) (if (i == 0) 0xFF else 0xD8).toByte() else ((i * 31 + seed) and 0xFF).toByte() }
}
