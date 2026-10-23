package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.StoredPart2
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.theme.Part2Strings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The drafts list the Part II home shows: newest first, worded from the bundle, and still counting a draft before the Guide has loaded. */
@OptIn(ExperimentalCoroutinesApi::class)
class Part2DraftsViewModelTest {
    private val repo = FakePart2Repository()
    private val bundle = SyntheticPart2Templates.bundle()

    private fun day(
        m: Int,
        d: Int,
    ) = SlotValue.DateValue(CalendarDate(2026, m, d))

    private fun complete(
        id: String,
        updatedAt: Long,
    ) =
        Part2Draft(id, LeaveEventInput(listOf(OccurrenceRequest(GRANT, mapOf("leave_from" to day(7, 5), "leave_to" to day(9, 4), "year" to SlotValue.NumberValue(2026))))), emptyMap(), 1L, updatedAt)

    private fun unfinished(
        id: String,
        updatedAt: Long,
    ) = Part2Draft(id, LeaveEventInput(listOf(OccurrenceRequest(GRANT, mapOf("leave_from" to day(7, 5))))), emptyMap(), 1L, updatedAt)

    private fun check(block: suspend TestScope.(Part2DraftsViewModel) -> Unit) =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), FakeCrashReporter(), dispatcher)
            guide.load()
            advanceUntilIdle()
            val vm = Part2DraftsViewModel(guide, repo, dispatcher)
            backgroundScope.launch(dispatcher) { vm.state.collect { } }
            advanceUntilIdle()
            block(vm)
        }

    @Test
    fun noDraftsMeansAnEmptyListAndNoHasAny() =
        check { vm ->
            assertEquals(DraftsUiState.Empty, vm.state.value)
        }

    @Test
    fun draftsListNewestFirstEachWordedFromItsSummary() =
        check { vm ->
            repo.saveDraft(complete("old", 1_700_000_000_000L))
            repo.saveDraft(unfinished("new", 1_700_100_000_000L))
            advanceUntilIdle()
            val state = vm.state.value
            assertTrue(state.hasAny)
            assertEquals(listOf("new", "old"), state.rows.map { it.id })
            assertEquals(Part2Strings.draftRowTitle(listOf(GRANT)), state.rows[0].title)
            assertEquals(Part2Strings.draftRowLine(null, "16/11/2023"), state.rows[0].line)
            assertEquals(Part2Strings.draftRowLine(Part2Strings.dateSpan("05/07/2026", "04/09/2026"), "14/11/2023"), state.rows[1].line)
        }

    @Test
    fun deletingOneLeavesTheOthers() =
        check { vm ->
            repo.saveDraft(complete("a", 10L))
            repo.saveDraft(complete("b", 20L))
            advanceUntilIdle()
            vm.delete("b")
            advanceUntilIdle()
            assertEquals(listOf("a"), vm.state.value.rows.map { it.id })
            vm.delete("a")
            advanceUntilIdle()
            assertFalse(vm.state.value.hasAny)
        }

    @Test
    fun anUnreadableDraftIsSaidSoAndNeverDropped() =
        runTest {
            val unreadable =
                object : Part2Repository by repo {
                    override fun observeDrafts(): Flow<StoredPart2<Part2Draft>> = repo.observeDrafts().map { StoredPart2(it.items, unreadable = 2) }
                }
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), FakeCrashReporter(), dispatcher).also { it.load() }
            val vm = Part2DraftsViewModel(guide, unreadable, dispatcher)
            backgroundScope.launch(dispatcher) { vm.state.collect { } }
            advanceUntilIdle()
            assertTrue(vm.state.value.unreadable)
            assertFalse(vm.state.value.hasAny, "nothing readable, so no row and no draft to open")
        }

    @Test
    fun aDraftCountsBeforeTheGuideHasLoadedAndIsWordedAfter() =
        runTest {
            repo.saveDraft(complete("a", 10L))
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(bundle)), FakeCrashReporter(), dispatcher)
            val vm = Part2DraftsViewModel(guide, repo, dispatcher)
            backgroundScope.launch(dispatcher) { vm.state.collect { } }
            advanceUntilIdle()
            assertTrue(vm.state.value.hasAny)
            assertEquals(emptyList(), vm.state.value.rows, "the Guide has not loaded, so there is nothing to word a row with")
            guide.load()
            advanceUntilIdle()
            assertEquals(listOf("a"), vm.state.value.rows.map { it.id })
        }

    @Test
    fun aBundleWithNoPartTwoSectionStillCountsTheDraftButWordsNoRow() =
        runTest {
            repo.saveDraft(complete("a", 10L))
            val dispatcher = StandardTestDispatcher(testScheduler)
            val old = bundle.copy(part2 = null)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(old)), FakeCrashReporter(), dispatcher).also { it.load() }
            val vm = Part2DraftsViewModel(guide, repo, dispatcher)
            backgroundScope.launch(dispatcher) { vm.state.collect { } }
            advanceUntilIdle()
            assertTrue(vm.state.value.hasAny)
            assertEquals(emptyList(), vm.state.value.rows)
        }
}
