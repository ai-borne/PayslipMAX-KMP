package com.payslipmax.pdfparser.part2.domain

/** Whether the previous order an order quotes is in the Bank. */
enum class ChainState { NO_QUOTE, FOUND, MISSING }

/**
 * An order's quoted last order number against the other orders' numbers, trimmed and case-folded. No quote means no flag. The order's own
 * number never answers its own quote. Numbers stay in memory: nothing here logs or stores one.
 */
object ChainCheck {
    fun of(
        order: VaultOrder,
        others: List<VaultOrder>,
    ): ChainState {
        val quoted = normal(order.lastOrderNo) ?: return ChainState.NO_QUOTE
        return if (others.any { it.id != order.id && normal(it.orderNo) == quoted }) ChainState.FOUND else ChainState.MISSING
    }

    private fun normal(number: String?): String? = number?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
}
