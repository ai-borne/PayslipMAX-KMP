package com.payslipmax.pdfparser.ui.screens.guide.part2.draft

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.Part2Draft
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.ui.theme.Part2CancelStrings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: a cancel slip and the corrected draft made with it point at each other (DECISIONS Round 22), so the officer can hop between them.
 * The pointer must survive every autosave (a write that dropped it would orphan the pair), name the button for the kind of slip it is on,
 * and vanish when the partner is deleted, so a tap never leads to a draft that is not there.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LeaveDraftPairTest : LeaveDraftTestBase() {
    private fun stored(
        id: String,
        code: String,
        pairedWith: String?,
    ) = Part2Draft(id, LeaveEventInput(listOf(OccurrenceRequest(code))), emptyMap(), 1, 2, null, pairedWith)

    @Test
    fun aCancelSlipOffersItsCorrectedDraftAndTheCorrectedDraftOffersTheCancelSlip() =
        runTest {
            repo.saveDraft(stored("cancel", "CANCEL", pairedWith = "fix"))
            repo.saveDraft(stored("fix", GRANT, pairedWith = "cancel"))

            val cancel = model(start = null, id = "cancel").also { advanceUntilIdle() }
            val fix = model(start = null, id = "fix").also { advanceUntilIdle() }

            assertEquals(PairLinkUi("fix", Part2CancelStrings.openCorrectedDraft), cancel.state.value.pair)
            assertEquals(PairLinkUi("cancel", Part2CancelStrings.openCancelSlip), fix.state.value.pair)
        }

    @Test
    fun aSlipThatCancelsOneOccurrenceIsAlsoACancelSlip() =
        runTest {
            repo.saveDraft(stored("one", "CANCELS", pairedWith = "fix"))
            repo.saveDraft(stored("fix", GRANT, pairedWith = "one"))

            assertEquals(Part2CancelStrings.openCorrectedDraft, model(start = null, id = "one").also { advanceUntilIdle() }.state.value.pair?.label)
        }

    @Test
    fun noPartnerOrAPartnerThatIsGoneMeansNoButton() =
        runTest {
            repo.saveDraft(stored("alone", GRANT, pairedWith = null))
            repo.saveDraft(stored("orphan", GRANT, pairedWith = "deleted"))

            assertNull(model(start = null, id = "alone").also { advanceUntilIdle() }.state.value.pair)
            assertNull(model(start = null, id = "orphan").also { advanceUntilIdle() }.state.value.pair)
        }

    @Test
    fun everyAutosaveKeepsThePartnerPointer() =
        runTest {
            repo.saveDraft(stored("cancel", "CANCEL", pairedWith = "fix"))
            repo.saveDraft(stored("fix", GRANT, pairedWith = "cancel"))
            val vm = model(start = null, id = "fix").also { advanceUntilIdle() }

            vm.setDate(0, "leave_from", from)
            advanceTimeBy(LeaveDraftViewModel.AUTOSAVE_DELAY_MS + 1)
            advanceUntilIdle()

            assertEquals("cancel", saved("fix")?.pairedWith)
        }

    @Test
    fun aDraftWithNoPartnerStaysWithoutOneAfterASave() =
        runTest {
            val vm = model()
            vm.setDate(0, "leave_from", from)
            advanceTimeBy(LeaveDraftViewModel.AUTOSAVE_DELAY_MS + 1)
            advanceUntilIdle()

            assertNull(saved()?.pairedWith)
        }

    @Test
    fun theButtonFollowsThePartnerLiveWhenItIsDeletedOrSavedAfterTheSlipLoaded() =
        runTest {
            repo.saveDraft(stored("cancel", "CANCEL", pairedWith = "fix"))
            val vm = model(start = null, id = "cancel").also { advanceUntilIdle() }
            assertNull(vm.state.value.pair, "the partner is not on the device yet")

            repo.saveDraft(stored("fix", GRANT, pairedWith = "cancel"))
            advanceUntilIdle()
            assertEquals("fix", vm.state.value.pair?.draftId)

            repo.deleteDraft("fix")
            advanceUntilIdle()
            assertNull(vm.state.value.pair, "a tap must never lead to a draft that is not there")
        }

    /** A repository that is slow to read one draft, so the list of drafts answers before the draft itself has loaded. */
    private class SlowDraftRead(
        private val inner: FakePart2Repository,
    ) : Part2Repository by inner {
        override suspend fun draft(id: String): Part2Draft? {
            delay(100)
            return inner.draft(id)
        }
    }

    @Test
    fun theButtonAppearsEvenWhenTheListOfDraftsAnswersBeforeTheSlipHasLoaded() =
        runTest {
            val inner = FakePart2Repository()
            inner.saveDraft(stored("cancel", "CANCEL", pairedWith = "fix"))
            inner.saveDraft(stored("fix", GRANT, pairedWith = "cancel"))

            val vm = LeaveDraftViewModel("cancel", SyntheticPart2Templates.lookup, SlowDraftRead(inner), null, SyntheticPart2Templates.registry(), { clock }, StandardTestDispatcher(testScheduler)) { 0L }
            advanceUntilIdle()

            assertEquals("fix", vm.state.value.pair?.draftId, "the list answered first, knowing nothing of the pair; loading the draft must still find the partner")
        }
}
