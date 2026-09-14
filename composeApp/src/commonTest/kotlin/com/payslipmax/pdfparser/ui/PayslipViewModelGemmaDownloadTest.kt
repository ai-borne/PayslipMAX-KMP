package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.insights.gemma.BaseModelInstallState
import com.payslipmax.pdfparser.insights.gemma.GemmaBaseModelInstaller
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * TDD tests for Gemma Base Model background install lifecycle in [PayslipViewModel].
 * Verifies that NeedsUserConfirmation / WaitingForWifi states are mapped to [isWaitingForWifi]
 * rather than confusing users with false fatal download errors.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelGemmaDownloadTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeInstaller = FakeGemmaInstaller()
    private lateinit var viewModel: PayslipViewModel

    private class FakeGemmaInstaller : GemmaBaseModelInstaller {
        private val _state = MutableStateFlow<BaseModelInstallState>(BaseModelInstallState.NotStarted)
        override val state: StateFlow<BaseModelInstallState> = _state.asStateFlow()
        var installCalled = false

        override suspend fun install() {
            installCalled = true
        }

        fun emitState(newState: BaseModelInstallState) {
            _state.value = newState
        }
    }

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val repository = PayslipRepository(FakePayslipDao(), FakePdfParser(), Dispatchers.Unconfined)
        viewModel =
            PayslipViewModel(
                repository = repository,
                gemmaBaseModelInstaller = fakeInstaller,
            )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun needsUserConfirmationSetsWaitingForWifiState() =
        runTest {
            fakeInstaller.emitState(BaseModelInstallState.NeedsUserConfirmation)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.isWaitingForWifi, "NeedsUserConfirmation should set isWaitingForWifi = true")
            assertFalse(state.isDownloadingModel, "NeedsUserConfirmation should not report active downloading")
            assertNull(state.modelDownloadError, "Waiting for Wi-Fi / confirmation is not an error")
        }

    @Test
    fun downloadingStateClearsWaitingForWifiAndSetsProgress() =
        runTest {
            fakeInstaller.emitState(BaseModelInstallState.NeedsUserConfirmation)
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isWaitingForWifi)

            fakeInstaller.emitState(BaseModelInstallState.Downloading(0.45f))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isWaitingForWifi, "Downloading should clear isWaitingForWifi")
            assertTrue(state.isDownloadingModel, "Downloading should set isDownloadingModel = true")
            assertEquals(0.45f, state.modelDownloadProgress)
            assertNull(state.modelDownloadError)
        }

    @Test
    fun installedStateClearsAllDownloadingFlags() =
        runTest {
            fakeInstaller.emitState(BaseModelInstallState.Installed("/path/to/model.bin"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isWaitingForWifi)
            assertFalse(state.isDownloadingModel)
            assertEquals(1f, state.modelDownloadProgress)
            assertNull(state.modelDownloadError)
        }

    @Test
    fun failedStateSetsErrorAndClearsWaitingAndDownloading() =
        runTest {
            fakeInstaller.emitState(BaseModelInstallState.Failed("Disk full"))
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertFalse(state.isWaitingForWifi)
            assertFalse(state.isDownloadingModel)
            assertEquals("Disk full", state.modelDownloadError)
        }

    @Test
    fun resumeModelDownloadTriggersInstallerInstall() =
        runTest {
            fakeInstaller.installCalled = false
            viewModel.resumeModelDownload()
            advanceUntilIdle()

            assertTrue(fakeInstaller.installCalled, "resumeModelDownload must invoke installer.install()")
        }

    @Test
    fun dismissModelBannerSetsDismissedFlag() =
        runTest {
            assertFalse(viewModel.uiState.value.isModelBannerDismissed)

            viewModel.dismissModelBanner()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isModelBannerDismissed)
        }

    @Test
    fun resumeModelDownloadResetsDismissedFlag() =
        runTest {
            viewModel.dismissModelBanner()
            advanceUntilIdle()
            assertTrue(viewModel.uiState.value.isModelBannerDismissed)

            viewModel.resumeModelDownload()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isModelBannerDismissed)
            assertTrue(fakeInstaller.installCalled)
        }
}
