package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.PrintedRow
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder

/**
 * Sizes, counts and token shapes of a draft or order, checked on the way in (the repository) and again on the way out (a stored row
 * is untrusted after a restore). Null means within bounds. Only size and shape are judged here; whether a typed date or number makes
 * sense is the engine's call, so a half-filled form can always be saved.
 */
object Part2Bounds {
    fun check(draft: Part2Draft): Part2WriteResult? {
        val ok =
            Part2Limits.isPlainId(draft.id) &&
                draft.event.requests.size <= Part2Limits.REQUESTS_MAX &&
                draft.event.requests.all(::request) &&
                (draft.event.lastOrder?.let(::ref) ?: true) &&
                revs(draft.templateRevs) &&
                (draft.printed?.let(::printed) ?: true) &&
                (draft.pairedWith?.let(Part2Limits::isPlainId) ?: true)
        return if (ok) null else Part2WriteResult.REJECTED
    }

    fun check(order: VaultOrder): Part2WriteResult? {
        val ok =
            Part2Limits.isPlainId(order.id) &&
                (order.draftId?.let(Part2Limits::isPlainId) ?: true) &&
                (order.orderNo?.let { it.length <= Part2Limits.ORDER_NO_MAX } ?: true)
        return if (ok) null else Part2WriteResult.REJECTED
    }

    private fun revs(revs: Map<String, String>): Boolean =
        revs.size <= Part2Limits.REVS_MAX &&
            revs.all { (code, rev) -> Part2Limits.isPlainToken(code, Part2Limits.CODE_MAX) && rev.length <= Part2Limits.REV_MAX }

    /** What the clerk was handed: counts, token shapes and the length of each rendered column. */
    private fun printed(snapshot: PrintedSnapshot): Boolean =
        snapshot.at >= 0 && revs(snapshot.revs) && snapshot.rows.size <= Part2Limits.PRINTED_ROWS_MAX && snapshot.rows.all(::row)

    private fun row(row: PrintedRow): Boolean =
        Part2Limits.isPlainToken(row.code, Part2Limits.CODE_MAX) &&
            row.data.size <= Part2Limits.PRINTED_DATA_MAX &&
            (listOf(row.narrative, row.from, row.to) + row.data + listOfNotNull(row.authority, row.certificate)).all { it.length <= Part2Limits.PRINTED_TEXT_MAX }

    private fun request(request: OccurrenceRequest): Boolean =
        Part2Limits.isPlainToken(request.code, Part2Limits.CODE_MAX) &&
            request.values.size <= Part2Limits.VALUES_PER_REQUEST_MAX &&
            request.values.all { (slot, value) -> Part2Limits.isPlainToken(slot, Part2Limits.SLOT_ID_MAX) && value(value) } &&
            request.answers.size <= Part2Limits.ANSWERS_PER_REQUEST_MAX &&
            request.answers.all { it.length <= Part2Limits.ANSWER_MAX }

    private fun value(value: SlotValue): Boolean =
        when (value) {
            is SlotValue.TextValue -> value.text.length <= Part2Limits.TEXT_MAX
            is SlotValue.RefValue -> ref(value.ref)
            is SlotValue.DateValue, is SlotValue.NumberValue -> true
        }

    private fun ref(ref: OrderRef): Boolean = ref.orderNo.length <= Part2Limits.ORDER_NO_MAX
}
