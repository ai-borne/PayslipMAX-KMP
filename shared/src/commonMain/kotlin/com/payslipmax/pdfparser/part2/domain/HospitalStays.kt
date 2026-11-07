package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate

/**
 * Which hospital stays in the Bank have ended but carry no regularising entry, and which of those are over two years old (a delay past two
 * years needs a time-barred sanction before the Part II orders). Only counts and ids leave this object, and [toString] hides them.
 *
 * A stay starts at a HOSADM entry and is closed by the first final discharge (HOSDISCH) or report for duty (RPDUHSP) that falls before the
 * next admission. It is regularised when an SLC, REGAL or REGALMS entry starts on or before the admission day and ends on or after it (or
 * has no To). A stay with no closing entry is still open and is never noted. Pure: the clock is passed in, nothing is stored or logged.
 */
object HospitalStays {
    private const val ADMISSION = "HOSADM"
    private val closing = setOf("HOSDISCH", "RPDUHSP")
    private val regularising = setOf("SLC", "REGAL", "REGALMS")
    private const val BAR_YEARS = 2

    fun check(
        entries: List<BankEntry>,
        today: CalendarDate,
    ): HospitalCheck {
        val admissions = entries.filter { it.code == ADMISSION }.sortedBy { key(it.from) }
        val unregularised = mutableSetOf<String>()
        val barred = mutableSetOf<String>()
        admissions.forEachIndexed { i, admission ->
            val until = admissions.getOrNull(i + 1)?.let { key(it.from) } ?: Int.MAX_VALUE
            val ended = entries.any { it.code in closing && key(it.from) >= key(admission.from) && key(it.from) < until }
            if (ended && !isRegularised(admission, entries)) {
                unregularised += admission.id
                if (key(today) > key(twoYearsAfter(admission.from))) barred += admission.id
            }
        }
        return HospitalCheck(unregularised, barred)
    }

    private fun isRegularised(
        admission: BankEntry,
        entries: List<BankEntry>,
    ): Boolean =
        entries.any {
            it.code in regularising && key(it.from) <= key(admission.from) && (it.to == null || key(it.to) >= key(admission.from))
        }

    /** A 29 February admission turns two on 28 February, since the later year has no leap day. */
    private fun twoYearsAfter(date: CalendarDate) = CalendarDate(date.year + BAR_YEARS, date.month, if (date.month == 2 && date.day == 29) 28 else date.day)

    private fun key(date: CalendarDate) = date.year * 10_000 + date.month * 100 + date.day
}

/** The admissions (by entry id) that are unregularised, and the subset that is time-barred. Contents are hidden from every string. */
data class HospitalCheck(
    val unregularised: Set<String>,
    val timeBarred: Set<String>,
) {
    override fun toString(): String = "HospitalCheck(hidden)"
}
