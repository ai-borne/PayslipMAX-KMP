package com.payslipmax.pdfparser.part2.domain

/** How many of an order's casualties are in the Bank: [entered] of the [expected] the order prints, or only [entered] when it printed none. */
data class OrderCompleteness(val entered: Int, val expected: Int?) {
    companion object {
        fun of(
            order: VaultOrder,
            entries: List<BankEntry>,
        ): OrderCompleteness = OrderCompleteness(OrderLink.covered(order.id, entries).size, order.expectedCount)
    }
}
