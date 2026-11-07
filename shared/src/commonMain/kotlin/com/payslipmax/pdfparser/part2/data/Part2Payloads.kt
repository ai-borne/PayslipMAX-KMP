package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import kotlinx.serialization.Serializable

/**
 * The plaintext inside an entry or order row, before encryption. Versioned (`v`): a later app adds fields (they are ignored here) or,
 * for a change this one cannot read, raises `v` (the row is then unreadable, kept on disk, and never rewritten by this app). The schema
 * never changes for it. Flat classes, so there is no polymorphic serializer for R8 to lose. The row id is repeated inside: GCM covers
 * the ciphertext, not the primary key beside it.
 */
internal const val PAYLOAD_VERSION = 1

/**
 * A date as the frozen branch wrote an order's date (`k` is always `d`), so a scanned order from that branch still opens. The unused
 * fields of its old slot payload are ignored on read.
 */
@Serializable
internal class DatePayload(
    val y: Int,
    val m: Int,
    val d: Int,
    val k: String = "d",
)

@Serializable
internal class EntryPayload(
    val v: Int,
    val id: String,
    val code: String,
    val from: DatePayload,
    val to: DatePayload? = null,
    val data: List<String> = emptyList(),
    val note: String = "",
    val orderId: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
internal class OrderPayload(
    val v: Int,
    val id: String,
    val orderNo: String? = null,
    val date: DatePayload? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val expectedCount: Int? = null,
    val lastOrderNo: String? = null,
)

private fun date(date: CalendarDate) = DatePayload(date.year, date.month, date.day)

private fun DatePayload.toDate(): CalendarDate {
    require(k == "d")
    return CalendarDate(y, m, d)
}

internal fun BankEntry.toPayload() = EntryPayload(PAYLOAD_VERSION, id, code, date(from), to?.let(::date), data, note, orderId, createdAt, updatedAt)

internal fun EntryPayload.toEntry() =
    BankEntry(
        id = id,
        code = code,
        from = from.toDate(),
        to = to?.toDate(),
        data = (data + List(Part2Limits.DATA_COLUMNS) { "" }).take(Part2Limits.DATA_COLUMNS),
        note = note,
        orderId = orderId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

internal fun VaultOrder.toPayload() = OrderPayload(PAYLOAD_VERSION, id, orderNo, orderDate?.let(::date), createdAt, updatedAt, expectedCount, lastOrderNo)

internal fun OrderPayload.toOrder() = VaultOrder(id, orderNo, date?.toDate(), createdAt, updatedAt, expectedCount, lastOrderNo)
