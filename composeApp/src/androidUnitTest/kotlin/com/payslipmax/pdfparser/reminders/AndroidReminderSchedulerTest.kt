package com.payslipmax.pdfparser.reminders

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.domain.Reminder
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: what the system holds must equal the planner's `upcoming` after every rebuild: a reminder the planner dropped (answered, dismissed,
 * deleted) must be cancelled, one it moved must move, and one it kept must not be doubled. The scheduler also adds no date of its own; its
 * delay is the planner's instant minus now.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidReminderSchedulerTest : RemindersAndroidTestBase() {
    private lateinit var work: WorkManager
    private val tomorrow = clock + 86_400_000L
    private val day = CalendarDate(2026, 10, 11)
    private val back = Reminder(ReminderKind.RETURN, "d1", tomorrow, day)
    private val add = Reminder(ReminderKind.ADD_ORDER, "d1", tomorrow + 3_600_000L, day, sequence = 1)

    @Before
    fun startWorkManager() {
        WorkManagerTestInitHelper.initializeTestWorkManager(context, Configuration.Builder().setExecutor(SynchronousExecutor()).build())
        work = WorkManager.getInstance(context)
    }

    private fun scheduler() = AndroidReminderScheduler({ work }) { clock }

    private fun states(key: String) = work.getWorkInfosForUniqueWork(key).get().map { it.state }

    @Test
    fun eachReminderIsOneQueuedJobNamedByItsKey() {
        scheduler().replaceAll(listOf(back, add))

        assertEquals(listOf(WorkInfo.State.ENQUEUED), states(back.key))
        assertEquals(listOf(WorkInfo.State.ENQUEUED), states(add.key))
    }

    @Test
    fun theJobWaitsForTheInstantThePlannerGaveAndHoldsNothingElse() {
        val request = scheduler().request(back)

        assertEquals(tomorrow - clock, request.workSpec.initialDelay)
        assertEquals(setOf(ReminderWorker.KIND, ReminderWorker.DRAFT), request.workSpec.input.keyValueMap.keys, "the input is the kind and the draft id only")
        assertEquals("RETURN", request.workSpec.input.getString(ReminderWorker.KIND))
        assertEquals("d1", request.workSpec.input.getString(ReminderWorker.DRAFT))
        assertEquals(ReminderWorker::class.java.name, request.workSpec.workerClassName)
    }

    @Test
    fun aReminderThatIsAlreadyPastWaitsNoTimeAtAll() {
        val request = scheduler().request(back.copy(at = clock - 5_000L))

        assertEquals(0L, request.workSpec.initialDelay)
    }

    @Test
    fun theJobNeedsNothingFromTheNetworkOrTheBattery() {
        val constraints = scheduler().request(back).workSpec.constraints

        assertTrue(!constraints.requiresCharging() && !constraints.requiresBatteryNotLow() && !constraints.requiresDeviceIdle())
        assertEquals(androidx.work.NetworkType.NOT_REQUIRED, constraints.requiredNetworkType)
    }

    @Test
    fun givingTheSameListAgainDoesNotDoubleAnything() {
        scheduler().replaceAll(listOf(back, add))
        scheduler().replaceAll(listOf(back, add))

        assertEquals(1, work.getWorkInfosForUniqueWork(back.key).get().count { it.state == WorkInfo.State.ENQUEUED })
        assertEquals(2, work.getWorkInfosByTag(AndroidReminderScheduler.TAG).get().count { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test
    fun aReminderTheListNoLongerHoldsIsCancelledAndTheOthersStay() {
        scheduler().replaceAll(listOf(back, add))

        scheduler().replaceAll(listOf(back))

        assertEquals(listOf(WorkInfo.State.CANCELLED), states(add.key))
        assertEquals(listOf(WorkInfo.State.ENQUEUED), states(back.key))
    }

    @Test
    fun anEmptyListCancelsEverything() {
        scheduler().replaceAll(listOf(back, add))

        scheduler().replaceAll(emptyList())

        assertEquals(emptyList(), work.getWorkInfosByTag(AndroidReminderScheduler.TAG).get().filter { it.state == WorkInfo.State.ENQUEUED })
    }

    @Test
    fun aMovedReminderIsReplacedNotAddedTwice() {
        scheduler().replaceAll(listOf(back))
        val moved = back.copy(at = tomorrow + 7_200_000L)

        scheduler().replaceAll(listOf(moved))

        val live = work.getWorkInfosForUniqueWork(back.key).get().filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, live.size)
    }

    @Test
    fun aJobThatIsRunningRightNowIsNotCancelledByARebuildThatNoLongerListsIt() {
        // The worker is held inside the fire check (its repository never answers), so its job stays RUNNING while the rebuild happens.
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val slow =
            object : com.payslipmax.pdfparser.part2.data.Part2Repository by repository {
                override fun observeDrafts(): kotlinx.coroutines.flow.Flow<com.payslipmax.pdfparser.part2.data.StoredPart2<com.payslipmax.pdfparser.part2.domain.Part2Draft>> = kotlinx.coroutines.flow.flow { gate.await() }
            }
        stopKoin()
        startKoin {
            modules(
                org.koin.dsl.module {
                    single { com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck(slow, planner) { clock } }
                    single { com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings.notificationText() }
                },
            )
        }
        scheduler().replaceAll(listOf(back))
        val id = work.getWorkInfosForUniqueWork(back.key).get().single().id
        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(id)
        assertEquals(WorkInfo.State.RUNNING, work.getWorkInfoById(id).get()!!.state, "the setup: the job is running")

        scheduler().replaceAll(emptyList())
        assertEquals(WorkInfo.State.RUNNING, work.getWorkInfoById(id).get()!!.state, "a job that has fired is posting its notification; a rebuild leaves it alone")
        scheduler().replaceAll(listOf(back))
        assertEquals(WorkInfo.State.RUNNING, work.getWorkInfoById(id).get()!!.state, "even a rebuild that lists the same key does not cancel it")
        gate.cancel()
    }

    @Test
    fun aJobThatAlreadyFinishedKeepsItsResultWhenARebuildNoLongerListsIt() {
        scheduler().replaceAll(listOf(back))
        val id = work.getWorkInfosForUniqueWork(back.key).get().single().id
        WorkManagerTestInitHelper.getTestDriver(context)!!.setInitialDelayMet(id)

        scheduler().replaceAll(emptyList())

        assertTrue(states(back.key).single() != WorkInfo.State.CANCELLED, "a job that already ran keeps its result")
    }

    @Suppress("unused")
    private val keepContextImport: Context get() = context
}
