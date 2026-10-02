package com.payslipmax.pdfparser.debugseed

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.database.PayslipDao
import com.payslipmax.pdfparser.di.variantModules
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.WithTestKoin
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.screens.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * The debug build's real wiring, end to end: the debug variant modules plus the two shared collaborators the
 * seed needs, rendered by the production Settings screen. A binding the section cannot resolve would crash
 * Settings only in a debug build, which nothing else here would catch.
 */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebugSeedWiringUiTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        ContextHolder.context = RuntimeEnvironment.getApplication()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        ContextHolder.context = null
    }

    @Test
    fun settingsOffersTheSeedSectionResolvedEntirelyThroughKoin() =
        runComposeUiTest {
            val dao = FakePayslipDao()
            val shared =
                module {
                    single<PayslipDao> { dao }
                    single { FinancialIntelligenceRepository(get(), Dispatchers.Unconfined) }
                }
            val viewModel = PayslipViewModel(PayslipRepository(dao, FakePdfParser(), testDispatcher))
            testDispatcher.scheduler.runCurrent()
            setContent { WithTestKoin(shared, *variantModules.toTypedArray()) { SettingsScreen(viewModel = viewModel, onNavigateTo = {}) } }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(DebugSeedStrings.TITLE).performScrollTo().assertIsDisplayed()
            onNodeWithText(DebugSeedStrings.label(SeedStep.entries.first())).performScrollTo().assertIsDisplayed()
        }
}
