package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate

/**
 * A leave draft as the officer left it: their [event] (the requests, answers and last order they typed) and the template revision each
 * drafted code had ([templateRevs], by code), so a later template change can be told from the officer's own. Never rendered text, except
 * [printed]: the rows as the clerk was last handed them, kept to tell a later edit from what was printed.
 * [dismissed] names the reminders the officer chose to stop seeing for this draft (`ReminderTask`); a completed print clears them, because a print
 * starts the reminders afresh. [toString] hides everything: a draft's dates, codes and order numbers must not reach a log through a string template.
 */
data class Part2Draft(
    val id: String,
    val event: LeaveEventInput,
    val templateRevs: Map<String, String>,
    val createdAt: Long,
    val updatedAt: Long,
    val printed: PrintedSnapshot? = null,
    val pairedWith: String? = null,
    val dismissed: Set<String> = emptySet(),
) {
    override fun toString(): String = "Part2Draft(requests=${event.requests.size})"
}

/**
 * A published order in the vault: its pages are separate rows. The order number and date are typed after the scan, so both start
 * unset. [draftId] says which draft it came from; deleting that draft leaves the order. [toString] hides everything, like [Part2Draft].
 */
data class VaultOrder(
    val id: String,
    val draftId: String?,
    val orderNo: String?,
    val orderDate: CalendarDate?,
    val createdAt: Long,
    val updatedAt: Long,
) {
    override fun toString(): String = "VaultOrder(${if (orderNo == null) "unnumbered" else "numbered"})"
}

/** Where a stored page sits: its [id], the order it belongs to and its [position] in that order (0 first). No page content. */
data class VaultPageRef(
    val id: String,
    val orderId: String,
    val position: Int,
)
