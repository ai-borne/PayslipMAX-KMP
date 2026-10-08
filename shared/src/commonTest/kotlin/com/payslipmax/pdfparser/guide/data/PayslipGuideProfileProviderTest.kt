package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.guide.domain.GuideProfileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The provider reads the payslips only when a screen subscribes (a cold flow), and follows the stored history, so a newly
 * imported payslip updates an open card. No payslip data is kept by the provider itself.
 */
class PayslipGuideProfileProviderTest {
    private fun payslip(month: Int) =
        ParsedPayslip(
            file = "t.pdf",
            year = 2026,
            monthNum = month,
            monthName = "",
            dateStr = "$month/2026",
            officer = Officer("N", "A", "P"),
            earnings = Earnings(basicPay = 121200.0, militaryServicePay = 15500.0, dearnessAllowance = 82020.0, houseRentAllowance = 36360.0),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(0.0, 0.0, 0.0),
            taxAndSavings = null,
        )

    @Test
    fun nothingIsReadUntilAScreenSubscribes() {
        var reads = 0
        val provider = PayslipGuideProfileProvider { flowOf(emptyList<ParsedPayslip>()).also { reads++ } }

        provider.profile()

        assertEquals(0, reads)
    }

    @Test
    fun theProfileIsTheLatestMonthOfTheStoredHistory() =
        runTest {
            val provider = PayslipGuideProfileProvider { flowOf(listOf(payslip(4), payslip(5))) }

            val profile = provider.profile().first()

            assertEquals(5, profile?.month)
            assertEquals(60, profile?.daPercent)
        }

    @Test
    fun noPayslipsMeansNoProfile() =
        runTest {
            assertNull(PayslipGuideProfileProvider { flowOf(emptyList()) }.profile().first())
        }

    @Test
    fun theProfileFollowsNewlyImportedPayslips() =
        runTest {
            val history = MutableStateFlow(listOf(payslip(4)))
            val provider: GuideProfileProvider = PayslipGuideProfileProvider { history }

            assertEquals(4, provider.profile().first()?.month)
            history.value = listOf(payslip(4), payslip(5))
            assertEquals(5, provider.profile().first()?.month)
        }

    @Test
    fun theDefaultProviderKnowsNothing() =
        runTest {
            assertNull(GuideProfileProvider.None.profile().first())
        }
}
