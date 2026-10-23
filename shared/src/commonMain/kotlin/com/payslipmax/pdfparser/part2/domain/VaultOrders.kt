package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.data.hasAngleBrackets

/** What the vault tells the rest of Part II. */
object VaultOrders {
    /**
     * The officer's last Part II order, which every slip quotes (para 6.7): the vault order with the latest order date among those that
     * have a number the slip can quote (within `Part2Limits.ORDER_NO_MAX`, plain text) and a real date (a scan whose number is not typed
     * yet is not an order to quote); the one edited last wins a tie. Null when the vault has none, which prints blank.
     */
    fun lastOrder(orders: List<VaultOrder>): OrderRef? =
        orders
            .mapNotNull { order ->
                val no = order.orderNo?.takeIf { it.isNotBlank() && it.length <= Part2Limits.ORDER_NO_MAX && !hasAngleBrackets(it) } ?: return@mapNotNull null
                val date = order.orderDate?.takeIf { CivilCalendar.exists(it) } ?: return@mapNotNull null
                Triple(CivilCalendar.dayNumber(date), order.updatedAt, OrderRef(no, date))
            }
            .maxWithOrNull(compareBy({ it.first }, { it.second }))
            ?.third
}
