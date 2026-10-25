package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * "Draft the year-end accumulation for Y" on 1 March of Y+1 (DECISIONS Round 10; note 7 p.134). It is found from a printed annual-leave
 * grant of that year and answered by a year-end request for the year, which Phase 8 gives a template; here a synthetic draft stands in for
 * it, naming the request by its code in the rule family and the year by the slot every annual template shares.
 */
class ReminderPlannerYearEndTest : ReminderTestBase() {
    private val printed = at(date(2026, 9, 20))
    private val january = at(date(2027, 1, 15))

    private fun grant(year: Int = 2026) = leave(date(year, 10, 5), date(year, 10, 9), leaveYear = year)

    private fun yearEnds(
        vararg drafts: Part2Draft,
        now: Long = january,
    ) = plan(drafts.toList(), now = now).of(ReminderKind.YEAR_END)

    private fun accumulation(
        year: Int,
        id: String = "acc",
    ) = draft(id, request("ACCLVE", year(year)))

    @Test
    fun aPrintedAnnualGrantRemindsOnFirstMarchOfTheNextYearAtNine() {
        val reminder = yearEnds(draft("d1", grant(), printedAt = printed)).single()
        assertEquals(at(date(2027, 3, 1)), reminder.at)
        assertEquals(date(2027, 3, 1), reminder.dueOn)
        assertEquals(2026, reminder.year)
        assertEquals("d1", reminder.draftId)
        assertEquals("year-end:2026", reminder.key)
    }

    @Test
    fun theAnnualGrantsCountAndNothingElseDoes() {
        for (code in listOf("GTDAL", "GTDALCF", "EXTNAL")) {
            assertEquals(1, yearEnds(draft("d-$code", leave(date(2026, 10, 5), date(2026, 10, 9), code = code), printedAt = printed)).size, code)
        }
        for (code in listOf("GTDCL", "GTDSPCL", "REJOINAL", "SYNTHETIC")) {
            assertTrue(yearEnds(draft("d-$code", leave(date(2026, 10, 5), date(2026, 10, 9), code = code), printedAt = printed)).isEmpty(), code)
        }
    }

    @Test
    fun aDraftNeverHandedOverGivesNone() {
        assertTrue(yearEnds(draft("d1", grant())).isEmpty())
    }

    @Test
    fun severalDraftsOfOneYearGiveOneReminderOpeningTheNewestPrinted() {
        val older = draft("a", grant(), printedAt = printed)
        val newer = draft("b", grant(), printedAt = printed + 1_000)
        assertEquals("b", yearEnds(older, newer).single().draftId)
        assertEquals("b", yearEnds(newer, older).single().draftId)
    }

    @Test
    fun aTieOnPrintTimeIsSettledByTheDraftIdSoTheAnswerDoesNotDependOnOrder() {
        val one = draft("a", grant(), printedAt = printed)
        val two = draft("b", grant(), printedAt = printed)
        assertEquals("a", yearEnds(one, two).single().draftId)
        assertEquals("a", yearEnds(two, one).single().draftId)
    }

    @Test
    fun twoLeaveYearsGiveTwoReminders() {
        val both = draft("d1", grant(2025), grant(2026), printedAt = printed)
        assertEquals(listOf(2025, 2026), yearEnds(both).map { it.year }.sortedBy { it })
    }

    @Test
    fun aYearTheCalendarCannotHoldGivesNoneAndTheEdgesAreKept() {
        for (year in listOf(1999, 2100, 0, -1)) {
            assertTrue(yearEnds(draft("d1", request("GTDAL", year(year)), printedAt = printed)).isEmpty(), "$year")
        }
        assertEquals(2099, yearEnds(draft("d1", request("GTDAL", year(2099)), printedAt = printed)).single().year)
        assertEquals(2000, yearEnds(draft("d1", request("GTDAL", year(2000)), printedAt = printed), now = at(date(2000, 6, 1))).single().year)
    }

    @Test
    fun aGrantWithNoYearOrATextYearGivesNone() {
        assertTrue(yearEnds(draft("d1", request("GTDAL"), printedAt = printed)).isEmpty())
        assertTrue(yearEnds(draft("d1", request("GTDAL", "year" to SlotValue.TextValue("2026")), printedAt = printed)).isEmpty())
    }

    @Test
    fun aYearEndRequestForThatYearAnswersItPrintedOrNot() {
        val grants = draft("d1", grant(), printedAt = printed)
        assertTrue(yearEnds(grants, accumulation(2026)).isEmpty())
    }

    @Test
    fun aYearEndRequestForAnotherYearOrWithoutOneAnswersNothing() {
        val grants = draft("d1", grant(), printedAt = printed)
        assertEquals(1, yearEnds(grants, accumulation(2025)).size)
        assertEquals(1, yearEnds(grants, draft("acc", request("ACCLVE"))).size)
        assertEquals(1, yearEnds(grants, draft("acc", request("ACCLVE", "year" to SlotValue.TextValue("2026")))).size)
        assertEquals(1, yearEnds(grants, draft("acc", request("GTDAL", year(2026)))).size)
    }

    @Test
    fun aYearEndRequestAnswersOnlyItsOwnYearAmongTwo() {
        val both = draft("d1", grant(2025), grant(2026), printedAt = printed)
        assertEquals(listOf(2025), yearEnds(both, accumulation(2026)).map { it.year })
    }

    @Test
    fun itIsComingBeforeFirstMarchAndDueFromNineThenOverdueUntilAnswered() {
        val grants = draft("d1", grant(), printedAt = printed)
        assertEquals(1, plan(listOf(grants), now = at(date(2027, 3, 1), 8, 59)).upcomingOf(ReminderKind.YEAR_END).size)
        val day = plan(listOf(grants), now = at(date(2027, 3, 1)))
        assertEquals(listOf(false), day.dueOf(ReminderKind.YEAR_END).map { it.overdue })
        val months = plan(listOf(grants), now = at(date(2027, 11, 1)))
        assertEquals(listOf("year-end:2026" to true), months.dueOf(ReminderKind.YEAR_END).map { it.reminder.key to it.overdue })
    }
}
