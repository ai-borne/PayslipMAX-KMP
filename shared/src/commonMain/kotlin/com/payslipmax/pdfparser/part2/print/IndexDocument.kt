package com.payslipmax.pdfparser.part2.print

/**
 * The orders index as the page prints it, every line already worded (by the one place that words it, so the paper and the Bank cannot
 * differ): the [title], the [printedOn] line, a [notice] that this is a personal reference copy, then the [years], and the [pageLabel] each
 * page carries (a template whose `{page}` and `{pages}` are filled in). It holds only what the officer typed and the manual's casualty
 * names: no identity, no note, no Data column. [toString] hides everything: an index must not reach a log.
 */
data class IndexDocument(
    val title: String,
    val printedOn: String,
    val notice: String,
    val years: List<IndexYear>,
    val pageLabel: String,
) {
    override fun toString(): String = "IndexDocument(years=${years.size})"
}

/** One year of From dates: its heading and its rows, newest first. */
data class IndexYear(val heading: String, val rows: List<IndexRow>) {
    override fun toString(): String = "IndexYear(rows=${rows.size})"
}

/** One casualty: its code and name in bold, then one line each of [details]. */
data class IndexRow(val headline: String, val details: List<String>) {
    override fun toString(): String = "IndexRow(details=${details.size})"
}
