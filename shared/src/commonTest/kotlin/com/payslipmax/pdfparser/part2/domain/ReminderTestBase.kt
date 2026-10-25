package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * The shared fixtures of the reminder tests: synthetic drafts, orders and instants only. Instants are built here from the calendar and a
 * zone's offset, not by the planner, so a test cannot agree with the planner by reusing its arithmetic. The officer is in India (UTC+5:30)
 * unless a test says otherwise.
 */
abstract class ReminderTestBase {
    protected val india = 19_800_000L
    protected val hour = 3_600_000L
    protected val dayMillis = 86_400_000L
    protected val planner = ReminderPlanner { india }

    protected fun date(
        year: Int,
        month: Int,
        day: Int,
    ) = CalendarDate(year, month, day)

    /** The instant of [hourOfDay]:[minute] on [on] by a clock [offset] ahead of UTC. */
    protected fun at(
        on: CalendarDate,
        hourOfDay: Int = 9,
        minute: Int = 0,
        offset: Long = india,
    ): Long = (CivilCalendar.dayNumber(on) + CivilCalendar.UNIX_DAYS_TO_2000) * dayMillis + hourOfDay * hour + minute * 60_000L - offset

    protected fun request(
        code: String,
        vararg values: Pair<String, SlotValue>,
    ) = OccurrenceRequest(code, mapOf(*values))

    protected fun on(
        slot: String,
        on: CalendarDate,
    ) = slot to SlotValue.DateValue(on)

    protected fun year(
        year: Int,
        slot: String = "year",
    ) = slot to SlotValue.NumberValue(year)

    /** An annual-leave grant from [from] to [to], for leave year [leaveYear], with an optional last suffix day. */
    protected fun leave(
        from: CalendarDate,
        to: CalendarDate,
        suffixLast: CalendarDate? = null,
        leaveYear: Int = from.year,
        code: String = "GTDAL",
    ): OccurrenceRequest {
        val values = mutableListOf(on("leave_from", from), on("leave_to", to), year(leaveYear))
        if (suffixLast != null) values += on("suffix_last", suffixLast)
        return OccurrenceRequest(code, values.toMap())
    }

    protected fun draft(
        id: String,
        vararg requests: OccurrenceRequest,
        printedAt: Long? = null,
    ) = Part2Draft(id, LeaveEventInput(requests.toList()), emptyMap(), 1, 1, printedAt?.let { PrintedSnapshot(it, emptyMap(), emptyList()) })

    /** A vault order linked to [draftId]; by default it has a number and a real date, which is what makes it published. */
    protected fun order(
        id: String,
        draftId: String?,
        no: String? = "9/26",
        on: CalendarDate? = date(2026, 7, 1),
    ) = VaultOrder(id, draftId, no, on, 2, 2)

    protected fun cancelSlip(
        id: String,
        code: String,
        no: String,
        on: CalendarDate,
        printedAt: Long? = null,
    ) = draft(id, request(code, "cancelled" to SlotValue.RefValue(OrderRef(no, on))), printedAt = printedAt)

    protected fun plan(
        drafts: List<Part2Draft>,
        orders: List<VaultOrder> = emptyList(),
        now: Long,
    ): ReminderPlan = planner.plan(drafts, orders, now)

    protected fun ReminderPlan.all(): List<Reminder> = due.map { it.reminder } + upcoming

    protected fun ReminderPlan.of(kind: ReminderKind): List<Reminder> = all().filter { it.kind == kind }

    protected fun ReminderPlan.dueOf(kind: ReminderKind): List<DueReminder> = due.filter { it.reminder.kind == kind }

    protected fun ReminderPlan.upcomingOf(kind: ReminderKind): List<Reminder> = upcoming.filter { it.kind == kind }
}
