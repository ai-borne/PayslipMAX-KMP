package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.TaxRegime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TaxParserUtilsTest {
    @Test
    fun testParseTaxAndSavings_DefaultOldRegime() {
        val text =
            """
            INCOME TAX DETAILS
            1. Gross Salary 1500000
            Total Taxable Income 1400000
            Standard Deduction 50000
            Net Taxable Income 1350000
            Total Tax Payable 220000
            Income Tax Deducted 200000
            Ed. Cess Deducted 8000
            """.trimIndent()

        val result = parseTaxAndSavings(text, null, "")
        assertNotNull(result)
        assertEquals(TaxRegime.OLD, result.taxRegime)
        assertEquals(1500000.0, result.grossSalaryYtd)
        assertEquals(1400000.0, result.totalTaxableIncome)
        assertEquals(50000.0, result.standardDeduction)
        assertEquals(1350000.0, result.netTaxableIncome)
        assertEquals(220000.0, result.totalTaxPayable)
        assertEquals(200000.0, result.taxDeductedYtd)
        assertEquals(8000.0, result.cessDeductedYtd)
    }

    @Test
    fun testParseTaxAndSavings_NewRegimeDetected() {
        val text =
            """
            INCOME TAX DETAILS
            (New Tax Regime)
            1. Gross Salary 1500000
            Total Taxable Income 1400000
            Standard Deduction 75000
            Net Taxable Income 1325000
            Total Tax Payable 180000
            Income Tax Deducted 150000
            Ed. Cess Deducted 6000
            """.trimIndent()

        val result = parseTaxAndSavings(text, null, "")
        assertNotNull(result)
        assertEquals(TaxRegime.NEW, result.taxRegime)
        assertEquals(75000.0, result.standardDeduction)
    }

    @Test
    fun testGrossSalaryWithEmbeddedDateDoesNotExtractDatePart() {
        val text =
            """
            Gross Salary upto 31/03/2024 852000
            Total Taxable Income 742000
            Standard Deduction 50000
            Net Taxable Income 692000
            Total Tax Payable 78000
            """.trimIndent()

        val result = parseTaxAndSavings(text, null, "")
        assertNotNull(result)
        assertEquals(852000.0, result.grossSalaryYtd)
        assertEquals(742000.0, result.totalTaxableIncome)
    }

    @Test
    fun testSlashPrecededNumbersIgnoredAsSalary() {
        val text =
            """
            Ref/12345/67890
            Gross Salary 950000
            Total Taxable Income 850000
            """.trimIndent()

        val result = parseTaxAndSavings(text, null, "")
        assertNotNull(result)
        assertEquals(950000.0, result.grossSalaryYtd)
    }

    // D3 (docs/Plan/04_TaxPlannerGoldStandard.md): a single-month grossSalaryYtd at the start/end of the
    // FY used to make totalTaxPayable > grossSalaryYtd look "implausible" and get replaced with a
    // fabricated round(grossSalaryYtd * 0.30) — corrupting real PCDA figures on every high-income
    // April/March payslip. This is exactly the April 2026 evidence-base shape: one month's gross,
    // full-year tax payable.
    @Test
    fun testTotalTaxPayableExceedingSingleMonthGrossIsKeptVerbatim() {
        val text =
            """
            INCOME TAX DETAILS
            (New Tax Regime)
            1. Gross Salary 586894
            Total Taxable Income 3487744
            Standard Deduction 75000
            Net Taxable Income 3412740
            Total Tax Payable 603822
            Income Tax Deducted 99567
            Ed. Cess Deducted 3983
            """.trimIndent()

        val result = parseTaxAndSavings(text, null, "")
        assertNotNull(result)
        assertEquals(603822.0, result.totalTaxPayable)
    }

    // A totalTaxPayable exceeding totalTaxableIncome itself (not just the unreliable grossSalaryYtd
    // field) is genuinely implausible -- tax can never exceed the income it's computed on. The guard
    // must leave the field unavailable rather than fabricate a replacement value.
    @Test
    fun testTotalTaxPayableExceedingTaxableIncomeIsMarkedUnavailable() {
        val text =
            """
            INCOME TAX DETAILS
            1. Gross Salary 1500000
            Total Taxable Income 1400000
            Standard Deduction 50000
            Net Taxable Income 1350000
            Total Tax Payable 9999999
            Income Tax Deducted 200000
            Ed. Cess Deducted 8000
            """.trimIndent()

        val result = parseTaxAndSavings(text, null, "")
        assertNotNull(result)
        assertEquals(null, result.totalTaxPayable)
    }
}
