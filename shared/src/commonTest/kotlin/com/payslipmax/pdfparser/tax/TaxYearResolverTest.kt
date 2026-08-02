package com.payslipmax.pdfparser.tax

import kotlin.test.Test
import kotlin.test.assertEquals

class TaxYearResolverTest {
    @Test
    fun testApril2026ResolvesToFy202627() {
        val resolved = TaxYearResolver.resolve(monthNum = 4, year = 2026)
        assertEquals("2026-27", resolved.financialYear)
        assertEquals("2027-28", resolved.assessmentYear)
    }

    @Test
    fun testMarch2027ResolvesToFy202627() {
        val resolved = TaxYearResolver.resolve(monthNum = 3, year = 2027)
        assertEquals("2026-27", resolved.financialYear)
        assertEquals("2027-28", resolved.assessmentYear)
    }

    @Test
    fun testApril2025ResolvesToFy202526() {
        val resolved = TaxYearResolver.resolve(monthNum = 4, year = 2025)
        assertEquals("2025-26", resolved.financialYear)
        assertEquals("2026-27", resolved.assessmentYear)
    }
}
