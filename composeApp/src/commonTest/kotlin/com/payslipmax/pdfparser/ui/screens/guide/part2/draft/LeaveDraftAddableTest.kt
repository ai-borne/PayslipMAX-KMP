package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.domain.LeaveEventEditor
import com.payslipmax.pdfparser.testing.SyntheticPart2Ribbons
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CLAIM
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY: a cancel slip must not be mixed with the codes of another ribbon (DECISIONS Round 22). The form lists, and the model accepts, only the
 * codes of the ribbon the slip started in, read from the layout, so a leave slip never offers a cancellation and a cancel slip never offers leave.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftAddableTest {
    private fun TestScope.model(first: String) =
        LeaveDraftViewModel("d", SyntheticPart2Ribbons.twoRibbons, com.payslipmax.pdfparser.testing.FakePart2Repository(), LeaveEventEditor.start(first), SyntheticPart2Templates.registry(), { 1L }, StandardTestDispatcher(testScheduler)) { 0L }

    @Test
    fun theFormOffersOnlyTheCodesOfTheRibbonTheSlipStartedIn() =
        runTest {
            assertFalse(CLAIM in model(GRANT).state.value.addable.map { it.code })
            assertTrue(EXTENSION in model(GRANT).state.value.addable.map { it.code })
            assertEquals(listOf(CLAIM), model(CLAIM).state.value.addable.map { it.code })
        }

    @Test
    fun anOccurrenceOfAnotherRibbonIsRefusedAndOneOfTheSameRibbonIsAdded() =
        runTest {
            val vm = model(GRANT)

            vm.addOccurrence(CLAIM)
            advanceUntilIdle()
            assertEquals(1, vm.state.value.sections.size, "a claim does not join a grant slip")

            vm.addOccurrence(EXTENSION)
            advanceUntilIdle()
            assertEquals(listOf(GRANT, EXTENSION), vm.state.value.sections.map { it.code })
        }

    @Test
    fun theOfferFollowsTheFirstOccurrenceEvenWhenItIsRemovedLater() =
        runTest {
            val vm = model(CLAIM)

            vm.addOccurrence(CLAIM)
            advanceUntilIdle()

            assertEquals(listOf(CLAIM, CLAIM), vm.state.value.sections.map { it.code })
            vm.addOccurrence(GRANT)
            advanceUntilIdle()
            assertEquals(2, vm.state.value.sections.size, "a grant does not join a claim slip")
        }
}
