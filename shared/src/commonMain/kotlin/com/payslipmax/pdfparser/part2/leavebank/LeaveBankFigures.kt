package com.payslipmax.pdfparser.part2.leavebank

import com.payslipmax.pdfparser.domain.LeaveCasCode
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.insights.LeaveBankSummary
import com.payslipmax.pdfparser.part2.domain.AcclveCalculator
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.part2.domain.YearDays
import com.payslipmax.pdfparser.part2.domain.YearlyBreakup

/** What to offer for the year-end accumulation of [year]: every figure is a suggestion the officer can overwrite, and null where nothing is known. */
data class AcclveSuggestion(
    val previousTotal: Int?,
    val ltcDays: Int,
    val accrued: Int,
    val alUsed: Int,
)

/** The drafted total of a year against the statement of the year after (which prints ACCLVE of that year). Only ever a message: nothing is changed by it. */
sealed interface CrossCheck {
    data object NoStatement : CrossCheck

    data class Matches(val total: Int) : CrossCheck

    data class Differs(
        val drafted: Int,
        val statement: Int,
    ) : CrossCheck
}

/**
 * The Leave Bank's rows turned into figures for the year-end orders (Part II plan, phase 8). It is the only place Part II reads Leave Bank
 * rows, and it takes the rows alone ([LeaveEntry]) from the one summary the app derives (`LeaveBankCalculator.summarize`): the slip engine
 * never sees a payslip. An ACCLVE row dated 31 December of Y is the accumulation of Y, printed on the statement of Y+1; an LVELTC row is dated
 * on the day of the encashment. The statement's own figures are never recomputed or corrected here.
 */
object LeaveBankFigures {
    private val newestLast = compareBy<LeaveEntry>({ it.casDate }, { it.orderDate })

    /** The rows of [summary]'s statement, or none when the app holds no statement that prints the table. */
    fun entriesOf(summary: LeaveBankSummary?): List<LeaveEntry> = summary?.entries.orEmpty()

    fun suggest(
        year: Int,
        entries: List<LeaveEntry>,
        earlierDraftTotal: Int?,
        alUsed: Int,
    ): AcclveSuggestion =
        AcclveSuggestion(
            previousTotal = accrual(entries, year - 1)?.totalDays ?: earlierDraftTotal,
            ltcDays = entries.filter { it.casCode == LeaveCasCode.LTC_ENCASHED && yearOf(it) == year }.sumOf { it.days },
            accrued = AcclveCalculator.accrual(alUsed),
            alUsed = alUsed,
        )

    fun crossCheck(
        year: Int,
        draftedTotal: Int,
        entries: List<LeaveEntry>,
    ): CrossCheck {
        val statement = accrual(entries, year)?.totalDays ?: return CrossCheck.NoStatement
        return if (statement == draftedTotal) CrossCheck.Matches(statement) else CrossCheck.Differs(draftedTotal, statement)
    }

    /** The year-by-year accumulation the statement shows, oldest year first, one row a year (the newest row of a year wins), for the encashment's break-up. */
    fun breakup(entries: List<LeaveEntry>): List<YearDays> =
        entries.filter { it.casCode == LeaveCasCode.ACCRUED }.sortedWith(newestLast)
            .mapNotNull { entry -> yearOf(entry)?.takeIf { it in YearlyBreakup.FIRST_YEAR..YearlyBreakup.LAST_YEAR && entry.days in 0..Part2Limits.NUMBER_MAX }?.let { YearDays(it, entry.days) } }
            .associateBy { it.year }.values.sortedBy { it.year }.takeLast(Part2Limits.BREAKUP_ROWS_MAX)

    /** The years between the first and the last of [rows] that have no row, for the officer to type if they accumulated leave in them. */
    fun missingYears(rows: List<YearDays>): List<Int> {
        if (rows.isEmpty()) return emptyList()
        val present = rows.map { it.year }.toSet()
        val first = rows.minOf { it.year }
        val last = rows.maxOf { it.year }
        return (first..last).filter { it !in present }
    }

    private fun accrual(
        entries: List<LeaveEntry>,
        year: Int,
    ): LeaveEntry? = entries.filter { it.casCode == LeaveCasCode.ACCRUED && yearOf(it) == year }.maxWithOrNull(newestLast)

    /** The year of an ISO `yyyy-MM-dd` date, or null for anything that is not one. */
    private fun yearOf(entry: LeaveEntry): Int? = entry.casDate.takeIf { it.length == ISO_LENGTH && it[YEAR_DIGITS] == '-' }?.take(YEAR_DIGITS)?.takeIf { y -> y.all { it in '0'..'9' } }?.toIntOrNull()

    private const val ISO_LENGTH = 10
    private const val YEAR_DIGITS = 4
}
