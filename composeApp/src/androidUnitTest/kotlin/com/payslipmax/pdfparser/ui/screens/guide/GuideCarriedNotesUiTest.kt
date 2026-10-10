package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.data.GuideNoteSaveResult
import com.payslipmax.pdfparser.guide.data.GuideNotesRepository
import com.payslipmax.pdfparser.guide.data.StoredGuideNotes
import com.payslipmax.pdfparser.testing.SyntheticGuideRuleChange
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * M7: notes carried from an earlier rule, a store that fails, the locked card and telemetry, on a card screen. WHY: a carried
 * note must move (never copy, never overwrite the card's own), a failed write must keep the user's draft and say so, and a locked
 * card must show nothing of notes and not even read them.
 */
class GuideCarriedNotesUiTest : GuideNotesUiBase() {
    @Test
    fun aNoteFromAnEarlierRuleShowsBelowUnderItsHeadingMarkedStaleAndEditingItMovesIt() {
        seed(old, "old rule words", rev = "oldrev01")
        openCard(new, guide = guide(SyntheticGuideRuleChange.bundle()))
        scrollTo(GuideMaintenanceStrings.noteCarriedHeading)
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedHeading).assertIsDisplayed()
        scrollTo(GuideMaintenanceStrings.noteCarriedStale)
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedStale).assertIsDisplayed()
        scrollTo(GuideMaintenanceStrings.noteEdit)
        tap(GuideMaintenanceStrings.noteEdit)
        type(" plus new")
        tap(GuideMaintenanceStrings.noteSave)

        assertEquals(listOf(new to "old rule words plus new"), stored().map { it.cardId to it.text }, "moved, not copied")
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCarriedHeading).assertDoesNotExist()
    }

    @Test
    fun aCarriedNoteCannotBeEditedWhileTheCardHasItsOwnNoteSoTheOwnIsNeverOverwritten() {
        seed(new, "own words", rev = "")
        seed(old, "old rule words", rev = "oldrev01")
        openCard(new, guide = guide(SyntheticGuideRuleChange.bundle()))
        scrollTo("old rule words")

        composeRule.onAllNodesWithText(GuideMaintenanceStrings.noteEdit).assertCountEquals(1)
        composeRule.onAllNodesWithText(GuideMaintenanceStrings.noteDelete).assertCountEquals(2)
    }

    @Test
    fun ifTheEarlierCopyCannotBeRemovedTheNewNoteIsKeptAndTheUserIsToldToDeleteTheOldOne() {
        seed(old, "old rule words", rev = "oldrev01")
        val failingDelete =
            object : GuideNotesRepository by store {
                override suspend fun delete(cardId: String): Unit = throw IllegalStateException("disk")
            }
        openCard(new, guide = guide(SyntheticGuideRuleChange.bundle()), repository = failingDelete)
        scrollTo(GuideMaintenanceStrings.noteEdit)
        tap(GuideMaintenanceStrings.noteEdit)
        tap(GuideMaintenanceStrings.noteSave)

        scrollTo(GuideMaintenanceStrings.noteMoveIncomplete)
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteMoveIncomplete).assertIsDisplayed()
        assertEquals(setOf(new, old), stored().map { it.cardId }.toSet())
    }

    @Test
    fun aStoreThatFailsToSaveKeepsTheDraftOpenWithARetryLine() {
        val broken =
            object : GuideNotesRepository by store {
                override suspend fun save(
                    cardId: String,
                    text: String,
                    cardRev: String,
                ): GuideNoteSaveResult = throw IllegalStateException("private words in a message")
            }
        openCard(repository = broken)
        scrollTo(GuideMaintenanceStrings.noteAdd)
        tap(GuideMaintenanceStrings.noteAdd)
        type("my draft")
        tap(GuideMaintenanceStrings.noteSave)

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteFailed).assertIsDisplayed()
        composeRule.onNode(hasSetTextAction() and hasText("my draft")).assertIsDisplayed()
        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty(), "a failed save tells telemetry nothing")
    }

    @Test
    fun aLockedCardShowsNoNoteSectionNoEditorAndNeverReadsTheNotes() {
        seed("RB-T5", "secret zebra words")
        var collectors = 0
        val counting =
            object : GuideNotesRepository by store {
                override fun observe(): Flow<StoredGuideNotes> = store.observe().onStart { collectors++ }
            }

        openCard(unlocked = false, repository = counting)

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteSectionTitle).assertDoesNotExist()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteAdd).assertDoesNotExist()
        composeRule.onNodeWithText("secret zebra words").assertDoesNotExist()
        assertEquals(0, collectors, "the locked card screen never started reading the notes")
    }

    @Test
    fun addingAndEditingNotesTellsTelemetryNothing() {
        openCard()
        scrollTo(GuideMaintenanceStrings.noteAdd)
        tap(GuideMaintenanceStrings.noteAdd)
        type("words")
        tap(GuideMaintenanceStrings.noteSave)

        assertTrue(crash.logs.isEmpty() && crash.keys.isEmpty() && crash.exceptions.isEmpty())
    }
}
