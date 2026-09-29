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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PcdaoAuditViewModelStationTest {
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
        mockAssetProvider = MockStationAssetProvider()
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
    fun testPeaceStationSwitchingReplacesPreviousPeaceSelection() =
        runTest {
            val payslip = createMockStationPayslip("03/2026", basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            // Initial auto-detection inferred POST_PEACE_HIGHER
            assertTrue(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.POST_PEACE_HIGHER))
            assertFalse(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.POST_PEACE_OTHER))

            // Toggling POST_PEACE_OTHER replaces POST_PEACE_HIGHER
            viewModel.toggleTile(SituationalTileKeys.POST_PEACE_OTHER)
            val tiles = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(tiles.contains(SituationalTileKeys.POST_PEACE_OTHER))
            assertFalse(tiles.contains(SituationalTileKeys.POST_PEACE_HIGHER))

            // Toggling POST_PEACE_HIGHER replaces POST_PEACE_OTHER
            viewModel.toggleTile(SituationalTileKeys.POST_PEACE_HIGHER)
            val tilesReplaced = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(tilesReplaced.contains(SituationalTileKeys.POST_PEACE_HIGHER))
            assertFalse(tilesReplaced.contains(SituationalTileKeys.POST_PEACE_OTHER))
        }

    @Test
    fun testStationSelectionRetainsFieldCollisionWithPeace() =
        runTest {
            val payslip = createMockStationPayslip("03/2026", basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            // Select POST_FIELD_HAFAA while POST_PEACE_HIGHER is active -> retains both to trigger recovery hazard
            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            val tilesWithCollision = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(tilesWithCollision.contains(SituationalTileKeys.POST_PEACE_HIGHER))
            assertTrue(tilesWithCollision.contains(SituationalTileKeys.POST_FIELD_HAFAA))
            assertTrue(viewModel.uiState.value.hazardTotal > 0.0, "TPTA + HAFAA collision must trigger recovery hazard")

            // Switching peace station to POST_PEACE_OTHER replaces POST_PEACE_HIGHER but retains POST_FIELD_HAFAA
            viewModel.toggleTile(SituationalTileKeys.POST_PEACE_OTHER)
            val tilesSwitched = viewModel.uiState.value.activeContext.activeTileIds
            assertTrue(tilesSwitched.contains(SituationalTileKeys.POST_PEACE_OTHER))
            assertFalse(tilesSwitched.contains(SituationalTileKeys.POST_PEACE_HIGHER))
            assertTrue(tilesSwitched.contains(SituationalTileKeys.POST_FIELD_HAFAA))
            assertTrue(viewModel.uiState.value.hazardTotal > 0.0)

            // Toggling off HAFAA removes collision hazard
            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            assertFalse(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.POST_FIELD_HAFAA))
            assertTrue(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.POST_PEACE_OTHER))
            assertEquals(0.0, viewModel.uiState.value.hazardTotal)
        }

    @Test
    fun testCeaChildCountRadioSingleChoiceSemantics() =
        runTest {
            val payslip = createMockStationPayslip("03/2026", basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            // Select 1 child
            viewModel.toggleTile(SituationalTileKeys.CEA_ONE_CHILD)
            assertEquals(1, viewModel.uiState.value.activeContext.numberOfChildrenCea)
            assertTrue(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.CEA_ONE_CHILD))

            // Select 2 children -> replaces 1 child
            viewModel.toggleTile(SituationalTileKeys.CEA_TWO_CHILDREN)
            assertEquals(2, viewModel.uiState.value.activeContext.numberOfChildrenCea)
            assertTrue(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.CEA_TWO_CHILDREN))
            assertFalse(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.CEA_ONE_CHILD))

            // Select CEA_NONE -> resets children count and clears CEA_TWO_CHILDREN
            viewModel.toggleTile(SituationalTileKeys.CEA_NONE)
            assertEquals(0, viewModel.uiState.value.activeContext.numberOfChildrenCea)
            assertTrue(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.CEA_NONE))
            assertFalse(viewModel.uiState.value.activeContext.activeTileIds.contains(SituationalTileKeys.CEA_TWO_CHILDREN))
        }

    private fun createMockStationPayslip(
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

    private class MockStationAssetProvider : PcdaoAssetProvider {
        override suspend fun loadAsset(fileName: String): String = "{}"
    }
}
