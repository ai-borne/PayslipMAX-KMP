package com.ssbmax.pdfparser.ui.screens

import androidx.compose.ui.test.*
import com.ssbmax.pdfparser.repository.PayslipRepository
import com.ssbmax.pdfparser.testing.FakePayslipDao
import com.ssbmax.pdfparser.testing.FakePdfParser
import com.ssbmax.pdfparser.ui.FakeBackupManager
import com.ssbmax.pdfparser.ui.PayslipViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34]) // Run on API 34 to match local SDK compatibility
class DashboardScreenUiTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var fakeGeminiService: com.ssbmax.pdfparser.insights.GeminiService
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        fakeBackupManager = FakeBackupManager()
        fakeGeminiService = com.ssbmax.pdfparser.insights.GeminiService()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository, fakeBackupManager, fakeGeminiService)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        org.koin.core.context.stopKoin()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testLoadingState() =
        runComposeUiTest {
            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdfTrigger = {},
                )
            }

            // Verify the progress spinner (dashboard_loading tag) exists
            onNodeWithTag("dashboard_loading").assertExists()
            // Verify other states are not shown
            onNodeWithTag("dashboard_empty").assertDoesNotExist()
            onNodeWithTag("dashboard_populated").assertDoesNotExist()
        }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testEmptyState() =
        runComposeUiTest {
            // Advance the scheduler to let the viewModel's init/loading coroutines run and finish
            testDispatcher.scheduler.runCurrent()

            setContent {
                DashboardScreen(
                    viewModel = viewModel,
                    onPickPdfTrigger = {},
                )
            }

            // Verify the empty state placeholder (dashboard_empty tag) exists
            onNodeWithTag("dashboard_empty").assertExists()
            // Verify loading indicator is gone
            onNodeWithTag("dashboard_loading").assertDoesNotExist()
        }
}
