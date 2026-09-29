package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.engine.FixationOption
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PcdaoAuditViewModelSandboxTest {
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
        mockAssetProvider = MockSandboxAssetProvider()
        rulesRepository = PcdaoRulesRepository(mockAssetProvider)
        viewModel =
            PcdaoAuditViewModel(
                payslipRepository = repository,
                rulesRepository = rulesRepository,
                defaultDispatcher = testDispatcher,
                coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testAutoInferredPromotionWhen6YearsTenureDetected() =
        runTest {
            val pastPayslip = createMockPayslip("03/2020", basicPay = 56100.0)
            val currentPayslip = createMockPayslip("03/2026", basicPay = 69000.0)
            fakeDao.insertPayslip(pastPayslip.toEncryptedEntity())
            fakeDao.insertPayslip(currentPayslip.toEncryptedEntity())

            viewModel.selectPayslip(currentPayslip)

            val state = viewModel.uiState.value
            assertTrue(state.isPromotionActive, "Promotion should be auto-flagged active with 6 years tenure")
            assertTrue(state.autoInferredTileIds.contains(SituationalTileKeys.PROMOTION_ACTIVE))

            val fixation = state.payFixationResult
            assertNotNull(fixation)
            assertEquals("10", fixation.fromLevel)
            assertEquals("11", fixation.toLevel)
        }

    @Test
    fun testSandboxLevelSelectionUpdatesFixationResult() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            viewModel.toggleTile(SituationalTileKeys.PROMOTION_ACTIVE)
            val defaultFixation = viewModel.uiState.value.payFixationResult
            assertNotNull(defaultFixation)
            assertEquals("10", defaultFixation.fromLevel)
            assertEquals("11", defaultFixation.toLevel)

            // Switch to Level 11 -> 12A sandbox projection
            viewModel.setSandboxLevels("11", "12A")
            val sandbox11To12A = viewModel.uiState.value.payFixationResult
            assertNotNull(sandbox11To12A)
            assertEquals("11", sandbox11To12A.fromLevel)
            assertEquals("12A", sandbox11To12A.toLevel)
            assertEquals(121200, sandbox11To12A.opt1FixedPay)

            // Switch to Level 12A -> 13 sandbox projection
            viewModel.setSandboxLevels("12A", "13")
            val sandbox12ATo13 = viewModel.uiState.value.payFixationResult
            assertNotNull(sandbox12ATo13)
            assertEquals("12A", sandbox12ATo13.fromLevel)
            assertEquals("13", sandbox12ATo13.toLevel)
            assertEquals(155900, sandbox12ATo13.opt1FixedPay)
            assertEquals(FixationOption.OPTION_2, sandbox12ATo13.recommendedOption)
        }

    private fun createMockPayslip(
        dateStr: String,
        basicPay: Double = 69000.0,
    ): ParsedPayslip {
        val split = dateStr.split("/")
        val month = split[0].toInt()
        val year = split[1].toInt()
        return ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = month,
            monthName = "Month_$month",
            dateStr = "$year-${if (month < 10) "0$month" else "$month"}-01",
            officer = Officer("Col R S Rathore", "01/142/987654", "ABCDE1234F"),
            earnings = Earnings(basicPay, 34500.0, 15500.0, 7200.0, 0.0, 0.0, 0.0, 0.0),
            deductions = Deductions(20000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(basicPay + 34500.0 + 15500.0 + 7200.0, 20000.0, basicPay),
            taxAndSavings = TaxAndSavings(1000.0, 900.0, 50.0, 850.0, 100.0, 80.0, 20.0, DsopFund(20000.0, 0.0, 0.0, 0.0, 0.0, 20000.0)),
        )
    }

    private class MockSandboxAssetProvider : PcdaoAssetProvider {
        override suspend fun loadAsset(fileName: String): String =
            when (fileName) {
                "pay_matrix_7th_cpc.json" ->
                    """
                    {
                      "commission": "7th Central Pay Commission",
                      "effective_date": "2016-01-01",
                      "index_of_rationalisation": {
                        "level_10_to_11": 2.57,
                        "level_12A_and_13": 2.67,
                        "level_13A_and_above": 2.72
                      },
                      "regular_officers_pay_matrix": {
                        "10": [56100, 57800, 59500, 61300, 63100, 65000, 67000, 69000, 71100, 73200],
                        "11": [69400, 71500, 73600, 75800, 78100, 80400, 82800, 85300, 87900, 90500],
                        "12A": [121200, 124800, 128500, 132400, 136400, 140500, 144700, 149000, 153500, 158100, 162800],
                        "13": [130600, 134500, 138500, 142700, 147000, 151400, 155900, 160600, 165400, 170400]
                      }
                    }
                    """.trimIndent()
                else -> "{}"
            }
    }
}
