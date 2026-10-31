package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.DutyDays

/** The chips on the Bank list. */
enum class BankFilter { ALL, NOT_ATTACHED, ATTACHED }

/** How long an entry has waited, as numbers; the words are UI copy. */
sealed interface WaitingAge {
    data class Days(val count: Int) : WaitingAge

    data class Months(val count: Int) : WaitingAge

    data class YearsMonths(val years: Int, val months: Int) : WaitingAge
}

data class BankRow(val entry: BankEntry, val status: BankStatus, val age: WaitingAge?)

data class BankYear(val year: Int, val rows: List<BankRow>)

/** The list for one chip, plus the count behind each chip (always over the whole Bank). */
data class BankView(val years: List<BankYear>, val all: Int, val notAttached: Int, val attached: Int)

/**
 * The Bank list: newest From date first, grouped by the year of From, with a waiting age for rows that have no order. Pure, with the day
 * passed in: nothing here reads a clock or stores anything.
 */
object BankIndex {
    private const val DAYS_ONLY_BELOW = 30
    private const val MONTHS_IN_YEAR = 12

    fun view(
        entries: List<BankEntry>,
        orderIds: Set<String>,
        filter: BankFilter,
        today: CalendarDate,
    ): BankView {
        val rows = entries.map { BankRow(it, BankStatus.of(it, orderIds), null) }
        val waiting = rows.count { it.status == BankStatus.NOT_ATTACHED }
        val shown =
            rows
                .filter { filter == BankFilter.ALL || (it.status == BankStatus.NOT_ATTACHED) == (filter == BankFilter.NOT_ATTACHED) }
                .sortedWith(compareByDescending<BankRow> { DutyDays.dayNumber(it.entry.from) }.thenByDescending { it.entry.createdAt }.thenBy { it.entry.id })
                .map { if (it.status == BankStatus.NOT_ATTACHED) it.copy(age = ageOf(it.entry.from, today)) else it }
        val years = shown.groupBy { it.entry.from.year }.map { (year, group) -> BankYear(year, group) }
        return BankView(years, rows.size, waiting, rows.size - waiting)
    }

    /** Days under 30, then whole calendar months, then years and months; null while the From date is still ahead. */
    fun ageOf(
        from: CalendarDate,
        today: CalendarDate,
    ): WaitingAge? {
        val days = DutyDays.dayNumber(today) - DutyDays.dayNumber(from)
        if (days < 0) return null
        if (days < DAYS_ONLY_BELOW) return WaitingAge.Days(days.toInt())
        val whole = (today.year - from.year) * MONTHS_IN_YEAR + (today.month - from.month) - if (today.day < from.day) 1 else 0
        val months = whole.coerceAtLeast(1)
        return if (months < MONTHS_IN_YEAR) WaitingAge.Months(months) else WaitingAge.YearsMonths(months / MONTHS_IN_YEAR, months % MONTHS_IN_YEAR)
    }
}
