package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.insights.gemma.GemmaModelFileOps
import com.payslipmax.pdfparser.insights.gemma.GemmaModelManifest
import com.payslipmax.pdfparser.insights.gemma.GemmaModelStorageManager
import com.payslipmax.pdfparser.insights.gemma.GemmaModelVersionManager
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
 * The Gemma license requires its Terms-of-Use notice to be shown to the user at/before the model is
 * (re)distributed to their device. The notice travels in the version manifest (`noticeText`); this
 * test locks in that `setLocalAiEnabled` lifts it out of the fetched manifest into UI state so the
 * settings screen can surface it — regressing this silently drops a redistribution-notice
 * obligation, so it must be encoded, not assumed.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PayslipViewModelGemmaNoticeTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    private val noticeText = "Gemma is provided under and subject to the Gemma Terms of Use found at ai.google.dev/gemma/terms"

    private val manifest =
        GemmaModelManifest(
            version = "v1",
            url = "https://cdn/x.litertlm",
            sha256 = "abc123",
            noticeText = noticeText,
            noticeUrl = "https://ai.google.dev/gemma/terms",
        )

    // Returns the manifest directly, keeping the flow off the network so the notice-surfacing step
    // is exercised deterministically (no ktor async handoff to race the test scheduler).
    private inner class FakeVersionManager : GemmaModelVersionManager(interimKey = null) {
        override suspend fun fetchManifest(): Result<GemmaModelManifest> = Result.success(manifest)
    }

    // Minimal in-memory file ops: an active `.litertlm` slot already present and recorded at "v1",
    // so the manifest-version check short-circuits before any real download is attempted.
    private class AlreadyInstalledFileOps(private val version: String) : GemmaModelFileOps {
        override fun exists(path: String): Boolean = path.endsWith(".litertlm")

        override fun move(
            fromPath: String,
            toPath: String,
        ): Boolean = true

        override fun delete(path: String): Boolean = true

        override fun readText(path: String): String? = if (path.endsWith(".version")) version else null

        override fun writeText(
            path: String,
            content: String,
        ): Boolean = true

        override fun sha256(path: String): String? = null
    }

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
    fun enablingLocalAiSurfacesGemmaTermsNoticeFromManifest() =
        runTest {
            val storage =
                GemmaModelStorageManager(
                    storageDir = { "/models" },
                    fileOps = AlreadyInstalledFileOps(version = "v1"),
                    isDeviceSupported = { true },
                )

            viewModel.setLocalAiEnabled(
                enabled = true,
                storage = storage,
                versionManager = FakeVersionManager(),
            )
            advanceUntilIdle()

            // Already on v1 → no download happened, but the license notice must still be surfaced.
            assertEquals(noticeText, viewModel.uiState.value.modelDownloadNotice)
            assertEquals(false, viewModel.uiState.value.isDownloadingModel)
        }
}
