package com.payslipmax.pdfparser.parser

/**
 * Reconstructs flat page text from positioned tokens in natural reading order (top-to-bottom rows,
 * left-to-right within a row), reusing [GridReconstructor]'s per-document row clustering. Used to feed
 * the income-tax / DSOP token pages to the existing text-based [parseTaxAndSavings] without any
 * platform-specific string extraction — those pages are flat key→value text, so a faithful linear
 * reading is all the regex parser needs.
 */
internal object TokenText {
    fun readingOrder(tokens: List<PositionedToken>): String {
        if (tokens.isEmpty()) return ""
        val grid = GridReconstructor.reconstruct(tokens)
        return grid.rows.joinToString(" ") { row -> row.cells.joinToString(" ") { it.text } }
    }
}
