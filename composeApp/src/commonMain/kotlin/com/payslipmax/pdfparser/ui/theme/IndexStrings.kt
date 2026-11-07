package com.payslipmax.pdfparser.ui.theme

/**
 * Copy for the printed Part II orders index (docs/Plan/13_OrdersBank_PhasePlan.md, Phase 5). The status words, the counter and the chain
 * flag are the Bank's and the shelf's own (`OrdersBankStrings`, `OrderStrings`), so the paper says what the screen says. The job name is
 * fixed: the system keeps it, so nothing the officer typed may ever be part of it.
 */
object IndexStrings {
    const val title = "Part II orders index"
    const val jobName = "Part II orders index"
    const val printButton = "Print or save as PDF"
    const val notice = OrderStrings.referenceCopy

    /** The page label template: the layout fills in `{page}` and `{pages}`. */
    const val pageLabel = "Page {page} of {pages}"

    /** Between the parts of one printed line. */
    const val separator = " · "

    fun printedOn(date: String): String = "Printed on $date"

    /** "Order no. 12/2024, 05/03/2024"; an order with no number or date says so in the shelf's words. */
    fun orderLine(
        orderNo: String?,
        date: String?,
    ): String = (orderNo?.let { "Order no. $it" } ?: OrderStrings.noOrderNumber) + ", " + (date ?: OrderStrings.noOrderDate)
}
