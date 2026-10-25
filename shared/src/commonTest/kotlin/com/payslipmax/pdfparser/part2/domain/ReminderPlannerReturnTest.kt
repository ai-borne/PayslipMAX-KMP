package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * "Draft the rejoining slip" on the day the officer is back (DECISIONS Round 10, 09:00 where the officer is; Round 25: from a printed
 * leave, until a rejoining draft exists within the leave). All dates are synthetic.
 */
class ReminderPlannerReturnTest : ReminderTestBase() {
    private val printed = at(date(2026, 9, 20))
    private val grant = leave(date(2026, 10, 5), date(2026, 10, 9))
    private val before = at(date(2026, 10, 1))

    private fun returns(
        vararg drafts: Part2Draft,
        now: Long = before,
    ) = plan(drafts.toList(), now = now).of(ReminderKind.RETURN)

    @Test
    fun aPrintedLeaveRemindsOnTheDayAfterItsLastDayAtNineWhereTheOfficerIs() {
        val reminder = returns(draft("d1", grant, printedAt = printed)).single()
        assertEquals(at(date(2026, 10, 10)), reminder.at)
        assertEquals(date(2026, 10, 10), reminder.dueOn)
        assertEquals("d1", reminder.draftId)
        assertEquals("return:d1", reminder.key)
    }

    @Test
    fun theReturnIsTheDayAfterTheLaterOfTheLastLeaveDayAndTheLastSuffixDay() {
        val withSuffix = leave(date(2026, 10, 5), date(2026, 10, 9), suffixLast = date(2026, 10, 12))
        assertEquals(date(2026, 10, 13), returns(draft("d1", withSuffix, printedAt = printed)).single().dueOn)
        // why: a suffix date inside the leave adds nothing; the leave's own last day decides.
        val inside = leave(date(2026, 10, 5), date(2026, 10, 9), suffixLast = date(2026, 10, 7))
        assertEquals(date(2026, 10, 10), returns(draft("d1", inside, printedAt = printed)).single().dueOn)
    }

    @Test
    fun theReturnRollsOverAMonthEndAndALeapDay() {
        assertEquals(date(2026, 11, 1), returns(draft("d1", leave(date(2026, 10, 20), date(2026, 10, 31)), printedAt = printed)).single().dueOn)
        assertEquals(date(2028, 2, 29), returns(draft("d1", leave(date(2028, 2, 20), date(2028, 2, 28)), printedAt = printed)).single().dueOn)
    }

    @Test
    fun aDraftNeverHandedOverIsNotALeaveYet() {
        // why: Round 18, the cost of a missing reminder is smaller than a wrong one; an experiment must not nag.
        assertTrue(returns(draft("d1", grant)).isEmpty())
    }

    @Test
    fun everyGrantWithADateSetsOneAndNoOtherRequestDoes() {
        for (code in listOf("GTDAL", "GTDALCF", "EXTNAL", "GTDCL", "GTDSPCL")) {
            val request = leave(date(2026, 10, 5), date(2026, 10, 9), code = code)
            assertEquals(1, returns(draft("d-$code", request, printedAt = printed)).size, code)
        }
        for (code in listOf("REJOINAL", "VOLREJ", "RECALL", "CANCEL", "SYNTHETIC")) {
            val request = leave(date(2026, 10, 5), date(2026, 10, 9), code = code)
            assertTrue(returns(draft("d-$code", request, printedAt = printed)).isEmpty(), code)
        }
    }

    @Test
    fun aLeaveWithoutAReadableLastDayHasNoReturn() {
        val noEnd = request("GTDAL", on("leave_from", date(2026, 10, 5)))
        val notADate = request("GTDAL", on("leave_from", date(2026, 10, 5)), on("leave_to", date(2026, 4, 31)))
        val text = request("GTDAL", "leave_to" to SlotValue.TextValue("10/10/2026"))
        for (case in listOf(noEnd, notADate, text)) assertTrue(returns(draft("d1", case, printedAt = printed)).isEmpty())
    }

    @Test
    fun anUnreadableSuffixDayIsIgnoredAndTheLeaveDecides() {
        val badSuffix = request("GTDAL", on("leave_to", date(2026, 10, 9)), on("suffix_last", date(2026, 2, 30)))
        assertEquals(date(2026, 10, 10), returns(draft("d1", badSuffix, printedAt = printed)).single().dueOn)
    }

    @Test
    fun twoLeaveYearsInOneDraftGiveOneReminderOnTheLastReturn() {
        val first = leave(date(2026, 12, 20), date(2026, 12, 31), leaveYear = 2026)
        val second = leave(date(2027, 1, 1), date(2027, 1, 10), leaveYear = 2027)
        val reminder = returns(draft("d1", first, second, printedAt = printed)).single()
        assertEquals(date(2027, 1, 11), reminder.dueOn)
    }

    @Test
    fun beforeTheDayItIsComingAndFromNineOnTheDayItIsDue() {
        val drafts = listOf(draft("d1", grant, printedAt = printed))
        val eightFiftyNine = plan(drafts, now = at(date(2026, 10, 10), 8, 59))
        assertEquals(emptyList(), eightFiftyNine.dueOf(ReminderKind.RETURN))
        assertEquals(1, eightFiftyNine.upcomingOf(ReminderKind.RETURN).size)
        val nine = plan(drafts, now = at(date(2026, 10, 10)))
        assertEquals(listOf(false), nine.dueOf(ReminderKind.RETURN).map { it.overdue })
        assertEquals(emptyList(), nine.upcomingOf(ReminderKind.RETURN))
    }

    @Test
    fun itIsOverdueFromTheNextLocalDayNotTheNextUtcDay() {
        val drafts = listOf(draft("d1", grant, printedAt = printed))
        // 23:30 in India on the 10th is 18:00 UTC the same day, and 00:30 on the 11th is 19:00 UTC on the 10th: the officer's day decides.
        assertEquals(listOf(false), plan(drafts, now = at(date(2026, 10, 10), 23, 30)).dueOf(ReminderKind.RETURN).map { it.overdue })
        assertEquals(listOf(true), plan(drafts, now = at(date(2026, 10, 11), 0, 30)).dueOf(ReminderKind.RETURN).map { it.overdue })
    }

    @Test
    fun itStaysDueAndOverdueForMonthsUntilAnswered() {
        val drafts = listOf(draft("d1", grant, printedAt = printed))
        val later = plan(drafts, now = at(date(2027, 6, 1)))
        assertEquals(listOf("return:d1"), later.dueOf(ReminderKind.RETURN).map { it.reminder.key })
        assertEquals(listOf(true), later.dueOf(ReminderKind.RETURN).map { it.overdue })
    }

    @Test
    fun aRejoiningDraftWithinTheLeaveAnswersIt() {
        // why: Round 25. The leave is 5 to 9 Oct; the officer came back on the 10th, or earlier (voluntarily or recalled).
        val answers = listOf(Triple("REJOINAL", "rejoined_on", 10), Triple("VOLREJ", "rejoined_on", 7), Triple("RECALL", "recalled_on", 5))
        for ((code, slot, day) in answers) {
            val answer = draft("r-$code", request(code, on(slot, date(2026, 10, day))))
            assertTrue(returns(draft("d1", grant, printedAt = printed), answer, now = at(date(2026, 10, 12))).isEmpty(), code)
        }
    }

    @Test
    fun aRejoiningOutsideTheLeaveLeavesItOpen() {
        for (day in listOf(date(2026, 10, 4), date(2026, 10, 11))) {
            val answer = draft("r", request("REJOINAL", on("rejoined_on", day)))
            assertEquals(1, returns(draft("d1", grant, printedAt = printed), answer, now = at(date(2026, 10, 12))).size, "$day")
        }
    }

    @Test
    fun aRejoiningWithNoReadableDayAnswersNothing() {
        val unreadable = draft("r", request("REJOINAL", "rejoined_on" to SlotValue.TextValue("10/10/2026")), request("RECALL", on("recalled_on", date(2026, 2, 31))))
        assertEquals(1, returns(draft("d1", grant, printedAt = printed), unreadable, now = at(date(2026, 10, 12))).size)
    }

    @Test
    fun theLeaveSlotOfARejoiningDoesNotCount() {
        // why: only the day the officer came back answers; a slot with the leave's name on a rejoining request is not that.
        val answer = draft("r", request("REJOINAL", on("leave_to", date(2026, 10, 8))))
        assertEquals(1, returns(draft("d1", grant, printedAt = printed), answer, now = at(date(2026, 10, 12))).size)
    }

    @Test
    fun aRejoiningDuringTheFirstOfTwoLeaveYearsAnswersThem() {
        // why: the window opens on the earliest first day of the draft, not the latest, or a voluntary rejoining in the first year is missed.
        val first = leave(date(2026, 12, 20), date(2026, 12, 31), leaveYear = 2026)
        val second = leave(date(2027, 1, 1), date(2027, 1, 10), leaveYear = 2027)
        val answer = draft("r", request("VOLREJ", on("rejoined_on", date(2026, 12, 25))))
        assertTrue(returns(draft("d1", first, second, printedAt = printed), answer, now = at(date(2027, 1, 12))).isEmpty())
    }

    @Test
    fun onlyARejoiningRequestAnswersEvenIfAnotherCarriesTheSameSlot() {
        val notRejoining = draft("r", request("GTDAL", on("rejoined_on", date(2026, 10, 10)), on("recalled_on", date(2026, 10, 10))))
        assertEquals(1, returns(draft("d1", grant, printedAt = printed), notRejoining, now = at(date(2026, 10, 12))).size)
    }

    @Test
    fun aRejoiningDraftAnswersOnlyTheLeaveItFallsWithin() {
        val other = leave(date(2026, 12, 1), date(2026, 12, 5))
        val answer = draft("r", request("REJOINAL", on("rejoined_on", date(2026, 12, 6))))
        val reminders = returns(draft("d1", grant, printedAt = printed), draft("d2", other, printedAt = printed), answer, now = at(date(2026, 12, 8)))
        assertEquals(listOf("return:d1"), reminders.map { it.key })
    }

    @Test
    fun aLeaveWithNoFirstDayIsAnsweredByARejoiningOnOrBeforeItsReturn() {
        val open = request("GTDAL", on("leave_to", date(2026, 10, 9)))
        val answer = draft("r", request("REJOINAL", on("rejoined_on", date(2026, 10, 6))))
        assertTrue(returns(draft("d1", open, printedAt = printed), answer, now = at(date(2026, 10, 12))).isEmpty())
        val late = draft("r", request("REJOINAL", on("rejoined_on", date(2026, 10, 11))))
        assertEquals(1, returns(draft("d1", open, printedAt = printed), late, now = at(date(2026, 10, 12))).size)
    }
}
