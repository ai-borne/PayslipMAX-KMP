package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.DsopFund
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
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

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelHistoryTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository, fakeBackupManager)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialLoadAutoExpandsLatestYearOnly() =
        runTest {
            fakeDao.insertPayslip(createMockPayslip("08/2023").toEncryptedEntity())
            fakeDao.insertPayslip(createMockPayslip("09/2024").toEncryptedEntity())

            val testViewModel = PayslipViewModel(repository, fakeBackupManager)

            assertEquals(setOf(2024), testViewModel.uiState.value.expandedHistoryYears)
        }

    @Test
    fun testToggleOlderYearLeavesLatestYearExpanded() =
        runTest {
            fakeDao.insertPayslip(createMockPayslip("08/2023").toEncryptedEntity())
            fakeDao.insertPayslip(createMockPayslip("09/2024").toEncryptedEntity())

            val testViewModel = PayslipViewModel(repository, fakeBackupManager)

            testViewModel.toggleHistoryYearExpanded(2023)
            assertEquals(setOf(2024, 2023), testViewModel.uiState.value.expandedHistoryYears)

            testViewModel.toggleHistoryYearExpanded(2023)
            assertEquals(setOf(2024), testViewModel.uiState.value.expandedHistoryYears)
        }

    @Test
    fun testNewLatestYearAutoExpandsAdditively() =
        runTest {
            fakeDao.insertPayslip(createMockPayslip("08/2023").toEncryptedEntity())
            fakeDao.insertPayslip(createMockPayslip("09/2024").toEncryptedEntity())

            val testViewModel = PayslipViewModel(repository, fakeBackupManager)
            testViewModel.toggleHistoryYearExpanded(2023)
            assertEquals(setOf(2024, 2023), testViewModel.uiState.value.expandedHistoryYears)

            fakeDao.insertPayslip(createMockPayslip("01/2025").toEncryptedEntity())

            assertEquals(setOf(2024, 2023, 2025), testViewModel.uiState.value.expandedHistoryYears)
        }

    @Test
    fun testSaveHistoryScrollPositionOnlyUpdatesScrollFields() =
        runTest {
            fakeDao.insertPayslip(createMockPayslip("08/2023").toEncryptedEntity())
            fakeDao.insertPayslip(createMockPayslip("09/2024").toEncryptedEntity())
            val testViewModel = PayslipViewModel(repository, fakeBackupManager)
            testViewModel.toggleHistoryYearExpanded(2023)
            val expandedBefore = testViewModel.uiState.value.expandedHistoryYears

            testViewModel.saveHistoryScrollPosition(3, 42)

            val state = testViewModel.uiState.value
            assertEquals(3, state.historyScrollIndex)
            assertEquals(42, state.historyScrollOffset)
            assertEquals(expandedBefore, state.expandedHistoryYears)
        }

    @Test
    fun testSelectHistoryDetailPayslipSetsIdWithoutTouchingSelectedPayslip() =
        runTest {
            fakeDao.insertPayslip(createMockPayslip("08/2023").toEncryptedEntity())
            val testViewModel = PayslipViewModel(repository, fakeBackupManager)
            val selectedBefore = testViewModel.uiState.value.selectedPayslip

            testViewModel.selectHistoryDetailPayslip("08/2023")

            val state = testViewModel.uiState.value
            assertEquals("08/2023", state.historyDetailPayslipId)
            assertEquals(selectedBefore, state.selectedPayslip)
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
