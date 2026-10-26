package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderTask
import com.payslipmax.pdfparser.part2.domain.ReminderTestBase
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakeReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: the operating system holds a copy of the reminders, and a copy that drifts from the drafts either nags the officer about something
 * answered or stays silent about something due. The sync is the one place that copies the planner's `upcoming` into the scheduler, so these
 * tests pin when it copies (every change that moves a reminder, entitlement included) and when it must not (an edit that moves none).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncTest : ReminderTestBase() {
    private val printedOn = date(2026, 10, 1)
    private var clock = at(printedOn)
    private val repository = FakePart2Repository()
    private val scheduler = FakeReminderScheduler()
    private val entitled = MutableStateFlow(true)

    private fun TestScope.sync() =
        ReminderSync(repository, scheduler, planner, now = { clock }, dispatcher = UnconfinedTestDispatcher(testScheduler)).also { it.start(entitled) }

    private val printedLeave = draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = at(printedOn))

    @Test
    fun anEntitledOfficersPlannedRemindersAreHandedToTheScheduler() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()

            assertEquals(plan(listOf(printedLeave), now = clock).upcoming, scheduler.pending)
            assertTrue(scheduler.pending.isNotEmpty(), "a printed leave has reminders to schedule")
        }

    @Test
    fun anOfficerWhoIsNotEntitledHasNothingScheduled() =
        runTest {
            entitled.value = false
            repository.saveDraft(printedLeave)
            sync()

            assertEquals(listOf(emptyList()), scheduler.calls, "the system is told to hold nothing, once")
        }

    @Test
    fun aLapseCancelsWhatWasScheduledAndARenewalSchedulesItAgain() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()
            val scheduled = scheduler.pending

            entitled.value = false
            assertEquals(emptyList(), scheduler.pending)

            entitled.value = true
            assertEquals(scheduled, scheduler.pending)
        }

    @Test
    fun dismissingAReminderTakesItOffTheSchedule() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()
            assertTrue(scheduler.pending.any { it.kind == ReminderKind.RETURN })

            repository.saveDraft(printedLeave.copy(dismissed = setOf(ReminderTask.RETURN)))

            assertFalse(scheduler.pending.any { it.kind == ReminderKind.RETURN })
            assertTrue(scheduler.pending.any { it.kind == ReminderKind.ADD_ORDER }, "only the dismissed kind goes")
        }

    @Test
    fun aPublishedOrderTakesAddTheOrderOffTheSchedule() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()
            assertTrue(scheduler.pending.any { it.kind == ReminderKind.ADD_ORDER })

            repository.saveOrder(order("o1", "d1"))

            assertFalse(scheduler.pending.any { it.kind == ReminderKind.ADD_ORDER })
        }

    @Test
    fun deletingTheDraftEmptiesTheSchedule() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()

            repository.deleteDraft("d1")

            assertEquals(emptyList(), scheduler.pending)
        }

    @Test
    fun anEditThatMovesNoReminderDoesNotTouchTheScheduler() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()
            val calls = scheduler.calls.size

            repository.saveDraft(printedLeave.copy(updatedAt = printedLeave.updatedAt + 1))

            assertEquals(calls, scheduler.calls.size, "autosave fires on every step; the system is not rebuilt for it")
        }

    @Test
    fun theClockIsReadAtEveryRebuild() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()
            assertTrue(scheduler.pending.any { it.kind == ReminderKind.RETURN })

            clock = at(date(2026, 10, 20))
            repository.saveDraft(printedLeave.copy(updatedAt = 9))

            assertFalse(scheduler.pending.any { it.kind == ReminderKind.RETURN }, "the return day has passed: it is due now, not upcoming")
        }

    @Test
    fun startingTwiceStartsOneCollection() =
        runTest {
            repository.saveDraft(printedLeave)
            val sync = sync()
            sync.start(entitled)
            val calls = scheduler.calls.size

            repository.saveDraft(printedLeave.copy(dismissed = setOf(ReminderTask.RETURN)))

            assertEquals(calls + 1, scheduler.calls.size)
        }

    @Test
    fun aSchedulerThatFailsDoesNotStopTheSyncAndTheNextChangeRetries() =
        runTest {
            repository.saveDraft(printedLeave)
            scheduler.failNext = true
            sync()
            assertEquals(emptyList(), scheduler.calls, "the first call failed")

            repository.saveDraft(printedLeave.copy(updatedAt = 9))

            assertTrue(scheduler.pending.isNotEmpty(), "the same list is offered again after a failure, not skipped as unchanged")
        }

    @Test
    fun hasRemindersIsTrueOnlyWhileSomethingIsDueOrComingUp() =
        runTest {
            val sync = sync()
            assertFalse(sync.hasReminders.value, "an empty device has none")

            repository.saveDraft(printedLeave)
            assertTrue(sync.hasReminders.value)

            entitled.value = false
            assertFalse(sync.hasReminders.value, "a lapsed officer has no reminders to be asked about")
        }

    @Test
    fun aReminderThatIsOnlyDueNowStillCountsAsAReminder() =
        runTest {
            val sync = sync()
            clock = at(date(2027, 3, 2))
            val answeredAndOld = draft("d2", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = at(printedOn))
            repository.saveOrder(order("o2", "d2"))
            repository.saveDraft(answeredAndOld)

            assertTrue(plan(listOf(answeredAndOld), listOf(order("o2", "d2")), clock).upcoming.isEmpty(), "the setup has nothing coming up")
            assertTrue(sync.hasReminders.value, "the overdue return and the year end are due now")
        }

    @Test
    fun afterAFailedChangeTheSameListAsBeforeIsGivenAgainBecauseTheSystemMayBeHalfUpdated() =
        runTest {
            repository.saveDraft(printedLeave)
            sync()
            val before = scheduler.pending
            scheduler.failNext = true

            repository.saveDraft(printedLeave.copy(dismissed = setOf(ReminderTask.RETURN)))
            assertEquals(before, scheduler.pending, "the failed call changed nothing we know of")
            val calls = scheduler.calls.size
            repository.saveDraft(printedLeave.copy(updatedAt = 99))

            assertEquals(calls + 1, scheduler.calls.size, "back to the earlier list: it is sent again, not skipped as unchanged")
            assertEquals(before, scheduler.pending)
        }

    @Test
    fun aCancelledSyncStopsInsteadOfSwallowingTheCancellation() =
        runTest {
            repository.saveDraft(printedLeave)
            val cancelling =
                object : ReminderScheduler {
                    var calls = 0

                    override fun replaceAll(reminders: List<com.payslipmax.pdfparser.part2.domain.Reminder>) {
                        calls++
                        throw kotlinx.coroutines.CancellationException("the sync was cancelled")
                    }
                }
            ReminderSync(repository, cancelling, planner, { clock }, UnconfinedTestDispatcher(testScheduler)).start(entitled)

            repository.saveDraft(printedLeave.copy(updatedAt = 9))

            assertEquals(1, cancelling.calls, "after a cancellation the collection is over: a later change does not call the scheduler again")
        }

    @Test
    fun startingTwiceReadsTheDevicesOnceForEveryChangeNotTwice() =
        runTest {
            var reads = 0
            val counting =
                ReminderSync(repository, scheduler, planner, now = {
                    reads++
                    clock
                }, dispatcher = UnconfinedTestDispatcher(testScheduler))
            counting.start(entitled)
            counting.start(entitled)
            val before = reads

            repository.saveDraft(printedLeave)

            assertEquals(1, reads - before, "one collection plans once per change; a second collection would plan twice")
        }
}
