package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate

/**
 * A published order in the vault: its pages are separate rows. The order number, date, the count of occurrences it prints ([expectedCount])
 * and the last order number it quotes ([lastOrderNo]) are typed after the scan, so all start unset.
 * [toString] hides everything: an order number is the officer's private data.
 */
data class VaultOrder(
    val id: String,
    val orderNo: String?,
    val orderDate: CalendarDate?,
    val createdAt: Long,
    val updatedAt: Long,
    val expectedCount: Int? = null,
    val lastOrderNo: String? = null,
) {
    override fun toString(): String = "VaultOrder(${if (orderNo == null) "unnumbered" else "numbered"})"
}

/** Where a stored page sits: its [id], the order it belongs to and its [position] in that order (0 first). No page content. */
data class VaultPageRef(
    val id: String,
    val orderId: String,
    val position: Int,
)
