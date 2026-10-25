package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.StoredPart2
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.ReminderPlanner
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.ui.screens.guide.GuideViewModel
import com.payslipmax.pdfparser.ui.screens.guide.part2.draft.Part2DraftsViewModel
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The Due list, worded from the planner's plan and the drafts list: what is due now and what is next, one line per reminder. The model
 * holds no date arithmetic and no entitlement (the screens decide who sees it); it follows the repository live.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DueViewModelTest : DueTestBase() {
    @Test
    fun nothingSavedMeansAnEmptyList() =
        check { vm, _ ->
            assertEquals(DueUiState.Empty, vm.state.value)
            assertTrue(vm.state.value.isEmpty)
        }

    @Test
    fun aLeaveSlipPastItsReturnDayIsDueOverdueWithItsWordsAndItsTitle() =
        check(now = at(date(10, 12))) { vm, drafts ->
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            publishAll(slip)
            advanceUntilIdle()
            val row = vm.state.value.due.single()
            assertEquals("return:d1", row.key)
            assertEquals("d1", row.draftId)
            assertEquals(Part2DueStrings.action(ReminderKind.RETURN, null), row.action)
            assertEquals(drafts.state.value.rows.single().title, row.title)
            assertEquals(Part2DueStrings.dueLine("10/10/2026", overdue = true), row.line)
            assertTrue(row.overdue)
            assertEquals(1, vm.state.value.dueCount)
        }

    @Test
    fun onTheDayItIsDueButNotOverdue() =
        check(now = at(date(10, 10))) { vm, _ ->
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            publishAll(slip)
            advanceUntilIdle()
            val row = vm.state.value.due.single()
            assertFalse(row.overdue)
            assertEquals(Part2DueStrings.dueLine("10/10/2026", overdue = false), row.line)
        }

    @Test
    fun beforeTheGuideHasLoadedARowStillShowsWithAPlainTitle() =
        check(guideLoaded = false, now = at(date(10, 12))) { vm, _ ->
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            publishAll(slip)
            advanceUntilIdle()
            assertEquals(Part2DueStrings.untitled, vm.state.value.due.single().title)
        }

    @Test
    fun comingUpShowsTheNextOfARepeatingReminderOnceNotEverySixth() =
        check(now = at(date(9, 21))) { vm, _ ->
            repo.saveDraft(leaveSlip("d1"))
            advanceUntilIdle()
            val addOrder = vm.state.value.upcoming.filter { it.action == Part2DueStrings.action(ReminderKind.ADD_ORDER, null) }
            assertEquals(1, addOrder.size)
            assertEquals(Part2DueStrings.dueLine("04/10/2026", overdue = false), addOrder.single().line)
            assertEquals(emptyList(), vm.state.value.due)
        }

    @Test
    fun aRepeatingReminderAlreadyDueIsNotListedAgainUnderComingUp() =
        check(now = at(date(10, 5))) { vm, _ ->
            repo.saveDraft(leaveSlip("d1"))
            advanceUntilIdle()
            val addOrder = Part2DueStrings.action(ReminderKind.ADD_ORDER, null)
            // why: the first "add the order" reminder (4 Oct) is due; its next repeat is the same task, so the list must not say it twice.
            assertEquals(1, vm.state.value.due.count { it.action == addOrder })
            assertEquals(0, vm.state.value.upcoming.count { it.action == addOrder })
            assertEquals(listOf(Part2DueStrings.action(ReminderKind.RETURN, null)), vm.state.value.upcoming.map { it.action })
        }

    @Test
    fun aYearEndReminderNamesItsLeaveYear() =
        check(now = at(date(9, 21))) { vm, _ ->
            val request = OccurrenceRequest("GTDAL", mapOf("year" to SlotValue.NumberValue(2026)))
            repo.saveDraft(Part2Draft("d1", LeaveEventInput(listOf(request)), emptyMap(), 1, 1, leaveSlip("x").printed))
            publishAll(leaveSlip("d1"))
            advanceUntilIdle()
            val row = vm.state.value.upcoming.single()
            assertEquals(Part2DueStrings.action(ReminderKind.YEAR_END, 2026), row.action)
            assertEquals(Part2DueStrings.dueLine("01/03/2027", overdue = false), row.line)
        }

    @Test
    fun linkingTheNumberedOrderTakesTheAddTheOrderRowAwayAndDeletingTheDraftEmptiesTheList() =
        check(now = at(date(10, 5))) { vm, _ ->
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            advanceUntilIdle()
            assertEquals(Part2DueStrings.action(ReminderKind.ADD_ORDER, null), vm.state.value.due.single { it.key.startsWith("add-order") }.action)
            repo.saveOrder(published("o1", "d1"))
            advanceUntilIdle()
            assertTrue(vm.state.value.due.none { it.key.startsWith("add-order") })
            repo.deleteDraft("d1")
            advanceUntilIdle()
            assertTrue(vm.state.value.isEmpty)
        }

    @Test
    fun theClockIsReadEachTimeTheListIsRebuilt() =
        check(now = at(date(10, 8))) { vm, _ ->
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            publishAll(slip)
            advanceUntilIdle()
            assertEquals(emptyList(), vm.state.value.due)
            assertEquals(1, vm.state.value.upcoming.size)
            nowMillis = at(date(10, 11))
            repo.saveDraft(leaveSlip("other", printedAt = null))
            advanceUntilIdle()
            assertEquals(listOf("return:d1"), vm.state.value.due.map { it.key })
        }

    @Test
    fun anUnreadableOrderIsSaidNotHidden() =
        check { vm, _ ->
            repo.unreadableOrders = 2
            repo.saveDraft(leaveSlip("d1"))
            // The fake reports the count when the orders next change, as the real repository does when a row cannot be read.
            repo.saveOrder(published("o1", "d1"))
            advanceUntilIdle()
            assertTrue(vm.state.value.unreadable)
        }

    @Test
    fun twoLeaveYearsOfOneDraftAreTwoYearEndRowsNotOne() =
        check(now = at(date(9, 21))) { vm, _ ->
            val grants = listOf(2026, 2027).map { OccurrenceRequest("GTDAL", mapOf("year" to SlotValue.NumberValue(it))) }
            repo.saveDraft(Part2Draft("d1", LeaveEventInput(grants), emptyMap(), 1, 1, leaveSlip("x").printed))
            publishAll(leaveSlip("d1"))
            advanceUntilIdle()
            assertEquals(
                listOf(Part2DueStrings.action(ReminderKind.YEAR_END, 2026), Part2DueStrings.action(ReminderKind.YEAR_END, 2027)),
                vm.state.value.upcoming.map { it.action },
            )
        }

    @Test
    fun anUnreadableDraftIsSaidNotHiddenEither() =
        runTest {
            val unreadableDrafts =
                object : Part2Repository by repo {
                    override fun observeDrafts(): Flow<StoredPart2<Part2Draft>> = repo.observeDrafts().map { StoredPart2(it.items, unreadable = 1) }
                }
            val dispatcher = StandardTestDispatcher(testScheduler)
            val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticPart2Templates.bundle())), FakeCrashReporter(), dispatcher)
            val drafts = Part2DraftsViewModel(guide, unreadableDrafts, dispatcher) { india }
            val vm = DueViewModel(unreadableDrafts, drafts, ReminderPlanner { india }, { at(date(10, 12)) }, dispatcher)
            backgroundScope.launch(dispatcher) { vm.state.collect { } }
            advanceUntilIdle()
            assertTrue(vm.state.value.unreadable)
        }

    @Test
    fun nothingAboutADraftReachesTheListsString() =
        check(now = at(date(10, 12))) { vm, _ ->
            val slip = leaveSlip("MARKER_DRAFT")
            repo.saveDraft(slip)
            publishAll(slip)
            advanceUntilIdle()
            assertFalse("MARKER_DRAFT" in vm.state.value.toString())
        }
}
