package com.payslipmax.pdfparser.part2.leavebank

import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.insights.LeaveBankSummary
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The feed hands the form the rows of the app's one Leave Bank summary and nothing else (plan, phase 8b): nothing before the summary is attached,
 * every later summary as it changes, and no work while no draft is open. Synthetic statements.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveBankFeedTest {
    private val first = listOf(accrued("2023-12-31", 30, 282), accrued("2024-12-31", 19, 301))
    private val second = listOf(accrued("2025-12-31", 30, 331))

    private fun summaryOf(rows: List<LeaveEntry>): LeaveBankSummary = checkNotNull(LeaveBankCalculator.summarize(listOf(payslip(2026, 12, LeaveAccumulation(rows)))))

    @Test
    fun nothingIsHandedOverBeforeTheSummaryIsAttached() =
        runTest {
            val heard = mutableListOf<List<LeaveEntry>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { LeaveBankFeed().rows.collect { heard += it } }
            runCurrent()
            assertEquals(emptyList(), heard, "a form waiting for the rows is not told there are none")
        }

    @Test
    fun theRowsOfTheAttachedSummaryComeThroughAndFollowItsChangesIncludingNoStatement() =
        runTest {
            val feed = LeaveBankFeed()
            val summary = MutableStateFlow<LeaveBankSummary?>(summaryOf(first))
            feed.attach(summary)
            val heard = mutableListOf<List<LeaveEntry>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { feed.rows.collect { heard += it } }
            runCurrent()
            summary.value = summaryOf(second)
            runCurrent()
            summary.value = null
            runCurrent()
            assertEquals(listOf(first, second, emptyList()), heard, "the statement's rows as the one summary holds them, and none when it has no statement")
        }

    @Test
    fun aListenerThatArrivesAfterTheAttachGetsTheCurrentRowsAndALaterAttachReplacesTheEarlierSummary() =
        runTest {
            val feed = LeaveBankFeed()
            val old = MutableStateFlow<LeaveBankSummary?>(summaryOf(first))
            feed.attach(old)
            val heard = mutableListOf<List<LeaveEntry>>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { feed.rows.collect { heard += it } }
            runCurrent()
            feed.attach(MutableStateFlow(summaryOf(second)))
            runCurrent()
            old.value = summaryOf(first + second)
            runCurrent()
            assertEquals(listOf(first, second), heard, "the replaced summary is no longer listened to")
        }

    @Test
    fun theSummaryIsListenedToOnlyWhileSomeoneListensToTheFeed() =
        runTest {
            val feed = LeaveBankFeed()
            val summary = MutableSharedFlow<LeaveBankSummary?>(replay = 1)
            summary.tryEmit(summaryOf(first))
            feed.attach(summary)
            runCurrent()
            assertEquals(0, summary.subscriptionCount.value, "attaching costs nothing")
            val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { feed.rows.collect {} }
            runCurrent()
            assertEquals(1, summary.subscriptionCount.value)
            job.cancel()
            runCurrent()
            assertEquals(0, summary.subscriptionCount.value, "closing the form stops the listening")
        }
}
