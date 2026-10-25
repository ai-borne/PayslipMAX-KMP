package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.data.hasAngleBrackets

/**
 * Every edit of a vault order's number and date, under `Part2Limits`: the number is cut at `ORDER_NO_MAX` and stored as typed (a half-typed
 * value must save; [numberIssue] says why one is not quoted), a date off the calendar is refused. The edit time moves only when a value
 * changed, because it breaks the tie between two orders on one date.
 */
object VaultOrderEditor {
    /** A scan with nothing typed yet; [draftId] is the draft it was added from, null for an order added from the vault. */
    fun blank(
        id: String,
        draftId: String?,
        now: Long,
    ): VaultOrder = VaultOrder(id, draftId, null, null, createdAt = now, updatedAt = now)

    fun setNumber(
        order: VaultOrder,
        raw: String,
        now: Long,
    ): VaultOrder = changed(order, order.copy(orderNo = raw.take(Part2Limits.ORDER_NO_MAX).takeIf { it.isNotBlank() }), now)

    /** A date off the calendar changes nothing; null clears it. */
    fun setDate(
        order: VaultOrder,
        date: CalendarDate?,
        now: Long,
    ): VaultOrder = if (date != null && !CivilCalendar.exists(date)) order else changed(order, order.copy(orderDate = date), now)

    /** Why the typed number cannot be quoted on a slip, or null. */
    fun numberIssue(order: VaultOrder): DraftIssueReason? = if (order.orderNo?.let(::hasAngleBrackets) == true) DraftIssueReason.MARKUP else null

    private fun changed(
        before: VaultOrder,
        after: VaultOrder,
        now: Long,
    ): VaultOrder = if (after == before) before else after.copy(updatedAt = now)
}
