package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import com.payslipmax.pdfparser.ui.theme.GuideStrings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * M8 end to end: the pieces M4 (a rule change) and M7 (private notes) built apart, walked together the way a user meets them
 * after an app update. Three notes were written under the old release: one on a card whose wording was then clarified (it must say
 * the card was updated since), one on a card that did not change (it must not), and one on the rule that was then replaced (it
 * follows the user to the new rule, marked as from an earlier version, and is no longer on the old card).
 * Home -> What's new -> the clarified card -> the unchanged card -> the new rule -> "Earlier rule" -> "See current rule" -> back
 * through the list to Home.
 *
 * The walk opens no dialog, so it can use a tall screen where every section is composed. An editor dialog needs the default
 * screen (a `w360dp-...` qualifier makes an `AlertDialog` text field spin until the test JVM runs out of memory): the editor
 * is covered by `GuideNotesEndToEndTest`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w360dp-h2400dp")
class GuideMaintenanceEndToEndTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val crash = FakeCrashReporter()
    private val clock = intArrayOf(0)
    private val store = FakeGuideNotesRepository(clock = { (++clock[0]).toLong() })
    private val nav = GuideNavState()
    private val newRelease = withRevisions(SyntheticGuideRuleChange.bundle())
    private val oldTitle = "Synthetic card $OLD?"
    private val newTitle = "Synthetic card $NEW?"
    private val clarifiedTitle = "Synthetic card $CLARIFIED?"
    private val unchangedTitle = "Synthetic card $UNCHANGED?"
    private val whatsNew = "What's new in the Guide (Nov 2026)"

    /** Each card gets a revision of its own, so a note written against an older one can be told from one written against this. */
    private fun withRevisions(bundle: GuideBundle) = bundle.copy(cards = bundle.cards.map { it.copy(rev = "now-${it.id}") })

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

    private fun tapFirst(text: String) {
        composeRule.onAllNodesWithText(text)[0].performClick()
        settle()
    }

    private fun back() {
        composeRule.runOnUiThread { nav.pop() }
        settle()
    }

    private fun show() {
        // Written by the previous release, so against the revisions the cards had then: two are older than today's, one is today's.
        runBlocking {
            store.save(OLD, "Old form is the zebra one", "before-the-update")
            store.save(CLARIFIED, "Clarified wording suits my case", "before-the-update")
            store.save(UNCHANGED, "Unchanged card, still true", "now-$UNCHANGED")
        }
        val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(newRelease)), crash, dispatcher, pins = GuidePinsModel(FakeGuidePinsStorage()))
        val notes = GuideNotesViewModel(store, guide, dispatcher)
        composeRule.setContent {
            GuideTab(nav, GuideAccess(true, onUnlock = {}), guide, GuideSearchViewModel(guide, dispatcher, notes), GuidePlatform(copy = {}, share = { _, _ -> }), notes)
        }
        settle()
    }

    @Test
    fun homeToWhatsNewToTheClarifiedCardToTheNewRuleToTheEarlierRuleAndBackWithEachNoteTold() {
        show()

        // 1. Home offers the change log. The note on the replaced rule has a successor, so it is not a note on a removed card.
        composeRule.onNodeWithText(whatsNew).assertIsDisplayed()
        composeRule.onAllNodesWithText(GuideMaintenanceStrings.notesRemovedRow(1)).assertCountEquals(0)
        tap(whatsNew)
        assertEquals(GuideDestination.Changes, nav.current)

        // 2. A clarified card: Updated, and its note was written against the old wording, so it says so.
        tapFirst(clarifiedTitle)
        assertEquals(GuideDestination.Card(CLARIFIED), nav.current)
        composeRule.onNodeWithText(GuideMaintenanceStrings.chipUpdated).assertIsDisplayed()
        composeRule.onNodeWithText("Clarified wording suits my case").assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteStale).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedStale).assertDoesNotExist()
        back()
        assertEquals(GuideDestination.Changes, nav.current)

        // 3. A card from the older entry that has not changed: its note was written against this very text, so no warning.
        tapFirst(unchangedTitle)
        assertEquals(GuideDestination.Card(UNCHANGED), nav.current)
        composeRule.onNodeWithText("Unchanged card, still true").assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteStale).assertDoesNotExist()
        composeRule.onNodeWithText(GuideMaintenanceStrings.chipUpdated).assertDoesNotExist()
        back()

        // 4. The new rule: the note from the rule it replaced comes with it, marked as from an earlier version; the user may add their own.
        tapFirst(newTitle)
        assertEquals(GuideDestination.Card(NEW), nav.current)
        composeRule.onNodeWithText(GuideMaintenanceStrings.chipUpdated).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedHeading).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedStale).assertIsDisplayed()
        composeRule.onNodeWithText("Old form is the zebra one").assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteAdd).assertIsDisplayed()

        // 5. Earlier rule: the replaced card shows its Replaced chip and notice, and no note (it moved to the current rule).
        tap(GuideMaintenanceStrings.earlierRule(SyntheticGuideRuleChange.EFFECTIVE))
        assertEquals(GuideDestination.Card(OLD), nav.current)
        composeRule.onNodeWithText(GuideMaintenanceStrings.chipReplacedOn(SyntheticGuideRuleChange.EFFECTIVE)).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.replacedNotice).assertIsDisplayed()
        composeRule.onNodeWithText("Old form is the zebra one").assertDoesNotExist()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedHeading).assertDoesNotExist()

        // 6. See current rule returns to the card just left (no stacking), so one Back reaches the list and a second reaches Home.
        tap(GuideMaintenanceStrings.seeCurrentRule)
        assertEquals(GuideDestination.Card(NEW), nav.current)
        composeRule.onNodeWithText("Old form is the zebra one").assertIsDisplayed()
        back()
        assertEquals(GuideDestination.Changes, nav.current)
        back()
        assertEquals(GuideDestination.Home, nav.current)
        composeRule.onNodeWithText(GuideStrings.homeTitle).assertIsDisplayed()

        // 7. Reading changed nothing, and the whole walk told telemetry nothing (no card id, note, date or count).
        assertEquals(setOf(OLD, CLARIFIED, UNCHANGED), runBlocking { store.observe().first().notes.map { it.cardId }.toSet() })
        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty(), "the whole walk tells telemetry nothing")
    }

    private companion object {
        const val OLD = SyntheticGuideRuleChange.OLD_CARD
        const val NEW = SyntheticGuideRuleChange.NEW_CARD
        const val CLARIFIED = SyntheticGuideRuleChange.CLARIFIED_CARD
        const val UNCHANGED = SyntheticGuideRuleChange.OLDER_ENTRY_CARD
    }
}
