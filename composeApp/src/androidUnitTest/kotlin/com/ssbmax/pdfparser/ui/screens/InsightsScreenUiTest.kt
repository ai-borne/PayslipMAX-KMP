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
@Config(sdk = [34])
class InsightsScreenUiTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var fakeBackupManager: FakeBackupManager
    private lateinit var repository: PayslipRepository
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        fakeBackupManager = FakeBackupManager()
        repository = PayslipRepository(fakeDao, fakeParser, Dispatchers.Unconfined)
        viewModel = PayslipViewModel(repository, fakeBackupManager)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        org.koin.core.context.stopKoin()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun testEmptyState() =
        runComposeUiTest {
            testDispatcher.scheduler.runCurrent()
            setContent {
                InsightsScreen(
                    viewModel = viewModel,
                    onNavigateTo = {},
                )
            }
            onNodeWithText("Please import or select a payslip to unlock financial insights.").assertExists()
        }
}
