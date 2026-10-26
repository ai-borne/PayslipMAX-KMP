package com.payslipmax.pdfparser.part2.reminders

import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderTask
import com.payslipmax.pdfparser.part2.domain.ReminderTestBase
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakeReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (Phase 7b device walk, 2026-10-26): a reboot drops every queued reminder job, and the app is not running to rebuild them. A boot
 * receiver asks the sync for one rebuild from the drafts and vault as they are, with no watching and no entitlement (no screen or billing is
 * loaded in a boot process), and it must never throw.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncRebuildTest : ReminderTestBase() {
    private var clock = at(date(2026, 10, 1))
    private val repository = FakePart2Repository()
    private val scheduler = FakeReminderScheduler()
    private val printedLeave = draft("d1", leave(date(2026, 10, 5), date(2026, 10, 9)), printedAt = at(date(2026, 10, 1)))

    @Test
    fun aRebuildAfterRebootQueuesWhatThePlannerSaysWithoutWatchingAnything() =
        runTest {
            repository.saveDraft(printedLeave)
            val sync = ReminderSync(repository, scheduler, planner, now = { clock }, dispatcher = UnconfinedTestDispatcher(testScheduler))

            sync.rebuildOnce()

            assertEquals(plan(listOf(printedLeave), now = clock).upcoming, scheduler.pending)
            assertEquals(1, scheduler.calls.size)
            repository.saveDraft(printedLeave.copy(dismissed = setOf(ReminderTask.RETURN)))
            assertEquals(1, scheduler.calls.size, "a one-shot rebuild starts no collection: later changes are the running app's job")
        }

    @Test
    fun aRebuildOnADeviceWithNoDraftsGivesTheSchedulerAnEmptyList() =
        runTest {
            ReminderSync(repository, scheduler, planner, now = { clock }, dispatcher = UnconfinedTestDispatcher(testScheduler)).rebuildOnce()

            assertEquals(listOf(emptyList()), scheduler.calls)
        }

    @Test
    fun aRebuildWhoseSchedulerFailsDoesNotThrowSoABootReceiverCannotCrash() =
        runTest {
            repository.saveDraft(printedLeave)
            scheduler.failNext = true

            ReminderSync(repository, scheduler, planner, now = { clock }, dispatcher = UnconfinedTestDispatcher(testScheduler)).rebuildOnce()

            assertEquals(emptyList(), scheduler.calls)
        }

    @Test
    fun aRebuildAfterRebootReadsTheVaultToo() =
        runTest {
            repository.saveDraft(printedLeave)
            repository.saveOrder(order("o1", "d1"))

            ReminderSync(repository, scheduler, planner, now = { clock }, dispatcher = UnconfinedTestDispatcher(testScheduler)).rebuildOnce()

            assertFalse(scheduler.pending.any { it.kind == ReminderKind.ADD_ORDER }, "the order is published, so add-the-order is answered")
            assertTrue(scheduler.pending.any { it.kind == ReminderKind.RETURN })
        }
}
