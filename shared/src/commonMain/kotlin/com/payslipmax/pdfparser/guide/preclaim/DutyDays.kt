package com.payslipmax.pdfparser.guide.preclaim

import com.payslipmax.pdfparser.calendar.CivilCalendar

/** The pre-claim keeps its name for the calendar date; the type itself moved to `CivilCalendar.kt` so Part II shares it. */
typealias CalendarDate = com.payslipmax.pdfparser.calendar.CalendarDate

internal sealed interface DutySpan {
    data class Days(val count: Int) : DutySpan

    data class Invalid(val issue: PreClaimIssue) : DutySpan
}

/**
 * Total Absence as the portal counts it: the To date minus the From date, both days counted (57 and 61 on the owner's claims). The
 * counting is [CivilCalendar]'s; this only words a refusal as the pre-claim issue the UI shows.
 */
internal object DutyDays {
    fun inclusive(
        from: CalendarDate?,
        to: CalendarDate?,
    ): DutySpan {
        if (from == null || to == null) return DutySpan.Invalid(PreClaimIssue.DATES_MISSING)
        if (!CivilCalendar.exists(from) || !CivilCalendar.exists(to)) return DutySpan.Invalid(PreClaimIssue.DATE_INVALID)
        val days = CivilCalendar.inclusiveDays(from, to) ?: return DutySpan.Invalid(PreClaimIssue.DATES_REVERSED)
        return DutySpan.Days(days)
    }
}
