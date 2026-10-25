package com.payslipmax.pdfparser.ui.screens.guide.part2.due

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The card on the Dashboard (DECISIONS Round 25): it shows after a payslip import completes, only while something is due now, and "Not now"
 * hides it until the next import. Nothing is stored. All state is the counts of this launch.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DueCardModelTest : DueTestBase() {
    private fun cardCheck(block: suspend kotlinx.coroutines.test.TestScope.(DueCardModel, () -> Int) -> Unit) =
        check(now = at(date(10, 12))) { vm, _ ->
            val model = DueCardModel(vm)
            val shown = MutableStateFlow(-1)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.count.collect { shown.value = it } }
            advanceUntilIdle()
            block(model) { shown.value }
        }

    private suspend fun dueSlip(id: String = "d1") {
        val slip = leaveSlip(id)
        repo.saveDraft(slip)
        publishAll(slip)
    }

    @Test
    fun beforeAnyImportTheCardIsHiddenEvenWithSomethingDue() =
        cardCheck { _, shown ->
            dueSlip()
            advanceUntilIdle()
            assertEquals(0, shown())
        }

    @Test
    fun afterAnImportItShowsHowManyAreDue() =
        cardCheck { model, shown ->
            dueSlip("d1")
            dueSlip("d2")
            model.onImportCompleted()
            advanceUntilIdle()
            assertEquals(2, shown())
        }

    @Test
    fun afterAnImportWithNothingDueItStaysHiddenAndAppearsWhenSomethingBecomesDue() =
        cardCheck { model, shown ->
            model.onImportCompleted()
            advanceUntilIdle()
            assertEquals(0, shown())
            dueSlip()
            advanceUntilIdle()
            assertEquals(1, shown())
        }

    @Test
    fun comingUpAloneDoesNotShowTheCard() =
        check(now = at(date(9, 21))) { vm, _ ->
            val model = DueCardModel(vm)
            val shown = MutableStateFlow(-1)
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.count.collect { shown.value = it } }
            repo.saveDraft(leaveSlip("d1"))
            model.onImportCompleted()
            advanceUntilIdle()
            assertEquals(0, shown.value)
        }

    @Test
    fun notNowHidesItUntilTheNextImport() =
        cardCheck { model, shown ->
            dueSlip()
            model.onImportCompleted()
            advanceUntilIdle()
            assertEquals(1, shown())
            model.notNow()
            advanceUntilIdle()
            assertEquals(0, shown())
            dueSlip("d2")
            advanceUntilIdle()
            assertEquals(0, shown())
            model.onImportCompleted()
            advanceUntilIdle()
            assertEquals(2, shown())
        }

    @Test
    fun anImportAfterNotNowShowsItEvenIfTheOfficerHadDismissedTwice() =
        cardCheck { model, shown ->
            dueSlip()
            model.onImportCompleted()
            model.notNow()
            model.onImportCompleted()
            model.notNow()
            advanceUntilIdle()
            assertEquals(0, shown())
            model.onImportCompleted()
            advanceUntilIdle()
            assertEquals(1, shown())
        }

    @Test
    fun theCardFollowsTheListLiveAndDisappearsWhenTheLastItemIsDone() =
        cardCheck { model, shown ->
            dueSlip()
            model.onImportCompleted()
            advanceUntilIdle()
            assertEquals(1, shown())
            repo.deleteDraft("d1")
            advanceUntilIdle()
            assertEquals(0, shown())
        }
}
