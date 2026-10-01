package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.engine.AllowanceCollisionCodes
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.SituationalCategory
import com.payslipmax.pcdao.reconciliation.SituationalTileKeys
import com.payslipmax.pcdao.reconciliation.SpecializedMilitaryFactor
import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
import com.payslipmax.pdfparser.Screen
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
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.screens.gateForScreen
import com.payslipmax.pdfparser.ui.screens.quickAccessTools
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
class PcdaoAuditViewModelTest {
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
                defaultDispatcher = testDispatcher,
                coroutineScope = kotlinx.coroutines.CoroutineScope(testDispatcher),
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateWithEmptyVault() {
        val state = viewModel.uiState.value
        assertTrue(state.availablePayslips.isEmpty())
        assertNull(state.selectedPayslip)
        assertNull(state.reconciliationResult)
    }

    @Test
    fun testAutoDetectionFromParsedPayslip() =
        runTest {
            val mockPayslip =
                createMockPayslip(
                    dateStr = "03/2026",
                    basicPay = 69000.0,
                    da = 34500.0,
                    tpta = 7200.0,
                    dsop = 45000.0,
                )
            fakeDao.insertPayslip(mockPayslip.toEncryptedEntity())

            val state = viewModel.uiState.value
            assertEquals(1, state.availablePayslips.size)
            assertEquals(mockPayslip, state.selectedPayslip)
            assertEquals("10", state.activeContext.inferredFlags.inferredRankLevel)
            assertTrue(state.activeContext.inferredFlags.inferredDaCrossed50)
            assertTrue(state.activeContext.activeTileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
            assertTrue(state.activeContext.activeTileIds.contains(SituationalTileKeys.DSOP_HIGH_PACING))
            assertNotNull(state.reconciliationResult)
        }

    @Test
    fun testMonthSelectionSwitchesContext() =
        runTest {
            val payslip1 = createMockPayslip("01/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0, dsop = 20000.0)
            val payslip2 = createMockPayslip("02/2026", basicPay = 121200.0, da = 60600.0, tpta = 7200.0, dsop = 50000.0)
            fakeDao.insertPayslip(payslip1.toEncryptedEntity())
            fakeDao.insertPayslip(payslip2.toEncryptedEntity())

            viewModel.selectPayslip(payslip1)
            assertEquals(payslip1, viewModel.uiState.value.selectedPayslip)
            assertEquals("10", viewModel.uiState.value.activeContext.inferredFlags.inferredRankLevel)

            viewModel.selectPayslip(payslip2)
            assertEquals(payslip2, viewModel.uiState.value.selectedPayslip)
            assertEquals("12A", viewModel.uiState.value.activeContext.inferredFlags.inferredRankLevel)
        }

    @Test
    fun testTogglingTileReactivelyUpdatesReconciliation() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0, dsop = 20000.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            // Initial state has no recovery hazard
            val initialHazard = viewModel.uiState.value.hazardTotal
            assertEquals(0.0, initialHazard)

            // Toggle Field Area (HAFAA) while Peace TPTA was drawn -> triggers advisory alarm, zero recovery distortion
            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            assertEquals(0.0, viewModel.uiState.value.hazardTotal)
            assertTrue(
                viewModel.uiState.value.filteredDiscrepancies.any {
                    it.type == DiscrepancyType.FORFEITURE_RISK && it.id == AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE
                },
                "Concurrent TPTA + HAFAA should trigger advisory alarm without inflating recovery hazard",
            )

            // Toggle it back off -> removes the alarm
            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            assertEquals(0.0, viewModel.uiState.value.hazardTotal)
            assertFalse(
                viewModel.uiState.value.filteredDiscrepancies.any {
                    it.id == AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE
                },
            )
        }

    @Test
    fun testTogglingPromotionTriggersPayFixation() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0, dsop = 20000.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            assertNull(viewModel.uiState.value.payFixationResult)
            assertFalse(viewModel.uiState.value.isPromotionActive)

            // Toggle Promotion Tile
            viewModel.toggleTile(SituationalTileKeys.PROMOTION_ACTIVE)
            assertTrue(viewModel.uiState.value.isPromotionActive)
            val fixation = viewModel.uiState.value.payFixationResult
            assertNotNull(fixation)
            assertTrue(fixation.opt1FixedPay > 0)
            assertTrue(fixation.opt2PostDniFixedPay > 0)

            // Toggle Promotion Tile Off
            viewModel.toggleTile(SituationalTileKeys.PROMOTION_ACTIVE)
            assertFalse(viewModel.uiState.value.isPromotionActive)
            assertNull(viewModel.uiState.value.payFixationResult)
        }

    @Test
    fun testSpecializedFactorToggle() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0, dsop = 20000.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            assertFalse(viewModel.uiState.value.activeContext.activeSpecializedFactors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))

            viewModel.toggleSpecializedFactor(SpecializedMilitaryFactor.SIACHEN_GLACIER)
            assertTrue(viewModel.uiState.value.activeContext.activeSpecializedFactors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))

            viewModel.toggleSpecializedFactor(SpecializedMilitaryFactor.SIACHEN_GLACIER)
            assertFalse(viewModel.uiState.value.activeContext.activeSpecializedFactors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))
        }

    @Test
    fun testFilterTabsAndRedressalLetter() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0, dsop = 20000.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            // Toggle CEA Two Children to create underpayment discrepancy
            viewModel.toggleTile(SituationalTileKeys.CEA_TWO_CHILDREN)

            viewModel.setFilter(FindingFilter.ENTITLEMENTS)
            val entList = viewModel.uiState.value.filteredDiscrepancies
            assertTrue(entList.isNotEmpty())

            viewModel.setFilter(FindingFilter.ALL)
            val allList = viewModel.uiState.value.filteredDiscrepancies
            assertTrue(allList.isNotEmpty())

            // Generate Redressal Letter
            val letter = viewModel.generateRedressalLetter()
            assertNotNull(letter)
            assertTrue(letter.recipient.contains("Defence Accounts"))
            assertTrue(letter.fullBodyText.isNotEmpty())
            assertEquals("PCDA(O) Official Representation", letter.title)
            assertEquals(letter, viewModel.uiState.value.generatedLetter)

            // Test PII Masking
            val maskedLetter = viewModel.generateRedressalLetter(maskPii = true)
            assertNotNull(maskedLetter)
            assertTrue(maskedLetter.isPiiMasked)
            assertEquals(maskedLetter, viewModel.uiState.value.generatedLetter)

            // Test Clear Generated Letter
            viewModel.clearGeneratedLetter()
            assertNull(viewModel.uiState.value.generatedLetter)
        }

    @Test
    fun testCategorySelectionAndAddFactorSheetVisibility() {
        assertEquals(SituationalCategory.POSTING, viewModel.uiState.value.selectedCategory)
        viewModel.selectCategory(SituationalCategory.HOUSING)
        assertEquals(SituationalCategory.HOUSING, viewModel.uiState.value.selectedCategory)

        assertFalse(viewModel.uiState.value.isAddFactorSheetVisible)
        viewModel.setAddFactorSheetVisible(true)
        assertTrue(viewModel.uiState.value.isAddFactorSheetVisible)
        viewModel.setAddFactorSheetVisible(false)
        assertFalse(viewModel.uiState.value.isAddFactorSheetVisible)
    }

    @Test
    fun testNavigationAndCatalogInvariant() {
        assertEquals(FeatureGate.PAYSLIPMAX_AI, gateForScreen(Screen.PcdaoAudit))
        assertTrue(quickAccessTools().any { it.gate == FeatureGate.PAYSLIPMAX_AI && it.target == Screen.PcdaoAudit })
    }

    private fun createMockPayslip(
        dateStr: String,
        basicPay: Double = 69000.0,
        da: Double = 34500.0,
        tpta: Double = 7200.0,
        dsop: Double = 20000.0,
    ) = dateStr.split("/").let { split ->
        val month = split[0].toInt()
        val year = split[1].toInt()
        ParsedPayslip(
            file = "payslip_$dateStr.pdf", year = year, monthNum = month, monthName = "Month_$month", dateStr = dateStr,
            officer = Officer("Col R S Rathore", "01/142/987654", "ABCDE1234F"),
            earnings = Earnings(basicPay, da, 15500.0, tpta, 0.0, 0.0, 0.0, 0.0),
            deductions = Deductions(dsop, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(basicPay + da + 15500.0 + tpta, dsop, basicPay),
            taxAndSavings = TaxAndSavings(1000.0, 900.0, 50.0, 850.0, 100.0, 80.0, 20.0, DsopFund(dsop, 0.0, 0.0, 0.0, 0.0, dsop)),
        )
    }

    private class MockAssetProvider : PcdaoAssetProvider {
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
