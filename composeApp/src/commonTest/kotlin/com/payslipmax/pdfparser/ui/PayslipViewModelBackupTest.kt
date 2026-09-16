package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.DsopFund
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.repository.RestoreMode
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelBackupTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testExportBackupSuccessAndRoundTripWithReplaceMode() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1, 2, 3), "test-pw", "08-2024.pdf")
            runCurrent()

            var exportResult: Result<ByteArray>? = null
            viewModel.exportBackup("BackupPass#2026") { exportResult = it }
            runCurrent()

            assertNotNull(exportResult)
            assertTrue(exportResult.isSuccess)
            val backupBytes = exportResult.getOrThrow()
            assertTrue(backupBytes.size > 44, "Backup bytes must be non-empty and exceed header overhead")

            repository.clearAll()
            fakeParser.result = Result.success(createMockPayslip("01/2025"))
            repository.importPayslip(byteArrayOf(4, 5, 6), "test-pw", "01-2025.pdf")
            runCurrent()
            assertEquals(1, viewModel.uiState.value.payslips.size)
            assertEquals("01/2025", viewModel.uiState.value.payslips.first().dateStr)

            var importResult: Result<Unit>? = null
            viewModel.importBackup(backupBytes, "BackupPass#2026", RestoreMode.REPLACE) { importResult = it }
            runCurrent()

            assertNotNull(importResult)
            assertTrue(importResult.isSuccess)
            val restoredList = viewModel.uiState.value.payslips
            assertEquals(1, restoredList.size)
            assertEquals("08/2024", restoredList.first().dateStr)
        }

    @Test
    fun testExportBackupEmptyRepositoryProducesValidArchive() =
        runTest {
            var exportResult: Result<ByteArray>? = null
            viewModel.exportBackup("EmptyPass123!") { exportResult = it }
            runCurrent()

            assertNotNull(exportResult)
            assertTrue(exportResult.isSuccess)
            val backupBytes = exportResult.getOrThrow()
            assertTrue(backupBytes.isNotEmpty())

            var importResult: Result<Unit>? = null
            viewModel.importBackup(backupBytes, "EmptyPass123!", RestoreMode.REPLACE) { importResult = it }
            runCurrent()

            assertNotNull(importResult)
            assertTrue(importResult.isSuccess)
            assertTrue(viewModel.uiState.value.payslips.isEmpty())
        }

    @Test
    fun testImportBackupWithMergeMode() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1), "pw", "a.pdf")
            runCurrent()

            var exportResult: Result<ByteArray>? = null
            viewModel.exportBackup("MergePass123") { exportResult = it }
            runCurrent()
            val backupBytes = exportResult!!.getOrThrow()

            repository.clearAll()
            fakeParser.result = Result.success(createMockPayslip("12/2024"))
            repository.importPayslip(byteArrayOf(2), "pw", "b.pdf")
            runCurrent()

            var importResult: Result<Unit>? = null
            viewModel.importBackup(backupBytes, "MergePass123", RestoreMode.MERGE) { importResult = it }
            runCurrent()

            assertNotNull(importResult)
            assertTrue(importResult.isSuccess)
            val dates = viewModel.uiState.value.payslips.map { it.dateStr }.toSet()
            assertEquals(setOf("12/2024", "08/2024"), dates)
        }

    @Test
    fun testImportBackupFailureWithWrongPassword() =
        runTest {
            fakeParser.result = Result.success(createMockPayslip("08/2024"))
            repository.importPayslip(byteArrayOf(1), "pw", "a.pdf")
            runCurrent()

            var exportResult: Result<ByteArray>? = null
            viewModel.exportBackup("CorrectPass#1") { exportResult = it }
            runCurrent()
            val backupBytes = exportResult!!.getOrThrow()

            var importResult: Result<Unit>? = null
            viewModel.importBackup(backupBytes, "WrongPass#2", RestoreMode.REPLACE) { importResult = it }
            runCurrent()

            assertNotNull(importResult)
            assertTrue(importResult.isFailure, "Import must fail when wrong password is supplied")
            assertEquals(1, viewModel.uiState.value.payslips.size)
            assertEquals("08/2024", viewModel.uiState.value.payslips.first().dateStr)
        }

    @Test
    fun testImportBackupFailureWithCorruptedPayload() =
        runTest {
            val garbageBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
            var importResult: Result<Unit>? = null
            viewModel.importBackup(garbageBytes, "AnyPassword", RestoreMode.REPLACE) { importResult = it }
            runCurrent()

            assertNotNull(importResult)
            assertTrue(importResult.isFailure, "Import must fail gracefully on corrupted payload")
        }

    @Test
    fun testExportAndImportMultiByteUtf8PasswordParityAtViewModelLevel() =
        runTest {
            val unicodePassword = "p@sswörd🔑🇮🇳"
            fakeParser.result = Result.success(createMockPayslip("06/2024"))
            repository.importPayslip(byteArrayOf(1, 2), "pw", "c.pdf")
            runCurrent()

            var exportResult: Result<ByteArray>? = null
            viewModel.exportBackup(unicodePassword) { exportResult = it }
            runCurrent()
            val backupBytes = exportResult!!.getOrThrow()

            repository.clearAll()
            runCurrent()
            assertTrue(viewModel.uiState.value.payslips.isEmpty())

            var importResult: Result<Unit>? = null
            viewModel.importBackup(backupBytes, unicodePassword, RestoreMode.REPLACE) { importResult = it }
            runCurrent()

            assertNotNull(importResult)
            assertTrue(importResult.isSuccess)
            assertEquals(listOf("06/2024"), viewModel.uiState.value.payslips.map { it.dateStr })
        }

    private fun createMockPayslip(dateStr: String) =
        dateStr.split("/").let { split ->
            val month = split[0].toInt()
            val year = split[1].toInt()
            ParsedPayslip(
                file = "payslip_$dateStr.pdf", year = year, monthNum = month, monthName = "Month_$month", dateStr = dateStr,
                officer = Officer("Name", "Acc", "PAN"),
                earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
                ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
                summary = PayslipSummary(100.0, 80.0, 20.0),
                taxAndSavings =
                    TaxAndSavings(
                        grossSalaryYtd = 1000.0,
                        totalTaxableIncome = 900.0,
                        standardDeduction = 50.0,
                        netTaxableIncome = 850.0,
                        totalTaxPayable = 100.0,
                        taxDeductedYtd = 80.0,
                        cessDeductedYtd = 20.0,
                        dsopFund = DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0),
                    ),
            )
        }
}
