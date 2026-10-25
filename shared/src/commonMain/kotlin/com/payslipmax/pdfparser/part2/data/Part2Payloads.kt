package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.OrderRef
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedRow
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import kotlinx.serialization.Serializable

/**
 * The plaintext inside a draft or order row, before encryption. Versioned (`v`): a later app adds fields (they are ignored here) or,
 * for a change this one cannot read, raises `v` (the row is then unreadable, kept on disk, and never rewritten by this app). The schema
 * never changes for it. Flat classes with string kinds, not sealed hierarchies, so there is no polymorphic serializer for R8 to lose.
 * The row id is repeated inside: GCM covers the ciphertext, not the primary key beside it.
 */
internal const val PAYLOAD_VERSION = 1

/** One slot value or order reference: [k] is `d` (date y/m/d), `n` (number), `t` (text) or `r` (order [no] with date y/m/d). */
@Serializable
internal class SlotPayload(
    val k: String,
    val y: Int = 0,
    val m: Int = 0,
    val d: Int = 0,
    val n: Int = 0,
    val t: String = "",
    val no: String = "",
)

@Serializable
internal class RequestPayload(
    val code: String,
    val values: Map<String, SlotPayload> = emptyMap(),
    val answers: List<String> = emptyList(),
)

/** One printed row: the rendered text of its columns. */
@Serializable
internal class PrintedRowPayload(
    val code: String,
    val narrative: String,
    val from: String = "",
    val to: String = "",
    val data: List<String> = emptyList(),
    val authority: String? = null,
    val certificate: String? = null,
)

/** What the clerk was last handed: when, the template revisions and the rows. Optional inside `v: 1`: an older app ignores it, a draft never printed has none. */
@Serializable
internal class PrintedPayload(
    val at: Long,
    val revs: Map<String, String> = emptyMap(),
    val rows: List<PrintedRowPayload> = emptyList(),
)

@Serializable
internal class DraftPayload(
    val v: Int,
    val id: String,
    val createdAt: Long,
    val updatedAt: Long,
    val last: SlotPayload? = null,
    val requests: List<RequestPayload> = emptyList(),
    val revs: Map<String, String> = emptyMap(),
    val printed: PrintedPayload? = null,
    /** The draft this one was made with: a cancel slip and its re-draft point at each other. Optional inside `v: 1`. */
    val pair: String? = null,
)

@Serializable
internal class OrderPayload(
    val v: Int,
    val id: String,
    val draftId: String? = null,
    val orderNo: String? = null,
    val date: SlotPayload? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

private fun date(date: CalendarDate) = SlotPayload("d", date.year, date.month, date.day)

private fun slot(value: SlotValue): SlotPayload =
    when (value) {
        is SlotValue.DateValue -> date(value.date)
        is SlotValue.NumberValue -> SlotPayload("n", n = value.number)
        is SlotValue.TextValue -> SlotPayload("t", t = value.text)
        is SlotValue.RefValue -> ref(value.ref)
    }

private fun ref(ref: OrderRef) = SlotPayload("r", ref.date.year, ref.date.month, ref.date.day, no = ref.orderNo)

private fun SlotPayload.toRef(): OrderRef {
    require(k == "r")
    return OrderRef(no, CalendarDate(y, m, d))
}

private fun SlotPayload.toValue(): SlotValue =
    when (k) {
        "d" -> SlotValue.DateValue(CalendarDate(y, m, d))
        "n" -> SlotValue.NumberValue(n)
        "t" -> SlotValue.TextValue(t)
        "r" -> SlotValue.RefValue(toRef())
        else -> throw IllegalArgumentException("unknown slot kind")
    }

internal fun Part2Draft.toPayload() =
    DraftPayload(
        v = PAYLOAD_VERSION,
        id = id,
        createdAt = createdAt,
        updatedAt = updatedAt,
        last = event.lastOrder?.let(::ref),
        requests = event.requests.map { r -> RequestPayload(r.code, r.values.mapValues { slot(it.value) }, r.answers.toList()) },
        revs = templateRevs,
        printed = printed?.toPayload(),
        pair = pairedWith,
    )

private fun PrintedSnapshot.toPayload() =
    PrintedPayload(at, revs, rows.map { PrintedRowPayload(it.code, it.narrative, it.from, it.to, it.data, it.authority, it.certificate) })

private fun PrintedPayload.toSnapshot() =
    PrintedSnapshot(at, revs, rows.map { PrintedRow(it.code, it.narrative, it.from, it.to, it.data, it.authority, it.certificate) })

internal fun DraftPayload.toDraft() =
    Part2Draft(
        id = id,
        event =
            LeaveEventInput(
                requests = requests.map { r -> OccurrenceRequest(r.code, r.values.mapValues { it.value.toValue() }, r.answers.toSet()) },
                lastOrder = last?.toRef(),
            ),
        templateRevs = revs,
        createdAt = createdAt,
        updatedAt = updatedAt,
        printed = printed?.toSnapshot(),
        pairedWith = pair,
    )

internal fun VaultOrder.toPayload() =
    OrderPayload(PAYLOAD_VERSION, id, draftId, orderNo, orderDate?.let(::date), createdAt, updatedAt)

internal fun OrderPayload.toOrder() =
    VaultOrder(
        id = id,
        draftId = draftId,
        orderNo = orderNo,
        orderDate =
            date?.let { p ->
                require(p.k == "d")
                CalendarDate(p.y, p.m, p.d)
            },
        createdAt = createdAt,
        updatedAt = updatedAt,
    )
