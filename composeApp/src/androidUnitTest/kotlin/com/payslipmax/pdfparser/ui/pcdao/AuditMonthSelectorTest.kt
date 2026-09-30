package com.payslipmax.pdfparser.ui.pcdao

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w412dp-h915dp")
@OptIn(ExperimentalTestApi::class)
class AuditMonthSelectorTest {
    @AfterTest
    fun tearDown() {
        try {
            org.koin.core.context.stopKoin()
        } catch (_: Exception) {
        }
    }

    private fun mockPayslip(
        year: Int,
        monthNum: Int,
        monthName: String,
    ) =
        ParsedPayslip(
            file = "test_$monthNum.pdf",
            year = year,
            monthNum = monthNum,
            monthName = monthName,
            dateStr = "${monthNum.toString().padStart(2, '0')}/$year",
            officer = Officer("Test Officer", "00/000/000000X", "AA****00A"),
            earnings = Earnings(basicPay = 100000.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 20000.0, netRemittance = 80000.0),
            taxAndSavings = null,
        )

    @Test
    fun monthSelector_displaysSelectedMonthNameAndYear() =
        runComposeUiTest {
            val slip = mockPayslip(2026, 8, "August")

            setContent {
                MonthSelectorRow(
                    availablePayslips = listOf(slip),
                    selectedPayslip = slip,
                    onSelect = {},
                )
            }

            onNodeWithText("August 2026").assertIsDisplayed()
        }

    @Test
    fun monthSelector_whenMultiplePayslips_opensDropdownAndSelects() =
        runComposeUiTest {
            val aug = mockPayslip(2026, 8, "August")
            val jul = mockPayslip(2026, 7, "July")
            var selected: ParsedPayslip? = null

            setContent {
                MonthSelectorRow(
                    availablePayslips = listOf(aug, jul),
                    selectedPayslip = aug,
                    onSelect = { selected = it },
                )
            }

            onNodeWithTag("pcdao_month_selector_chip").performClick()
            waitForIdle()

            onNodeWithText("July 2026").assertIsDisplayed()
            onNodeWithText("July 2026").performClick()
            waitForIdle()

            assertEquals(jul, selected)
        }
}
