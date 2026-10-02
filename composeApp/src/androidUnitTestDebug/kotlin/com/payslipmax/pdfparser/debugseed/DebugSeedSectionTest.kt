package com.payslipmax.pdfparser.debugseed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.database.toEncryptedEntity
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.repository.FinancialIntelligenceRepository
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The developer action: an explicit, labelled button seeds; another removes; a refusal says why. */
@OptIn(ExperimentalTestApi::class, ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DebugSeedSectionTest {
    private val dao = FakePayslipDao()
    private val seeder = DebugSeeder(dao, FinancialIntelligenceRepository(dao, Dispatchers.Unconfined))

    /** Settings hosts the section inside its own scrolling column; the test does the same. */
    @Composable
    private fun Scrollable(content: @Composable () -> Unit) = Column(Modifier.verticalScroll(rememberScrollState())) { content() }

    private fun stored() = runBlocking { dao.getAllPayslips().first().map { it.dateStr } }

    @Test
    fun theSectionIsLabelledSyntheticAndSeedsNothingUntilAButtonIsPressed() =
        runComposeUiTest {
            setContent { Scrollable { DebugSeedSection(DebugSeedViewModel(seeder, Dispatchers.Unconfined)) } }

            onNodeWithText(DebugSeedStrings.TITLE).assertIsDisplayed()
            assertTrue(stored().isEmpty(), "nothing is seeded automatically")
        }

    @Test
    fun pressingAScenarioButtonSeedsItsMonthsAndReportsThem() =
        runComposeUiTest {
            setContent { Scrollable { DebugSeedSection(DebugSeedViewModel(seeder, Dispatchers.Unconfined)) } }

            onNodeWithText(DebugSeedStrings.label(SeedStep.HELD_TPTA)).performScrollTo().performClick()
            waitForIdle()

            assertEquals(listOf("01/2018", "02/2018", "03/2018"), stored())
            onNodeWithText(DebugSeedStrings.seeded(listOf("01/2018", "02/2018", "03/2018"))).performScrollTo().assertIsDisplayed()
        }

    @Test
    fun aRefusedSeedSaysWhichMonthsBlockedItAndWritesNothing() =
        runComposeUiTest {
            runBlocking { dao.insertPayslip(realPayslip().toEncryptedEntity()) }
            setContent { Scrollable { DebugSeedSection(DebugSeedViewModel(seeder, Dispatchers.Unconfined)) } }

            onNodeWithText(DebugSeedStrings.label(SeedStep.HELD_TPTA)).performScrollTo().performClick()
            waitForIdle()

            onNodeWithText(DebugSeedStrings.collision(listOf("03/2018"))).performScrollTo().assertIsDisplayed()
            assertEquals(listOf("03/2018"), stored())
        }

    @Test
    fun removeSeedDataDeletesTheSeededMonthsOnly() =
        runComposeUiTest {
            runBlocking { dao.insertPayslip(realPayslip(2025, 8).toEncryptedEntity()) }
            setContent { Scrollable { DebugSeedSection(DebugSeedViewModel(seeder, Dispatchers.Unconfined)) } }
            onNodeWithText(DebugSeedStrings.label(SeedStep.HELD_TPTA)).performScrollTo().performClick()
            waitForIdle()

            onNodeWithText(DebugSeedStrings.REMOVE).performScrollTo().performClick()
            waitForIdle()

            assertEquals(listOf("08/2025"), stored())
            onNodeWithText(DebugSeedStrings.removed(3)).performScrollTo().assertIsDisplayed()
        }

    private fun realPayslip(
        year: Int = 2018,
        month: Int = 3,
    ) = ParsedPayslip(
        file = "real.pdf", year = year, monthNum = month, monthName = "M",
        dateStr = "${month.toString().padStart(2, '0')}/$year",
        officer = Officer("Real Name", "11/111/111111X", "BBBBB1111B"),
        earnings = Earnings(basicPay = 100000.0), deductions = Deductions(), ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
        taxAndSavings = null,
    )
}
