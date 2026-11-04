package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.repository.PreparedRestore
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness.Companion.PASSWORD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P7, P10 "From P4" item 4): while its preview is on screen a prepared restore holds the archive's key and the picked
 * file, a copy of everything the officer owns. Every way out of the preview must wipe the key and delete the file: Cancel, the sheet
 * dismissed, the view model cleared (its owner gone), a commit that fails and one that works. One missed exit keeps both for the process.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelExitsTest {
    private val harness = BackupViewModelHarness()

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    /**
     * Opens a whole backup of a one-payslip device (with its settings row) to its preview, and returns the prepared restore it holds. The
     * main dispatcher only runs when the test says so, so an exit that leaves its clean-up to a later dispatch is caught.
     */
    private suspend fun TestScope.previewed(): PreparedRestore {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        harness.dao.insertSettings(AppSettingsEntity())
        harness.seedPayslip("08/2024")
        harness.viewModel.restore(harness.pickedBackupOfTheDevice(), Secret.of(PASSWORD))
        advanceUntilIdle()
        assertIs<BackupUiState.Preview>(harness.viewModel.state.value)
        return assertNotNull(harness.viewModel.heldRestore, "the preview holds its prepared restore").also { assertFalse(it.isClosed) }
    }

    /** At once: the key is wiped and the file deleted. Then the waiting restore unwinds: its file is no longer open. */
    private fun TestScope.assertReleased(held: PreparedRestore) {
        assertTrue(held.isClosed, "the archive key is wiped")
        assertNull(harness.viewModel.heldRestore, "nothing is held after the preview")
        assertTrue(harness.files.pickedKeys.isEmpty(), "the picked file is deleted")
        advanceUntilIdle()
        assertEquals(0, harness.files.openReads, "the restore finished and closed its file")
    }

    @Test
    fun cancelClosesThePreparedRestoreAndDeletesThePickedFile() =
        runTest {
            val held = previewed()

            harness.viewModel.cancel()

            assertReleased(held)
            assertEquals(BackupUiState.Idle(null), harness.viewModel.state.value)
        }

    @Test
    fun dismissingTheSheetClosesThePreparedRestoreAndDeletesThePickedFile() =
        runTest {
            val held = previewed()

            harness.viewModel.dismiss()

            assertReleased(held)
            assertEquals(BackupUiState.Idle(null), harness.viewModel.state.value)
        }

    /** The framework clears the view model on the main thread and its scope is cancelled: the release cannot wait for a later dispatch,
     *  and a restore cut short by it is not a failure, so nothing is reported. */
    @Test
    fun clearingTheViewModelClosesThePreparedRestoreAndDeletesThePickedFileAtOnce() =
        runTest {
            val held = previewed()

            harness.clear()

            assertTrue(held.isClosed, "the archive key is wiped when the view model is cleared")
            assertTrue(harness.files.pickedKeys.isEmpty(), "the picked file is deleted when the view model is cleared")
            advanceUntilIdle()
            assertEquals(0, harness.files.openReads)
            assertTrue(harness.reporter.exceptions.isEmpty(), "a restore its owner cut short sends nothing")
        }

    @Test
    fun aCommitThatWorksClosesThePreparedRestoreAndDeletesThePickedFile() =
        runTest {
            val held = previewed()

            harness.viewModel.commit(RestoreMode.REPLACE)
            advanceUntilIdle()

            assertIs<BackupUiState.Done>(harness.viewModel.state.value)
            assertReleased(held)
        }

    @Test
    fun aCommitThatFailsClosesThePreparedRestoreAndDeletesThePickedFile() =
        runTest {
            val held = previewed()
            harness.dao.failInsertSettingsWhen = { true }

            harness.viewModel.commit(RestoreMode.REPLACE)
            advanceUntilIdle()

            assertIs<BackupUiState.Failed>(harness.viewModel.state.value)
            assertReleased(held)
        }
}
