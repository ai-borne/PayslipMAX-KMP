package com.payslipmax.pdfparser.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.DsopFund
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import kotlin.test.Test

/**
 * Insights analyses stored payslips, so its month dropdown must list only months that have one; a calendar
 * with empty months would let the user open an analysis of nothing. (Phase 1 recorded the dropdown as a
 * calendar; it already lists payslips only, and this pins that.)
 */
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class InsightsMonthSelectorTest {
    private fun slip(
        year: Int,
        month: Int,
        name: String,
    ) = ParsedPayslip(
        file = "p.pdf", year = year, monthNum = month, monthName = name, dateStr = "$month/$year",
        officer = Officer("N", "A", "P"),
        earnings = Earnings(100.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
        deductions = Deductions(10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0, 10.0),
        ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
        summary = PayslipSummary(100.0, 80.0, 20.0),
        taxAndSavings = TaxAndSavings(1000.0, 900.0, 50.0, 850.0, 100.0, 80.0, 20.0, DsopFund(100.0, 10.0, 0.0, 0.0, 0.0, 110.0)),
    )

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun dropdownListsOnlyMonthsThatHaveAPayslip() =
        runComposeUiTest {
            val jan = slip(2026, 1, "January")
            val mar = slip(2026, 3, "March")
            setContent { InsightsTopBar(payslips = listOf(jan, mar), selected = mar, onSelectPayslip = {}) }

            onNodeWithText("March 2026").performClick()
            onNodeWithText("January 2026").assertExists()
            onNodeWithText("February 2026").assertDoesNotExist()
        }
}
