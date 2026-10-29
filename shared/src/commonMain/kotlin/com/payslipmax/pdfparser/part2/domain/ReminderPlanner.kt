package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.calendar.CivilCalendar

/**
 * Works out every reminder from the drafts and the vault, and owns all the timing: when a reminder falls, in which day, and whether it is
 * due. Pure: the instant is handed in and [offsetAt] gives the device's offset from UTC at an instant (it moves with daylight saving), so a
 * test can place the officer anywhere. Nothing is stored; the same drafts, orders and instant always give the same plan, so a rebuild is safe
 * to run as often as the drafts change. The planner names no code: which requests set a reminder is [ReminderRules]'.
 *
 * Only a printed draft sets a reminder (a draft never handed over is not an event yet). A draft whose published order has been cancelled in
 * full is void (the cancel is read from [CancelStatus], never stored) and neither sets nor answers a reminder.
 */
class ReminderPlanner(
    private val policy: ReminderPolicy = ReminderPolicy(),
    private val offsetAt: (atMillis: Long) -> Long,
) {
    /** One reminder with the number of the day it falls on, counted as `CivilCalendar.dayNumber` counts. */
    private class Dated(val reminder: Reminder, val day: Long)

    /** A leave as far as another leave's return is concerned: its first day and the day the officer returns, counted as days. */
    private class Span(val from: Long, val back: Long)

    /** The reminders one cause gives, in date order, and the instant the last of them stops being due (null: it stays until answered). */
    private class Series(val events: List<Dated>, val endsAt: Long? = null)

    fun plan(
        drafts: List<Part2Draft>,
        orders: List<VaultOrder>,
        nowMillis: Long,
    ): ReminderPlan {
        val ordersOf = orders.groupBy { it.draftId }
        val live = CancelStatus.liveDrafts(drafts, orders)
        val requests = live.flatMap { it.event.requests }
        val rejoined = requests.mapNotNull(ReminderRules::rejoinDay)
        val answeredYears = requests.mapNotNull(ReminderRules::yearEndYear).toSet()
        val dismissed = live.flatMapTo(HashSet()) { it.dismissed }
        val printed = live.filter { it.printed != null }
        val spans = printed.flatMap { draft -> draft.event.requests.mapNotNull(::span) }
        val series =
            printed.mapNotNull { returnSeries(it, rejoined, spans) } +
                printed.mapNotNull { addOrderSeries(it, ordersOf[it.id].orEmpty()) } +
                yearEndSeries(printed, answeredYears, dismissed)
        return classify(series, nowMillis)
    }

    private fun classify(
        series: List<Series>,
        nowMillis: Long,
    ): ReminderPlan {
        val today = localDayNumber(nowMillis)
        val due = mutableListOf<DueReminder>()
        val upcoming = mutableListOf<Reminder>()
        for (one in series) {
            val (passed, coming) = one.events.partition { it.reminder.at <= nowMillis }
            upcoming += coming.map { it.reminder }
            val current = passed.lastOrNull() ?: continue
            val ended = one.endsAt != null && nowMillis >= one.endsAt
            if (!ended) due += DueReminder(current.reminder, overdue = today > current.day)
        }
        val byDate = compareBy<Reminder>({ it.at }, { it.key })
        return ReminderPlan(
            due = due.sortedWith(compareBy(byDate) { it.reminder }),
            upcoming = upcoming.sortedWith(byDate).take(policy.upcomingMax),
        )
    }

    private fun span(request: OccurrenceRequest): Span? {
        val from = ReminderRules.leaveFrom(request) ?: return null
        val back = ReminderRules.returnDay(request) ?: return null
        return Span(CivilCalendar.dayNumber(from), CivilCalendar.dayNumber(back))
    }

    /**
     * "Draft the rejoining slip" on the day the officer returns, until a rejoining falls between the leave's first day and that day. Not while
     * another printed leave starts on or before that day and returns after it: the officer is still away, and that leave asks in its turn.
     */
    private fun returnSeries(
        draft: Part2Draft,
        rejoined: List<CalendarDate>,
        spans: List<Span>,
    ): Series? {
        if (ReminderTask.RETURN in draft.dismissed) return null
        val requests = draft.event.requests
        val last = requests.mapNotNull(ReminderRules::returnDay).maxByOrNull(CivilCalendar::dayNumber) ?: return null
        val first = requests.mapNotNull(ReminderRules::leaveFrom).minByOrNull(CivilCalendar::dayNumber)
        val lastDay = CivilCalendar.dayNumber(last)
        val firstDay = first?.let(CivilCalendar::dayNumber) ?: Long.MIN_VALUE
        if (rejoined.any { CivilCalendar.dayNumber(it) in firstDay..lastDay }) return null
        // A draft's own leaves never return later than its latest return, so only another draft's leave can satisfy this.
        if (spans.any { it.from <= lastDay && it.back > lastDay }) return null
        return Series(listOfNotNull(dated(ReminderKind.RETURN, draft.id, lastDay)))
    }

    /** "Add the published order" from the latest print, until a numbered, dated order is linked to the draft. */
    private fun addOrderSeries(
        draft: Part2Draft,
        linked: List<VaultOrder>,
    ): Series? {
        val printedAt = draft.printed?.at ?: return null
        if (ReminderTask.ADD_ORDER in draft.dismissed) return null
        if (VaultOrders.quotable(linked).isNotEmpty()) return null
        val start = localDayNumber(printedAt)
        val days = (0 until policy.addOrderMax).map { start + policy.addOrderFirstDays + policy.addOrderEveryDays.toLong() * it }
        val events = days.mapIndexedNotNull { sequence, day -> dated(ReminderKind.ADD_ORDER, draft.id, day, sequence) }
        return Series(events, endsAt = events.lastOrNull()?.let { instant(it.day + policy.addOrderEveryDays) })
    }

    /** "Draft the year-end accumulation" on the next year's date, for each leave year of a printed annual grant that has none yet. */
    private fun yearEndSeries(
        printed: List<Part2Draft>,
        answered: Set<Int>,
        dismissed: Set<String>,
    ): List<Series> {
        val byYear = printed.flatMap { draft -> draft.event.requests.mapNotNull(ReminderRules::annualYear).distinct().map { it to draft } }.groupBy({ it.first }, { it.second })
        return byYear.filterKeys { it !in answered && ReminderTask.yearEnd(it) !in dismissed }.mapNotNull { (year, drafts) ->
            val newest = drafts.minWith(compareByDescending<Part2Draft> { it.printed?.at }.thenBy { it.id })
            // A year whose next 1 March the calendar cannot hold (2100) has no date, so `dated` drops it.
            val day = CivilCalendar.dayNumber(CalendarDate(year + 1, policy.yearEndMonth, policy.yearEndDay))
            dated(ReminderKind.YEAR_END, newest.id, day, year = year)?.let { Series(listOf(it)) }
        }
    }

    private fun dated(
        kind: ReminderKind,
        draftId: String,
        day: Long,
        sequence: Int = 0,
        year: Int? = null,
    ): Dated? {
        val dueOn = CivilCalendar.fromDayNumber(day) ?: return null
        return Dated(Reminder(kind, draftId, instant(day), dueOn, sequence, year), day)
    }

    /** The instant of [ReminderPolicy.hour] o'clock on [day] where the officer is, following the offset in force at that time. */
    private fun instant(day: Long): Long {
        val local = (day + CivilCalendar.UNIX_DAYS_TO_2000) * MILLIS_PER_DAY + policy.hour * MILLIS_PER_HOUR
        return local - offsetAt(local)
    }

    /**
     * Milliseconds from [now] to the next full hour where the officer is (1 ms to 1 hour, never 0). A reminder falls due on the hour and "overdue"
     * turns over at midnight, so a screen that stays open asks this when to read the clock again.
     */
    fun millisUntilNextHour(now: Long): Long = MILLIS_PER_HOUR - (now + offsetAt(now)).mod(MILLIS_PER_HOUR)

    private fun localDayNumber(millis: Long): Long = (millis + offsetAt(millis)).floorDiv(MILLIS_PER_DAY) - CivilCalendar.UNIX_DAYS_TO_2000

    private companion object {
        const val MILLIS_PER_HOUR = 3_600_000L
        const val MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR
    }
}
