package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderTask
import com.payslipmax.pdfparser.part2.domain.ReminderTestBase
import com.payslipmax.pdfparser.testing.FakePart2Repository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a notification is posted by a worker the operating system starts, possibly in a process nobody has opened for weeks, from a copy the
 * app may not have rebuilt yet. So the worker asks the drafts, at that moment, whether this reminder is still due; an answered, dismissed or
 * deleted one must stay silent rather than nag.
 */
class ReminderFireCheckTest : ReminderTestBase() {
    private val repository = FakePart2Repository()
    private var clock = at(date(2026, 10, 10))
    private val printedLeave = draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = at(date(2026, 10, 1)))

    private fun check() = ReminderFireCheck(repository, planner, now = { clock })

    @Test
    fun aReminderWhoseDayHasComeIsPosted() =
        runTest {
            repository.saveDraft(printedLeave)

            assertTrue(check().shouldPost(ReminderKind.RETURN, "d1"))
        }

    @Test
    fun aReminderOfAnotherKindOrAnotherDraftIsNotPosted() =
        runTest {
            repository.saveDraft(printedLeave)

            assertFalse(check().shouldPost(ReminderKind.YEAR_END, "d1"), "no year end is due yet")
            assertFalse(check().shouldPost(ReminderKind.RETURN, "d2"), "no such draft")
        }

    @Test
    fun aReminderThatIsNotDueYetIsNotPosted() =
        runTest {
            clock = at(date(2026, 10, 2))
            repository.saveDraft(printedLeave)

            assertFalse(check().shouldPost(ReminderKind.RETURN, "d1"))
        }

    @Test
    fun aDismissedReminderIsNotPosted() =
        runTest {
            repository.saveDraft(printedLeave.copy(dismissed = setOf(ReminderTask.RETURN)))

            assertFalse(check().shouldPost(ReminderKind.RETURN, "d1"))
        }

    @Test
    fun aDeletedDraftIsNotPosted() =
        runTest {
            repository.saveDraft(printedLeave)
            repository.deleteDraft("d1")

            assertFalse(check().shouldPost(ReminderKind.RETURN, "d1"))
        }

    @Test
    fun anAnsweredAddTheOrderReminderIsNotPosted() =
        runTest {
            clock = at(date(2026, 10, 16))
            repository.saveDraft(printedLeave)
            assertTrue(check().shouldPost(ReminderKind.ADD_ORDER, "d1"), "before the order is added")

            repository.saveOrder(order("o1", "d1"))

            assertFalse(check().shouldPost(ReminderKind.ADD_ORDER, "d1"))
        }

    @Test
    fun aDraftExistsUntilItIsDeleted() =
        runTest {
            assertFalse(check().draftExists("d1"))
            repository.saveDraft(printedLeave)
            assertTrue(check().draftExists("d1"))
            assertFalse(check().draftExists("d2"))

            repository.deleteDraft("d1")

            assertFalse(check().draftExists("d1"))
        }
}
