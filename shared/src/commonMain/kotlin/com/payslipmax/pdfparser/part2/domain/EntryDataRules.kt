package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.guide.preclaim.DutyDays

/**
 * Which Data boxes behave specially, judged from the manual's own caption (the bundle carries no other type for a box). A box whose caption
 * asks for dd/mm/yyyy is a date: it takes the calendar picker and is kept as dd/mm/yyyy text. A box that counts days is pre-filled from the
 * From-To span, unless the caption is a running total (the manual's "total No of days"), which the officer still types.
 * Plain string scans only: Part II domain code uses no regex (see Part2LearnSecurityContractTest).
 */
object EntryDataRules {
    private const val DATE_CAPTION = "dd/mm/yyyy"
    private const val DATE_LENGTH = 10
    private const val DAY_DIGITS = 2
    private const val MONTH_START = 3
    private const val SECOND_SLASH = 5
    private const val YEAR_START = 6

    fun isDate(caption: String): Boolean = caption.contains(DATE_CAPTION, ignoreCase = true)

    /** The word "day" or "days" on its own (letters only, so "(days)" and "No. of days" count, "daysworth" does not). */
    fun isDayCount(caption: String): Boolean {
        if (isDate(caption) || caption.contains("total", ignoreCase = true)) return false
        val words = caption.lowercase().map { if (it.isLetter()) it else ' ' }.joinToString("").split(' ')
        return words.any { it == "day" || it == "days" }
    }

    /** The date a dd/mm/yyyy text holds, or null where it is not one. Only a real day (31 April is refused) counts. */
    fun parse(text: String): CalendarDate? {
        val value = text.trim()
        if (value.length != DATE_LENGTH || value[DAY_DIGITS] != '/' || value[SECOND_SLASH] != '/') return null
        val day = value.substring(0, DAY_DIGITS)
        val month = value.substring(MONTH_START, SECOND_SLASH)
        val year = value.substring(YEAR_START)
        if (!(day + month + year).all { it in '0'..'9' }) return null
        return CalendarDate(year.toInt(), month.toInt(), day.toInt()).takeIf { DutyDays.exists(it) }
    }
}
