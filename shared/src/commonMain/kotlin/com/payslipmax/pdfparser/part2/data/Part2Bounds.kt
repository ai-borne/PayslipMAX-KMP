package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.VaultOrder

/**
 * Sizes, counts and token shapes of an entry or order, checked on the way in (the repository) and again on the way out (a stored row is
 * untrusted after a restore). Null means within bounds. Only size and shape are judged here; whether a typed date makes sense is the
 * form's call, so what the form accepted can always be saved.
 */
object Part2Bounds {
    fun check(entry: BankEntry): Part2WriteResult? {
        val ok =
            Part2Limits.isPlainId(entry.id) &&
                Part2Limits.isPlainToken(entry.code, Part2Limits.CODE_MAX) &&
                (entry.orderId?.let(Part2Limits::isPlainId) ?: true) &&
                entry.data.size == Part2Limits.DATA_COLUMNS &&
                entry.data.all { it.length <= Part2Limits.TEXT_MAX } &&
                entry.note.length <= Part2Limits.TEXT_MAX
        return if (ok) null else Part2WriteResult.REJECTED
    }

    fun check(order: VaultOrder): Part2WriteResult? {
        val ok = Part2Limits.isPlainId(order.id) && (order.orderNo?.let { it.length <= Part2Limits.ORDER_NO_MAX } ?: true)
        return if (ok) null else Part2WriteResult.REJECTED
    }
}
