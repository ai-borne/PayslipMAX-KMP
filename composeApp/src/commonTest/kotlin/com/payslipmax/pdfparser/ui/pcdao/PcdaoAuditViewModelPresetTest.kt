package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.reconciliation.MissionPresetId
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
class PcdaoAuditViewModelPresetTest {
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
        mockAssetProvider = MockPresetAssetProvider()
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
    fun testApplyMissionPreset_RrCiOps_ActivatesCfaaSprHraTlcAndCalculatesArrears() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            viewModel.applyMissionPreset(MissionPresetId.RR_CI_OPS)

            val state = viewModel.uiState.value
            assertEquals(MissionPresetId.RR_CI_OPS, state.activePresetId)
            val activeTiles = state.activeContext.activeTileIds
            assertTrue(activeTiles.contains(SituationalTileKeys.POST_FIELD_CFAA), "Must activate CFAA")
            assertTrue(activeTiles.contains(SituationalTileKeys.HOUSE_FAMILY_SPR), "Must activate Family SPR")
            assertTrue(activeTiles.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION), "Must activate TLC")

            // Reconciler should detect CFAA underpayment since payslip had 0 field allowance
            assertNotNull(state.reconciliationResult)
            assertTrue(state.unclaimedTotal > 0.0, "Should have unclaimed dues for CFAA + SPR HRA")
        }

    @Test
    fun testApplyMissionPreset_ThenManualToggle_RetainsBaseAndAddsManualTile() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            // 1. Apply baseline preset
            viewModel.applyMissionPreset(MissionPresetId.RR_CI_OPS)
            val initialTiles = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(initialTiles.contains(SituationalTileKeys.POST_FIELD_CFAA))
            assertTrue(initialTiles.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))

            // 2. Add 2 Children CEA manually
            viewModel.toggleTile(SituationalTileKeys.CEA_TWO_CHILDREN)

            val updatedState = viewModel.uiState.value
            val updatedTiles = updatedState.activeContext.activeTileIds
            assertTrue(updatedTiles.contains(SituationalTileKeys.POST_FIELD_CFAA), "Must retain CFAA")
            assertTrue(updatedTiles.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION), "Must retain TLC")
            assertTrue(updatedTiles.contains(SituationalTileKeys.HOUSE_FAMILY_SPR), "Must retain SPR")
            assertTrue(updatedTiles.contains(SituationalTileKeys.CEA_TWO_CHILDREN), "Must add CEA 2 Children")
            assertEquals(2, updatedState.activeContext.numberOfChildrenCea)
        }

    @Test
    fun testMonthSwitchPreservesStickyContextOverride() =
        runTest {
            val payslipA = createMockPayslip("01/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0)
            val payslipB = createMockPayslip("02/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslipA.toEncryptedEntity())
            fakeDao.insertPayslip(payslipB.toEncryptedEntity())

            // Select Payslip A and apply Siachen Brigade preset
            viewModel.selectPayslip(payslipA)
            viewModel.applyMissionPreset(MissionPresetId.SIACHEN_BRIGADE)

            var stateA = viewModel.uiState.value
            assertEquals(payslipA, stateA.selectedPayslip)
            assertEquals(MissionPresetId.SIACHEN_BRIGADE, stateA.activePresetId)
            assertTrue(stateA.activeContext.activeTileIds.contains(SituationalTileKeys.POST_SIACHEN))
            assertTrue(stateA.activeContext.activeSpecializedFactors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))

            // Switch to Payslip B (fresh, should auto-infer default context)
            viewModel.selectPayslip(payslipB)
            val stateB = viewModel.uiState.value
            assertEquals(payslipB, stateB.selectedPayslip)
            assertFalse(stateB.activeContext.activeTileIds.contains(SituationalTileKeys.POST_SIACHEN))
            assertFalse(stateB.activeContext.activeSpecializedFactors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))

            // Switch back to Payslip A (must restore sticky override with Siachen preset!)
            viewModel.selectPayslip(payslipA)
            stateA = viewModel.uiState.value
            assertEquals(payslipA, stateA.selectedPayslip)
            assertEquals(MissionPresetId.SIACHEN_BRIGADE, stateA.activePresetId)
            assertTrue(stateA.activeContext.activeTileIds.contains(SituationalTileKeys.POST_SIACHEN))
            assertTrue(stateA.activeContext.activeSpecializedFactors.contains(SpecializedMilitaryFactor.SIACHEN_GLACIER))
        }

    @Test
    fun testApplyMissionPreset_AmcHospital_ActivatesNpaCompounding() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 121200.0, da = 60600.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            viewModel.applyMissionPreset(MissionPresetId.AMC_HOSPITAL)

            val state = viewModel.uiState.value
            assertEquals(MissionPresetId.AMC_HOSPITAL, state.activePresetId)
            assertTrue(state.activeContext.activeTileIds.contains(SituationalTileKeys.CADRE_AMC_NPA))
            assertTrue(state.activeContext.activeTileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
            assertTrue(state.activeContext.activeTileIds.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
            assertTrue(state.activeContext.activeSpecializedFactors.contains(SpecializedMilitaryFactor.NON_PRACTICING_ALLOWANCE_AMC))
        }

    @Test
    fun testHousingRadioBehaviorInToggleTile() =
        runTest {
            val payslip = createMockPayslip("03/2026", basicPay = 69000.0, da = 34500.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            // Select Family SPR
            viewModel.toggleTile(SituationalTileKeys.HOUSE_FAMILY_SPR)
            assertTrue(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.HOUSE_FAMILY_SPR))

            // Select Govt MQ -> replaces Family SPR
            viewModel.toggleTile(SituationalTileKeys.HOUSE_GOVT_MQ)
            val tiles = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(tiles.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
            assertFalse(tiles.contains(SituationalTileKeys.HOUSE_FAMILY_SPR), "Govt MQ must replace Family SPR")

            // Toggle TLC concession -> can coexist with housing selection
            viewModel.toggleTile(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION)
            val tilesWithTlc = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(tilesWithTlc.contains(SituationalTileKeys.HOUSE_GOVT_MQ))
            assertTrue(tilesWithTlc.contains(SituationalTileKeys.HOUSE_TWO_LOCATION_CONCESSION))
        }

    private fun createMockPayslip(
        dateStr: String,
        basicPay: Double = 69000.0,
        da: Double = 34500.0,
        tpta: Double = 7200.0,
    ): ParsedPayslip {
        val split = dateStr.split("/")
        val month = split[0].toInt()
        val year = split[1].toInt()
        return ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = month,
            monthName = "Month_$month",
            dateStr = dateStr,
            officer = Officer("Col R S Rathore", "01/142/987654", "ABCDE1234F"),
            earnings = Earnings(basicPay, da, 15500.0, tpta, 0.0, 0.0, 0.0, 0.0),
            deductions = Deductions(20000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(basicPay + da + 15500.0 + tpta, 20000.0, basicPay),
            taxAndSavings = null,
        )
    }

    private class MockPresetAssetProvider : PcdaoAssetProvider {
        override suspend fun loadAsset(fileName: String): String = "{}"
    }
}
