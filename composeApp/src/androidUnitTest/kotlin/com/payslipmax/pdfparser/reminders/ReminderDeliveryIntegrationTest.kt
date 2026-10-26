package com.payslipmax.pdfparser.reminders

import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.payslipmax.pdfparser.Screen
import com.payslipmax.pdfparser.openIn
import com.payslipmax.pdfparser.part2.reminders.ReminderSync
import com.payslipmax.pdfparser.ui.screens.guide.GuideDestination
import com.payslipmax.pdfparser.ui.screens.guide.GuideNavState
import com.payslipmax.pdfparser.ui.screens.guide.Part2Destination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (plan, Phase 7b integration): the whole chain, with the real planner, sync, scheduler, worker, notifier and link, over WorkManager's
 * test driver: a printed leave is scheduled, its time comes, the generic notification is posted, and the tap lands on Guide, Part II, Due.
 * Each part is tested alone elsewhere; this proves they are joined the way the app joins them.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderDeliveryIntegrationTest : RemindersAndroidTestBase() {
    private lateinit var work: WorkManager
    private val entitled = MutableStateFlow(true)

    @Before
    fun startWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        work = WorkManager.getInstance(context)
        clock = returnDay - 3_600_000L
    }

    private fun startSync() = ReminderSync(repository, AndroidReminderScheduler({ work }) { clock }, planner, { clock }, Dispatchers.Unconfined).also { it.start(entitled) }

    private fun queued() = work.getWorkInfosByTag(AndroidReminderScheduler.TAG).get().filter { it.state == WorkInfo.State.ENQUEUED }

    @Test
    fun aPrintedLeaveIsScheduledDeliveredAndTheTapOpensDue() =
        runBlocking {
            save(printedLeave())
            startSync()
            val returnJob = work.getWorkInfosForUniqueWork("return:d1").get().first { it.state == WorkInfo.State.ENQUEUED }

            clock = returnDay + 60_000L
            WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(returnJob.id)

            val note = posted().single()
            assertEquals("A Part II reminder is due.", note.extras.getString("android.text"))
            val guide = GuideNavState(listOf(GuideDestination.Search))
            val shown = mutableListOf<Screen>()
            ReminderLinkIntent.offer(shadowOf(note.contentIntent).savedIntent, pending)
            assertTrue(pending.openIn(guide) { shown += it })
            assertEquals(Part2Destination.Due, guide.current)
            assertEquals(listOf(Screen.Guide), shown)
        }

    @Test
    fun dismissingTheReminderBeforeItsTimeLeavesTheNotificationUnposted() =
        runBlocking {
            save(printedLeave())
            startSync()
            val returnJob = work.getWorkInfosForUniqueWork("return:d1").get().first { it.state == WorkInfo.State.ENQUEUED }

            save(printedLeave(dismissed = setOf("return")))
            clock = returnDay + 60_000L

            assertTrue(queued().none { it.id == returnJob.id }, "the rebuild took it off the queue")
            assertEquals(emptyList(), posted())
        }

    @Test
    fun aLapseCancelsEveryQueuedReminderAndRenewalQueuesThemAgain() =
        runBlocking {
            save(printedLeave())
            startSync()
            assertTrue(queued().isNotEmpty())

            entitled.value = false
            assertEquals(emptyList(), queued())

            entitled.value = true
            assertTrue(queued().isNotEmpty())
        }

    @Test
    fun aJobThatFiresAfterTheDraftWasDeletedWithoutARebuildStaysSilent() =
        runBlocking {
            // No sync is running here: this is the app killed before it could rebuild, so the queued job outlives its draft.
            val draft = printedLeave()
            save(draft)
            AndroidReminderScheduler({ work }) { clock }.replaceAll(planner.plan(listOf(draft), emptyList(), clock).upcoming)
            val returnJob = work.getWorkInfosForUniqueWork("return:d1").get().first { it.state == WorkInfo.State.ENQUEUED }
            repository.deleteDraft("d1")
            clock = returnDay + 60_000L

            WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(returnJob.id)

            assertEquals(WorkInfo.State.SUCCEEDED, work.getWorkInfoById(returnJob.id).get()!!.state, "the job ran")
            assertEquals(emptyList(), posted())
            assertNull(pending.link.value)
        }
}
