package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.subscription.DevOverride
import com.payslipmax.pdfparser.subscription.FeatureGate
import com.payslipmax.pdfparser.subscription.isDebugBuild
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import kotlin.test.assertTrue

/**
 * Integration tests verifying the deterministic offline intelligence engine integration with
 * the ViewModel. Cloud Gemini / AI auto-run removed; tests now cover offline premium gate
 * and payslip selection flow.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAutoRunIntegrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var repository: PayslipRepository
    private lateinit var fakeFinancialRepo: FakeFinancialIntelligenceRepository

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        repository = PayslipRepository(fakeDao, FakePdfParser(), Dispatchers.Unconfined)
        fakeFinancialRepo = FakeFinancialIntelligenceRepository(fakeDao = fakeDao)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PayslipViewModel(repository, fakeFinancialRepo)

    private fun buildPayslip(dateStr: String): ParsedPayslip {
        val parts = dateStr.split("/")
        val month = parts[0].toInt()
        val year = parts[1].toInt()
        return ParsedPayslip(
            file = "payslip_$dateStr.pdf",
            year = year,
            monthNum = month,
            monthName = "Month_$month",
            dateStr = dateStr,
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(basicPay = 100.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(100.0, 80.0, 20.0),
            taxAndSavings = null,
        )
    }

    // ── Premium flag ON → isPremiumEnabled reflects it ────────────────────────

    @Test
    fun testPremiumFlagOnReflectedInUiState() =
        runTest {
            val vm = createViewModel()
            fakeDao.insertPayslip(buildPayslip("08/2024").toEncryptedEntity())
            advanceUntilIdle()

            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isPremiumEnabled)
        }

    // ── Premium flag OFF → isPremiumEnabled is false ──────────────────────────

    @Test
    fun testPremiumFlagOffReflectedInUiState() =
        runTest {
            val vm = createViewModel()
            vm.setPremiumEnabled(false)
            advanceUntilIdle()

            assertFalse(vm.uiState.value.isPremiumEnabled)
        }

    // ── Payslip loads → selectedPayslip is set ────────────────────────────────

    @Test
    fun testSelectedPayslipSetAfterLoad() =
        runTest {
            val vm = createViewModel()
            fakeDao.insertPayslip(buildPayslip("08/2024").toEncryptedEntity())
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.selectedPayslip)
            assertEquals("08/2024", vm.uiState.value.selectedPayslip?.dateStr)
        }

    // ── Month switch updates selectedPayslip ──────────────────────────────────

    @Test
    fun testMonthSwitchUpdatesSelectedPayslip() =
        runTest {
            val vm = createViewModel()
            val payslip1 = buildPayslip("07/2024")
            val payslip2 = buildPayslip("08/2024")
            fakeDao.insertPayslip(payslip1.toEncryptedEntity())
            fakeDao.insertPayslip(payslip2.toEncryptedEntity())
            advanceUntilIdle()

            vm.selectPayslip(payslip1)
            advanceUntilIdle()

            assertEquals("07/2024", vm.uiState.value.selectedPayslip?.dateStr)
        }

    // ── DevOverride.FORCE_FREE overrides premium gate (debug builds only) ─────

    @Test
    fun testForceFreeSuppressesPremiumAccess() =
        runTest {
            if (!isDebugBuild()) return@runTest

            val vm = createViewModel()
            vm.setDevOverride(DevOverride.FORCE_FREE)
            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            assertFalse(vm.hasAccess(FeatureGate.PREMIUM_INTELLIGENCE))
        }
}
