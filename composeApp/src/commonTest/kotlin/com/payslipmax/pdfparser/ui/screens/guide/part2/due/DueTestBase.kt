package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.PrintedSnapshot
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.screens.guide.preclaim.PreClaimDates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

/**
 * Fixtures of the Due tests: a fake repository, a loaded (or not yet loaded) synthetic Guide, a clock the test moves and the officer in
 * India. Instants are built here with the picker's own conversion (`PreClaimDates`), not the planner's, so a test cannot agree with the
 * planner by sharing its arithmetic. Drafts and orders are synthetic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class DueTestBase {
    protected val repo = FakePart2Repository()
    protected val india = 19_800_000L
    protected var nowMillis = 0L

    protected fun date(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = CalendarDate(year, month, day)

    protected fun at(
        date: CalendarDate,
        hour: Int = 9,
    ): Long = PreClaimDates.toUtcMillis(date) + hour * 3_600_000L - india

    /** A printed leave slip with no leave year, so only its return reminder (and its add-the-order series, until an order is linked) exist. */
    protected fun leaveSlip(
        id: String,
        from: CalendarDate = date(10, 5),
        to: CalendarDate = date(10, 9),
        printedAt: Long? = at(date(9, 20)),
    ) = Part2Draft(
        id = id,
        event = LeaveEventInput(listOf(OccurrenceRequest("GTDAL", mapOf("leave_from" to SlotValue.DateValue(from), "leave_to" to SlotValue.DateValue(to))))),
        templateRevs = emptyMap(),
        createdAt = 1,
        updatedAt = 1,
        printed = printedAt?.let { PrintedSnapshot(it, emptyMap(), emptyList()) },
    )

    /** A numbered, dated vault order linked to [draftId]: the draft's order is published. */
    protected fun published(
        id: String,
        draftId: String,
    ) = VaultOrder(id, draftId, "9/26", date(7, 1), 2, 2)

    protected suspend fun publishAll(vararg drafts: Part2Draft) = drafts.forEach { repo.saveOrder(published("o-${it.id}", it.id)) }

    /** A Due model over [over] (a repository a test wraps to count or refuse writes), collected so it stays live; the Guide is not loaded. */
    protected fun TestScope.dueOver(
        over: Part2Repository,
        now: Long,
    ): DueViewModel {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPart2Templates.bundle())), FakeCrashReporter(), dispatcher)
        val vm = DueViewModel(over, Part2DraftsViewModel(guide, over, dispatcher) { india }, ReminderPlanner { india }, { now }, dispatcher)
        backgroundScope.launch(dispatcher) { vm.state.collect { } }
        return vm
    }

    protected fun check(
        guideLoaded: Boolean = true,
        now: Long = at(date(9, 21)),
        block: suspend TestScope.(DueViewModel, Part2DraftsViewModel) -> Unit,
    ) =
        runTest {
            nowMillis = now
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPart2Templates.bundle())), FakeCrashReporter(), dispatcher)
            if (guideLoaded) guide.load()
            advanceUntilIdle()
            val drafts = Part2DraftsViewModel(guide, repo, dispatcher) { india }
            val vm = DueViewModel(repo, drafts, ReminderPlanner { india }, { nowMillis }, dispatcher)
            backgroundScope.launch(dispatcher) { drafts.state.collect { } }
            backgroundScope.launch(dispatcher) { vm.state.collect { } }
            advanceUntilIdle()
            block(vm, drafts)
        }
}
