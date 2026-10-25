package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Phase 11 item 34: "draft your rejoining slip" falls on a leave's return day, but when a second printed leave (an extension, or any leave
 * that carries on from it) covers that day the officer is still away, so the first leave's reminder must give way to the second's. Only a
 * printed leave of a draft that still stands can do that. All dates are synthetic.
 */
class ReminderPlannerExtensionTest : ReminderTestBase() {
    private val printed = at(date(2026, 9, 20))
    private val now = at(date(2026, 10, 2))

    private fun returns(vararg drafts: Part2Draft) = plan(drafts.toList(), now = now).of(ReminderKind.RETURN).map { it.key to it.dueOn }

    private fun leaveDraft(
        id: String,
        from: Int,
        to: Int,
        month: Int = 10,
        printedAt: Long? = printed,
    ) = draft(id, request("GTDAL", on("leave_from", date(2026, month, from)), on("leave_to", date(2026, month, to))), printedAt = printedAt)

    @Test
    fun aLeaveStartingOnTheReturnDayAndRunningLongerTakesOverTheReminder() {
        // 5 to 9 Oct returns on the 10th; the extension 10 to 20 Oct is that very day, so only the extension's return (21 Oct) is asked for.
        assertEquals(listOf("return:ext" to date(2026, 10, 21)), returns(leaveDraft("first", 5, 9), leaveDraft("ext", 10, 20)))
    }

    @Test
    fun anOverlappingLeaveThatEndsLaterTakesOverToo() {
        assertEquals(listOf("return:ext" to date(2026, 10, 21)), returns(leaveDraft("first", 5, 9), leaveDraft("ext", 8, 20)))
    }

    @Test
    fun aLeaveStartingAfterTheReturnDayLeavesTheFirstReminderAlone() {
        // The officer was back on the 10th and 11th; the next leave starts on the 12th.
        assertEquals(listOf("return:first" to date(2026, 10, 10), "return:next" to date(2026, 10, 21)), returns(leaveDraft("first", 5, 9), leaveDraft("next", 12, 20)).sortedBy { it.second.day })
    }

    @Test
    fun aLeaveThatEndsOnTheSameDayDoesNotTakeOver() {
        // why: two slips for the same days (a re-draft, a duplicate) must not silence each other; both still end on the 9th.
        assertEquals(setOf("return:a", "return:b"), returns(leaveDraft("a", 5, 9), leaveDraft("b", 5, 9)).map { it.first }.toSet())
    }

    @Test
    fun aLeaveThatEndedBeforeTheReturnDayDoesNotTakeOver() {
        // The other leave (1 to 8 Oct) returns on the 9th, a day before this one starts, so neither covers the other's return day.
        assertEquals(setOf("return:first", "return:earlier"), returns(leaveDraft("first", 10, 14), leaveDraft("earlier", 1, 8)).map { it.first }.toSet())
    }

    @Test
    fun aChainOfExtensionsLeavesOnlyTheLastReminder() {
        val chain = arrayOf(leaveDraft("one", 1, 4), leaveDraft("two", 5, 9), leaveDraft("three", 10, 14))
        assertEquals(listOf("return:three" to date(2026, 10, 15)), returns(*chain))
    }

    @Test
    fun anExtensionNeverHandedOverDoesNotTakeOver() {
        // why: a printed leave is what makes a reminder; an unprinted draft of an extension is not yet an event.
        assertEquals(listOf("return:first" to date(2026, 10, 10)), returns(leaveDraft("first", 5, 9), leaveDraft("ext", 10, 20, printedAt = null)))
    }

    @Test
    fun anExtensionWhoseOrderWasCancelledDoesNotTakeOver() {
        val ext = leaveDraft("ext", 10, 20)
        val extOrder = order("o-ext", "ext", no = "0123/26", on = date(2026, 6, 20))
        val cancel = cancelSlip("c1", "CANCEL", "0123/26", date(2026, 6, 20), printed + 1)
        val plan = plan(listOf(leaveDraft("first", 5, 9), ext, cancel), listOf(extOrder), now)
        assertEquals(listOf("return:first"), plan.of(ReminderKind.RETURN).map { it.key })
    }

    @Test
    fun anExtensionWithNoReadableFirstDayCannotBeSaidToCoverTheDay() {
        val noStart = draft("ext", request("GTDAL", on("leave_to", date(2026, 10, 20))), printedAt = printed)
        assertEquals(setOf("return:first", "return:ext"), returns(leaveDraft("first", 5, 9), noStart).map { it.first }.toSet())
    }

    @Test
    fun theDayItMustCoverIsTheReturnDayAfterTheSuffixNotAfterTheLastLeaveDay() {
        // 5 to 9 Oct with a suffix to the 12th returns on the 13th. An extension from the 13th covers that day; one from the 14th does not.
        val withSuffix = draft("first", leave(date(2026, 10, 5), date(2026, 10, 9), suffixLast = date(2026, 10, 12)), printedAt = printed)
        assertEquals(listOf("return:ext" to date(2026, 10, 21)), returns(withSuffix, leaveDraft("ext", 13, 20)))
        assertEquals(setOf("return:first", "return:ext"), returns(withSuffix, leaveDraft("ext", 14, 20)).map { it.first }.toSet())
    }

    @Test
    fun theExtensionOfADraftIsFoundWhateverTheOrderOfTheInputs() {
        val a = leaveDraft("first", 5, 9)
        val b = leaveDraft("ext", 10, 20)
        assertEquals(returns(a, b), returns(b, a))
    }
}
