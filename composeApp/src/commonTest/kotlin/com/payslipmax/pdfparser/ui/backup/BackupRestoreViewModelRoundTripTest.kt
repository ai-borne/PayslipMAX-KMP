package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.RestoreMode
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
import kotlin.test.assertTrue

/**
 * WHY: a backup is only worth making if what the view model writes comes back through the view model: REPLACE leaves exactly the backup,
 * MERGE keeps the device's own months, a wrong password or a damaged file changes nothing, and a password in any script works both ways.
 * Moved from PayslipViewModelBackupTest (docs/Plan/14a P7): the same round trips, through Back Up, the preview and its commit.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelRoundTripTest {
    private val harness = BackupViewModelHarness()
    private val viewModel get() = harness.viewModel

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    /** Backs up through the view model and stages the file it shared, as the officer would pick it on the same phone. */
    private fun backUpAndPick(password: String): String {
        viewModel.backUp(Secret.of(password), BackupContents(payslips = 1, part2Items = 0))
        assertIs<BackupUiState.Done>(viewModel.state.value)
        return harness.files.stage(assertNotNull(harness.files.exportedBytes))
    }

    private fun restore(
        picked: String,
        password: String,
        mode: RestoreMode,
    ): BackupUiState {
        viewModel.restore(picked, Secret.of(password))
        if (viewModel.state.value is BackupUiState.Preview) viewModel.commit(mode)
        return viewModel.state.value
    }

    @Test
    fun aBackupRestoredWithReplaceLeavesExactlyTheBackup() =
        runTest {
            harness.seedPayslip("08/2024")
            val picked = backUpAndPick("BackupPass#2026")
            assertTrue(assertNotNull(harness.files.exportedBytes).size > 44, "Backup bytes must exceed header overhead")
            harness.repository.clearAll()
            harness.seedPayslip("01/2025")

            assertIs<BackupUiState.Done>(restore(picked, "BackupPass#2026", RestoreMode.REPLACE))

            assertEquals(listOf("08/2024"), harness.payslipDates())
        }

    @Test
    fun anEmptyDeviceBacksUpToAFileThatRestores() =
        runTest {
            val picked = backUpAndPick("EmptyPass123!")

            assertIs<BackupUiState.Done>(restore(picked, "EmptyPass123!", RestoreMode.REPLACE))
            assertTrue(harness.payslipDates().isEmpty())
        }

    @Test
    fun mergeKeepsTheDevicesOwnMonths() =
        runTest {
            harness.seedPayslip("08/2024")
            val picked = backUpAndPick("MergePass123")
            harness.repository.clearAll()
            harness.seedPayslip("12/2024")

            assertIs<BackupUiState.Done>(restore(picked, "MergePass123", RestoreMode.MERGE))

            assertEquals(setOf("12/2024", "08/2024"), harness.payslipDates().toSet())
        }

    @Test
    fun aWrongPasswordFailsAndChangesNothing() =
        runTest {
            harness.seedPayslip("08/2024")
            val picked = backUpAndPick("CorrectPass#1")

            assertIs<BackupUiState.Failed>(restore(picked, "WrongPass#2", RestoreMode.REPLACE))

            assertEquals(listOf("08/2024"), harness.payslipDates())
        }

    @Test
    fun aCorruptedFileFailsGracefully() =
        runTest {
            assertIs<BackupUiState.Failed>(restore(harness.files.stage(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)), "AnyPassword", RestoreMode.REPLACE))
        }

    @Test
    fun aMultiByteUtf8PasswordWorksBothWays() =
        runTest {
            val unicodePassword = "p@sswörd🔑🇮🇳"
            harness.seedPayslip("06/2024")
            val picked = backUpAndPick(unicodePassword)
            harness.repository.clearAll()

            assertIs<BackupUiState.Done>(restore(picked, unicodePassword, RestoreMode.REPLACE))

            assertEquals(listOf("06/2024"), harness.payslipDates())
        }
}
