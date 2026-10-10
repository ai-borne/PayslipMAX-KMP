package com.payslipmax.pdfparser.ui.screens.guide

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.testing.FakeCrashReporter
import com.payslipmax.pdfparser.testing.FakeGuideNotesRepository
import com.payslipmax.pdfparser.testing.FakeGuidePinsStorage
import com.payslipmax.pdfparser.testing.FakeGuideRepository
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
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
 * M7 end to end, in the order a user lives it: Home, a pinned card, add a note; the app is updated and the card's text
 * changes (the stale line); a later update replaces the rule (the note follows to the new card, marked stale); editing it
 * there moves it so no copy stays on the old rule; and a note on a card that is gone is reachable and deletable from Home.
 * Each "update" is a new Guide model over the same notes store, which is what a restarted app is. The default screen is
 * used because the editor is a dialog with a text field.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GuideNotesEndToEndTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val dispatcher = StandardTestDispatcher()
    private val crash = FakeCrashReporter()
    private val clock = intArrayOf(0)
    private val store = FakeGuideNotesRepository(clock = { (++clock[0]).toLong() })
    private val pins = FakeGuidePinsStorage()
    private val nav = GuideNavState()

    private fun withRev(
        bundle: GuideBundle,
        rev: String,
    ): GuideBundle = bundle.copy(cards = bundle.cards.map { it.copy(rev = rev) })

    private val releases =
        listOf(
            withRev(SyntheticGuideBundle.withFigures(), "rev00001"),
            withRev(SyntheticGuideBundle.withFigures(), "rev00002"),
            SyntheticGuideRuleChange.bundle(),
        )

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

    private fun stored() = runBlocking { store.observe().first().notes.associate { it.cardId to it.text } }

    @Test
    fun homeToCardAddNoteStaleBannerCarriedToTheNewRuleEditMovesItAndTheRemovedList() {
        var release by mutableIntStateOf(0)
        pins.stored = OLD
        composeRule.setContent {
            key(release) {
                val guide = GuideViewModel(FakeGuideRepository(GuideLoadResult.Loaded(releases[release])), crash, dispatcher, pins = GuidePinsModel(pins))
                val notes = GuideNotesViewModel(store, guide, dispatcher)
                GuideTab(nav, GuideAccess(true, onUnlock = {}), guide, GuideSearchViewModel(guide, dispatcher, notes), GuidePlatform(copy = {}, share = { _, _ -> }), notes)
            }
        }
        settle()

        // 1. Home -> the pinned card -> add a note.
        tap("Synthetic card $OLD?")
        scrollTo(GuideMaintenanceStrings.noteAdd)
        tap(GuideMaintenanceStrings.noteAdd)
        composeRule.onNode(hasSetTextAction()).performTextInput("Remember the zebra form")
        tap(GuideMaintenanceStrings.noteSave)
        assertEquals(mapOf(OLD to "Remember the zebra form"), stored())
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteStale).assertDoesNotExist()

        // 2. The card's text changes in an update: the same note now says so.
        composeRule.runOnUiThread { release = 1 }
        settle()
        scrollTo("Remember the zebra form")
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteStale).assertIsDisplayed()

        // 3. The rule is replaced: the note follows to the new card, marked as from an earlier version.
        composeRule.runOnUiThread {
            release = 2
            nav.popToHome()
            nav.push(GuideDestination.Card(NEW))
        }
        settle()
        scrollTo(GuideMaintenanceStrings.noteCarriedHeading)
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedHeading).assertIsDisplayed()
        scrollTo(GuideMaintenanceStrings.noteCarriedStale)
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedStale).assertIsDisplayed()
        scrollTo(GuideMaintenanceStrings.noteEdit)

        // 4. Editing it there moves it: one note on the new card, none left on the old one.
        tap(GuideMaintenanceStrings.noteEdit)
        composeRule.onNode(hasSetTextAction()).performTextInput(" and the new form")
        tap(GuideMaintenanceStrings.noteSave)
        assertEquals(mapOf(NEW to "Remember the zebra form and the new form"), stored())

        // 5. A note on a card the Guide does not hold stays reachable from Home.
        runBlocking { store.save("RB-GONE", "left behind", "rev00001") }
        composeRule.runOnUiThread { nav.popToHome() }
        settle()
        scrollTo(GuideMaintenanceStrings.notesRemovedRow(1))
        tap(GuideMaintenanceStrings.notesRemovedRow(1))
        composeRule.onNodeWithText("RB-GONE").assertIsDisplayed()
        composeRule.onNodeWithText("left behind").assertIsDisplayed()
        tap(GuideMaintenanceStrings.noteDelete)
        tap(GuideMaintenanceStrings.noteDeleteConfirm)
        assertEquals(mapOf(NEW to "Remember the zebra form and the new form"), stored())

        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty(), "the whole walk tells telemetry nothing")
        composeRule.onNodeWithText(GuideStrings.homeTitle).assertDoesNotExist()
    }

    private companion object {
        const val OLD = SyntheticGuideRuleChange.OLD_CARD
        const val NEW = SyntheticGuideRuleChange.NEW_CARD
    }
}
