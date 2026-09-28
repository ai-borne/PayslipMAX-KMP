package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PcdaoAuditViewModelTimelineTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var repository: PayslipRepository
    private lateinit var mockAssetProvider: PcdaoAssetProvider
    private lateinit var rulesRepository: PcdaoRulesRepository
    private lateinit var viewModel: PcdaoAuditViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        mockAssetProvider = MockAssetProvider()
        rulesRepository = PcdaoRulesRepository(mockAssetProvider)
        viewModel =
            PcdaoAuditViewModel(
                payslipRepository = repository,
                rulesRepository = rulesRepository,
                coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testViewModelLoadsPayslipsNewestFirstAndSelectsLatest() =
        runTest {
            val slip1 = createPayslip("2025-04-01", 2025, 4, 144700.0)
            val slip2 = createPayslip("2026-08-01", 2026, 8, 149000.0)
            val slip3 = createPayslip("2025-12-01", 2025, 12, 144700.0)

            fakeDao.insertPayslip(slip1.toEncryptedEntity())
            fakeDao.insertPayslip(slip2.toEncryptedEntity())
            fakeDao.insertPayslip(slip3.toEncryptedEntity())

            val freshViewModel =
                PcdaoAuditViewModel(
                    payslipRepository = repository,
                    rulesRepository = rulesRepository,
                    coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
                )

            val state = freshViewModel.uiState.value
            assertEquals(3, state.availablePayslips.size)
            assertEquals("2026-08-01", state.availablePayslips[0].dateStr)
            assertEquals("2025-12-01", state.availablePayslips[1].dateStr)
            assertEquals("2025-04-01", state.availablePayslips[2].dateStr)
            assertEquals("2026-08-01", state.selectedPayslip?.dateStr)
        }

    @Test
    fun testViewModelGroupsByFinancialYear() =
        runTest {
            val slip1 = createPayslip("2025-05-01", 2025, 5, 144700.0)
            val slip2 = createPayslip("2026-07-01", 2026, 7, 149000.0)

            fakeDao.insertPayslip(slip1.toEncryptedEntity())
            fakeDao.insertPayslip(slip2.toEncryptedEntity())

            val state = viewModel.uiState.value
            assertTrue(state.groupedMonths.containsKey("FY 2026-27"))
            assertTrue(state.groupedMonths.containsKey("FY 2025-26"))
        }

    @Test
    fun testViewModelCumulativeRollupAndToggleView() =
        runTest {
            val slip1 = createPayslip("2025-12-01", 2025, 12, 144700.0)
            val slip2 = createPayslip("2026-01-01", 2026, 1, 149000.0)

            fakeDao.insertPayslip(slip1.toEncryptedEntity())
            fakeDao.insertPayslip(slip2.toEncryptedEntity())

            assertFalse(viewModel.uiState.value.isCumulativeViewActive)
            viewModel.toggleTile(SituationalTileKeys.HOUSE_FAMILY_SPR)

            val state = viewModel.uiState.value
            assertTrue(state.hasCumulativeArrears)
            assertNotNull(state.cumulativeRollup)
            assertTrue(state.cumulativeRollup.totalUnderpaidArrears > 0.0)

            viewModel.toggleCumulativeView()
            val cumulativeState = viewModel.uiState.value
            assertTrue(cumulativeState.isCumulativeViewActive)
            assertEquals(cumulativeState.cumulativeRollup?.totalUnderpaidArrears, cumulativeState.unclaimedTotal)
        }

    @Test
    fun testCumulativeRedressalLetterGeneration() =
        runTest {
            val slip1 = createPayslip("2025-12-01", 2025, 12, 144700.0)
            val slip2 = createPayslip("2026-01-01", 2026, 1, 149000.0)

            fakeDao.insertPayslip(slip1.toEncryptedEntity())
            fakeDao.insertPayslip(slip2.toEncryptedEntity())

            viewModel.toggleTile(SituationalTileKeys.HOUSE_FAMILY_SPR)
            viewModel.toggleCumulativeView()

            val letter = viewModel.generateRedressalLetter()
            assertNotNull(letter)
            assertTrue(letter.disputeMonth.contains("Months"))
            assertTrue(letter.fullBodyText.contains("Months"))
        }

    private fun createPayslip(
        dateStr: String,
        year: Int,
        month: Int,
        basicPay: Double,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = month,
            monthName = "Month_$month",
            dateStr = dateStr,
            officer = Officer("Col R S Rathore", "01/142/987654", "ABCDE1234F"),
            earnings = Earnings(basicPay = basicPay, dearnessAllowance = 89400.0, militaryServicePay = 15500.0),
            deductions = Deductions(dsopSubscription = 40000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(basicPay + 89400.0 + 15500.0, 40000.0, basicPay),
            taxAndSavings = null,
        )

    private class MockAssetProvider : PcdaoAssetProvider {
        override suspend fun loadAsset(fileName: String): String = "{}"
    }
}
