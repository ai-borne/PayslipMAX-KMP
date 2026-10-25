package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Phase 11 item 32 (DECISIONS Round 26): the officer can dismiss a reminder, and it is remembered with the draft. A dismissal stops the whole
 * series of that kind for that draft, in the Due list and in what a scheduler is given, and nothing else. A year end is one reminder per leave
 * year across drafts, so a dismissal on any draft that stands covers it. All dates are synthetic.
 */
class ReminderPlannerDismissTest : ReminderTestBase() {
    private val printed = at(date(2026, 9, 20))
    private val now = at(date(2026, 10, 12))

    private fun slip(
        id: String,
        dismissed: Set<String> = emptySet(),
        year: Int = 2026,
    ) = draft(id, leave(date(2026, 10, 5), date(2026, 10, 9), leaveYear = year), printedAt = printed).copy(dismissed = dismissed)

    private fun kinds(
        id: String,
        vararg drafts: Part2Draft,
    ) = plan(drafts.toList(), now = now).all().filter { it.draftId == id }.map { it.kind }.toSet()

    private val everything = setOf(ReminderKind.RETURN, ReminderKind.ADD_ORDER, ReminderKind.YEAR_END)

    @Test
    fun theTaskNamesAreStablePlainTokens() {
        val same = Reminder(ReminderKind.ADD_ORDER, "d1", 0, date(2026, 10, 1), sequence = 4)
        assertEquals("add-order", same.task)
        assertEquals("add-order", same.copy(sequence = 0).task)
        assertEquals("return", Reminder(ReminderKind.RETURN, "d1", 0, date(2026, 10, 1)).task)
        assertEquals("year-end-2026", Reminder(ReminderKind.YEAR_END, "d1", 0, date(2027, 3, 1), year = 2026).task)
        assertEquals("year-end-2027", ReminderTask.yearEnd(2027))
    }

    @Test
    fun withNothingDismissedAllThreeKindsStand() {
        assertEquals(everything, kinds("d1", slip("d1")))
    }

    @Test
    fun dismissingTheReturnStopsOnlyTheReturn() {
        assertEquals(everything - ReminderKind.RETURN, kinds("d1", slip("d1", setOf(ReminderTask.RETURN))))
    }

    @Test
    fun dismissingAddTheOrderStopsTheWholeSeriesAndOnlyThatDrafts() {
        val one = slip("d1", setOf(ReminderTask.ADD_ORDER))
        val two = slip("d2")
        val all = plan(listOf(one, two), now = now).all()
        assertEquals(0, all.count { it.draftId == "d1" && it.kind == ReminderKind.ADD_ORDER })
        assertTrue(all.any { it.draftId == "d2" && it.kind == ReminderKind.ADD_ORDER })
        assertTrue(all.any { it.draftId == "d1" && it.kind == ReminderKind.RETURN })
    }

    @Test
    fun aDismissedSeriesLeavesNothingForAScheduler() {
        val upcoming = plan(listOf(slip("d1", setOf(ReminderTask.ADD_ORDER, ReminderTask.RETURN, ReminderTask.yearEnd(2026)))), now = at(date(2026, 9, 21))).upcoming
        assertEquals(emptyList(), upcoming)
    }

    @Test
    fun aYearEndDismissedOnAnyStandingDraftCoversThatYearOnly() {
        val owner = slip("new", year = 2026).copy(printed = PrintedSnapshot(printed + 1_000, emptyMap(), emptyList()))
        val older = slip("old", setOf(ReminderTask.yearEnd(2026)))
        val plan = plan(listOf(owner, older), now = now)
        assertEquals(emptyList(), plan.of(ReminderKind.YEAR_END))

        val other = slip("two-years").copy(event = LeaveEventInput(listOf(leave(date(2026, 10, 5), date(2026, 10, 9), leaveYear = 2026), leave(date(2027, 1, 5), date(2027, 1, 9), leaveYear = 2027))))
        val years = plan(listOf(other.copy(dismissed = setOf(ReminderTask.yearEnd(2026)))), now = now).of(ReminderKind.YEAR_END).map { it.year }
        assertEquals(listOf(2027), years)
    }

    @Test
    fun aDismissalOnADraftWhoseOrderWasCancelledDoesNotCount() {
        // why: a void draft sets and answers nothing, so what it was told to hush does not hush the same year on a draft that stands.
        val voided = slip("voided", setOf(ReminderTask.yearEnd(2026)))
        val voidedOrder = order("o1", "voided", no = "0123/26", on = date(2026, 6, 20))
        val cancel = cancelSlip("c1", "CANCEL", "0123/26", date(2026, 6, 20), printed + 1)
        val standing = slip("standing")
        val plan = plan(listOf(voided, cancel, standing), listOf(voidedOrder), now)
        assertEquals(listOf("standing"), plan.of(ReminderKind.YEAR_END).map { it.draftId })
    }

    @Test
    fun anUnknownTokenDismissesNothing() {
        assertEquals(everything, kinds("d1", slip("d1", setOf("whatever", "year-end-1999", ""))))
    }
}
