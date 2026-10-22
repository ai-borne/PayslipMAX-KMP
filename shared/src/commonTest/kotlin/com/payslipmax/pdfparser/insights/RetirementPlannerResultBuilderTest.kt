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
        // The placeholder DA is 50% of the placeholder Basic + MSP, the base PCDA pays DA on.
        assertEquals(50.0, result.daPercentage, 1e-9)
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

    @Test
    fun leaveEncashmentDaysAreTheDaysTheAmountIsFor() {
        // The Day-1 corpus row is labelled with this count; it must follow the leave-days input, capped like the amount.
        val payslip =
            ParsedPayslip(
                file = "t.pdf",
                year = 2026,
                monthNum = 4,
                monthName = "",
                dateStr = "04/2026",
                officer = Officer("N", "A", "P"),
                earnings = Earnings(basicPay = 130000.0, militaryServicePay = 15500.0, dearnessAllowance = 65000.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(0.0, 0.0, 0.0),
                taxAndSavings = null,
            )

        val partial = RetirementPlannerResultBuilder.build(payslip, overrideLeaveDays = 222)
        assertEquals(222, partial.leaveEncashmentDays)
        assertEquals(RetirementCalculatorEngine.leaveEncashment(130000.0, 15500.0, 65000.0, 222), partial.leaveEncashment)
        assertEquals(300, RetirementPlannerResultBuilder.build(payslip, overrideLeaveDays = 400).leaveEncashmentDays)
        assertEquals(300, RetirementPlannerResultBuilder.build(payslip).leaveEncashmentDays)
        assertEquals(0, RetirementPlannerResultBuilder.build(payslip, overrideLeaveDays = -5).leaveEncashmentDays)
    }

    @Test
    fun npaCountsAsPayAndTheDaRateIsTakenOnBasicMspAndNpa() {
        // Handbook P&A 2023: NPA is reckonable for pension and gratuity (ch. 31 para C, p. 279) and for leave encashment
        // (ch. 29, p. 269); DA is paid on Basic + MSP + NPA (ch. 15, p. 130), so 67,750 here is a 50% rate, not 67.75%.
        val medicalOfficer =
            ParsedPayslip(
                file = "t.pdf",
                year = 2026,
                monthNum = 4,
                monthName = "",
                dateStr = "04/2026",
                officer = Officer("N", "A", "P"),
                earnings =
                    Earnings(basicPay = 100000.0, militaryServicePay = 15500.0, nonPracticingAllowance = 20000.0, dearnessAllowance = 67750.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(0.0, 0.0, 0.0),
                taxAndSavings = null,
            )

        val result = RetirementPlannerResultBuilder.build(medicalOfficer, overrideQualifyingYears = 10.0)

        assertEquals(50.0, result.daPercentage, 1e-9)
        assertEquals(135500.0, result.reckonableEmoluments)
        assertEquals(67750.0, result.basicPension) // 50% of Basic + MSP + NPA
        assertEquals(101625.0, result.netMonthlyPensionFull, 1e-6) // pension + 50% DR
        assertEquals(1016250.0, result.retirementGratuity, 1e-6) // 1/4 x (Basic + MSP + NPA + DA) x 20 half-years
        assertEquals(1800000.0, result.leaveEncashment, 1e-6) // (Basic + NPA + DA on them) / 30 x 300
    }
}
