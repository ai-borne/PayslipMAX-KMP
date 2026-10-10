package com.payslipmax.pdfparser.ui.screens.guide

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import com.payslipmax.pdfparser.guide.domain.GUIDE_NOTE_MAX_CHARS
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.GuideMaintenanceStrings
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * M7 on a card screen: the "Your note" section. WHY each check: the section must say plainly that the note is the user's
 * own and not official, a draft must not outlive the dialog, a change in the card since the note was written must be
 * said, a note moved from an earlier rule must not leave a copy behind, and a locked card must show nothing of notes and
 * must not even read them. Telemetry hears nothing. No `qualifiers`: a `w360dp-...` screen makes any dialog text field
 * spin until the test JVM runs out of memory (see GuideSuggestionUiTest), so cards are opened on their own and scrolled to.
 */
class GuideNotesUiTest : GuideNotesUiBase() {
    @Test
    fun aCardWithNoNoteOffersToAddOneAndSaysItIsPrivateAndNotOfficial() {
        openCard()

        scrollTo(GuideMaintenanceStrings.noteAdd)

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteSectionTitle).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteNotOfficial).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteAdd).assertIsDisplayed()
    }

    @Test
    fun addingANoteSavesItAgainstTheCardsRevisionAndShowsItWithEditAndDelete() {
        openCard()
        scrollTo(GuideMaintenanceStrings.noteAdd)
        tap(GuideMaintenanceStrings.noteAdd)

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteEditorNotice).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteSave).assertIsNotEnabled()
        type("Ask the unit about the zebra form")
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCounter(33)).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteSave).assertIsEnabled()
        tap(GuideMaintenanceStrings.noteSave)

        val note = stored().single()
        assertEquals("RB-T5", note.cardId)
        assertEquals(REV, note.cardRev)
        assertEquals("Ask the unit about the zebra form", note.text)
        scrollTo("Ask the unit about the zebra form")
        composeRule.onNodeWithText("Ask the unit about the zebra form").assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteEdit).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteDelete).assertIsDisplayed()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteAdd).assertDoesNotExist()
    }

    @Test
    fun theFieldStopsAtTheLimitAndTheCounterSaysSo() {
        openCard()
        scrollTo(GuideMaintenanceStrings.noteAdd)
        tap(GuideMaintenanceStrings.noteAdd)

        type("w".repeat(GUIDE_NOTE_MAX_CHARS + 100))

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteCounter(GUIDE_NOTE_MAX_CHARS)).assertIsDisplayed()
        tap(GuideMaintenanceStrings.noteSave)
        assertEquals(GUIDE_NOTE_MAX_CHARS, stored().single().text.length)
    }

    @Test
    fun editingStartsFromTheSavedTextAndSavesTheChange() {
        seed("RB-T5", "first words")
        openCard()
        scrollTo(GuideMaintenanceStrings.noteEdit)
        tap(GuideMaintenanceStrings.noteEdit)

        composeRule.onNode(hasSetTextAction() and hasText("first words")).assertIsDisplayed()
        type(" and more")
        tap(GuideMaintenanceStrings.noteSave)

        assertEquals("first words and more", stored().single().text)
    }

    @Test
    fun deletingAsksFirstCancelKeepsTheNoteAndConfirmingRemovesIt() {
        seed("RB-T5", "keep me")
        openCard()
        scrollTo(GuideMaintenanceStrings.noteDelete)
        tap(GuideMaintenanceStrings.noteDelete)

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteDeleteTitle).assertIsDisplayed()
        tap(AppStrings.btnCancel)
        assertEquals(1, stored().size)

        tap(GuideMaintenanceStrings.noteDelete)
        tap(GuideMaintenanceStrings.noteDeleteConfirm)

        assertEquals(emptyList(), stored())
        scrollTo(GuideMaintenanceStrings.noteAdd)
        composeRule.onNodeWithText("keep me").assertDoesNotExist()
    }

    @Test
    fun cancellingDiscardsTheDraftSoReopeningStartsEmpty() {
        openCard()
        scrollTo(GuideMaintenanceStrings.noteAdd)
        tap(GuideMaintenanceStrings.noteAdd)
        type("half typed")

        tap(AppStrings.btnCancel)
        tap(GuideMaintenanceStrings.noteAdd)

        composeRule.onNodeWithText("half typed").assertDoesNotExist()
        composeRule.onNodeWithText(GuideMaintenanceStrings.noteSave).assertIsNotEnabled()
        assertEquals(emptyList(), stored())
    }

    @Test
    fun aNoteWrittenForAnOlderRevisionOfTheCardSaysTheCardWasUpdated() {
        seed("RB-T5", "old revision words", rev = "oldrev99")
        openCard()
        scrollTo("old revision words")

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteStale).assertIsDisplayed()
    }

    @Test
    fun aNoteForTheCurrentRevisionShowsNoUpdatedLine() {
        seed("RB-T5", "current words")
        openCard()
        scrollTo("current words")

        composeRule.onNodeWithText(GuideMaintenanceStrings.noteStale).assertDoesNotExist()
    }
}
