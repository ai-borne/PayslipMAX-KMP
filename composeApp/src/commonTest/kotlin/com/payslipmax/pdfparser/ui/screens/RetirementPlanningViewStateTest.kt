package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.insights.DataConfidenceLevel
import com.payslipmax.pdfparser.insights.RetirementPlannerResultBuilder
import com.payslipmax.pdfparser.insights.RetirementYearResolver
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetirementPlanningViewStateTest {
    @Test
    fun testRetirementPlannerResultBuildingFromPayslip() {
        val payslip =
            ParsedPayslip(
                file = "payslip_05/2026.pdf",
                year = 2026,
                monthNum = 5,
                monthName = "MAY2026",
                dateStr = "05/2026",
                officer = Officer("BRIGADIER SUNIL", "12345", "ABCDE1234F"),
                earnings = Earnings(basicPay = 140000.0, militaryServicePay = 15500.0, dearnessAllowance = 70000.0),
                deductions = Deductions(dsopSubscription = 25000.0),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(225500.0, 25000.0, 200500.0),
                taxAndSavings = TaxAndSavings(dsopFund = DsopFund(closingBalance = 5000000.0)),
            )

        val result = RetirementPlannerResultBuilder.build(payslip)

        assertEquals(DataConfidenceLevel.HIGH, result.confidenceLevel)
        assertEquals(77750.0, result.basicPension) // (140000 + 15500)/2
        assertEquals(54, result.ageNextBirthday) // Estimated age next birthday for 140k basic
        assertEquals(56, RetirementYearResolver.resolveSuperannuationAge("BRIGADIER SUNIL"))
        assertEquals(5000000.0, result.dsopBalance)
        assertTrue(result.totalDay1Corpus > 5000000.0)
        assertEquals(3, result.commutationScenarios.size)
    }

    @Test
    fun testRetirementPlannerResultBuildingWithoutDsopBalance() {
        val payslip =
            ParsedPayslip(
                file = "payslip_05/2026.pdf",
                year = 2026,
                monthNum = 5,
                monthName = "MAY2026",
                dateStr = "05/2026",
                officer = Officer("MAJOR KUMAR", "12345", "ABCDE1234F"),
                earnings = Earnings(basicPay = 90000.0, militaryServicePay = 15500.0, dearnessAllowance = 45000.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(150500.0, 0.0, 150500.0),
                taxAndSavings = TaxAndSavings(),
            )

        val result = RetirementPlannerResultBuilder.build(payslip)

        assertEquals(DataConfidenceLevel.MODERATE, result.confidenceLevel)
        assertEquals(52750.0, result.basicPension)
        assertEquals(48, result.ageNextBirthday)
        assertEquals(54, RetirementYearResolver.resolveSuperannuationAge("MAJOR KUMAR"))
        assertEquals(0.0, result.dsopBalance)
    }
}
