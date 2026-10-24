package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.print.SlipDocument
import com.payslipmax.pdfparser.part2.print.SlipDocumentOrder
import com.payslipmax.pdfparser.part2.print.SlipDocumentRow
import com.payslipmax.pdfparser.ui.theme.Part2Strings

/**
 * The lines under a row's narrative, in column order: From and To, each Data column that holds something, the authority and the
 * certificate. The one place that decides them, so the preview, the paper and the HRMS check cannot disagree about what a row says.
 */
internal fun SlipRowUi.detailLines(): List<String> =
    listOfNotNull(Part2Strings.fromToLine(from, to).takeIf { it.isNotEmpty() }) +
        data.mapIndexedNotNull { i, text -> text.takeIf { it.isNotEmpty() }?.let { Part2Strings.dataLine(i + 1, it) } } +
        listOfNotNull(authority?.let(Part2Strings::authorityLine), certificate?.let(Part2Strings::certificateLine))

/** The slip as the paper prints it: the rows only, with the fixed lines of the app. No check, no source label, no tick: those are for the officer. */
internal fun SlipUi.toDocument(): SlipDocument =
    SlipDocument(
        title = Part2Strings.slipDocumentTitle,
        lastOrder = lastOrder,
        notice = Part2Strings.slipNotice,
        orders = orders.map { order -> SlipDocumentOrder(order.title, order.rows.map { SlipDocumentRow(it.code, it.narrative, it.detailLines()) }) },
        pageLabel = Part2Strings.pageLabel,
    )
