package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.database.PayslipPdfEntity
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.repository.PayslipRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import com.payslipmax.pdfparser.testing.FakePdfParser
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.OVER_CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.table
import com.payslipmax.pdfparser.ui.PayslipViewModel
import com.payslipmax.pdfparser.ui.selectHistoryDetailPayslip
import com.payslipmax.pdfparser.ui.theme.AppStrings
import com.payslipmax.pdfparser.ui.theme.LeaveBankStrings
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
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/**
 * The payslip detail screen as both platforms host it: the leave section follows the kill switch (it must stay invisible in a
 * release built with the flag off, even for a payslip that carries a table), a month without a table never shows it, and the
 * original PDF is fetched once and handed to the platform as bytes plus file name (it used to be fetched twice).
 */
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PayslipReplicaLeaveWiringTest {
    private val testDispatcher = StandardTestDispatcher()
    private val dec2025 = payslip(2025, 12, table(OVER_CAP_DAYS), file = "dec.pdf")
    private val jul2025 = payslip(2025, 7, leave = null, file = "jul.pdf")
    private val pdfBytes = byteArrayOf(1, 2, 3)
    private lateinit var viewModel: PayslipViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val dao = FakePayslipDao()
        runBlocking {
            listOf(dec2025, jul2025).forEach { dao.insertPayslip(it.toEncryptedEntity()) }
            dao.insertPayslipPdf(PayslipPdfEntity(dec2025.dateStr, pdfBytes))
        }
        viewModel = PayslipViewModel(PayslipRepository(dao, FakePdfParser(), Dispatchers.Unconfined))
        testDispatcher.scheduler.runCurrent()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun aDecemberPayslipShowsTheLeaveSectionExactlyWhenTheKillSwitchIsOn() =
        runComposeUiTest {
            viewModel.selectHistoryDetailPayslip(dec2025.dateStr)
            setContent { PayslipReplicaDetailScreen(viewModel = viewModel, onBack = {}) }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(AppStrings.pdfCardTapHint).assertExists()
            val section = onNodeWithText(LeaveBankStrings.sectionTitle)
            if (isLeaveBankEnabled()) section.assertExists() else section.assertDoesNotExist()
        }

    @Test
    fun aMonthWithoutATableNeverShowsTheLeaveSection() =
        runComposeUiTest {
            viewModel.selectHistoryDetailPayslip(jul2025.dateStr)
            setContent { PayslipReplicaDetailScreen(viewModel = viewModel, onBack = {}) }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(AppStrings.pdfCardTapHint).assertExists()
            onNodeWithText(LeaveBankStrings.sectionTitle).assertDoesNotExist()
        }

    @Test
    fun openingTheOriginalHandsTheStoredBytesAndFileNameToThePlatformOnce() =
        runComposeUiTest {
            val opened = mutableListOf<Pair<ByteArray, String>>()
            viewModel.selectHistoryDetailPayslip(dec2025.dateStr)
            setContent { PayslipReplicaDetailScreen(viewModel = viewModel, onBack = {}, onOpenPdf = { bytes, name -> opened += bytes to name }) }
            testDispatcher.scheduler.runCurrent()

            onNodeWithText(AppStrings.pdfCardTapHint).performClick()
            testDispatcher.scheduler.runCurrent()
            waitForIdle()

            assertEquals(1, opened.size)
            assertContentEquals(pdfBytes, opened.single().first)
            assertEquals("dec.pdf", opened.single().second)
        }
}
