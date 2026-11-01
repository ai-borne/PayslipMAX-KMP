package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.testing.FakeBackupFiles
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a backup holds everything the officer owns, so the view model must leave no copy of it lying about: a file is shared only when
 * the whole export worked, a picked file is gone after the restore (whether it worked or not) and when the officer says no, and a view
 * model without backup wiring fails instead of keeping a picked file.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelBackupFilesTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val files = FakeBackupFiles()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val dao = FakePayslipDao()
        viewModel =
            PayslipViewModel(
                PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined),
                backupService = PayslipBackupService(dao, Dispatchers.Unconfined),
                backupFiles = files,
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun aSuccessfulExportSharesTheFileExactlyOnce() =
        runTest {
            var result: Result<Unit>? = null
            viewModel.exportBackup("Pass#12345") { result = it }
            runCurrent()

            assertTrue(assertNotNull(result).isSuccess)
            assertEquals(1, files.shareCount)
            assertNotNull(files.exportedBytes)
        }

    @Test
    fun anExportThatCannotBeSharedFailsAndLeavesNoFile() =
        runTest {
            files.shareFails = true
            var result: Result<Unit>? = null
            viewModel.exportBackup("Pass#12345") { result = it }
            runCurrent()

            assertTrue(assertNotNull(result).isFailure)
            assertEquals(0, files.shareCount)
            assertNull(files.exportedBytes)
        }

    @Test
    fun aPickedFileIsGoneAfterARestoreThatFailsAndAfterOneThatWorks() =
        runTest {
            var result: Result<RestoreReport>? = null
            viewModel.importBackup(files.stage(byteArrayOf(1, 2, 3)), "AnyPassword", RestoreMode.REPLACE) { result = it }
            runCurrent()

            assertTrue(assertNotNull(result).isFailure)
            assertTrue(files.pickedKeys.isEmpty(), "a failed restore still deletes the picked copy")

            viewModel.exportBackup("Pass#12345") {}
            runCurrent()
            viewModel.importBackup(files.stage(assertNotNull(files.exportedBytes)), "Pass#12345", RestoreMode.REPLACE) { result = it }
            runCurrent()

            assertTrue(assertNotNull(result).isSuccess)
            assertTrue(files.pickedKeys.isEmpty(), "a restore that worked deletes the picked copy")
        }

    @Test
    fun sayingNoToReplaceOrMergeDeletesThePickedFile() {
        val picked = files.stage(byteArrayOf(1))

        viewModel.discardPickedBackup(picked)

        assertTrue(files.pickedKeys.isEmpty())
    }

    @Test
    fun aViewModelWithoutBackupWiringFailsAndKeepsNoPickedFile() =
        runTest {
            val bare = PayslipViewModel(PayslipRepository(FakePayslipDao(), FakePdfParser(), Dispatchers.Unconfined), backupFiles = files)
            var result: Result<RestoreReport>? = null

            bare.importBackup(files.stage(byteArrayOf(1)), "x", RestoreMode.REPLACE) { result = it }
            runCurrent()

            assertTrue(assertNotNull(result).isFailure)
            assertTrue(files.pickedKeys.isEmpty())
        }
}
