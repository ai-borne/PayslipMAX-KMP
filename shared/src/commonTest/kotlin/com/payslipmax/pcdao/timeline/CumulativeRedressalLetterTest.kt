package com.payslipmax.pcdao.timeline

import com.payslipmax.pcdao.redressal.ExportFormat
import com.payslipmax.pcdao.redressal.RedressalLetterGenerator
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CumulativeRedressalLetterTest {
    @Test
    fun testGenerateCumulativeRedressalLetter() {
        val payslip =
            ParsedPayslip(
                file = "col_rathore.pdf",
                year = 2026,
                monthNum = 2,
                monthName = "February",
                dateStr = "2026-02-01",
                officer = Officer("Col R. S. Rathore", "01/142/987654", "ABCDE1234F"),
                earnings = Earnings(basicPay = 149000.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(grossPay = 286723.0, totalDeductions = 40000.0, netRemittance = 246723.0),
                taxAndSavings = null,
            )

        val rollup =
            CumulativeArrearsRollup(
                totalUnderpaidArrears = 585680.0,
                totalRecoveryHazard = 0.0,
                auditedMonthCount = 20,
                startMonthDateStr = "01/2025",
                endMonthDateStr = "08/2026",
                monthlyBreakdowns =
                    listOf(
                        MonthArrearsBreakdown("01/2025", "January", 2025, 144700.0, "SPR HRA", 28940.0, 0.0, 28940.0),
                        MonthArrearsBreakdown("02/2025", "February", 2025, 144700.0, "SPR HRA", 28940.0, 0.0, 28940.0),
                    ),
                primaryClaimTitle = "Selected Place of Residence (SPR) HRA Arrears",
                summaryText = "Cumulative back-dues of ₹585680 across 20 uploaded payslips.",
            )

        val request =
            RedressalLetterGenerator.createRequestFromCumulativeRollup(
                payslip = payslip,
                rollup = rollup,
                rank = "Colonel",
                exportFormat = ExportFormat.MARKDOWN,
            )

        assertEquals("20 Months (01/2025 to 08/2026)", request.disputeMonth)
        assertEquals(2, request.lineItems.size)
        assertEquals(28940.0, request.lineItems[0].netDue)

        val letter = RedressalLetterGenerator.generateLetter(request, "28 September 2026")
        assertTrue(letter.fullBodyText.contains("20 Months (01/2025 to 08/2026)"))
        assertTrue(letter.fullBodyText.contains("January 2025 - SPR HRA"))
        assertEquals(57880.0, letter.totalNetDue)
    }
}
