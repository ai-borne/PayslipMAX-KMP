package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.domain.BankEntry
import com.payslipmax.pdfparser.part2.domain.BankFilter
import com.payslipmax.pdfparser.part2.domain.BankIndex
import com.payslipmax.pdfparser.part2.domain.BankRow
import com.payslipmax.pdfparser.part2.domain.BankStatus
import com.payslipmax.pdfparser.part2.domain.ChainCheck
import com.payslipmax.pdfparser.part2.domain.ChainState
import com.payslipmax.pdfparser.part2.domain.OrderCompleteness
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.print.IndexDocument
import com.payslipmax.pdfparser.part2.print.IndexRow
import com.payslipmax.pdfparser.part2.print.IndexYear
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import com.payslipmax.pdfparser.ui.theme.IndexStrings
import com.payslipmax.pdfparser.ui.theme.OrderStrings
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings

/**
 * Words the whole Bank as the printed index: the same grouping, order, status words, counter and chain flag the Bank and the shelf show,
 * because they come from the same functions. It reads a casualty's code, From and To, the order it is attached to and the manual's name
 * for the code, and nothing else: the note and the Data columns are the officer's private text and are never read here.
 */
internal object IndexDocumentBuilder {
    fun build(
        entries: List<BankEntry>,
        orders: List<VaultOrder>,
        nameOf: (String) -> String?,
        today: CalendarDate,
    ): IndexDocument {
        val byId = orders.associateBy { it.id }
        val years =
            BankIndex.view(entries, byId.keys, BankFilter.ALL, today).years.map { year ->
                IndexYear(OrdersBankStrings.yearHeading(year.year), year.rows.map { row(it, byId, entries, orders, nameOf) })
            }
        return IndexDocument(IndexStrings.title, IndexStrings.printedOn(PreClaimDates.format(today)), IndexStrings.notice, years, IndexStrings.pageLabel)
    }

    private fun row(
        row: BankRow,
        byId: Map<String, VaultOrder>,
        entries: List<BankEntry>,
        orders: List<VaultOrder>,
        nameOf: (String) -> String?,
    ): IndexRow {
        val entry = row.entry
        val headline = entry.code + (nameOf(entry.code)?.let { IndexStrings.separator + it } ?: "")
        val dates = dateRange(entry.from, entry.to) + IndexStrings.separator + OrdersBankStrings.status(row.status)
        val order = if (row.status == BankStatus.ATTACHED) byId[entry.orderId] else null
        return IndexRow(headline, listOfNotNull(dates, order?.let { orderLine(it, entries, orders) }))
    }

    private fun orderLine(
        order: VaultOrder,
        entries: List<BankEntry>,
        orders: List<VaultOrder>,
    ): String {
        val parts = mutableListOf(IndexStrings.orderLine(order.orderNo, order.orderDate?.let(PreClaimDates::format)), OrderStrings.counter(OrderCompleteness.of(order, entries)))
        if (ChainCheck.of(order, orders) == ChainState.MISSING) parts += OrderStrings.checkChain
        return parts.joinToString(IndexStrings.separator)
    }
}
