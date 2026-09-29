package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.reconciliation.SpecializedMilitaryFactor
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
class PcdaoAuditViewModelAsyncTest {
    private val testDispatcher = StandardTestDispatcher()

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
        repository = PayslipRepository(fakeDao, fakeParser, testDispatcher)
        mockAssetProvider = MockAsyncAssetProvider()
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
    fun testImmediateUiStateUpdateBeforeCalculationCompletes() =
        runTest(testDispatcher) {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())
            advanceUntilIdle()

            // Verify tile state updates immediately on caller thread before advancing scheduler
            assertFalse(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.CEA_ONE_CHILD))
            viewModel.toggleTile(SituationalTileKeys.CEA_ONE_CHILD)

            // Immediate UI update check without advanceUntilIdle
            assertTrue(
                viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.CEA_ONE_CHILD),
                "Tile selection state must update immediately on Main thread for 60 FPS responsiveness",
            )

            advanceUntilIdle()
            assertEquals(1, viewModel.uiState.value.activeContext.numberOfChildrenCea)
        }

    @Test
    fun testRapidTileTogglesCancelPrecedingJobsAndConsolidateResult() =
        runTest(testDispatcher) {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())
            advanceUntilIdle()

            // Trigger rapid toggles in succession
            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            val job1 = viewModel.activeCalculationJob
            assertNotNull(job1)

            viewModel.toggleTile(SituationalTileKeys.CEA_TWO_CHILDREN)
            val job2 = viewModel.activeCalculationJob
            assertNotNull(job2)

            // Assert preceding job was immediately cancelled
            assertTrue(job1.isCancelled, "Preceding calculation job must be cancelled on subsequent toggle")
            assertFalse(job2.isCancelled, "Latest calculation job must remain active")

            advanceUntilIdle()

            assertTrue(job2.isCompleted, "Final calculation job should complete successfully")
            val activeTiles = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(activeTiles.contains(SituationalTileKeys.POST_FIELD_HAFAA))
            assertTrue(activeTiles.contains(SituationalTileKeys.CEA_TWO_CHILDREN))
            assertEquals(2, viewModel.uiState.value.activeContext.numberOfChildrenCea)
            assertNotNull(viewModel.uiState.value.reconciliationResult)
        }

    @Test
    fun testRapidSpecializedFactorTogglesDebounceExecution() =
        runTest(testDispatcher) {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())
            advanceUntilIdle()

            viewModel.toggleSpecializedFactor(SpecializedMilitaryFactor.SIACHEN_GLACIER)
            val job1 = viewModel.activeCalculationJob
            assertNotNull(job1)

            viewModel.toggleSpecializedFactor(SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES)
            val job2 = viewModel.activeCalculationJob
            assertNotNull(job2)

            assertTrue(job1.isCancelled, "Preceding factor calculation job must be cancelled")
            advanceUntilIdle()

            assertTrue(job2.isCompleted)
            val factors = viewModel.uiState.value.activeContext.activeSpecializedFactors
            assertTrue(factors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))
            assertTrue(factors.contains(SpecializedMilitaryFactor.MARCOS_SPECIAL_FORCES))
        }

    @Test
    fun testAtomicPayFixationUpdateOnDefaultDispatcher() =
        runTest(testDispatcher) {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isPromotionActive)
            assertNull(viewModel.uiState.value.payFixationResult)

            // Toggle Promotion ON
            viewModel.toggleTile(SituationalTileKeys.PROMOTION_ACTIVE)
            assertTrue(viewModel.uiState.value.isPromotionActive, "Promotion flag should update immediately in context")

            advanceUntilIdle()
            val fixation = viewModel.uiState.value.payFixationResult
            assertNotNull(fixation, "Pay fixation result must be populated atomically after background calculation")
            assertTrue(fixation.opt1FixedPay > 0)

            // Toggle Promotion OFF
            viewModel.toggleTile(SituationalTileKeys.PROMOTION_ACTIVE)
            assertFalse(viewModel.uiState.value.isPromotionActive)

            advanceUntilIdle()
            assertNull(viewModel.uiState.value.payFixationResult, "Pay fixation result must be reset to null when toggled off")
        }

    @Test
    fun testSelectPayslipExecutesRollupAsynchronously() =
        runTest(testDispatcher) {
            val slip1 = createMockPayslip("01/2026", basicPay = 69000.0, tpta = 7200.0)
            val slip2 = createMockPayslip("02/2026", basicPay = 121200.0, tpta = 7200.0)
            fakeDao.insertPayslip(slip1.toEncryptedEntity())
            fakeDao.insertPayslip(slip2.toEncryptedEntity())
            advanceUntilIdle()

            viewModel.selectPayslip(slip2)
            val job = viewModel.activeCalculationJob
            assertNotNull(job)

            advanceUntilIdle()
            assertTrue(job.isCompleted)
            assertEquals(slip2, viewModel.uiState.value.selectedPayslip)
            assertEquals("12A", viewModel.uiState.value.activeContext.inferredFlags.inferredRankLevel)
            assertNotNull(viewModel.uiState.value.cumulativeRollup)
        }

    private fun createMockPayslip(
        dateStr: String,
        basicPay: Double,
        tpta: Double,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = 2026,
            monthNum = 3,
            monthName = "March",
            dateStr = dateStr,
            officer = Officer("Col R S Rathore", "01/142/987654", "ABCDE1234F"),
            earnings = Earnings(basicPay = basicPay, dearnessAllowance = basicPay * 0.5, militaryServicePay = 15500.0, transportAllowance = tpta),
            deductions = Deductions(dsopSubscription = 20000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(basicPay + basicPay * 0.5 + 15500.0 + tpta, 20000.0, basicPay),
            taxAndSavings = null,
        )

    private class MockAsyncAssetProvider : PcdaoAssetProvider {
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
                        "12A": [121200, 124800, 128500, 132400, 136400, 140400, 144700, 149000, 153500]
                      }
                    }
                    """.trimIndent()
                else -> "{}"
            }
    }
}
