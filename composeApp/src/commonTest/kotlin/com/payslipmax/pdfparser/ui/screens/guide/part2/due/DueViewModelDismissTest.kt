package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.part2.domain.ReminderKind
import com.payslipmax.pdfparser.part2.domain.SlotValue
import com.payslipmax.pdfparser.ui.theme.Part2DueStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Dismissing a reminder from the Due list (DECISIONS Round 26): it is remembered with the draft, writes that one field and nothing else, covers
 * the whole series of that kind, leaves other drafts alone, writes nothing twice, and leaves the row on screen if the write was refused.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DueViewModelDismissTest : DueTestBase() {
    @Test
    fun everyRowCarriesTheNameItIsDismissedUnder() =
        check(now = at(date(10, 12))) { vm, _ ->
            repo.saveDraft(leaveSlip("d1"))
            val request = OccurrenceRequest("GTDAL", mapOf("year" to SlotValue.NumberValue(2026)))
            repo.saveDraft(Part2Draft("d2", LeaveEventInput(listOf(request)), emptyMap(), 1, 1, leaveSlip("x").printed))
            advanceUntilIdle()
            val all = vm.state.value.due + vm.state.value.upcoming
            assertEquals(setOf("return", "add-order", "year-end-2026"), all.map { it.task }.toSet())
        }

    @Test
    fun dismissingTheOverdueReturnStoresItWithTheDraftWithoutTouchingWhenItWasEdited() =
        check(now = at(date(10, 12))) { vm, _ ->
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            publishAll(slip)
            advanceUntilIdle()
            val row = vm.state.value.due.single()

            vm.dismiss(row.draftId, row.task)
            advanceUntilIdle()

            val stored = repo.draft("d1")!!
            assertEquals(setOf("return"), stored.dismissed)
            assertEquals(slip.copy(dismissed = setOf("return")), stored, "only the dismissed field changed: not its edit time, its print record or its answers")
            assertTrue(vm.state.value.isEmpty)
        }

    @Test
    fun dismissingAddTheOrderTakesItOutOfDueAndComingUpAndLeavesTheReturn() =
        check(now = at(date(10, 12))) { vm, _ ->
            repo.saveDraft(leaveSlip("d1"))
            advanceUntilIdle()
            val addOrder = Part2DueStrings.action(ReminderKind.ADD_ORDER, null)
            assertEquals(1, vm.state.value.due.count { it.action == addOrder })

            vm.dismiss("d1", "add-order")
            advanceUntilIdle()

            assertEquals(0, (vm.state.value.due + vm.state.value.upcoming).count { it.action == addOrder })
            assertEquals(listOf("return"), vm.state.value.due.map { it.task })
        }

    @Test
    fun aYearEndIsDismissedUnderItsLeaveYearOnItsOwnDraft() =
        check(now = at(date(9, 21))) { vm, _ ->
            val request = OccurrenceRequest("GTDAL", mapOf("year" to SlotValue.NumberValue(2026)))
            repo.saveDraft(Part2Draft("d1", LeaveEventInput(listOf(request)), emptyMap(), 1, 1, leaveSlip("x").printed))
            publishAll(leaveSlip("d1"))
            advanceUntilIdle()

            vm.dismiss("d1", vm.state.value.upcoming.single().task)
            advanceUntilIdle()

            assertEquals(setOf("year-end-2026"), repo.draft("d1")!!.dismissed)
            assertTrue(vm.state.value.isEmpty)
        }

    @Test
    fun otherDraftsKeepTheirRemindersAndDismissingTwiceChangesNothingMore() =
        check(now = at(date(10, 12))) { vm, _ ->
            val one = leaveSlip("d1")
            val two = leaveSlip("d2")
            repo.saveDraft(one)
            repo.saveDraft(two)
            publishAll(one, two)
            advanceUntilIdle()

            vm.dismiss("d1", "return")
            vm.dismiss("d1", "return")
            advanceUntilIdle()

            assertEquals(listOf("return:d2"), vm.state.value.due.map { it.key })
            assertEquals(emptySet(), repo.draft("d2")!!.dismissed)
            assertEquals(setOf("return"), repo.draft("d1")!!.dismissed)
        }

    @Test
    fun twoNamesOnOneDraftAreBothKeptAndTheSameNameAgainIsNotWrittenAgain() =
        runTest {
            var writes = 0
            val counting =
                object : Part2Repository by repo {
                    override suspend fun saveDraft(draft: Part2Draft): Part2WriteResult {
                        writes++
                        return repo.saveDraft(draft)
                    }
                }
            repo.saveDraft(leaveSlip("d1"))
            val vm = dueOver(counting, at(date(10, 12)))
            advanceUntilIdle()

            vm.dismiss("d1", "return")
            advanceUntilIdle()
            vm.dismiss("d1", "return")
            advanceUntilIdle()
            assertEquals(1, writes, "a name already dismissed is not written again")

            vm.dismiss("d1", "add-order")
            advanceUntilIdle()
            assertEquals(2, writes)
            assertEquals(setOf("return", "add-order"), repo.draft("d1")!!.dismissed, "the second dismissal adds to the first")
        }

    @Test
    fun dismissingForADraftThatIsGoneMakesNothing() =
        check { vm, _ ->
            vm.dismiss("ghost", "return")
            advanceUntilIdle()

            assertEquals(null, repo.draft("ghost"))
            assertTrue(vm.state.value.isEmpty)
        }

    @Test
    fun aWriteThatIsRefusedLeavesTheRowShown() =
        runTest {
            val refusing =
                object : Part2Repository by repo {
                    override suspend fun saveDraft(draft: Part2Draft): Part2WriteResult = Part2WriteResult.REJECTED
                }
            val slip = leaveSlip("d1")
            repo.saveDraft(slip)
            repo.saveOrder(published("o1", "d1"))
            val vm = dueOver(refusing, at(date(10, 12)))
            advanceUntilIdle()

            vm.dismiss("d1", "return")
            advanceUntilIdle()

            assertEquals(emptySet(), repo.draft("d1")!!.dismissed)
            assertEquals(1, vm.state.value.dueCount)
        }
}
