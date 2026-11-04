package com.payslipmax.pdfparser.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.backup.BackupUiState
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.BackupColors
import com.payslipmax.pdfparser.ui.theme.BackupRestoreStrings
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P8, D10, D11, invariant 4): nothing is skipped silently, so before a restore writes anything the officer sees what the
 * backup holds and what it could not read, on an empty device too. Replace with unreadable rows deletes what the device holds and restores
 * less, so it asks a second time and names the loss. Back Up asks for the password twice, since a typo makes a backup nobody can open.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupPreviewUiTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    // ---- Back Up: confirm password -------------------------------------------------------------------------------------

    @Test
    fun aConfirmThatDiffersDisablesBackUpAndSaysWhy() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Idle(null), calls = calls) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("pass 1!")
            onNodeWithTag(BackupConfirmFieldTag).performTextInput("pass 2!")
            onNodeWithTag(BackUpButtonTag).assertIsNotEnabled()
            onNodeWithText(BackupRestoreStrings.passwordMismatch).assertIsDisplayed()
        }

    @Test
    fun aMatchingConfirmEnablesBackUpAndHandsOnThePasswordOnce() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Idle(null), calls = calls) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("pass 1!")
            onNodeWithTag(BackupConfirmFieldTag).performTextInput("pass 1!")
            onNodeWithTag(BackUpButtonTag).assertIsEnabled().performClick()
            assertEquals("pass 1!", calls.backedUp)
            assertEquals(null, calls.restored)
        }

    @Test
    fun restoreDoesNotNeedTheConfirmField() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Idle(null), calls = calls) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("pass 1!")
            onNodeWithTag(RestoreButtonTag).assertIsEnabled().performClick()
            assertEquals("pass 1!", calls.restored)
        }

    // ---- Preview -------------------------------------------------------------------------------------------------------

    @Test
    fun thePreviewShowsDateFormatAndCountsAndSaysHoldsNeverAdds() =
        runComposeUiTest {
            setContent { SheetUnderTest(BackupUiState.Preview(previewOf())) }
            onNodeWithTag(RestorePreviewCardTag).assertIsDisplayed()
            onNodeWithText(BackupRestoreStrings.previewHolds(previewOf())).assertIsDisplayed()
            onNodeWithText(BackupRestoreStrings.previewFormat(6)).assertIsDisplayed()
            onNodeWithTag(RestorePreviewDateTag).assertIsDisplayed()
            assertTrue(BackupRestoreStrings.previewHolds(previewOf()).startsWith("This backup holds"))
            onNodeWithTag(RestorePreviewSkippedTag).assertDoesNotExist()
        }

    @Test
    fun anOlderFileWithNoDateShowsNoDateLine() =
        runComposeUiTest {
            setContent { SheetUnderTest(BackupUiState.Preview(previewOf(exportedAt = null))) }
            onNodeWithTag(RestorePreviewCardTag).assertIsDisplayed()
            onNodeWithTag(RestorePreviewDateTag).assertDoesNotExist()
        }

    @Test
    fun skippedRowsAreNamedInTheWarningColourInDarkAndLight() {
        val seen = mutableMapOf<String, Pair<Color, Color>>()
        for ((name, scheme) in listOf("light" to lightColorScheme(), "dark" to darkColorScheme())) {
            runComposeUiTest {
                var expected = Color.Unspecified
                setContent {
                    MaterialTheme(colorScheme = scheme) {
                        expected = BackupColors.skipped()
                        SheetUnderTest(BackupUiState.Preview(previewOf(skippedExample)))
                    }
                }
                val node = onNodeWithTag(RestorePreviewSkippedTag)
                node.assertIsDisplayed()
                assertTrue(BackupRestoreStrings.skippedList(skippedExample) in node.fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text })
                val actual = node.textColor()
                assertEquals(expected, actual, "$name: skipped rows use the Theme.kt warning token")
                assertNotEquals(scheme.onSurface, actual, "$name: not the body colour")
                seen[name] = expected to actual
            }
        }
        assertNotEquals(seen.getValue("light").first, seen.getValue("dark").first, "each theme has its own readable value")
    }

    @Test
    fun theRestoreChoiceShowsOnAnEmptyDeviceToo() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Preview(previewOf()), contents = BackupContents(0, 0), calls = calls) }
            onNodeWithTag(RestorePreviewCardTag).assertIsDisplayed()
            onNodeWithTag(RestoreConfirmButtonTag).assertIsDisplayed().performClick()
            assertEquals(listOf(RestoreMode.REPLACE), calls.commits, "an empty device has nothing to merge with: Replace")
            onNodeWithText(AppStrings.settingsRestoreMergeBtn).assertDoesNotExist()
        }

    @Test
    fun aDeviceWithDataAsksReplaceOrMergeAndCancelDiscards() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Preview(previewOf()), calls = calls) }
            onNodeWithText(AppStrings.settingsRestoreMergeBtn).performClick()
            onNodeWithText(AppStrings.settingsRestoreReplaceBtn).performClick()
            onNodeWithTag(RestoreCancelButtonTag).performClick()
            assertEquals(listOf(RestoreMode.MERGE, RestoreMode.REPLACE), calls.commits, "a clean Replace asks nothing more")
            assertEquals(1, calls.cancels)
        }

    // ---- D11 -----------------------------------------------------------------------------------------------------------

    @Test
    fun replaceWithSkippedRowsAsksAgainAndNamesWhatWillBeLost() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Preview(previewOf(skippedExample)), calls = calls) }
            onNodeWithText(AppStrings.settingsRestoreReplaceBtn).performClick()
            assertTrue(calls.commits.isEmpty(), "nothing is written on the first tap")
            onNodeWithText(BackupRestoreStrings.replaceSkipsMessage(skippedExample)).assertIsDisplayed()
            onNodeWithText(BackupRestoreStrings.goBackBtn).performClick()
            assertTrue(calls.commits.isEmpty(), "Go back writes nothing")
            onNodeWithText(AppStrings.settingsRestoreReplaceBtn).performClick()
            onNodeWithText(BackupRestoreStrings.replaceAnywayBtn).performClick()
            assertEquals(listOf(RestoreMode.REPLACE), calls.commits)
        }

    @Test
    fun mergeWithSkippedRowsDoesNotAskAgain() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Preview(previewOf(skippedExample)), calls = calls) }
            onNodeWithText(AppStrings.settingsRestoreMergeBtn).performClick()
            assertEquals(listOf(RestoreMode.MERGE), calls.commits)
            onNodeWithText(BackupRestoreStrings.replaceAnywayBtn).assertDoesNotExist()
        }

    /**
     * WHY (found in the P8 Pixel walk): after the activity was recreated in dark mode the sheet sat lower than its content, and Cancel fell
     * below the screen with no way to reach it. A short window, a large font or a small phone does the same, so the body scrolls: every
     * button stays reachable however little room the sheet has.
     */
    @Test
    fun aSheetWithLittleRoomStillReachesCancelByScrolling() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { Box(Modifier.height(SHORT_WINDOW)) { SheetUnderTest(BackupUiState.Preview(previewOf(skippedExample)), calls = calls) } }
            onNodeWithTag(RestoreCancelButtonTag).performScrollTo().assertIsDisplayed().performClick()
            assertEquals(1, calls.cancels)
        }

    private companion object {
        val SHORT_WINDOW = 240.dp
    }
}
