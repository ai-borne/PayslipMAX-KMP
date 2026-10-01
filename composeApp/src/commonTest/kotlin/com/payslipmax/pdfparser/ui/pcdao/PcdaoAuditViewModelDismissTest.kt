package com.payslipmax.pdfparser.ui.pcdao

import com.payslipmax.pcdao.engine.AllowanceCollisionCodes
import com.payslipmax.pcdao.model.AuditDiscrepancy
import com.payslipmax.pcdao.model.DiscrepancySeverity
import com.payslipmax.pcdao.model.DiscrepancyType
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciliationResult
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
class PcdaoAuditViewModelDismissTest {
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
        mockAssetProvider = MockDismissAssetProvider()
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
    fun testDismissDiscrepancyExcludesFromFilteredDiscrepancies() =
        runTest {
            val payslip = createMockPayslip("03/2026", 2026, 3, basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)

            val initialDiscrepancies = viewModel.uiState.value.filteredDiscrepancies
            assertTrue(
                initialDiscrepancies.any { it.id == AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE },
                "Should contain TPTA field collision alarm initially",
            )
            assertTrue(viewModel.uiState.value.dismissedDiscrepancyIds.isEmpty())

            viewModel.dismissDiscrepancy(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE)

            val updatedDiscrepancies = viewModel.uiState.value.filteredDiscrepancies
            assertFalse(
                updatedDiscrepancies.any { it.id == AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE },
                "Filtered discrepancies must exclude the dismissed alarm",
            )
            assertTrue(
                viewModel.uiState.value.dismissedDiscrepancyIds.contains(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE),
            )
        }

    @Test
    fun testDismissAlarmDecrementsAlarmsCountInRealTime() =
        runTest {
            val payslip = createMockPayslip("03/2026", 2026, 3, basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)

            val initialAlarms = viewModel.uiState.value.alarmsCount
            assertTrue(initialAlarms >= 1, "Should have at least 1 alarm for TPTA in field")

            viewModel.dismissDiscrepancy(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE)

            val updatedAlarms = viewModel.uiState.value.alarmsCount
            assertEquals(initialAlarms - 1, updatedAlarms, "Dismissing alarm must decrement alarmsCount in real time")
        }

    @Test
    fun testResetDismissedDiscrepanciesRestoresActiveAlarms() =
        runTest {
            val payslip = createMockPayslip("03/2026", 2026, 3, basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip.toEncryptedEntity())

            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            val initialAlarms = viewModel.uiState.value.alarmsCount

            viewModel.dismissDiscrepancy(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE)
            assertEquals(initialAlarms - 1, viewModel.uiState.value.alarmsCount)

            viewModel.resetDismissedDiscrepancies()

            assertTrue(viewModel.uiState.value.dismissedDiscrepancyIds.isEmpty())
            assertEquals(initialAlarms, viewModel.uiState.value.alarmsCount)
            assertTrue(
                viewModel.uiState.value.filteredDiscrepancies.any {
                    it.id == AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE
                },
            )
        }

    @Test
    fun testDismissalPreservedAcrossMonthSelection() =
        runTest {
            val payslip1 = createMockPayslip("03/2026", 2026, 3, basicPay = 69000.0, tpta = 7200.0)
            val payslip2 = createMockPayslip("02/2026", 2026, 2, basicPay = 69000.0, tpta = 7200.0)
            fakeDao.insertPayslip(payslip1.toEncryptedEntity())
            fakeDao.insertPayslip(payslip2.toEncryptedEntity())

            // Select month 1 and trigger HAFAA alarm
            viewModel.selectPayslip(payslip1)
            viewModel.toggleTile(SituationalTileKeys.POST_FIELD_HAFAA)
            viewModel.dismissDiscrepancy(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE)
            assertTrue(
                viewModel.uiState.value.dismissedDiscrepancyIds.contains(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE),
            )

            // Switch to month 2
            viewModel.selectPayslip(payslip2)
            assertFalse(
                viewModel.uiState.value.dismissedDiscrepancyIds.contains(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE),
                "Month 2 should have independent dismissal state",
            )

            // Switch back to month 1
            viewModel.selectPayslip(payslip1)
            assertTrue(
                viewModel.uiState.value.dismissedDiscrepancyIds.contains(AllowanceCollisionCodes.ALARM_TPTA_FIELD_CONVEYANCE),
                "Month 1 dismissal must be preserved across month selection",
            )
        }

    @Test
    fun testUiStateDirectFilteringAndAlarmsCount() {
        val alarmDiscrepancy =
            AuditDiscrepancy(
                id = "ALARM_1",
                title = "Alarm Advisory",
                type = DiscrepancyType.FORFEITURE_RISK,
                severity = DiscrepancySeverity.WARNING,
                entitledAmount = 7200.0,
                drawnAmount = 7200.0,
                netDue = 0.0,
                annualImpact = 0.0,
                authority = "TR-230(B)",
                explanation = "Advisory",
                recommendedAction = "Action",
            )
        val underpaymentDiscrepancy =
            AuditDiscrepancy(
                id = "UNDERPAY_1",
                title = "DA Arrears",
                type = DiscrepancyType.UNDERPAYMENT,
                severity = DiscrepancySeverity.WARNING,
                entitledAmount = 34500.0,
                drawnAmount = 30000.0,
                netDue = 4500.0,
                annualImpact = 54000.0,
                authority = "DA Rule",
                explanation = "Arrears",
                recommendedAction = "Action",
            )
        val recon =
            ShadowLedgerReconciliationResult(
                lineItems = emptyList(),
                discrepancies = listOf(alarmDiscrepancy, underpaymentDiscrepancy),
                totalUnclaimedAnnual = 54000.0,
                totalRecoveryHazard = 0.0,
                criticalAlarmCount = 0,
                summaryMessage = "Test",
            )

        val initialState =
            PcdaoAuditUiState(
                reconciliationResult = recon,
                dismissedDiscrepancyIds = emptySet(),
            )
        assertEquals(1, initialState.alarmsCount)
        assertEquals(2, initialState.filteredDiscrepancies.size)

        val dismissedState = initialState.copy(dismissedDiscrepancyIds = setOf("ALARM_1"))
        assertEquals(0, dismissedState.alarmsCount)
        assertEquals(1, dismissedState.filteredDiscrepancies.size)
        assertEquals("UNDERPAY_1", dismissedState.filteredDiscrepancies.first().id)

        val alarmFilterState = dismissedState.copy(selectedFilter = FindingFilter.ALARMS)
        assertTrue(alarmFilterState.filteredDiscrepancies.isEmpty())
    }

    private fun createMockPayslip(
        dateStr: String,
        year: Int,
        monthNum: Int,
        basicPay: Double,
        tpta: Double,
    ): ParsedPayslip =
        ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "Month_$monthNum",
            dateStr = dateStr,
            officer = Officer("Col Test Officer", "01/142/987654", "ABCDE1234F"),
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = basicPay * 0.5,
                    militaryServicePay = 15500.0,
                    transportAllowance = tpta,
                ),
            deductions = Deductions(dsopSubscription = 20000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(basicPay + basicPay * 0.5 + 15500.0 + tpta, 20000.0, basicPay),
            taxAndSavings = null,
        )

    private class MockDismissAssetProvider : PcdaoAssetProvider {
        override suspend fun loadAsset(fileName: String): String = "{}"
    }
}
