package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.data.StoredGuideNotes
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * M7 on Guide Home: a quiet line when stored notes could not be read (no button; the rows stay on the device), and a row
 * that opens "Notes on removed cards" only when there are some. WHY: an unreadable note must never vanish without a word,
 * and a note whose card is gone must stay reachable so the user can read or delete it. Default screen: this deletes
 * through a confirmation dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideNotesHomeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val clock = intArrayOf(0)
    private val store = FakeGuideNotesRepository(clock = { (++clock[0]).toLong() })

    private fun show(
        repository: GuideNotesRepository = store,
        unlocked: Boolean = true,
    ) {
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideBundle.withFigures())), FakeCrashReporter(), dispatcher, pins = GuidePinsModel(FakeGuidePinsStorage()))
        val notes = GuideNotesViewModel(repository, guide, dispatcher)
        val search = GuideSearchViewModel(guide, dispatcher, notes)
        composeRule.setContent { GuideTab(GuideNavState(), GuideAccess(unlocked, onUnlock = {}), guide, search, GuidePlatform(copy = {}, share = { _, _ -> }), notes) }
        settle()
    }

    // A screen subscribes to the notes while it recomposes, after the first pass over the dispatcher, so go round more than once.
    private fun settle() {
        repeat(3) {
            dispatcher.scheduler.advanceUntilIdle()
            composeRule.waitForIdle()
        }
    }

    private fun tap(text: String) {
        composeRule.onNodeWithText(text).performClick()
        settle()
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
        composeRule.waitForIdle()
    }

    private fun seed(
        cardId: String,
        text: String,
    ) = runBlocking { store.save(cardId, text, "rev") }

    private fun unreadable(count: Int) =
        object : GuideNotesRepository by store {
            override fun observe(): Flow<StoredGuideNotes> = store.observe().map { StoredGuideNotes(it.notes, unreadable = count) }
        }

    @Test
    fun withNoNotesHomeShowsNeitherTheUnreadableLineNorTheRemovedRow() {
        show()

        composeRule.onNodeWithText(GuideMaintenanceStrings.notesUnreadable(1)).assertDoesNotExist()
        composeRule.onNodeWithText(GuideMaintenanceStrings.notesRemovedRow(1)).assertDoesNotExist()
    }

    @Test
    fun unreadableNotesAreCountedInAQuietLineWithNoButton() {
        show(unreadable(2))

        scrollTo(GuideMaintenanceStrings.notesUnreadable(2))
        composeRule.onNodeWithText("2 notes could not be read").assertIsDisplayed()
        assertEquals("1 note could not be read", GuideMaintenanceStrings.notesUnreadable(1))
    }

    @Test
    fun theRemovedRowAppearsOnlyWhenANoteSitsOnARemovedCardAndOpensTheListNewestFirst() {
        seed("RB-GONE-1", "older words")
        seed("RB-GONE-2", "newer words")
        seed("RB-T5", "a live card's note")
        show()

        scrollTo(GuideMaintenanceStrings.notesRemovedRow(2))
        tap(GuideMaintenanceStrings.notesRemovedRow(2))

        composeRule.onNodeWithText(GuideMaintenanceStrings.notesRemovedTitle).assertIsDisplayed()
        composeRule.onNodeWithText("RB-GONE-1").assertIsDisplayed()
        composeRule.onNodeWithText("newer words").assertIsDisplayed()
        composeRule.onNodeWithText("a live card's note").assertDoesNotExist()
        val newerAbove = composeRule.onNodeWithText("RB-GONE-2").fetchSemanticsNode().positionInRoot.y < composeRule.onNodeWithText("RB-GONE-1").fetchSemanticsNode().positionInRoot.y
        assertEquals(true, newerAbove, "newest first")
    }

    @Test
    fun deletingARemovedNoteAsksFirstThenRemovesItAndTheLastOneEmptiesTheList() {
        seed("RB-GONE-1", "only words")
        show()
        scrollTo(GuideMaintenanceStrings.notesRemovedRow(1))
        tap(GuideMaintenanceStrings.notesRemovedRow(1))

        tap(GuideMaintenanceStrings.noteDelete)
        tap(AppStrings.btnCancel)
        composeRule.onNodeWithText("only words").assertIsDisplayed()

        tap(GuideMaintenanceStrings.noteDelete)
        tap(GuideMaintenanceStrings.noteDeleteConfirm)

        composeRule.onNodeWithText(GuideMaintenanceStrings.notesRemovedNone).assertIsDisplayed()
        assertEquals(emptyList(), runBlocking { store.observe().first().notes })
    }

    @Test
    fun notesAreFreeSoALockedUserStillSeesTheHomeLines() {
        seed("RB-GONE-1", "words")
        show(unlocked = false)

        scrollTo(GuideMaintenanceStrings.notesRemovedRow(1))
        composeRule.onNodeWithText(GuideMaintenanceStrings.notesRemovedRow(1)).assertIsDisplayed()
    }
}
