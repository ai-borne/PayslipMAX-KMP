package com.payslipmax.pdfparser.part2.domain

/** One line of a year-by-year list: the leave accumulated in [year]. */
data class YearDays(
    val year: Int,
    val days: Int,
)

/** The result of reading a year-by-year list: its rows, or the reason it was refused. */
sealed interface BreakupRead {
    data class Rows(val rows: List<YearDays>) : BreakupRead

    data class Refused(val reason: DraftIssueReason) : BreakupRead
}

/**
 * The year-by-year list a `years` slot holds (the break-up of an encashment): one line `YYYY D` per year, years strictly ascending. The
 * stored form is that text, so no new stored field exists and a draft keeps its break-up through backup and restore like any answer.
 * A line break or a semicolon ends a line, so the list can be typed one line a year in a box of several lines or on one line ("2011 30; 2013 30"). Reading is strict and plain-scan (no regex): digits only, the year in [FIRST_YEAR]..[LAST_YEAR], the days in 0..[Part2Limits.NUMBER_MAX].
 */
object YearlyBreakup {
    const val FIRST_YEAR = 1950
    const val LAST_YEAR = 2100
    private const val LINE_PARTS = 2
    private const val SEPARATOR = ';'

    fun parse(text: String): BreakupRead {
        if (text.isBlank()) return BreakupRead.Refused(DraftIssueReason.NOT_ANSWERED)
        if (text.length > Part2Limits.BREAKUP_TEXT_MAX) return BreakupRead.Refused(DraftIssueReason.TOO_LONG)
        if ('<' in text || '>' in text) return BreakupRead.Refused(DraftIssueReason.MARKUP)
        val rows = mutableListOf<YearDays>()
        for (line in text.replace(SEPARATOR, '\n').lines().map { it.trim() }.filter { it.isNotEmpty() }) {
            val parts = line.split(' ').filter { it.isNotEmpty() }
            if (parts.size != LINE_PARTS || !parts.all(::digits)) return BreakupRead.Refused(DraftIssueReason.WRONG_KIND)
            val year = parts[0].toIntOrNull()
            val days = parts[1].toIntOrNull()
            if (year == null || days == null || year !in FIRST_YEAR..LAST_YEAR || days > Part2Limits.NUMBER_MAX) return BreakupRead.Refused(DraftIssueReason.OUT_OF_RANGE)
            if (rows.isNotEmpty() && year <= rows.last().year) return BreakupRead.Refused(DraftIssueReason.WRONG_KIND)
            rows += YearDays(year, days)
            if (rows.size > Part2Limits.BREAKUP_ROWS_MAX) return BreakupRead.Refused(DraftIssueReason.TOO_LONG)
        }
        return if (rows.isEmpty()) BreakupRead.Refused(DraftIssueReason.NOT_ANSWERED) else BreakupRead.Rows(rows)
    }

    /** The canonical text of [rows]: one `YYYY D` line each, in the order given. */
    fun format(rows: List<YearDays>): String = rows.joinToString("\n") { "${it.year} ${it.days}" }

    fun total(rows: List<YearDays>): Int = rows.sumOf { it.days }

    private fun digits(part: String) = part.isNotEmpty() && part.all { it in '0'..'9' }
}
