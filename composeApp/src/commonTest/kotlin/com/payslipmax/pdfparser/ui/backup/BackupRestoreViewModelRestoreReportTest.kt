package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.crypto.Secret
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestoredRows
import com.payslipmax.pdfparser.repository.SkippedRows
import com.payslipmax.pdfparser.testing.SyntheticPart2Backups
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

/**
 * WHY: the Settings restore must be able to tell the officer how many rows a restore left out, per kind (plan Phase 11 item 10, 14a P5), so
 * the counts have to reach the view model's state: in the preview, before the officer commits, and in the result. A file with unreadable
 * rows is made by hand; the production restore decides what is skipped. Moved from PayslipViewModelRestoreReportTest (docs/Plan/14a P7).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BackupRestoreViewModelRestoreReportTest {
    private val harness = BackupViewModelHarness()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() {
        harness.clear()
        Dispatchers.resetMain()
    }

    /** The rows the preview says it will leave out, then the rows the committed restore reports. */
    private fun restore(bytes: ByteArray): Pair<SkippedRows, SkippedRows> {
        harness.viewModel.restore(harness.files.stage(bytes), Secret.of(PASSWORD))
        val previewed = assertIs<BackupUiState.Preview>(harness.viewModel.state.value).preview.skipped
        harness.viewModel.commit(RestoreMode.REPLACE)
        val done = assertIs<BackupUiState.Done>(harness.viewModel.state.value).summary
        return previewed to assertIs<BackupSummary.Restored>(done).skipped
    }

    @Test
    fun theStateCarriesTheNumberOfPartTwoRowsTheRestoreLeftOut() =
        runTest {
            val bytes = SyntheticPart2Backups.withUnreadableRows(PASSWORD, unreadableEntries = 2, unreadableOrders = 1)

            val expected = SkippedRows(part2Entries = 2, part2Orders = 1)
            assertEquals(expected to expected, restore(bytes))
        }

    /** 14 U1 / P10 "From P8" item 3: the result says what came back, so a restore that skipped a row is not mistaken for one that wrote all. */
    @Test
    fun theStateCarriesWhatTheRestoreWroteAndNotTheRowsItSkipped() =
        runTest {
            val bytes = SyntheticPart2Backups.withUnreadableRows(PASSWORD, unreadableEntries = 2, unreadableOrders = 1)
            harness.viewModel.restore(harness.files.stage(bytes), Secret.of(PASSWORD))
            harness.viewModel.commit(RestoreMode.REPLACE)

            val done = assertIs<BackupUiState.Done>(harness.viewModel.state.value).summary

            assertEquals(RestoredRows(payslips = 1), assertIs<BackupSummary.Restored>(done).restored, "the one readable payslip came back; the 3 unreadable Bank rows did not")
        }

    @Test
    fun aRestoreWithNothingToSkipReportsZero() =
        runTest {
            assertEquals(SkippedRows.NONE to SkippedRows.NONE, restore(SyntheticPart2Backups.withUnreadableRows(PASSWORD, 0, 0)))
        }

    private companion object {
        const val PASSWORD = "BackupPass#2026"
    }
}
