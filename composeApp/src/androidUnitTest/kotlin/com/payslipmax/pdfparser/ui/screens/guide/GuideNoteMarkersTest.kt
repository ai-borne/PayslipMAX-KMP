package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * M7: the small "Note" marker on rows, and note words in search. WHY: the marker tells the user a card has a note without
 * opening it; search must find a word that is only in a note but say it was the note (and never print it); and a locked user
 * must get neither (they have no note section either). A tall screen keeps whole lists composed; nothing here opens a dialog.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideNoteMarkersTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val crash = FakeCrashReporter()
    private val store = FakeGuideNotesRepository(clock = { 5_000L })
    private val pins = FakeGuidePinsStorage()
    private val marker = GuideMaintenanceStrings.noteMarker
    private val inNote = GuideMaintenanceStrings.noteMarkerInNote

    private fun show(
        unlocked: Boolean,
        stack: List<GuideDestination> = emptyList(),
    ) {
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(SyntheticGuideBundle.withFigures())), crash, dispatcher, pins = GuidePinsModel(pins))
        val notes = GuideNotesViewModel(store, guide, dispatcher)
        val search = GuideSearchViewModel(guide, dispatcher, notes)
        composeRule.setContent {
            GuideTab(GuideNavState(stack), GuideAccess(unlocked, onUnlock = {}), guide, search, GuidePlatform(copy = {}, share = { _, _ -> }), notes)
        }
        settle()
    }

    // A screen subscribes to the notes while it recomposes, after the first pass over the dispatcher, so go round more than once.
    private fun settle() {
        repeat(3) {
            dispatcher.scheduler.advanceUntilIdle()
            composeRule.waitForIdle()
        }
    }

    private fun seed(
        cardId: String,
        text: String,
    ) = runBlocking { store.save(cardId, text, "rev") }

    private fun searchFor(text: String) {
        composeRule.onNode(hasSetTextAction()).performTextInput(text)
        settle()
    }

    private val feed = listOf(GuideDestination.Area("travel"), GuideDestination.Case("td-da"))

    @Test
    fun aFeedRowWithANoteCarriesTheNoteMarkerAndOthersDoNot() {
        seed("RB-T5", "my words")
        show(unlocked = true, stack = feed)

        composeRule.onAllNodesWithText(marker).assertCountEquals(1)
        composeRule.onNodeWithText("Synthetic card RB-T5?").assertIsDisplayed()
    }

    @Test
    fun aLockedUserSeesNoMarkerOnAnyRow() {
        seed("RB-T5", "my words")
        show(unlocked = false, stack = feed)

        composeRule.onAllNodesWithText(marker).assertCountEquals(0)
    }

    @Test
    fun aPinnedRowWithANoteCarriesTheMarkerToo() {
        seed("RB-T5", "my words")
        pins.stored = "RB-T5"
        show(unlocked = true)

        composeRule.onNodeWithText(GuideStrings.pinnedSection).assertIsDisplayed()
        composeRule.onAllNodesWithText(marker).assertCountEquals(1)
    }

    @Test
    fun aWordThatIsOnlyInANoteFindsTheCardMarkedInYourNoteWithoutShowingTheNote() {
        seed("RB-T5", "Ask about the zebra form")
        show(unlocked = true, stack = listOf(GuideDestination.Search))

        searchFor("zebra")

        composeRule.onNodeWithText("Synthetic card RB-T5?").assertIsDisplayed()
        composeRule.onAllNodesWithText(inNote).assertCountEquals(1)
        composeRule.onAllNodesWithText("Ask about the zebra form", substring = true).assertCountEquals(0)
        composeRule.onAllNodesWithText("zebra", substring = true, ignoreCase = true).assertCountEquals(1) // only the query field itself
    }

    @Test
    fun aLockedUserNeverMatchesOnANoteAndSeesNoMarker() {
        seed("RB-T5", "Ask about the zebra form")
        show(unlocked = false, stack = listOf(GuideDestination.Search))

        searchFor("zebra")

        composeRule.onNodeWithText("Synthetic card RB-T5?").assertDoesNotExist()
        composeRule.onAllNodesWithText(inNote).assertCountEquals(0)
        composeRule.onAllNodesWithText(marker).assertCountEquals(0)
    }

    @Test
    fun aCardThatMatchesOnItsOwnWordsShowsTheNoteMarkerButNotInYourNote() {
        seed("RB-T5", "unrelated words")
        show(unlocked = true, stack = listOf(GuideDestination.Search))

        searchFor("synthetic card RB-T5")

        composeRule.onAllNodesWithText(inNote).assertCountEquals(0)
        composeRule.onAllNodesWithText(marker).assertCountEquals(1)
    }

    @Test
    fun searchingAndMarkingTellTelemetryNothing() {
        seed("RB-T5", "Ask about the zebra form")
        show(unlocked = true, stack = listOf(GuideDestination.Search))
        searchFor("zebra")

        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty())
    }
}
