package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.DutyDays

/** One order on the shelf: the order, how many of its casualties are in the Bank and whether the order it quotes is. */
data class ShelfRow(val order: VaultOrder, val completeness: OrderCompleteness, val chain: ChainState)

/** The orders of one year of order date, or the orders with no date when [year] is null. */
data class ShelfYear(val year: Int?, val rows: List<ShelfRow>)

/** The shelf: the same data as the Bank, seen by order. Text rows, newest order date first, dated years first and undated orders last. */
object OrderShelf {
    fun view(
        orders: List<VaultOrder>,
        entries: List<BankEntry>,
    ): List<ShelfYear> {
        val rows =
            orders
                .sortedWith(compareByDescending<VaultOrder> { it.orderDate?.let(DutyDays::dayNumber) ?: Long.MIN_VALUE }.thenByDescending { it.createdAt }.thenBy { it.id })
                .map { ShelfRow(it, OrderCompleteness.of(it, entries), ChainCheck.of(it, orders)) }
        val dated = rows.filter { it.order.orderDate != null }.groupBy { it.order.orderDate!!.year }.map { (year, group) -> ShelfYear(year, group) }
        val undated = rows.filter { it.order.orderDate == null }
        return dated + if (undated.isEmpty()) emptyList() else listOf(ShelfYear(null, undated))
    }
}
