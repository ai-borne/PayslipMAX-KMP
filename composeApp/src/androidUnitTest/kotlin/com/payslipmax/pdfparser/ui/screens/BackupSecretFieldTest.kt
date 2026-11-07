@file:OptIn(ExperimentalTestApi::class, ExperimentalFoundationApi::class)

package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10): the password is typed into a secure `TextFieldState`, which keeps the text in a buffer and in its undo history
 * (the spike showed deleting alone leaves it in the history, and `undo` brings it back). So when the form leaves the screen the entries
 * are overwritten, emptied and their history dropped, a call gets its own copy that the officer's retry does not use up, and none of it
 * is ever saved with the screen's state. Compose cannot be made to zero the `String` it makes of each keystroke; that limit is recorded
 * in 14a, not hidden.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSecretFieldTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun TypedPassword.hasNothingLeft() = isEmpty && !field.undoState.canUndo && !field.undoState.canRedo

    @Test
    fun wipingEmptiesTheEntryAndUndoCannotBringTheTextBack() {
        val typed = TypedPassword(TextFieldState("Zq7!secretWord"))
        typed.field.edit { append("X") }
        assertTrue(typed.field.undoState.canUndo, "the setup: Compose keeps what was typed in its history")

        typed.wipe()

        assertTrue(typed.hasNothingLeft(), "an empty entry whose history cannot restore the password")
        typed.field.undoState.undo()
        assertEquals("", typed.field.text.toString())
    }

    @Test
    fun takingACopyLeavesTheEntryTypedSoARetryNeedsNoRetypingAndWipingTheCopyDoesNotTouchIt() {
        val typed = TypedPassword(TextFieldState("retry-me"))

        val first = typed.take()
        first.close()
        val second = typed.take()

        assertEquals("retry-me", second.reveal { it.concatToString() })
        assertEquals("retry-me", typed.field.text.toString(), "the field keeps its text until the form leaves")
        second.close()
    }

    @Test
    fun theEntriesAreWipedWhenAPreviewReplacesTheFormAndStayWipedWhenItComesBack() =
        runComposeUiTest {
            val fields = BackupPasswordFields()
            var state by mutableStateOf<BackupUiState>(BackupUiState.Idle(null))
            setContent { SheetUnderTest(state, fields = fields) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("Zq7!secretWord")
            onNodeWithTag(BackupConfirmFieldTag).performTextInput("Zq7!secretWord")
            assertFalse(fields.password.isEmpty)

            state = BackupUiState.Preview(previewOf())
            waitForIdle()

            assertTrue(fields.password.hasNothingLeft(), "the password entry")
            assertTrue(fields.confirm.hasNothingLeft(), "the confirm entry")
            state = BackupUiState.Idle(null)
            waitForIdle()
            assertTrue(fields.password.hasNothingLeft(), "back to the form, nothing is typed")
        }

    @Test
    fun theEntriesAreWipedWhenTheSheetLeavesTheScreen() =
        runComposeUiTest {
            val fields = BackupPasswordFields()
            var shown by mutableStateOf(true)
            setContent { if (shown) SheetUnderTest(BackupUiState.Idle(null), fields = fields) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("Zq7!secretWord")

            shown = false
            waitForIdle()

            assertTrue(fields.password.hasNothingLeft())
        }

    @Test
    fun aMismatchDisablesBackUpSaysSoAndAMatchEnablesIt() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Idle(null), calls = calls) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("abc")
            onNodeWithTag(BackupConfirmFieldTag).performTextInput("abd")

            onNodeWithTag(BackUpButtonTag).assertIsNotEnabled()
            onNodeWithText(BackupRestoreStrings.passwordMismatch).assertIsDisplayed()
            assertEquals(null, calls.backedUp, "nothing starts on a mismatch")

            onNodeWithTag(BackupConfirmFieldTag).performTextClearance()
            onNodeWithTag(BackupConfirmFieldTag).performTextInput("abc")
            onNodeWithTag(BackUpButtonTag).assertIsEnabled()
            onNodeWithTag(BackUpButtonTag).performClick()
            assertEquals("abc", calls.backedUp)
        }

    @Test
    fun noPasswordCodeSavesItsTextOrPutsItInAString() {
        val files = listOf("BackupPasswordFields.kt", "BackupRestoreComponents.kt", "BackupRestoreSheet.kt", "BackupRestoreSettingsCard.kt")
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile ?: error("repo root not found")
        val code =
            files.associateWith {
                File(dir, "composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/$it")
                    .readLines()
                    .map { line -> line.trim() }
                    .filterNot { line -> line.startsWith("*") || line.startsWith("/*") || line.startsWith("//") }
                    .joinToString("\n")
            }
        assertTrue(code.values.all { it.length > 300 }, "the scan read real files")

        val saved = Regex("""rememberSaveable|rememberTextFieldState|Saver|SavedStateHandle""")
        assertEquals(emptyList(), code.filterValues { saved.containsMatchIn(it) }.keys.toList(), "a password is never held in saved state")
        val asString = Regex("""text\.toString\(\)|concatToString|\bString\(""")
        assertEquals(emptyList(), code.filterValues { asString.containsMatchIn(it) }.keys.toList(), "no password code makes a String of the text")
    }
}
