package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The plan as a whole: the nearest 20 upcoming, the order of both lists, a rebuild that always gives the same answer, the clock of the
 * officer's place (including a zone whose offset changes) and the policy being the only home of the numbers. The drafts here carry no
 * leave year and each has a published order, so only their return reminder is in the plan unless a test says otherwise.
 */
class ReminderPlannerPlanTest : ReminderTestBase() {
    private val printed = at(date(2026, 9, 20))

    /** A one-day leave on [lastDay] of October (returns the next day). Leaves on adjacent days would take each other over, so tests that need many use [spaced]. */
    private fun returning(
        id: String,
        lastDay: Int,
        month: Int = 10,
    ) = draft(id, request("GTDAL", on("leave_from", date(2026, month, lastDay)), on("leave_to", date(2026, month, lastDay))), printedAt = printed)

    /** The one-day leave number [n]: three days after the one before, so none touches another and each returns on its own day (1 Oct + 3n + 1). */
    private fun spaced(n: Int): Part2Draft {
        val day = CivilCalendar.fromDayNumber(CivilCalendar.dayNumber(date(2026, 10, 1)) + 3L * n)!!
        return draft(numbered(n), request("GTDAL", on("leave_from", day), on("leave_to", day)), printedAt = printed)
    }

    private fun numbered(number: Int) = "d${number.toString().padStart(2, '0')}"

    private fun published(drafts: List<Part2Draft>) = drafts.map { order("o-${it.id}", it.id) }

    private fun planOf(
        drafts: List<Part2Draft>,
        now: Long,
        using: ReminderPlanner = planner,
    ) = using.plan(drafts, published(drafts), now)

    @Test
    fun noDraftsGiveAnEmptyPlan() {
        assertEquals(ReminderPlan.Empty, plan(emptyList(), now = printed))
    }

    @Test
    fun thePolicyDefaultsAreTheDecidedCadence() {
        // why: DECISIONS Round 10 - 09:00; 14 days after printing, then every 30, at most 6; 1 March; the nearest 20.
        assertEquals(ReminderPolicy(9, 14, 30, 6, 3, 1, 20), ReminderPolicy())
    }

    @Test
    fun onlyTheNearestTwentyUpcomingAreKeptAndTheyAreTheNearest() {
        val drafts = (1..30).map(::spaced)
        val upcoming = planOf(drafts, now = printed).upcoming
        assertEquals(20, upcoming.size)
        assertEquals((1..20).map { "return:${numbered(it)}" }, upcoming.map { it.key })
    }

    @Test
    fun theCapCountsOnlyWhatIsComingNotWhatIsDue() {
        val drafts = (1..40).map(::spaced)
        // Just after the twelfth return day (1 Oct + 3 x 12 + 1): twelve are due, twenty-eight are coming, and only twenty of those are kept.
        val plan = planOf(drafts, now = at(date(2026, 11, 7)))
        assertEquals(12, plan.due.size)
        assertEquals(20, plan.upcoming.size)
    }

    @Test
    fun dueIsOldestFirstAndComingIsNearestFirstWithTheKeyBreakingTies() {
        val drafts = listOf(returning("b", 20), returning("a", 20), returning("c", 10), returning("d", 25))
        val plan = planOf(drafts, now = at(date(2026, 10, 22)))
        assertEquals(listOf("return:c", "return:a", "return:b"), plan.due.map { it.reminder.key })
        assertEquals(listOf("return:d"), plan.upcoming.map { it.key })
    }

    @Test
    fun aRebuildGivesTheSamePlanWhateverTheOrderOfTheInputs() {
        val drafts = listOf(returning("a", 12), returning("b", 15), draft("c", printedAt = printed), draft("e", leave(date(2026, 10, 1), date(2026, 10, 3)), printedAt = printed))
        val orders = listOf(order("o1", "c"), order("o2", "zz"))
        val now = at(date(2026, 10, 14))
        val once = plan(drafts, orders, now)
        assertEquals(once, plan(drafts, orders, now))
        assertEquals(once, plan(drafts.reversed(), orders.reversed(), now))
    }

    @Test
    fun theSameDraftsGiveTheSameKeysAsTimePassesSoASchedulerCanReplaceByKey() {
        val drafts = listOf(returning("a", 12))
        assertEquals(planOf(drafts, now = at(date(2026, 10, 1))).upcoming.map { it.key }, planOf(drafts, now = at(date(2026, 10, 2))).upcoming.map { it.key })
    }

    @Test
    fun everyReminderIsAtNineWhereTheOfficerIsWhateverThePlace() {
        for (offset in listOf(0L, india, -4 * hour, 13 * hour, -3 * hour - 30 * 60_000L)) {
            val drafts = listOf(returning("a", 9))
            val reminder = planOf(drafts, at(date(2026, 10, 1), offset = offset), ReminderPlanner { offset }).upcoming.single()
            assertEquals(at(date(2026, 10, 10), offset = offset), reminder.at, "offset $offset")
            assertEquals(date(2026, 10, 10), reminder.dueOn)
        }
    }

    @Test
    fun aZoneThatChangesItsOffsetOnTheDayIsFollowed() {
        // Clocks go forward by an hour at the start of 10 Oct (UTC): 09:00 that day is at +1h, not at the offset of the day before.
        val switch = at(date(2026, 10, 10), 0, offset = 0)
        val zone: (Long) -> Long = { if (it >= switch) hour else 0L }
        val reminder = planOf(listOf(returning("a", 9)), at(date(2026, 10, 1), offset = 0), ReminderPlanner(offsetAt = zone)).upcoming.single()
        assertEquals(at(date(2026, 10, 10), offset = hour), reminder.at)
    }

    @Test
    fun theNumbersComeFromThePolicyAndFromNowhereElse() {
        val custom = ReminderPolicy(hour = 7, addOrderFirstDays = 3, addOrderEveryDays = 10, addOrderMax = 2, yearEndMonth = 4, yearEndDay = 2, upcomingMax = 3)
        val drafts = listOf(draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = at(date(2026, 10, 1), 10)))
        val now = at(date(2026, 10, 2))
        val upcoming = ReminderPlanner(custom) { india }.plan(drafts, emptyList(), now).upcoming
        assertEquals(listOf(at(date(2026, 10, 4), 7), at(date(2026, 10, 10), 7), at(date(2026, 10, 14), 7)), upcoming.map { it.at })
        val wide = ReminderPlanner(custom.copy(upcomingMax = 20)) { india }.plan(drafts, emptyList(), now).upcoming
        assertEquals(date(2027, 4, 2), wide.single { it.kind == ReminderKind.YEAR_END }.dueOn)
        assertEquals(2, wide.count { it.kind == ReminderKind.ADD_ORDER })
    }

    @Test
    fun aPlanAndAReminderNeverShowTheDraftInAString() {
        val plan = planOf(listOf(returning("MARKER_DRAFT", 9)), now = printed)
        assertTrue("MARKER_DRAFT" !in plan.toString() + plan.upcoming.first().toString())
    }
}
