package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (Phase 11 item 35): a Due list left open past midnight (or past nine in the morning) kept its old labels, because it was rebuilt only when a
 * draft or an order changed. These tests move the clock and virtual time with NO edit to any draft: the list must follow the clock by itself.
 * The model here keeps its clock watch running for ever, so these tests move time with `advanceTimeBy` and never `advanceUntilIdle`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DueViewModelClockTest : DueTestBase() {
    private fun TestScope.watching(): DueViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPart2Templates.bundle())), FakeCrashReporter(), dispatcher)
        guide.load()
        val drafts = Part2DraftsViewModel(guide, repo, dispatcher) { india }
        val vm = DueViewModel(repo, drafts, ReminderPlanner { india }, { nowMillis }, dispatcher)
        backgroundScope.launch(dispatcher) { drafts.state.collect { } }
        backgroundScope.launch(dispatcher) { vm.state.collect { } }
        runCurrent()
        return vm
    }

    private val minute = 60_000L

    @Test
    fun aListLeftOpenPastMidnightTurnsFromDueToOverdueWithoutAnyEdit() =
        runTest {
            nowMillis = at(date(10, 10), hour = 23) + 30 * minute
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            publishAll(slip)
            val vm = watching()
            assertEquals(listOf(false), vm.state.value.due.map { it.overdue }, "due today, not yet overdue")
            nowMillis = at(date(10, 11), hour = 0) + 5 * minute
            advanceTimeBy(31 * minute)
            runCurrent()
            assertEquals(listOf(true), vm.state.value.due.map { it.overdue }, "the day changed: overdue since yesterday")
        }

    @Test
    fun aListLeftOpenAcrossNineOClockGainsTheReminderThatFellDueThen() =
        runTest {
            nowMillis = at(date(10, 10), hour = 8) + 50 * minute
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            publishAll(slip)
            val vm = watching()
            assertEquals(emptyList(), vm.state.value.due)
            assertEquals(1, vm.state.value.upcoming.size)
            nowMillis = at(date(10, 10), hour = 9) + minute
            advanceTimeBy(11 * minute)
            runCurrent()
            assertEquals(listOf("return:d1"), vm.state.value.due.map { it.key })
            assertTrue(vm.state.value.upcoming.isEmpty())
        }
}
