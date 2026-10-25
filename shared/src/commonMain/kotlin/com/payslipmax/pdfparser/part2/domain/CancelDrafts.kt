package com.payslipmax.pdfparser.part2.domain

/**
 * How a cancel slip and its re-draft start (Part II plan, phase 6c; DECISIONS Round 22). Para 11 (p.8): an order cannot be amended, so a
 * wrong one is cancelled and its occurrences notified afresh. Pure; the screens and the repository decide when and where they are saved.
 */
object CancelDrafts {
    /** A new slip with one request cancelling every occurrence of [order], whose number and date are typed or picked from the vault. */
    fun cancelEvent(order: OrderRef): LeaveEventInput =
        LeaveEventInput(listOf(OccurrenceRequest(CancelRules.ALL_OCCURRENCES, mapOf(CancelRules.CANCELLED to SlotValue.RefValue(order)))))

    /**
     * The corrected draft: every request of [source] with its answers, in order. The last-order line is read live from the vault and the
     * print record, ticks and template revisions belong to the old slip, so none of them is carried.
     */
    fun redraftEvent(source: LeaveEventInput): LeaveEventInput = LeaveEventInput(source.requests)

    /** Whether [event] cancels something: any of its requests is one of the cancel codes. */
    fun isCancel(event: LeaveEventInput): Boolean = event.requests.any { it.code in CancelRules.CODES }
}
