package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import com.payslipmax.pdfparser.ui.theme.AppStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Regression test for a Compose Multiplatform iOS bug (CMP-3089/2986 family): a plain String-backed OutlinedTextField lets Compose infer
// cursor position by diffing old vs. new text, which races with iOS's async UIKit text-input bridge and scrambles fast typing. The field
// used to work around it by always pinning the TextFieldValue selection to the end of the text. P10 moved the password to a secure
// TextFieldState (docs/Plan/14a P10), which has no value/onValueChange round trip to race, and keeps the same promise another way: an
// input transformation puts the caret at the end after every edit. This test can't reproduce the iOS timing race (Robolectric is
// synchronous), but it locks in the pinning so a future edit can't silently drop it.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupRestorePasswordFieldTest {
    @AfterTest
    fun tearDown() {
        // RobolectricTestRunner instantiates PayslipApplication, whose onCreate starts Koin.
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun supportingTextDisplaysGuidanceAndWarning() =
        runComposeUiTest {
            setContent {
                BackupRestorePasswordField(password = TextFieldState())
            }

            onNodeWithText(AppStrings.settingsBackupPasswordLabel).assertIsDisplayed()
            onNodeWithText(AppStrings.settingsBackupPasswordHint).assertIsDisplayed()

            // In Material3's text fields the placeholder is shown when the field is focused and empty
            val field = onNodeWithTag(BackupRestorePasswordFieldTestTag)
            field.performClick()
            onNodeWithText(AppStrings.settingsBackupPasswordPlaceholder).assertIsDisplayed()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun fieldAcceptsAlphanumericAndSpecialSymbols() =
        runComposeUiTest {
            val password = TextFieldState()
            setContent {
                BackupRestorePasswordField(password = password)
            }

            val field = onNodeWithTag(BackupRestorePasswordFieldTestTag)
            val testInput = "P@ssw0rd!#$ 123"
            field.performTextInput(testInput)

            assertEquals(testInput, password.text.toString())
            val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
            assertEquals(TextRange(testInput.length), selection)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun selectionPinningPreservedWithSupportingText() =
        runComposeUiTest {
            val password = TextFieldState()
            setContent {
                BackupRestorePasswordField(password = password)
            }

            val field = onNodeWithTag(BackupRestorePasswordFieldTestTag)
            field.performTextInput("abcd")
            // Attempt to move the cursor into the middle of the text -- the pinning forces the caret back to the end after every edit
            // (including SetSelection), so this is expected to be overridden and the next keystroke appends at the end.
            field.performTextInputSelection(TextRange(1))
            field.performTextInput("X")

            // The real value is read from the field's state: the node's own semantics are bullets (see BackupSecretFieldTest).
            assertEquals("abcdX", password.text.toString())

            val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
            assertEquals(TextRange(5), selection)
        }
}
