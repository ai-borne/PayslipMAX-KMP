package com.payslipmax.pdfparser.part2.print

/**
 * The slip as the page prints it, every line already worded (by the one place that words the preview, so the paper and the screen cannot
 * differ): the [title], the officer's [lastOrder] line, a [notice] that this is a draft for the clerk, then the [orders], and the
 * [pageLabel] each page carries (a template whose `{page}` and `{pages}` are filled in). It holds no identity, no check and no source label, only what the clerk keys in. [toString] hides everything: a slip must not reach a log.
 */
data class SlipDocument(
    val title: String,
    val lastOrder: String,
    val notice: String,
    val orders: List<SlipDocumentOrder>,
    val pageLabel: String,
) {
    override fun toString(): String = "SlipDocument(orders=${orders.size})"
}

/** One order: its heading (with the group letters it carries) and its rows in date order. */
data class SlipDocumentOrder(val title: String, val rows: List<SlipDocumentRow>) {
    override fun toString(): String = "SlipDocumentOrder(rows=${rows.size})"
}

/** One row: the code in bold, the narrative, then every column that holds something, one line each ([details]). */
data class SlipDocumentRow(val code: String, val narrative: String, val details: List<String>) {
    override fun toString(): String = "SlipDocumentRow(details=${details.size})"
}
