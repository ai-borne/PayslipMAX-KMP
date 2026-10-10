package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.data.GuideNoteSaveResult
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.data.StoredGuideNotes
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M7: edits through the notes view model. WHY: a note is the user's own words, so a move must never lose them (a failed second
 * step keeps the first), never overwrite the card's own note, and a store that fails must come back as an outcome, not a crash
 * or a log line.
 */
class GuideNotesEditViewModelTest : GuideNotesViewModelTestBase() {
    @Test
    fun aNoteOnAReplacedCardShowsOnTheNewCardAsStaleAndEditingItMovesIt() =
        test(guide = guide(SyntheticGuideRuleChange.bundle())) { model ->
            store.save(SyntheticGuideRuleChange.OLD_CARD, "old rule words", "oldrev01")
            testScheduler.advanceUntilIdle()

            val shown = model.state.value.forCard(SyntheticGuideRuleChange.NEW_CARD)
            assertNull(shown.own)
            val carried = shown.carried.single()
            assertEquals("old rule words", carried.text)
            assertTrue(carried.stale && carried.editable)
            assertEquals(SyntheticGuideRuleChange.OLD_CARD, carried.sourceCardId)

            assertEquals(GuideNoteOutcome.SAVED, save(model, SyntheticGuideRuleChange.NEW_CARD, "moved words", movedFrom = carried.sourceCardId))

            val rows = store.observe().first().notes
            assertEquals(listOf(SyntheticGuideRuleChange.NEW_CARD), rows.map { it.cardId }, "no duplicate stays on the earlier rule")
            val after = model.state.value.forCard(SyntheticGuideRuleChange.NEW_CARD)
            assertEquals("moved words", after.own?.text)
            assertEquals(emptyList(), after.carried)
        }

    @Test
    fun ifRemovingTheEarlierCopyFailsTheNewNoteIsKeptAndTheOutcomeSaysSo() {
        val failing =
            object : GuideNotesRepository by store {
                override suspend fun delete(cardId: String): Unit = throw IllegalStateException("disk")
            }
        test(guide = guide(SyntheticGuideRuleChange.bundle()), repository = failing) { model ->
            store.save(SyntheticGuideRuleChange.OLD_CARD, "old rule words", "oldrev01")
            testScheduler.advanceUntilIdle()

            val outcome = save(model, SyntheticGuideRuleChange.NEW_CARD, "moved words", movedFrom = SyntheticGuideRuleChange.OLD_CARD)

            assertEquals(GuideNoteOutcome.SAVED_NOT_MOVED, outcome)
            val rows = store.observe().first().notes.associate { it.cardId to it.text }
            assertEquals("moved words", rows[SyntheticGuideRuleChange.NEW_CARD], "the first step is not lost")
            assertEquals("old rule words", rows[SyntheticGuideRuleChange.OLD_CARD], "the earlier copy is left, not half removed")
        }
    }

    @Test
    fun aCarriedNoteCannotBeMovedOntoACardThatHasItsOwnNoteSoNothingIsOverwritten() =
        test(guide = guide(SyntheticGuideRuleChange.bundle())) { model ->
            store.save(SyntheticGuideRuleChange.NEW_CARD, "own words", "rev00001")
            store.save(SyntheticGuideRuleChange.OLD_CARD, "old rule words", "oldrev01")
            testScheduler.advanceUntilIdle()

            val shown = model.state.value.forCard(SyntheticGuideRuleChange.NEW_CARD)

            assertTrue(shown.own!!.editable)
            assertFalse(shown.carried.single().editable, "editing would replace the card's own note")
            assertEquals("own words", shown.own?.text)
        }

    @Test
    fun savingBlankTextErasesTheNoteAndNeverTouchesTheEarlierCopy() =
        test(guide = guide(SyntheticGuideRuleChange.bundle())) { model ->
            store.save(SyntheticGuideRuleChange.NEW_CARD, "own words", "rev00001")
            store.save(SyntheticGuideRuleChange.OLD_CARD, "old rule words", "oldrev01")
            testScheduler.advanceUntilIdle()

            val outcome = save(model, SyntheticGuideRuleChange.NEW_CARD, "   ", movedFrom = SyntheticGuideRuleChange.OLD_CARD)

            assertEquals(GuideNoteOutcome.DELETED, outcome)
            assertEquals(listOf(SyntheticGuideRuleChange.OLD_CARD), store.observe().first().notes.map { it.cardId })
        }

    @Test
    fun aCardTheGuideDoesNotHoldIsRefusedAndStoresNothing() =
        test { model ->
            assertEquals(GuideNoteOutcome.FAILED, save(model, "RB-NOPE", "words"))
            assertEquals(emptyList(), store.observe().first().notes)
        }

    @Test
    fun aStoreThatFailsGivesAnOutcomeInsteadOfACrashAndNothingReachesTelemetry() {
        val broken =
            object : GuideNotesRepository {
                override fun observe(): Flow<StoredGuideNotes> = flow { throw IllegalStateException("cannot read") }

                override suspend fun save(
                    cardId: String,
                    text: String,
                    cardRev: String,
                ): GuideNoteSaveResult = throw IllegalStateException("secret words in message")

                override suspend fun delete(cardId: String): Unit = throw IllegalStateException("cannot delete")
            }
        test(repository = broken) { model ->
            assertEquals(GuideNotesState.None.unreadable, model.state.value.unreadable)
            assertEquals(GuideNoteOutcome.FAILED, save(model, "RB-T5", "secret words"))
            assertFalse(delete(model, "RB-T5"))
            assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty())
        }
    }
}
