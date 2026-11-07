package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.RestoredRows
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.backup.BackupSummary
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
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P8, U1): before this sheet an officer saw no progress, could tap twice, never saw a result, and read a raw message when
 * a backup failed. Each test draws one state of the sheet (idle, working, done, failed) and checks what the officer can see and do in it,
 * with the real composables and no view model (the view model's own transitions are tested in `ui/backup`).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupSheetUiTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    // ---- Idle ----------------------------------------------------------------------------------------------------------

    @Test
    fun idleShowsWhenTheLastBackupWasMadeOrThatThereWasNone() =
        runComposeUiTest {
            var state by mutableStateOf<BackupUiState>(BackupUiState.Idle(1_762_128_000_000L))
            setContent { SheetUnderTest(state) }
            onNodeWithTag(BackupLastBackupTag).assertIsDisplayed()
            val shown = onNodeWithTag(BackupLastBackupTag).fetchSemanticsNode().config[SemanticsProperties.Text].joinToString { it.text }
            assertTrue(shown.startsWith(BackupRestoreStrings.lastBackup("")), shown)
            assertTrue(shown.length > BackupRestoreStrings.lastBackup("").length, "a date follows: $shown")

            state = BackupUiState.Idle(null)
            onNodeWithText(BackupRestoreStrings.lastBackupNever).assertIsDisplayed()
        }

    // ---- Working -------------------------------------------------------------------------------------------------------

    @Test
    fun workingShowsASpinnerUntilTheTotalIsKnownThenABarAndTheStageLine() =
        runComposeUiTest {
            var state by mutableStateOf<BackupUiState>(BackupUiState.Working(BackupOp.RESTORE, BackupStage.OPENING, 0, 0))
            setContent { SheetUnderTest(state) }
            onNodeWithTag(BackupProgressSpinnerTag).assertIsDisplayed()
            onNodeWithTag(BackupProgressBarTag).assertDoesNotExist()
            onNodeWithText(BackupRestoreStrings.progressLine(BackupOp.RESTORE, BackupStage.OPENING, 0, 0)).assertIsDisplayed()

            state = BackupUiState.Working(BackupOp.RESTORE, BackupStage.PAGES, 30, 120)
            onNodeWithTag(BackupProgressSpinnerTag).assertDoesNotExist()
            val range = onNodeWithTag(BackupProgressBarTag).fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
            assertEquals(ProgressBarRangeInfo(0.25f, 0f..1f), range)
            onNodeWithText("Restoring pages: 30 of 120").assertIsDisplayed()
        }

    @Test
    fun workingDisablesBothButtonsAndTheFieldsSoNothingStartsTwice() =
        runComposeUiTest {
            val calls = SheetCalls()
            setContent { SheetUnderTest(BackupUiState.Working(BackupOp.BACKUP, BackupStage.READING, 0, 0), calls = calls) }
            onNodeWithTag(BackUpButtonTag).assertIsNotEnabled()
            onNodeWithTag(RestoreButtonTag).assertIsNotEnabled()
            onNodeWithTag(BackupRestorePasswordFieldTestTag).assertIsNotEnabled()
            onNodeWithTag(BackupConfirmFieldTag).assertIsNotEnabled()
        }

    @Test
    fun anIdleSheetHasLiveButtons() =
        runComposeUiTest {
            setContent { SheetUnderTest(BackupUiState.Idle(null)) }
            onNodeWithTag(BackUpButtonTag).assertIsEnabled()
            onNodeWithTag(RestoreButtonTag).assertIsEnabled()
        }

    // ---- Done / Failed -------------------------------------------------------------------------------------------------

    @Test
    fun aFinishedBackupShowsCountsOnly() =
        runComposeUiTest {
            setContent { SheetUnderTest(BackupUiState.Done(BackupSummary.BackedUp(payslips = 48, part2Items = 3))) }
            onNodeWithTag(BackupResultCardTag).assertIsDisplayed()
            onNodeWithText(backupDoneStatus(BackupContents(48, 3)).message).assertIsDisplayed()
            val tree = onRoot().printToString()
            assertFalse("₹" in tree, "counts only, never amounts")
        }

    @Test
    fun aRestoreThatLeftRowsOutSaysSoPerKind() =
        runComposeUiTest {
            var warning = Color.Unspecified
            setContent {
                warning = BackupColors.skipped()
                SheetUnderTest(BackupUiState.Done(BackupSummary.Restored(RestoredRows(payslips = 3), skippedExample)))
            }
            onNodeWithText(AppStrings.statusRestoreComplete).assertIsDisplayed()
            val counts = BackupRestoreStrings.restoredCounts(RestoredRows(payslips = 3))!!
            onNodeWithText(counts).assertIsDisplayed()
            assertNotEquals(warning, onNodeWithText(counts).textColor(), "what came back is not shown as a warning")
            assertEquals(warning, onNodeWithText(BackupRestoreStrings.restoredSkipped(skippedExample)).textColor(), "the loss is in the warning colour")
            assertNotEquals(warning, onNodeWithText(AppStrings.statusRestoreComplete).textColor())
            onNodeWithText(BackupRestoreStrings.restoredSkipped(skippedExample)).assertIsDisplayed()
        }

    @Test
    fun aCleanRestoreSaysCompleteWhatCameBackAndNothingLost() =
        runComposeUiTest {
            setContent { SheetUnderTest(BackupUiState.Done(BackupSummary.Restored(RestoredRows(payslips = 48, part2Orders = 12, part2Pages = 120), SkippedRows.NONE))) }
            onNodeWithText(AppStrings.statusRestoreComplete).assertIsDisplayed()
            onNodeWithText("Restored 48 payslips, 12 orders, 120 pages.").assertIsDisplayed()
            assertEquals(0, onAllNodesWithText(BackupRestoreStrings.restoredSkipped(skippedExample)).fetchSemanticsNodes().size)
        }

    @Test
    fun aFailureShowsItsOneFixedLineAndNothingElseAboutIt() =
        runComposeUiTest {
            setContent { SheetUnderTest(BackupUiState.Failed(BackupFailure.WRONG_PASSWORD)) }
            onNodeWithText(BackupRestoreStrings.failure(BackupFailure.WRONG_PASSWORD)).assertIsDisplayed()
            onNodeWithTag(BackupResultCardTag).assertDoesNotExist()
            onNodeWithTag(BackUpButtonTag).assertIsEnabled()
        }

    // ---- Security ------------------------------------------------------------------------------------------------------

    /**
     * Nothing a screen reader or the display reads carries the typed password: `Text`, `EditableText` and, since P10's secure field, also
     * `InputText`, which Compose fills with the raw value on every text field (docs/Plan/14a P10 "From P8" item 2, closed). The line
     * filter that used to exempt `InputText` is gone, so any node that holds the password fails this test.
     */
    @Test
    fun noPasswordReachesAnyTextTheScreenReaderOrTheDisplayReads() =
        runComposeUiTest {
            setContent { SheetUnderTest(BackupUiState.Idle(null)) }
            onNodeWithTag(BackupRestorePasswordFieldTestTag).performTextInput("Zq7!secretWord")
            onNodeWithTag(BackupConfirmFieldTag).performTextInput("Zq7!secretWord")
            val leaks = onRoot(useUnmergedTree = true).printToString().lines().filter { "secretWord" in it }
            assertTrue(leaks.isEmpty(), "the typed password must never be in any text a screen reader reads: $leaks")
            val masked = onNodeWithTag(BackupRestorePasswordFieldTestTag).fetchSemanticsNode().config
            assertEquals("Zq7!secretWord".length, masked[SemanticsProperties.InputText].text.length, "the mask keeps the length the field had, as the old one did")
        }
}
