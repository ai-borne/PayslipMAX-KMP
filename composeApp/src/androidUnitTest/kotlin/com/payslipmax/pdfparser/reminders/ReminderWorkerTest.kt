package com.payslipmax.pdfparser.reminders

import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the worker runs when the system says, possibly weeks after the app was last open and in a process Part II never started in. So it
 * must answer from the database at that moment (an answered or dismissed reminder stays silent), must treat its input as untrusted, and
 * must never crash or retry in a loop: a reminder that cannot be posted is simply not posted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReminderWorkerTest : RemindersAndroidTestBase() {
    private fun run(
        kind: String? = "RETURN",
        draft: String? = "d1",
    ): ListenableWorker.Result {
        val input = workDataOf(*listOfNotNull(kind?.let { ReminderWorker.KIND to it }, draft?.let { ReminderWorker.DRAFT to it }).toTypedArray())
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).setInputData(input).build()
        return runBlocking { worker.doWork() }
    }

    @Test
    fun aReminderThatIsStillDueIsPosted() {
        save(printedLeave())

        assertEquals(ListenableWorker.Result.success(), run())

        assertEquals(1, posted().size)
    }

    @Test
    fun aDismissedReminderStaysSilent() {
        save(printedLeave(dismissed = setOf("return")))

        assertEquals(ListenableWorker.Result.success(), run())

        assertEquals(emptyList(), posted())
    }

    @Test
    fun aReminderForADeletedDraftStaysSilent() {
        assertEquals(ListenableWorker.Result.success(), run(draft = "gone"))

        assertEquals(emptyList(), posted())
    }

    @Test
    fun aReminderOfAKindThatIsNotDueStaysSilent() {
        save(printedLeave())

        run(kind = "YEAR_END")

        assertEquals(emptyList(), posted())
    }

    @Test
    fun anInputThatIsMissingOrMalformedIsIgnoredWithoutACrashOrARetry() {
        save(printedLeave())
        for ((kind, draft) in listOf(null to "d1", "RETURN" to null, "return" to "d1", "OTHER" to "d1", "RETURN" to "../d1", "RETURN" to "d1;x", "RETURN" to "a".repeat(65))) {
            assertEquals(ListenableWorker.Result.success(), run(kind, draft), "kind=$kind draft=$draft")
        }
        assertEquals(emptyList(), posted())
    }

    @Test
    fun withNoKoinAtAllNothingIsPosted() {
        save(printedLeave())
        stopKoin()

        assertEquals(ListenableWorker.Result.success(), run())

        assertEquals(emptyList(), posted())
    }

    @Test
    fun withPartTwoOffTheAppsKoinHasNoReminderBindingsAndNothingIsPosted() {
        // The real flag-off case: the app's Koin is running, it just never loaded part2Module.
        save(printedLeave())
        stopKoin()
        org.koin.core.context.startKoin { modules(org.koin.dsl.module { }) }

        assertEquals(ListenableWorker.Result.success(), run())

        assertEquals(emptyList(), posted())
    }

    @Test
    fun withTheFireCheckBoundButNoWordsNothingIsPosted() {
        save(printedLeave())
        stopKoin()
        org.koin.core.context.startKoin { modules(org.koin.dsl.module { single { com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck(repository, planner) { clock } } }) }

        assertEquals(ListenableWorker.Result.success(), run())

        assertEquals(emptyList(), posted())
    }

    @Test
    fun withNotificationsTurnedOffNothingIsPostedAndTheWorkEndsQuietly() {
        save(printedLeave())
        shadowManager.setNotificationsEnabled(false)

        assertEquals(ListenableWorker.Result.success(), run())

        assertEquals(emptyList(), posted())
    }

    @Test
    fun theTapOfWhatWasPostedNamesThisReminderOnly() {
        save(printedLeave())

        run()

        val intent = org.robolectric.Shadows.shadowOf(posted().single().contentIntent).savedIntent
        assertEquals("RETURN", intent.getStringExtra(ReminderLinkIntent.EXTRA_KIND))
        assertEquals("d1", intent.getStringExtra(ReminderLinkIntent.EXTRA_DRAFT))
        assertTrue(intent.extras!!.keySet().size == 2)
    }

    @Test
    fun aDraftsStoreThatCannotBeReadMeansNoNotificationAndNoCrashAndNoRetry() {
        val broken =
            object : com.payslipmax.pdfparser.part2.data.Part2Repository by repository {
                override fun observeDrafts(): kotlinx.coroutines.flow.Flow<com.payslipmax.pdfparser.part2.data.StoredPart2<com.payslipmax.pdfparser.part2.domain.Part2Draft>> = error("the store could not be opened")
            }
        stopKoin()
        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck(broken, planner) { clock } }
                    single { com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings.notificationText() }
                },
            )
        }

        assertEquals(ListenableWorker.Result.success(), run())

        assertEquals(emptyList(), posted())
    }

    private fun withWalkSwitch(
        debug: Boolean,
        on: Boolean,
    ) {
        val switch = com.payslipmax.pdfparser.part2.reminders.ShortDelayReminderScheduler(com.payslipmax.pdfparser.testing.FakeReminderScheduler(), isDebug = { debug }, now = { clock })
        switch.useShortDelay(on)
        stopKoin()
        org.koin.core.context.startKoin {
            modules(
                org.koin.dsl.module {
                    single { com.payslipmax.pdfparser.part2.reminders.ReminderFireCheck(repository, planner) { clock } }
                    single { com.payslipmax.pdfparser.ui.theme.Part2ReminderStrings.notificationText() }
                    single { switch }
                },
            )
        }
    }

    @Test
    fun withTheDebugWalkSwitchOnAReminderThatIsNotDueYetIsStillPosted() {
        // The switch brings a reminder forward in time only; the real plan still says it is not due. A walk needs to see it arrive.
        clock = returnDay - 3_600_000L
        save(printedLeave())
        withWalkSwitch(debug = true, on = true)

        run()

        assertEquals(1, posted().size)
    }

    @Test
    fun withTheDebugWalkSwitchOffTheSameReminderStaysSilent() {
        clock = returnDay - 3_600_000L
        save(printedLeave())
        withWalkSwitch(debug = true, on = false)

        run()

        assertEquals(emptyList(), posted())
    }

    @Test
    fun outsideADebugBuildTheSwitchChangesNothing() {
        clock = returnDay - 3_600_000L
        save(printedLeave())
        withWalkSwitch(debug = false, on = true)

        run()

        assertEquals(emptyList(), posted())
    }

    @Test
    fun theWalkSwitchDoesNotPostWhatTheOfficerAlreadyAnsweredOrDeleted() {
        // Even on a walk, a reminder for a draft that is gone is not posted: the switch skips the due check, never the draft check.
        withWalkSwitch(debug = true, on = true)

        run(draft = "gone")

        assertEquals(emptyList(), posted())
    }
}
