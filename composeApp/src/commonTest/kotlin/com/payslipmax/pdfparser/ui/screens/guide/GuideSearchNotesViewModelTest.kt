package com.payslipmax.pdfparser.ui.screens.guide

import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.data.StoredGuideNotes
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * M7: search through the view model also looks in the user's notes. WHY the cases: the notes are private, so a locked user
 * must neither match on them nor cause them to be read; a result must say it came from the note without carrying a word of
 * it; and a change to the notes (a save, a delete) must change the results without another keystroke.
 */
class GuideSearchNotesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = FakeGuideNotesRepository(clock = { 1L })
    private var reads = 0
    private val counting =
        object : GuideNotesRepository by store {
            override fun observe(): Flow<StoredGuideNotes> = store.observe().onStart { reads++ }
        }
    private val guide = GuideViewModel(FakeGuideRepository(), FakeCrashReporter(), dispatcher)
    private val notes = GuideNotesViewModel(counting, guide, dispatcher)
    private val search = GuideSearchViewModel(guide, dispatcher, notes)

    private fun test(block: suspend TestScope.() -> Unit) =
        runTest(dispatcher) {
            guide.load()
            testScheduler.advanceUntilIdle()
            block()
        }

    private fun TestScope.type(text: String): GuideSearchState {
        search.onQueryChange(text)
        testScheduler.advanceUntilIdle()
        return search.state.value
    }

    private fun GuideSearchState.rows() = assertIs<GuideSearchState.Results>(this).rows

    @Test
    fun anUnlockedUserFindsAWordThatIsOnlyInANoteAndTheRowSaysSoWithoutCarryingIt() =
        test {
            search.setUnlocked(true)
            store.save("RB-T5", "Ask about the zebra form", "rev")
            testScheduler.advanceUntilIdle()

            val row = type("zebra").rows().single()

            assertEquals("RB-T5", row.cardId)
            assertTrue(row.matchedInNote)
            assertFalse(row.toString().contains("zebra"), "the row holds the card's own words, never the note's")
        }

    @Test
    fun aLockedUserNeverMatchesOnANoteAndTheNotesAreNotEvenRead() =
        test {
            store.save("RB-T5", "Ask about the zebra form", "rev")
            testScheduler.advanceUntilIdle()

            assertEquals(emptyList(), type("zebra").rows())
            assertEquals(0, reads, "a locked search never subscribes to the notes")
        }

    @Test
    fun savingAndDeletingANoteChangesTheResultsWithoutAnotherKeystroke() =
        test {
            search.setUnlocked(true)
            assertEquals(emptyList(), type("zebra").rows())

            store.save("RB-T5", "zebra", "rev")
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("RB-T5"), search.state.value.rows().map { it.cardId })

            store.delete("RB-T5")
            testScheduler.advanceUntilIdle()
            assertEquals(emptyList(), search.state.value.rows())
        }

    @Test
    fun unlockingStartsMatchingOnNotesAndRelockingStopsAtOnce() =
        test {
            store.save("RB-T5", "zebra", "rev")
            testScheduler.advanceUntilIdle()
            assertEquals(emptyList(), type("zebra").rows())

            search.setUnlocked(true)
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("RB-T5"), search.state.value.rows().map { it.cardId })

            search.setUnlocked(false)
            testScheduler.advanceUntilIdle()
            assertEquals(emptyList(), search.state.value.visibleTo(false).let { (it as? GuideSearchState.Results)?.rows.orEmpty() })
        }

    @Test
    fun aCardThatMatchesByItsOwnTitleIsNotMarkedAsFoundInTheNote() =
        test {
            search.setUnlocked(true)
            store.save("RB-T5", "zebra", "rev")
            testScheduler.advanceUntilIdle()

            val row = type("synthetic card RB-T5").rows().single { it.cardId == "RB-T5" }

            assertFalse(row.matchedInNote)
        }
}
