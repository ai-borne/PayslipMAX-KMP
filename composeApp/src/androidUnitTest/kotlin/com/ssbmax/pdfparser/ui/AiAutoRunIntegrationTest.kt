package com.ssbmax.pdfparser.ui

import com.ssbmax.pdfparser.database.toEncryptedEntity
import com.ssbmax.pdfparser.repository.PayslipRepository
import com.ssbmax.pdfparser.testing.FakePayslipDao
import com.ssbmax.pdfparser.testing.FakePdfParser
import io.ktor.client.network.sockets.SocketTimeoutException
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for the AI auto-run guard using [FakeFinancialIntelligenceRepository]
 * (no real HTTP). Verifies the full condition-check → trigger → state-update chain.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiAutoRunIntegrationTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var fakeFinancialRepo: FakeFinancialIntelligenceRepository

    private val helper = AiAutoRunGuardTest()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, FakePdfParser(), Dispatchers.Unconfined)
        fakeFinancialRepo = FakeFinancialIntelligenceRepository(fakeDao = fakeDao)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = PayslipViewModel(repository, fakeBackupManager, fakeFinancialRepo)

    // ── Premium ON + no cache → auto-run fires ────────────────────────────────

    @Test
    fun testAutoRunFiresWhenPremiumOnAndNoCache() =
        runTest {
            val vm = createViewModel()
            fakeDao.insertPayslip(helper.createMockPayslip("08/2024").toEncryptedEntity())
            advanceUntilIdle()

            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isPremiumEnabled)
            assertFalse(vm.uiState.value.isAiLoading)
            assertNotNull(vm.uiState.value.aiInsights)
            assertEquals("Fake AI narrative", vm.uiState.value.aiInsights)
            assertEquals(1, fakeFinancialRepo.generateCallCount)
        }

    // ── Cache exists → no auto-run ────────────────────────────────────────────

    @Test
    fun testAutoRunNotFiredWhenCacheExists() =
        runTest {
            fakeFinancialRepo.cachedInsightsByMonth["08/2024"] = "Cached insights"
            val vm = createViewModel()
            fakeDao.insertPayslip(helper.createMockPayslip("08/2024").toEncryptedEntity())
            advanceUntilIdle()

            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isPremiumEnabled)
            assertEquals("Cached insights", vm.uiState.value.aiInsights)
            assertEquals(0, fakeFinancialRepo.generateCallCount)
        }

    // ── Month switch to uncached month → auto-run fires ───────────────────────

    @Test
    fun testAutoRunFiresOnMonthSwitchWithNoCache() =
        runTest {
            val vm = createViewModel()
            val payslip1 = helper.createMockPayslip("07/2024")
            val payslip2 = helper.createMockPayslip("08/2024")
            fakeDao.insertPayslip(payslip1.toEncryptedEntity())
            fakeDao.insertPayslip(payslip2.toEncryptedEntity())
            advanceUntilIdle()

            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            // Switch to older month — no cache for it
            vm.clearAiInsights()
            vm.selectPayslip(payslip1)
            advanceUntilIdle()

            assertNotNull(vm.uiState.value.aiInsights)
            assertEquals("07/2024", vm.uiState.value.selectedPayslip?.dateStr)
        }

    // ── AI generation fails → aiError is set, not a crash ────────────────────

    @Test
    fun testAutoRunSetsAiErrorOnFailure() =
        runTest {
            fakeFinancialRepo.narrativeResult = Result.failure(Exception("Gemini error"))
            val vm = createViewModel()
            fakeDao.insertPayslip(helper.createMockPayslip("08/2024").toEncryptedEntity())
            advanceUntilIdle()

            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isPremiumEnabled)
            assertNull(vm.uiState.value.aiInsights)
            assertFalse(vm.uiState.value.isAiLoading)
            assertEquals(
                "Unable to generate AI insights due to a connection issue. Please check your network and try again.",
                vm.uiState.value.aiError,
            )
        }

    @Test
    fun testAutoRunSetsFriendlyTimeoutErrorOnSocketTimeout() =
        runTest {
            fakeFinancialRepo.narrativeResult = Result.failure(SocketTimeoutException("timeout"))
            val vm = createViewModel()
            fakeDao.insertPayslip(helper.createMockPayslip("08/2024").toEncryptedEntity())
            advanceUntilIdle()

            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            assertTrue(vm.uiState.value.isPremiumEnabled)
            assertNull(vm.uiState.value.aiInsights)
            assertFalse(vm.uiState.value.isAiLoading)
            assertEquals(
                "The server is taking too long to respond. Please check your internet connection and try again.",
                vm.uiState.value.aiError,
            )
        }

    // ── Same month re-select → no double-run (cache guard) ───────────────────

    @Test
    fun testAutoRunDoesNotDoubleFireForSameMonth() =
        runTest {
            val vm = createViewModel()
            val payslip = helper.createMockPayslip("08/2024")
            fakeDao.insertPayslip(payslip.toEncryptedEntity())
            advanceUntilIdle()

            vm.setPremiumEnabled(true)
            advanceUntilIdle()

            // First run completed — cache is now populated inside fakeFinancialRepo
            assertEquals(1, fakeFinancialRepo.generateCallCount)
            assertNotNull(vm.uiState.value.aiInsights)

            // Re-select the same payslip
            vm.selectPayslip(payslip)
            advanceUntilIdle()

            // Generate must NOT have been called again (cache hit)
            assertEquals(1, fakeFinancialRepo.generateCallCount)
        }
}
