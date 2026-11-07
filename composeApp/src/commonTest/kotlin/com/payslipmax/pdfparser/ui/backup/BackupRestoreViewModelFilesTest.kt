package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.ui.backup.BackupViewModelHarness.Companion.PASSWORD
import com.payslipmax.pdfparser.ui.screens.BackupContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a backup holds everything the officer owns, so the view model must leave no copy of it lying about: a file is shared only when
 * the whole export worked, and a picked file is gone after the restore (whether it worked or not) and when the officer says no.
 * Moved from PayslipViewModelBackupFilesTest (docs/Plan/14a P7).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelFilesTest {
    private val harness = BackupViewModelHarness()
    private val files = harness.files
    private val contents = BackupContents(payslips = 0, part2Items = 1)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun aSuccessfulExportSharesTheFileExactlyOnce() =
        runTest {
            harness.viewModel.backUp(Secret.of(PASSWORD), contents)

            assertIs<BackupUiState.Done>(harness.viewModel.state.value)
            assertEquals(1, files.shareCount)
            assertNotNull(files.exportedBytes)
        }

    @Test
    fun anExportThatCannotBeSharedFailsAndLeavesNoFile() =
        runTest {
            files.shareFails = true
            harness.viewModel.backUp(Secret.of(PASSWORD), contents)

            assertIs<BackupUiState.Failed>(harness.viewModel.state.value)
            assertEquals(0, files.shareCount)
            assertNull(files.exportedBytes)
        }

    @Test
    fun aBackupThatDoesNotReadBackIsNeverSharedAndLeavesNoFile() =
        runTest {
            files.corruptWrittenByteAt = 40
            harness.viewModel.backUp(Secret.of(PASSWORD), contents)

            assertIs<BackupUiState.Failed>(harness.viewModel.state.value)
            assertEquals(1, files.verifyCount, "the written file was read back")
            assertEquals(0, files.shareCount, "the share sheet never opened")
            assertNull(files.exportedBytes)
        }

    @Test
    fun aPickedFileIsGoneAfterARestoreThatFailsAndAfterOneThatWorks() =
        runTest {
            harness.viewModel.restore(files.stage(byteArrayOf(1, 2, 3)), Secret.of("AnyPassword"))

            assertIs<BackupUiState.Failed>(harness.viewModel.state.value)
            assertTrue(files.pickedKeys.isEmpty(), "a failed restore still deletes the picked copy")

            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), Secret.of(PASSWORD))
            harness.viewModel.commit(RestoreMode.REPLACE)

            assertIs<BackupUiState.Done>(harness.viewModel.state.value)
            assertTrue(files.pickedKeys.isEmpty(), "a restore that worked deletes the picked copy")
        }

    @Test
    fun sayingNoToReplaceOrMergeDeletesThePickedFile() =
        runTest {
            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), Secret.of(PASSWORD))

            harness.viewModel.cancel()

            assertTrue(files.pickedKeys.isEmpty())
        }

    /** Rewritten in P7 from "a view model without backup wiring keeps no picked file": that view model cannot be built any more, and the
     *  one restore it now refuses is a pick while a preview is open. The intent is the same: a pick that is not restored leaves no file. */
    @Test
    fun aPickTheViewModelRefusesKeepsNoPickedFile() =
        runTest {
            harness.viewModel.restore(harness.pickedBackupOfTheDevice(), Secret.of(PASSWORD))
            val refused = harness.pickedBackupOfTheDevice()

            harness.viewModel.restore(refused, Secret.of(PASSWORD))

            assertTrue(refused !in files.pickedKeys)
        }
}
