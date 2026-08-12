package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import com.payslipmax.pdfparser.domain.TaxAndSavings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaxLedgerAggregatorTest {
    private fun createPayslip(
        year: Int,
        monthNum: Int,
        grossPay: Double = 100000.0,
        basicPay: Double = 60000.0,
        da: Double = 30000.0,
        incomeTax: Double = 5000.0,
        dsop: Double = 10000.0,
        agif: Double = 5000.0,
        fieldAllowance: Double = 4200.0,
        riskHardshipAllowance: Double = 0.0,
        rawEarnings: Map<String, Double> = emptyMap(),
        hra: Double = 8000.0,
    ): ParsedPayslip {
        val monthStr = if (monthNum < 10) "0$monthNum" else "$monthNum"
        return ParsedPayslip(
            file = "payslip_$year$monthStr.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "Month$monthNum",
            dateStr = "$monthStr/$year",
            officer = Officer("John Doe", "12345", "ABCDE1234F"),
            earnings =
                Earnings(
                    basicPay = basicPay,
                    dearnessAllowance = da,
                    fieldAllowance = fieldAllowance,
                    riskHardshipAllowance = riskHardshipAllowance,
                    houseRentAllowance = hra,
                ),
            deductions =
                Deductions(
                    incomeTax = incomeTax,
                    dsopSubscription = dsop,
                    agif = agif,
                ),
            ledgerBalances = LedgerBalances(),
            summary =
                PayslipSummary(
                    grossPay = grossPay,
                    totalDeductions = incomeTax + dsop + agif,
                    netRemittance = grossPay - (incomeTax + dsop + agif),
                ),
            taxAndSavings =
                TaxAndSavings(
                    grossSalaryYtd = grossPay,
                    taxDeductedYtd = incomeTax,
                ),
            rawEarnings = rawEarnings,
        )
    }

    @Test
    fun testFinancialYearDetermination() {
        assertEquals("2024-25", TaxLedgerAggregator.computeFinancialYear(2024, 4))
        assertEquals("2024-25", TaxLedgerAggregator.computeFinancialYear(2024, 12))
        assertEquals("2024-25", TaxLedgerAggregator.computeFinancialYear(2025, 1))
        assertEquals("2025-26", TaxLedgerAggregator.computeFinancialYear(2025, 4))
    }

    @Test
    fun testFormatIndianCurrency() {
        assertEquals("8,02,444", TaxLedgerAggregator.formatIndianCurrency(802444.0))
        assertEquals("68,365", TaxLedgerAggregator.formatIndianCurrency(68365.0))
        assertEquals("70,200", TaxLedgerAggregator.formatIndianCurrency(70200.0))
        assertEquals("3,75,001", TaxLedgerAggregator.formatIndianCurrency(375001.0))
        assertEquals("500", TaxLedgerAggregator.formatIndianCurrency(500.0))
    }

    @Test
    fun testExtractRiskHardshipAllowanceKeepsSeparateFromField() {
        // D8: RH/field buckets must stay separate so each can be capped against its own Rule 2BB
        // category (Section10CapPolicy) -- a merged total "cannot be category-correct".
        val p1 = createPayslip(2024, 4, fieldAllowance = 0.0, riskHardshipAllowance = 6000.0)
        assertEquals(6000.0, TaxLedgerAggregator.extractRiskHardshipAllowance(p1))
        assertEquals(0.0, TaxLedgerAggregator.extractFieldAreaAllowance(p1))

        val p2 = createPayslip(2024, 4, fieldAllowance = 0.0, riskHardshipAllowance = 0.0, rawEarnings = mapOf("RHA" to 4200.0))
        assertEquals(4200.0, TaxLedgerAggregator.extractRiskHardshipAllowance(p2))
        assertEquals(0.0, TaxLedgerAggregator.extractFieldAreaAllowance(p2))
    }

    @Test
    fun testExtractFieldAreaAllowanceFallsBackToRawFieldKeyword() {
        val p = createPayslip(2024, 4, fieldAllowance = 0.0, riskHardshipAllowance = 0.0, rawEarnings = mapOf("FIELD ALLC" to 2700.0))
        assertEquals(2700.0, TaxLedgerAggregator.extractFieldAreaAllowance(p))
        assertEquals(0.0, TaxLedgerAggregator.extractRiskHardshipAllowance(p))
    }

    @Test
    fun testMultiMonthAggregationForFy() {
        val payslips =
            listOf(
                createPayslip(2024, 4, grossPay = 100000.0),
                createPayslip(2024, 5, grossPay = 100000.0),
                createPayslip(2024, 6, grossPay = 100000.0),
            )

        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")

        assertEquals("2024-25", summary.financialYear)
        assertEquals("2025-26", summary.assessmentYear)
        assertEquals(3, summary.parsedMonthCount)
        assertEquals(300000.0, summary.ytdGross)
        assertEquals(15000.0, summary.ytdTaxDeducted)
        assertEquals(30000.0, summary.ytdDsop)
        assertEquals(15000.0, summary.ytdAgif)
        assertEquals(12600.0, summary.ytdFieldAllowance)
        assertEquals(24000.0, summary.ytdHra)
    }

    @Test
    fun testTwelveMonthRunRateProjection() {
        val payslips =
            (4..9).map { month ->
                createPayslip(2024, month, grossPay = 100000.0, dsop = 10000.0, agif = 5000.0)
            }

        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")

        assertEquals(6, summary.parsedMonthCount)
        assertEquals(600000.0, summary.ytdGross)
        assertEquals(1200000.0, summary.projectedAnnualGross)
        assertEquals(120000.0, summary.projectedAnnualDsop)
        assertEquals(60000.0, summary.projectedAnnualAgif)
        assertEquals(50400.0, summary.projectedAnnualFieldAllowance)
    }

    @Test
    fun testMissingMonthsDetection() {
        val payslips =
            listOf(
                createPayslip(2024, 4),
                createPayslip(2024, 5),
                createPayslip(2024, 7),
            )

        val summary = TaxLedgerAggregator.aggregateFy(payslips, targetFy = "2024-25")

        assertEquals(3, summary.parsedMonthCount)
        assertTrue(summary.missingMonthNums.contains(6))
        assertEquals(9, summary.missingMonthNums.size)
    }
}
