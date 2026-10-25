package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * The rule family for reminders: which requests set one and which answers they read. Like every `*Rules.kt` it may name codes (the planner
 * may not); the slot ids are the ones the templates declare, shared with [LeaveRules]. The planner asks this family, by role, and never
 * names a code itself. Every date it hands back is a real date, so the planner can count with it.
 */
object ReminderRules {
    /** The requests that carry a leave's dates, so the day the officer returns is known: the annual and the casual grants. */
    val RETURN_GRANTS: Set<String> = LeaveRules.ANNUAL_LEAVE + LeaveRules.CASUAL_LEAVE

    /** The requests that bring the officer back, so the return reminder is answered: rejoining, voluntary rejoining and recall. */
    val REJOINING: Set<String> = setOf("REJOINAL", "VOLREJ", "RECALL")

    /** The request for the year-end accumulation of a leave year, which answers the year-end reminder (its template joins in Phase 8). */
    const val YEAR_END_REQUEST = "ACCLVE"

    private const val REJOINED_ON = "rejoined_on"
    private const val RECALLED_ON = "recalled_on"

    /** The first day of the leave, or null when it was not answered or is not a real date. */
    fun leaveFrom(request: OccurrenceRequest): CalendarDate? = dateOf(request, LeaveRules.LEAVE_FROM)

    /** The day the officer returns: the day after the later of the last day of leave and the last suffix day. Null without a real last day of leave. */
    fun returnDay(request: OccurrenceRequest): CalendarDate? {
        if (request.code !in RETURN_GRANTS) return null
        val last = dateOf(request, LeaveRules.LEAVE_TO) ?: return null
        val suffix = dateOf(request, LeaveRules.SUFFIX_LAST)
        val later = if (suffix != null && CivilCalendar.dayNumber(suffix) > CivilCalendar.dayNumber(last)) suffix else last
        return CivilCalendar.nextDay(later)
    }

    /** The day a rejoining request says the officer came back, or null for any other request or an answer that is not a real date. */
    fun rejoinDay(request: OccurrenceRequest): CalendarDate? =
        if (request.code in REJOINING) dateOf(request, REJOINED_ON) ?: dateOf(request, RECALLED_ON) else null

    /**
     * The leave year an annual grant covers, or null for any other request or a year the calendar cannot hold. The range is not only tidiness:
     * storage does not bound a number (`Part2Bounds` judges sizes), so a crafted backup could carry any year, and the calendar counts years by
     * a loop, which a year of billions would turn into a hang. A year whose next 1 March falls outside the calendar (2100) is dropped by the planner.
     */
    fun annualYear(request: OccurrenceRequest): Int? =
        if (request.code in LeaveRules.ANNUAL_LEAVE) numberOf(request, LeaveRules.YEAR)?.takeIf { it in CivilCalendar.FIRST_YEAR..CivilCalendar.LAST_YEAR } else null

    /** The leave year a year-end request answers, or null for any other request or an unreadable year. */
    fun yearEndYear(request: OccurrenceRequest): Int? = if (request.code == YEAR_END_REQUEST) numberOf(request, LeaveRules.YEAR) else null

    private fun dateOf(
        request: OccurrenceRequest,
        slot: String,
    ): CalendarDate? = (request.values[slot] as? SlotValue.DateValue)?.date?.takeIf(CivilCalendar::exists)

    private fun numberOf(
        request: OccurrenceRequest,
        slot: String,
    ): Int? = (request.values[slot] as? SlotValue.NumberValue)?.number
}
