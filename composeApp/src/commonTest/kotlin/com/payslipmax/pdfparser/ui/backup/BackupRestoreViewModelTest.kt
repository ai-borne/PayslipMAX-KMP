package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.repository.BackupFailure
import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupStage
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness.Companion.PASSWORD
import com.payslipmax.pdfparser.ui.screens.BackupContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P7): the sheet is a picture of one state, so every way through a backup or a restore must land on the state that
 * tells the officer what happened, and a restore must stop at a preview, writing nothing, until the officer chooses. A second tap while
 * one runs must not start another: two backups race for the one export file, and two restores would leave a prepared one unclosed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelTest {
    private val harness = BackupViewModelHarness()
    private val contents = BackupContents(payslips = 1, part2Items = 0)

    private fun unconfined() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun idleShowsTheLastBackupTimeAndASuccessfulBackupMovesIt() =
        runTest {
            unconfined()
            harness.lastBackup.save(1_700_000_000_000L)
            assertEquals(BackupUiState.Idle(1_700_000_000_000L), harness.viewModel.state.value)

            harness.seedPayslip("08/2024")
            harness.viewModel.backUp(PASSWORD, contents)
            harness.viewModel.dismiss()

            assertEquals(BackupUiState.Idle(harness.clock), harness.viewModel.state.value)
        }

    @Test
    fun aBackupGoesFromIdleThroughWorkingToDone() =
        runTest {
            unconfined()
            harness.seedPayslip("08/2024")
            harness.record(this)

            harness.viewModel.backUp(PASSWORD, contents)

            assertIs<BackupUiState.Idle>(harness.states.first())
            assertEquals(BackupUiState.Working(BackupOp.BACKUP, BackupStage.OPENING, 0, 0), harness.states.getOrNull(1))
            assertEquals(BackupUiState.Done(BackupSummary.BackedUp(payslips = 1, part2Items = 0)), harness.states.last())
            assertEquals(1, harness.files.shareCount)
        }

    @Test
    fun aBackupThatFailsGoesFromWorkingToFailedWithItsCode() =
        runTest {
            unconfined()
            harness.files.shareFails = true
            harness.record(this)

            harness.viewModel.backUp(PASSWORD, contents)

            assertIs<BackupUiState.Working>(harness.states.getOrNull(1))
            assertEquals(BackupUiState.Failed(BackupFailure.WRITE_FAILED), harness.states.last())
        }

    @Test
    fun aRestoreStopsAtAPreviewThatWritesNothingThenCommitsWhatTheOfficerChose() =
        runTest {
            unconfined()
            harness.seedPayslip("08/2024")
            val picked = harness.pickedBackupOfTheDevice()
            harness.repository.clearAll()
            harness.seedPayslip("01/2025")
            harness.record(this)

            harness.viewModel.restore(picked, PASSWORD)

            val preview = assertIs<BackupUiState.Preview>(harness.viewModel.state.value).preview
            assertEquals(1, preview.payslips)
            assertEquals(listOf("01/2025"), harness.payslipDates(), "a preview writes nothing")
            assertEquals(BackupUiState.Working(BackupOp.RESTORE, BackupStage.OPENING, 0, 0), harness.states.getOrNull(1))

            harness.viewModel.commit(RestoreMode.MERGE)

            assertEquals(BackupUiState.Done(BackupSummary.Restored(SkippedRows.NONE)), harness.viewModel.state.value)
            assertEquals(setOf("08/2024", "01/2025"), harness.payslipDates().toSet())
            val afterPreview = harness.states.dropWhile { it !is BackupUiState.Preview }.drop(1)
            assertIs<BackupUiState.Working>(afterPreview.firstOrNull(), "the commit shows its own progress")
        }

    @Test
    fun aPreviewThatIsCancelledGoesBackToIdleAndWritesNothing() =
        runTest {
            unconfined()
            harness.seedPayslip("08/2024")
            val picked = harness.pickedBackupOfTheDevice()
            harness.repository.clearAll()

            harness.viewModel.restore(picked, PASSWORD)
            harness.viewModel.cancel()

            assertEquals(BackupUiState.Idle(null), harness.viewModel.state.value)
            assertTrue(harness.payslipDates().isEmpty())
        }

    @Test
    fun aRestoreThatCannotOpenGoesToFailedWithoutAPreview() =
        runTest {
            unconfined()
            harness.record(this)

            harness.viewModel.restore(harness.files.stage(ByteArray(300) { 7 }), PASSWORD)

            assertTrue(harness.states.none { it is BackupUiState.Preview })
            assertEquals(BackupUiState.Failed(BackupFailure.WRONG_PASSWORD), harness.viewModel.state.value)
        }

    /** A cancel lets the waiting restore finish later; when it does, it must not overwrite the backup the officer started meanwhile. */
    @Test
    fun aCancelledPreviewNeverOverwritesTheOperationThatFollowsIt() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            harness.seedPayslip("08/2024")
            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), PASSWORD)
            advanceUntilIdle()
            harness.record(this)

            harness.viewModel.cancel()
            harness.viewModel.backUp(PASSWORD, contents)
            advanceUntilIdle()

            val afterBackUp = harness.states.dropWhile { it !is BackupUiState.Working }
            assertTrue(afterBackUp.none { it is BackupUiState.Idle }, "the backup's states were overwritten: $afterBackUp")
            assertEquals(BackupUiState.Done(BackupSummary.BackedUp(payslips = 1, part2Items = 0)), harness.viewModel.state.value)
        }

    @Test
    fun aSecondBackUpTapWhileOneIsWorkingIsIgnored() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            harness.seedPayslip("08/2024")

            harness.viewModel.backUp(PASSWORD, contents)
            harness.viewModel.backUp(PASSWORD, contents)
            advanceUntilIdle()

            assertEquals(1, harness.files.verifyCount, "one backup was written")
            assertEquals(1, harness.files.shareCount)
        }

    @Test
    fun aSecondRestoreWhileOneIsWorkingOrPreviewedIsIgnoredAndItsPickedFileDeleted() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            harness.seedPayslip("08/2024")
            val first = harness.pickedBackupOfTheDevice()
            val second = harness.pickedBackupOfTheDevice()
            val third = harness.pickedBackupOfTheDevice()

            harness.viewModel.restore(first, PASSWORD)
            harness.viewModel.restore(second, PASSWORD)
            advanceUntilIdle()
            assertIs<BackupUiState.Preview>(harness.viewModel.state.value)
            harness.viewModel.restore(third, PASSWORD)
            harness.viewModel.backUp(PASSWORD, contents)
            advanceUntilIdle()

            assertEquals(1, harness.files.readCount, "one restore was opened")
            assertEquals(0, harness.files.verifyCount, "no backup started over a preview")
            assertTrue(harness.files.pickedKeys.isEmpty(), "the ignored picks left no file")
            assertIs<BackupUiState.Preview>(harness.viewModel.state.value)
        }
}
