package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.repository.RestoreReport
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.SyntheticPart2Backups
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

/**
 * WHY: the Settings restore must be able to tell the officer how many Part II rows a restore left out (plan Phase 11 item 10), so the
 * count has to reach the view model's callback. A file with unreadable rows is made by hand; the production restore decides what is skipped.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelRestoreReportTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        val dao = FakePayslipDao()
        viewModel =
            PayslipViewModel(
                PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined),
                backupService = PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2)),
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun restore(bytes: ByteArray): Result<RestoreReport> {
        var result: Result<RestoreReport>? = null
        viewModel.importBackup(bytes, PASSWORD, RestoreMode.REPLACE) { result = it }
        return assertNotNull(result)
    }

    @Test
    fun theCallbackGetsTheNumberOfPartTwoRowsTheRestoreLeftOut() =
        runTest {
            runCurrent()
            val bytes = SyntheticPart2Backups.withUnreadableRows(PASSWORD, unreadableDrafts = 2, unreadableOrders = 1)

            assertEquals(3, restore(bytes).getOrThrow().skipped)
        }

    @Test
    fun aRestoreWithNothingToSkipReportsZero() =
        runTest {
            runCurrent()

            assertEquals(0, restore(SyntheticPart2Backups.withUnreadableRows(PASSWORD, 0, 0)).getOrThrow().skipped)
        }

    private companion object {
        const val PASSWORD = "BackupPass#2026"
    }
}
