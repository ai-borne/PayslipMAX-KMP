package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

// Regression test for a Compose Multiplatform iOS bug (CMP-3089/2986 family): a plain
// String-backed OutlinedTextField lets Compose infer cursor position by diffing old vs.
// new text, which races with iOS's async UIKit text-input bridge and scrambles fast typing.
// BackupRestorePasswordField works around it by always pinning the TextFieldValue selection
// to the end of the text. This test can't reproduce the iOS timing race (Robolectric is
// synchronous), but it locks in the pinning behavior so a future edit can't silently drop it.
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
    fun selectionStaysPinnedToEndAfterMidStringEdit() =
        runComposeUiTest {
            var password = ""
            setContent {
                BackupRestorePasswordField(
                    password = password,
                    onPasswordChange = { password = it },
                )
            }

            val field = onNodeWithTag(BackupRestorePasswordFieldTestTag)
            field.performTextInput("abcd")
            // Attempt to move the cursor into the middle of the text -- the pinning fix forces
            // selection back to the end on every onValueChange (including SetSelection), so this
            // is expected to be overridden and the next keystroke appends at the end.
            field.performTextInputSelection(TextRange(1))
            field.performTextInput("X")

            // The field masks its displayed/reported text (a11y "password" semantics), so the
            // real value is read back from the onPasswordChange callback, not node semantics.
            assertEquals("abcdX", password)

            val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
            assertEquals(TextRange(password.length), selection)
        }
}
