package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The one upgrade-sheet wiring every screen shares. Settings routes the sheet's privacy link to the in-app
 * policy instead of the web page, so that hook has to survive the move into the shared composable.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayslipUpgradeSheetUiTest {
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
    fun theSheetsPrivacyLinkRunsTheHookTheScreenPassed() =
        runComposeUiTest {
            var opened = 0
            setContent { PayslipUpgradeSheet(viewModel, onDismiss = {}, onPrivacyClick = { opened++ }) }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(AppStrings.settingsHelpPrivacyTitle).performSemanticsAction(SemanticsActions.OnClick)

            assertEquals(1, opened)
        }
}
