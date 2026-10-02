package com.payslipmax.pdfparser.ui.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.WithTestKoin
import com.payslipmax.pdfparser.ui.PayslipViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.koin.dsl.bind
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val FIRST_LABEL = "first developer section"
private const val SECOND_LABEL = "second developer section"

private class FirstSection : DeveloperToolsSection {
    @Composable
    override fun Content() = Text(FIRST_LABEL)
}

private class SecondSection : DeveloperToolsSection {
    @Composable
    override fun Content() = Text(SECOND_LABEL)
}

/** Settings shows whatever developer sections Koin provides, and nothing when it provides none (release). */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsDeveloperToolsUiTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ContextHolder.context = RuntimeEnvironment.getApplication()
        viewModel = PayslipViewModel(PayslipRepository(FakePayslipDao(), FakePdfParser(), testDispatcher))
        testDispatcher.scheduler.runCurrent()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        ContextHolder.context = null
    }

    @Test
    fun settingsRendersEveryDeveloperSectionKoinProvides() =
        runComposeUiTest {
            val sections =
                module {
                    factory { FirstSection() } bind DeveloperToolsSection::class
                    factory { SecondSection() } bind DeveloperToolsSection::class
                }
            setContent { WithTestKoin(sections) { SettingsScreen(viewModel = viewModel, onNavigateTo = {}) } }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(FIRST_LABEL).performScrollTo().assertIsDisplayed()
            onNodeWithText(SECOND_LABEL).performScrollTo().assertIsDisplayed()
        }

    @Test
    fun settingsRendersNoDeveloperSectionWhenKoinProvidesNone() =
        runComposeUiTest {
            setContent { WithTestKoin(module { }) { SettingsScreen(viewModel = viewModel, onNavigateTo = {}) } }
            testDispatcher.scheduler.runCurrent()

            assertEquals(0, onAllNodesWithText("developer section", substring = true).fetchSemanticsNodes().size)
        }
}
