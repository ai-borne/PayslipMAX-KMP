package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * "Add the published order": 14 days after printing, then every 30 days, at most 6 (DECISIONS Round 10); a reprint restarts it (Round 18);
 * the item leaves Due 30 days after the sixth (Round 25). The expected days below were counted with an outside calendar: printed on 1 Oct
 * 2026 they fall on 15 Oct, 14 Nov, 14 Dec, 13 Jan, 12 Feb and 14 Mar, and the item ends on 13 Apr 2027.
 */
class ReminderPlannerAddOrderTest : ReminderTestBase() {
    private val printedOn = at(date(2026, 10, 1), 10)
    private val slip = draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = printedOn)
    private val days =
        listOf(date(2026, 10, 15), date(2026, 11, 14), date(2026, 12, 14), date(2027, 1, 13), date(2027, 2, 12), date(2027, 3, 14))

    private fun addOrders(
        now: Long,
        orders: List<VaultOrder> = emptyList(),
        vararg drafts: Part2Draft = arrayOf(slip),
    ) = plan(drafts.toList(), orders, now).of(ReminderKind.ADD_ORDER)

    @Test
    fun sixRemindersAtNineOnTheCountedDaysAreAllComingBeforeTheFirst() {
        val reminders = addOrders(at(date(2026, 10, 2)))
        assertEquals(days, reminders.map { it.dueOn })
        assertEquals(days.map { at(it) }, reminders.map { it.at })
        assertEquals((0..5).toList(), reminders.map { it.sequence })
        assertEquals("add-order:d1:3", reminders[3].key)
        assertEquals(emptyList(), plan(listOf(slip), now = at(date(2026, 10, 2))).dueOf(ReminderKind.ADD_ORDER))
    }

    private fun dueSequences(now: Long) = plan(listOf(slip), now = now).dueOf(ReminderKind.ADD_ORDER).map { it.reminder.sequence }

    private fun upcomingSequences(now: Long) = plan(listOf(slip), now = now).upcomingOf(ReminderKind.ADD_ORDER).map { it.sequence }

    @Test
    fun theFirstIsDueFromNineOnItsDayAndOverdueFromTheNextDay() {
        val day = plan(listOf(slip), now = at(date(2026, 10, 15))).dueOf(ReminderKind.ADD_ORDER)
        assertEquals(listOf(0 to false), day.map { it.reminder.sequence to it.overdue })
        assertEquals(listOf(1, 2, 3, 4, 5), upcomingSequences(at(date(2026, 10, 15))))
        val next = plan(listOf(slip), now = at(date(2026, 10, 16))).dueOf(ReminderKind.ADD_ORDER)
        assertEquals(listOf(0 to true), next.map { it.reminder.sequence to it.overdue })
    }

    @Test
    fun theLatestPassedReminderIsTheOneDueAndTheRestAreComing() {
        assertEquals(listOf(2), dueSequences(at(date(2026, 12, 20))))
        assertEquals(listOf(3, 4, 5), upcomingSequences(at(date(2026, 12, 20))))
    }

    @Test
    fun theSixthStaysDueForThirtyDaysThenTheItemIsGone() {
        val lastDay = date(2027, 3, 14)
        assertEquals(listOf(5), dueSequences(at(lastDay)))
        assertEquals(listOf(5), dueSequences(at(date(2027, 4, 13), 8, 59)))
        assertEquals(emptyList(), dueSequences(at(date(2027, 4, 13))))
        assertEquals(emptyList(), upcomingSequences(at(date(2027, 4, 13))))
    }

    @Test
    fun noSeventhEverComes() {
        assertEquals(6, addOrders(at(date(2026, 10, 2))).size)
        assertTrue(addOrders(at(date(2030, 1, 1))).isEmpty())
    }

    @Test
    fun theCountStartsFromTheDayWhereTheOfficerIsNotTheUtcDay() {
        // 20:00 UTC on 1 Oct is 01:30 on 2 Oct in India: the 14 days run from the 2nd.
        val lateEvening = draft("d1", printedAt = at(date(2026, 10, 1), 20, 0, offset = 0))
        assertEquals(date(2026, 10, 16), addOrders(at(date(2026, 10, 2)), drafts = arrayOf(lateEvening)).first().dueOn)
    }

    @Test
    fun aReprintRestartsTheRemindersFromTheLatestPrint() {
        val reprinted = slip.copy(printed = slip.printed!!.copy(at = at(date(2026, 11, 1), 10)))
        assertEquals(date(2026, 11, 15), addOrders(at(date(2026, 11, 2)), drafts = arrayOf(reprinted)).first().dueOn)
    }

    @Test
    fun aDraftNeverHandedOverGetsNone() {
        assertTrue(addOrders(at(date(2026, 10, 2)), drafts = arrayOf(draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9))))).isEmpty())
    }

    @Test
    fun aNumberedDatedOrderLinkedToTheDraftEndsThem() {
        assertTrue(addOrders(at(date(2026, 10, 20)), listOf(order("o1", "d1"))).isEmpty())
    }

    @Test
    fun anOrderThatCannotYetBeQuotedDoesNotEndThem() {
        // why: the point of the reminder is an order number and date to quote; a scan without them has not done that.
        val unfinished =
            listOf(order("o1", "d1", no = null), order("o2", "d1", on = null), order("o3", "d1", no = "  "), order("o4", "d1", no = "<9>"), order("o5", "d1", on = date(2026, 2, 30)))
        for (case in unfinished) assertEquals(6, addOrders(at(date(2026, 10, 2)), listOf(case)).size, "${case.id}")
    }

    @Test
    fun anOrderOfAnotherDraftOrOfNoDraftDoesNotEndThem() {
        val others = listOf(order("o1", "other"), order("o2", null))
        assertEquals(6, addOrders(at(date(2026, 10, 2)), others).size)
    }

    @Test
    fun eachPrintedDraftHasItsOwnSeries() {
        val second = draft("d2", printedAt = at(date(2026, 10, 3), 10))
        val reminders = addOrders(at(date(2026, 10, 4)), drafts = arrayOf(slip, second))
        assertEquals(12, reminders.size)
        assertEquals(setOf("d1", "d2"), reminders.map { it.draftId }.toSet())
    }

    @Test
    fun aPrintedCancelSlipHasItsOwnSeriesUntilItsCancelOrderIsLinked() {
        val cancel = cancelSlip("c1", "CANCEL", "0123/26", date(2026, 6, 20), printedAt = printedOn)
        assertEquals(6, addOrders(at(date(2026, 10, 2)), drafts = arrayOf(cancel)).size)
        assertTrue(addOrders(at(date(2026, 10, 2)), listOf(order("o9", "c1")), cancel).isEmpty())
    }
}
