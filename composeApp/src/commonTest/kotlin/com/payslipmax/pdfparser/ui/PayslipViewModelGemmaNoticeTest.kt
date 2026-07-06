package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.repository.PayslipRepository
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

/**
 * `setLocalAiEnabled` is now a pure preference flip: it only controls which source
 * (local Gemma vs. cloud Gemini) [FinancialIntelligenceRepository]'s narrative insight generation
 * reads from. The Tier 6 base model's own mandatory download is a separate, unconditional
 * `PayslipViewModel.init` trigger (`GemmaBaseModelInstaller`) — it no longer piggybacks on this
 * toggle at all, so this test locks in that the toggle does nothing but persist the setting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelGemmaNoticeTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repository = PayslipRepository(FakePayslipDao(), FakePdfParser(), Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository, FakeBackupManager())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun enablingLocalAiPersistsThePreferenceOnly() =
        runTest {
            viewModel.setLocalAiEnabled(true)
            advanceUntilIdle()

            assertEquals(true, viewModel.uiState.value.useLocalAi)
            assertEquals(false, viewModel.uiState.value.isDownloadingModel)
        }

    @Test
    fun disablingLocalAiPersistsThePreferenceOnly() =
        runTest {
            viewModel.setLocalAiEnabled(true)
            advanceUntilIdle()

            viewModel.setLocalAiEnabled(false)
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.useLocalAi)
        }
}
