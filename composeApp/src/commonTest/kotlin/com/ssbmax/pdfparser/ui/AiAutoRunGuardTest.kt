package com.ssbmax.pdfparser.ui

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import com.ssbmax.pdfparser.database.toEncryptedEntity
import com.ssbmax.pdfparser.domain.Deductions
import com.ssbmax.pdfparser.domain.DsopFund
import com.ssbmax.pdfparser.domain.Earnings
import com.ssbmax.pdfparser.domain.LedgerBalances
import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.domain.PayslipSummary
import com.ssbmax.pdfparser.domain.TaxAndSavings
import com.ssbmax.pdfparser.insights.EngineResult
import com.ssbmax.pdfparser.insights.WealthOptimizationEngine
import com.ssbmax.pdfparser.repository.PayslipRepository
import com.ssbmax.pdfparser.testing.FakePayslipDao
import com.ssbmax.pdfparser.testing.FakePdfParser
import com.ssbmax.pdfparser.ui.screens.InsightsState
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
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Platform-agnostic guard tests (no HTTP). HTTP-dependent auto-run integration
 * tests live in AiAutoRunIntegrationTest (androidUnitTest only).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAutoRunGuardTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, FakePdfParser(), Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── Guard: premium OFF → no auto-run ─────────────────────────────────────

    @Test
    fun testAutoRunNotFiredWhenPremiumOff() =
        runTest {
            val vm = PayslipViewModel(repository, fakeBackupManager)
            fakeDao.insertPayslip(createMockPayslip("08/2024").toEncryptedEntity())
            runCurrent()

            assertFalse(vm.uiState.value.isPremiumEnabled)
            assertFalse(vm.uiState.value.isAiLoading)
            assertNull(vm.uiState.value.aiInsights)
        }

    // ── Guard: no financial repo → no auto-run regardless of premium ──────────

    @Test
    fun testAutoRunNotFiredWhenNoFinancialRepo() =
        runTest {
            val vm = PayslipViewModel(repository, fakeBackupManager, financialIntelligenceRepository = null)
            fakeDao.insertPayslip(createMockPayslip("08/2024").toEncryptedEntity())
            runCurrent()

            vm.setPremiumEnabled(true)
            runCurrent()

            assertTrue(vm.uiState.value.isPremiumEnabled)
            assertNull(vm.uiState.value.aiInsights)
            assertFalse(vm.uiState.value.isAiLoading)
            assertNull(vm.uiState.value.aiError)
        }

    // ── InsightsState optimization mapping ───────────────────────────────────

    @Test
    fun testInsightsStateContainsOptimizationResult() {
        val payslip = createMockPayslip("08/2024")
        val expectedOpt = WealthOptimizationEngine.analyze(payslip)

        val state =
            InsightsState(
                currentRecord =
                    LedgerRecordEntity(
                        dateStr = "08/2024", year = 2024, monthNum = 8,
                        basicPay = 60000.0, dearnessAllowance = 15000.0, militaryServicePay = 15500.0,
                        transportAllowance = 3600.0, transportAllowanceDa = 1000.0,
                        houseRentAllowance = 12000.0, grossPay = 100000.0,
                        dsopSubscription = 5000.0, incomeTax = 4000.0, netPay = 70000.0,
                    ),
                previousRecord = null,
                historySorted = emptyList(),
                engineResult =
                    EngineResult(
                        healthScore = 80,
                        anomalies = emptyList(),
                        monthlySavingRate = 10.0,
                        taxRatio = 4.0,
                    ),
                scoreDelta = null,
                optimizationResult = expectedOpt,
                momChanges = emptyList(),
                previousMonthLabel = null,
            )

        assertEquals("OLD", state.optimizationResult.regimeAssumed)
        assertEquals(expectedOpt.totalPotentialTaxSaving, state.optimizationResult.totalPotentialTaxSaving)
        assertTrue(state.optimizationResult.totalPotentialTaxSaving >= 0.0)
    }

    @Test
    fun testOptimizationResultComputedFromPayslip() {
        val result = WealthOptimizationEngine.analyze(createMockPayslip("08/2024"))
        assertEquals("OLD", result.regimeAssumed)
        assertTrue(result.opportunities.isNotEmpty())
        assertTrue(result.totalPotentialTaxSaving >= 0.0)
    }

    @Test
    fun testOptimizationResultRegimeIsAlwaysOldForAnyMonth() {
        listOf("05/2024", "08/2024", "01/2025").forEach { dateStr ->
            val result = WealthOptimizationEngine.analyze(createMockPayslip(dateStr))
            assertEquals("OLD", result.regimeAssumed, "Regime must be OLD for $dateStr")
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    internal fun createMockPayslip(dateStr: String): ParsedPayslip {
        val (monthStr, yearStr) = dateStr.split("/")
        return ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = yearStr.toInt(),
            monthNum = monthStr.toInt(),
            monthName = "Month_$monthStr",
            dateStr = dateStr,
            officer = Officer("test Kumar", "12345678", "ABCDE1234F"),
            earnings =
                Earnings(
                    basicPay = 60000.0,
                    dearnessAllowance = 15000.0,
                    militaryServicePay = 15500.0,
                    transportAllowance = 3600.0,
                    transportAllowanceDa = 1000.0,
                    houseRentAllowance = 12000.0,
                ),
            deductions = Deductions(dsopSubscription = 5000.0, agif = 5000.0, incomeTax = 4000.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(100000.0, 70000.0, 30000.0),
            taxAndSavings =
                TaxAndSavings(
                    grossSalaryYtd = 600000.0,
                    totalTaxableIncome = 550000.0,
                    standardDeduction = 50000.0,
                    netTaxableIncome = 500000.0,
                    totalTaxPayable = 12500.0,
                    taxDeductedYtd = 10000.0,
                    cessDeductedYtd = 500.0,
                    dsopFund = DsopFund(5000.0, 5000.0, 0.0, 0.0, 0.0, 105000.0),
                ),
        )
    }
}
