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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelTest {
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
    fun testInitialState() {
        val state = viewModel.uiState.value
        assertTrue(state.payslips.isEmpty())
        assertNull(state.selectedPayslip)
        assertFalse(state.isLoading) // Ends up false after immediate fake Dao collection
        assertNull(state.error)
        assertFalse(state.importSuccess)
    }

    @Test
    fun testUiStateDefaultValues() {
        val state = PayslipUiState()
        assertTrue(state.payslips.isEmpty())
        assertNull(state.selectedPayslip)
        assertTrue(state.isLoading)
        assertNull(state.error)
        assertFalse(state.importSuccess)
    }

    @Test
    fun testObservePayslipsUpdatesState() =
        runTest {
            val mockPayslip = createMockPayslip("08/2024")
            fakeDao.insertPayslip(mockPayslip.toEncryptedEntity())

            // The flow collection in observePayslips() is active due to UnconfinedTestDispatcher.
            // Inserting into fakeDao should immediately trigger updates in UI state.
            val state = viewModel.uiState.value
            assertEquals(1, state.payslips.size)
            assertEquals(mockPayslip, state.selectedPayslip)
            assertNotNull(state.taxOptimizationResult)
        }

    @Test
    fun testTaxOptimizationResultNullWhenEmpty() {
        assertNull(viewModel.uiState.value.taxOptimizationResult)
    }

    @Test
    fun testSelectPayslip() {
        val mock1 = createMockPayslip("08/2024")
        val mock2 = createMockPayslip("09/2024")

        viewModel.selectPayslip(mock1)
        assertEquals(mock1, viewModel.uiState.value.selectedPayslip)

        viewModel.selectPayslip(mock2)
        assertEquals(mock2, viewModel.uiState.value.selectedPayslip)
    }

    @Test
    fun testImportPayslipSuccess() =
        runTest {
            val mockPayslip = createMockPayslip("08/2024")
            fakeParser.result = Result.success(mockPayslip)

            viewModel.importPayslip(byteArrayOf(1), "pass", "file.pdf")

            val state = viewModel.uiState.value
            assertEquals(mockPayslip, state.selectedPayslip)
            assertTrue(state.importSuccess)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun testImportPayslipFailure() =
        runTest {
            val exception = Exception("Invalid Password")
            fakeParser.result = Result.failure(exception)

            viewModel.importPayslip(byteArrayOf(1), "pass", "file.pdf")

            val state = viewModel.uiState.value
            assertNull(state.selectedPayslip)
            assertFalse(state.importSuccess)
            assertFalse(state.isLoading)
            assertEquals("Invalid Password", state.importError)
        }

    @Test
    fun testDeletePayslip() =
        runTest {
            val mock1 = createMockPayslip("08/2024")
            val mock2 = createMockPayslip("09/2024")
            fakeDao.insertPayslip(mock1.toEncryptedEntity())
            fakeDao.insertPayslip(mock2.toEncryptedEntity())

            // Re-create ViewModel so that both items are loaded initially and the last one (mock2) is selected
            val testViewModel = PayslipViewModel(repository)
            assertEquals(mock2, testViewModel.uiState.value.selectedPayslip)

            // Delete selected
            testViewModel.deletePayslip("09/2024")

            val state = testViewModel.uiState.value
            assertEquals(1, state.payslips.size)
            assertEquals(mock1, state.selectedPayslip)
        }

    @Test
    fun testDeleteNonSelectedPayslip() =
        runTest {
            val mock1 = createMockPayslip("08/2024")
            val mock2 = createMockPayslip("09/2024")
            fakeDao.insertPayslip(mock1.toEncryptedEntity())
            fakeDao.insertPayslip(mock2.toEncryptedEntity())

            val testViewModel = PayslipViewModel(repository)
            assertEquals(mock2, testViewModel.uiState.value.selectedPayslip)

            // Delete non-selected mock1
            testViewModel.deletePayslip("08/2024")

            val state = testViewModel.uiState.value
            assertEquals(1, state.payslips.size)
            assertEquals(mock2, state.selectedPayslip) // selected remains mock2
        }

    @Test
    fun testClearErrorAndResetImportSuccess() {
        val exception = Exception("Error")
        fakeParser.result = Result.failure(exception)
        viewModel.importPayslip(byteArrayOf(1), "pass", "file.pdf")

        assertNotNull(viewModel.uiState.value.importError)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.importError)

        fakeParser.result = Result.success(createMockPayslip("08/2024"))
        viewModel.importPayslip(byteArrayOf(1), "pass", "file.pdf")

        assertTrue(viewModel.uiState.value.importSuccess)
        viewModel.resetImportSuccess()
        assertFalse(viewModel.uiState.value.importSuccess)
    }

    @Test
    fun testSeedMockData() =
        runTest {
            viewModel.seedMockData()

            val state = viewModel.uiState.value
            // Seeding database populates 48 items
            assertEquals(48, state.payslips.size)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun testClearAllData() =
        runTest {
            fakeDao.insertPayslip(createMockPayslip("08/2024").toEncryptedEntity())
            assertEquals(1, viewModel.uiState.value.payslips.size)

            viewModel.clearAllData()

            val state = viewModel.uiState.value
            assertTrue(state.payslips.isEmpty())
            assertNull(state.selectedPayslip)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun testGetAvailableYears() =
        runTest {
            val mock1 = createMockPayslip("08/2023")
            val mock2 = createMockPayslip("09/2024")
            val mock3 = createMockPayslip("10/2024")
            fakeDao.insertPayslip(mock1.toEncryptedEntity())
            fakeDao.insertPayslip(mock2.toEncryptedEntity())
            fakeDao.insertPayslip(mock3.toEncryptedEntity())

            val testViewModel = PayslipViewModel(repository)
            val years = testViewModel.getAvailableYears()

            assertEquals(2, years.size)
            assertEquals(2024, years[0])
            assertEquals(2023, years[1])
        }

    @Test
    fun testGetMonthsForYear() =
        runTest {
            val mock1 = createMockPayslip("08/2023")
            val mock2 = createMockPayslip("09/2024")
            val mock3 = createMockPayslip("10/2024")
            fakeDao.insertPayslip(mock1.toEncryptedEntity())
            fakeDao.insertPayslip(mock2.toEncryptedEntity())
            fakeDao.insertPayslip(mock3.toEncryptedEntity())

            val testViewModel = PayslipViewModel(repository)
            val months2024 = testViewModel.getMonthsForYear(2024)

            assertEquals(2, months2024.size)
            assertEquals(10, months2024[0].monthNum)
            assertEquals(9, months2024[1].monthNum)

            val months2023 = testViewModel.getMonthsForYear(2023)
            assertEquals(1, months2023.size)
            assertEquals(8, months2023[0].monthNum)
        }

    @Test
    fun testSelectByYearMonth() =
        runTest {
            val mock1 = createMockPayslip("08/2023")
            val mock2 = createMockPayslip("09/2024")
            fakeDao.insertPayslip(mock1.toEncryptedEntity())
            fakeDao.insertPayslip(mock2.toEncryptedEntity())

            val testViewModel = PayslipViewModel(repository)
            assertEquals(mock2, testViewModel.uiState.value.selectedPayslip)

            testViewModel.selectByYearMonth(2023, 8)
            assertEquals(mock1, testViewModel.uiState.value.selectedPayslip)

            testViewModel.selectByYearMonth(2025, 1)
            assertEquals(mock1, testViewModel.uiState.value.selectedPayslip)
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
                taxAndSavings = TaxAndSavings(1000.0, 900.0, 50.0, 850.0, 100.0, 80.0, 20.0, DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0)),
            )
        }
}
