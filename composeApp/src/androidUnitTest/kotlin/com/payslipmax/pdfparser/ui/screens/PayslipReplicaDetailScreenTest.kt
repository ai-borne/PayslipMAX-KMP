package com.payslipmax.pdfparser.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
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
import com.payslipmax.pdfparser.ui.FakeBackupManager
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.selectHistoryDetailPayslip
import com.payslipmax.pdfparser.ui.startEditingSession
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves the lifecycle guarantees Phase 2 adds ahead of the Phase 3 nav cutover: cleanup runs on
 * every exit path (not just save/cancel), and the visible back arrow shares the same mid-edit
 * two-step logic as system back (decision 4) — the fix for the latent cross-payslip correction bug.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayslipReplicaDetailScreenTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    private val testDateStr = "08/2024"

    private fun mockPayslip() =
        ParsedPayslip(
            file = "test.pdf",
            year = 2024,
            monthNum = 8,
            monthName = "August",
            dateStr = testDateStr,
            officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
            earnings = Earnings(basicPay = 100000.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
            taxAndSavings = null,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        runBlocking { fakeDao.insertPayslip(mockPayslip().toEncryptedEntity()) }
        viewModel = PayslipViewModel(repository, fakeBackupManager)
        testDispatcher.scheduler.runCurrent()
        viewModel.selectHistoryDetailPayslip(testDateStr)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLeavingCompositionMidEditClearsEditSessionState() =
        runComposeUiTest {
            var mounted by mutableStateOf(true)
            setContent {
                if (mounted) {
                    PayslipReplicaDetailScreen(viewModel = viewModel, onBack = {})
                }
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithContentDescription("Start Editing").performClick()
            testDispatcher.scheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isEditModeActive)

            // Leave by a means other than the arrow/system back (e.g. native swipe bypassing
            // BackHandler) — the DisposableEffect, not the gesture, must clean up.
            mounted = false
            testDispatcher.scheduler.runCurrent()
            waitForIdle()

            assertFalse(viewModel.uiState.value.isEditModeActive)
            assertTrue(viewModel.uiState.value.draftCorrections.isEmpty())
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBackArrowMidEditCancelsEditModeAndStaysOnScreen() =
        runComposeUiTest {
            var backInvoked = false
            setContent {
                PayslipReplicaDetailScreen(viewModel = viewModel, onBack = { backInvoked = true })
            }
            testDispatcher.scheduler.runCurrent()

            viewModel.startEditingSession(testDateStr)
            testDispatcher.scheduler.runCurrent()
            assertTrue(viewModel.uiState.value.isEditModeActive)

            onNodeWithContentDescription(AppStrings.btnBack).performClick()
            testDispatcher.scheduler.runCurrent()

            assertFalse(viewModel.uiState.value.isEditModeActive)
            assertFalse(backInvoked)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testBackArrowWhenNotEditingCallsOnBack() =
        runComposeUiTest {
            var backInvoked = false
            setContent {
                PayslipReplicaDetailScreen(viewModel = viewModel, onBack = { backInvoked = true })
            }
            testDispatcher.scheduler.runCurrent()

            onNodeWithContentDescription(AppStrings.btnBack).performClick()
            testDispatcher.scheduler.runCurrent()

            assertTrue(backInvoked)
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testUnresolvedPayslipIdExitsWithoutCrashing() =
        runComposeUiTest {
            var backInvoked = false
            viewModel.selectHistoryDetailPayslip("01/1999")
            setContent {
                PayslipReplicaDetailScreen(viewModel = viewModel, onBack = { backInvoked = true })
            }
            testDispatcher.scheduler.runCurrent()
            waitForIdle()

            assertTrue(backInvoked)
        }
}
