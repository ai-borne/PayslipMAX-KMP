package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetirementPlannerResultBuilderTest {
    @Test
    fun buildWithNullPayslipProducesPreliminaryEstimate() {
        val result = RetirementPlannerResultBuilder.build(null)
        assertEquals(DataConfidenceLevel.PRELIMINARY, result.confidenceLevel)
        assertTrue(result.basicPension > 0.0)
        assertTrue(result.totalDay1Corpus > 0.0)
        assertEquals(3, result.commutationScenarios.size)
    }

    @Test
    fun buildWithParsedPayslipProducesHighConfidenceWhenDsopPresent() {
        val payslip =
            ParsedPayslip(
                file = "payslip_04/2026.pdf",
                year = 2026,
                monthNum = 4,
                monthName = "APR2026",
                dateStr = "04/2026",
                officer = Officer("COLONEL JOHN DOE", "12345", "ABCDE1234F"),
                earnings = Earnings(basicPay = 130000.0, militaryServicePay = 15500.0, dearnessAllowance = 65000.0),
                deductions = Deductions(dsopSubscription = 20000.0),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(210500.0, 20000.0, 190500.0),
                taxAndSavings = TaxAndSavings(dsopFund = DsopFund(closingBalance = 4500000.0)),
            )

        val result = RetirementPlannerResultBuilder.build(payslip)
        assertEquals(DataConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(72750.0, result.basicPension) // (130000 + 15500)/2
        assertEquals(4500000.0, result.dsopBalance)
        assertTrue(result.totalDay1Corpus > 4500000.0)
        assertEquals("JOHN DOE", result.officerName)
        assertEquals("COLONEL", result.officerRank)
        assertEquals(result.totalDay1Corpus, result.taxFreeCorpus)
        assertEquals(result.netMonthlyPensionCommuted50, result.taxableMonthlyPension)
        assertTrue(result.commutationBreakEvenRoi > 0.0)
    }
}
