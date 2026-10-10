package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.data.StoredGuideNotes
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * M7: the notes view model places the user's notes against the loaded Guide and writes edits. WHY each rule: a note is the
 * user's own words, so none may be lost (a failed second step of a move keeps the first), none may overwrite another
 * (a carried note cannot be moved onto a card that has its own), and a store that fails must show up as a result the
 * screen can report, not as a crash or a log line. Nothing here may reach telemetry.
 */
class GuideNotesViewModelTest : GuideNotesViewModelTestBase() {
    @Test
    fun nothingIsPlacedUntilTheGuideHasLoadedSoNoNoteIsCalledRemoved() =
        runTest(dispatcher) {
            store.save("RB-T5", "my words", "rev00001")
            val guide = guide()
            val model = watch(notes(guide = guide))

            testScheduler.advanceUntilIdle()
            assertEquals(emptySet(), model.state.value.notedCards)
            assertEquals(emptyList(), model.state.value.removed)

            guide.load()
            testScheduler.advanceUntilIdle()
            assertEquals(setOf("RB-T5"), model.state.value.notedCards)
            assertEquals(emptyList(), model.state.value.removed)
        }

    @Test
    fun theStoreIsNotReadAtAllUntilAScreenCollectsTheState() =
        runTest(dispatcher) {
            var reads = 0
            val counting =
                object : GuideNotesRepository by store {
                    override fun observe(): Flow<StoredGuideNotes> = store.observe().onStart { reads++ }
                }
            val guide = guide()
            val model = notes(counting, guide)
            guide.load()
            testScheduler.runCurrent()
            assertEquals(0, reads, "a locked card screen never collects, so it never decrypts a note")

            watch(model)
            testScheduler.runCurrent()
            assertEquals(1, reads)
        }

    @Test
    fun addEditAndDeleteANoteOnACard() =
        test { model ->
            assertEquals(GuideNoteOutcome.SAVED, save(model, "RB-T5", "first words"))
            assertEquals("first words", model.state.value.forCard("RB-T5").own?.text)

            assertEquals(GuideNoteOutcome.SAVED, save(model, "RB-T5", "  edited words  "))
            assertEquals("edited words", model.state.value.forCard("RB-T5").own?.text, "the domain trims")

            assertTrue(delete(model, "RB-T5"))
            assertNull(model.state.value.forCard("RB-T5").own)
            assertEquals(emptySet(), model.state.value.notedCards)
        }

    @Test
    fun aNoteIsWrittenAgainstTheCardsCurrentRevisionAndIsStaleOnlyWhenItMovesOn() =
        runTest(dispatcher) {
            val first = guide(withRev(SyntheticGuideBundle.withFigures(), "rev00001"))
            first.load()
            testScheduler.advanceUntilIdle()
            val model = watch(notes(guide = first))
            save(model, "RB-T5", "my words")
            assertEquals("rev00001", store.observe().first().notes.single().cardRev)
            assertFalse(model.state.value.forCard("RB-T5").own!!.stale)

            // The app is updated and the card's text (so its revision) changed: the same stored note is now stale.
            val updated = guide(withRev(SyntheticGuideBundle.withFigures(), "rev00002"))
            val after = watch(notes(guide = updated))
            updated.load()
            testScheduler.advanceUntilIdle()
            assertTrue(after.state.value.forCard("RB-T5").own!!.stale)
        }

    @Test
    fun theUnreadableCountPassesThroughAndRowsThatCannotBeReadAreNotListedAnywhere() {
        val withUnreadable =
            object : GuideNotesRepository by store {
                override fun observe(): Flow<StoredGuideNotes> = store.observe().map { StoredGuideNotes(it.notes, unreadable = 2) }
            }
        test(repository = withUnreadable) { model ->
            store.save("RB-T5", "readable words", "rev00001")
            testScheduler.advanceUntilIdle()

            assertEquals(2, model.state.value.unreadable)
            assertEquals(setOf("RB-T5"), model.state.value.notedCards)
        }
    }

    @Test
    fun notesOnCardsThatNoLongerExistAreListedNewestFirstWithTheirCardIds() =
        test { model ->
            val clock = intArrayOf(0)
            val ordered = FakeGuideNotesRepository(clock = { (++clock[0]).toLong() })
            ordered.save("RB-GONE-1", "older words", "r")
            ordered.save("RB-GONE-2", "newer words", "r")
            val otherGuide = guide()
            val other = watch(notes(ordered, otherGuide))
            otherGuide.load()
            testScheduler.advanceUntilIdle()

            assertEquals(listOf("RB-GONE-2" to "newer words", "RB-GONE-1" to "older words"), other.state.value.removed.map { it.cardId to it.text })
            assertEquals(emptySet(), other.state.value.notedCards, "a removed card shows no marker anywhere")
            assertTrue(model.state.value.removed.isEmpty())
        }

    @Test
    fun theSearchWordsCoverTheNotesShownOnEachCardIncludingCarriedOnes() =
        test(guide = guide(SyntheticGuideRuleChange.bundle())) { model ->
            store.save(SyntheticGuideRuleChange.NEW_CARD, "alpha words", "rev00001")
            store.save(SyntheticGuideRuleChange.OLD_CARD, "beta words", "oldrev01")
            testScheduler.advanceUntilIdle()

            val words = model.state.value.searchNotes.words(SyntheticGuideRuleChange.NEW_CARD)

            assertEquals(setOf("alpha", "beta", "words"), words)
        }

    @Test
    fun noPrintedFormOfTheStateTheCardNotesOrTheItemsContainsNoteText() =
        test { model ->
            save(model, "RB-T5", "very private zebra words")
            store.save("RB-GONE", "another private zebra", "r")
            testScheduler.advanceUntilIdle()

            val state = model.state.value
            val printed = listOf(state.toString(), state.forCard("RB-T5").toString(), state.forCard("RB-T5").own.toString(), state.removed.single().toString(), state.searchNotes.toString())
            assertTrue(printed.none { it.contains("zebra") || it.contains("private") }, printed.joinToString())
        }
}
