package com.payslipmax.pdfparser.part2.print

import android.print.PrintJobInfo
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

/**
 * WHY: Android's `onFinish` cannot tell a printed slip from a cancelled dialog, and the printed time starts the "add the published
 * order" reminder. So only a job the system reports COMPLETED counts (Round 18); every other ending, a job that never ends and a job
 * that cannot be read are "not printed", which costs the officer a missing reminder and never a wrong one.
 */
class PrintJobWatchTest {
    private fun states(vararg states: Int): () -> Int {
        val queue = states.toMutableList()
        return { if (queue.size > 1) queue.removeAt(0) else queue.first() }
    }

    @Test
    fun aJobThatReachesCompletedIsPrintedAfterItsEarlierStates() =
        runTest {
            val pauses = mutableListOf<Long>()
            val outcome = PrintJobWatch.outcome(states(PrintJobInfo.STATE_CREATED, PrintJobInfo.STATE_QUEUED, PrintJobInfo.STATE_STARTED, PrintJobInfo.STATE_COMPLETED)) { pauses += it }

            assertEquals(PrintOutcome.COMPLETED, outcome)
            assertEquals(listOf(PrintJobWatch.POLL_MS, PrintJobWatch.POLL_MS, PrintJobWatch.POLL_MS), pauses)
        }

    @Test
    fun aJobAlreadyCompletedNeedsNoWait() =
        runTest {
            var paused = false

            assertEquals(PrintOutcome.COMPLETED, PrintJobWatch.outcome(states(PrintJobInfo.STATE_COMPLETED)) { paused = true })
            assertEquals(false, paused)
        }

    @Test
    fun aCancelledOrFailedJobIsNotPrinted() =
        runTest {
            for (end in listOf(PrintJobInfo.STATE_CANCELED, PrintJobInfo.STATE_FAILED)) {
                var pauses = 0
                val outcome = PrintJobWatch.outcome(states(PrintJobInfo.STATE_CREATED, PrintJobInfo.STATE_QUEUED, end)) { pauses++ }

                assertEquals(PrintOutcome.NOT_PRINTED, outcome)
                assertEquals(2, pauses, "the watch stops the moment the job ends: it is not polled for half an hour")
            }
        }

    @Test
    fun aJobThatIsOnlyQueuedOrStartedIsNotPrintedYet() =
        runTest {
            var waited = 0L
            val outcome = PrintJobWatch.outcome(states(PrintJobInfo.STATE_QUEUED, PrintJobInfo.STATE_STARTED, PrintJobInfo.STATE_BLOCKED)) { waited += it }

            assertEquals(PrintOutcome.NOT_PRINTED, outcome, "accepted by the system is not the same as printed")
            assertEquals(Part2Limits.PRINT_WATCH_MS, waited, "gives up after the watch limit and not before")
        }

    @Test
    fun aJobThatCannotBeReadIsNotPrinted() =
        runTest {
            assertEquals(PrintOutcome.NOT_PRINTED, PrintJobWatch.outcome({ error("the job is gone") }) { })
            assertEquals(PrintOutcome.NOT_PRINTED, PrintJobWatch.outcome({ throw UnsupportedOperationException() }) { })
        }
}
