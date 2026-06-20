package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TaxProjectionAuditorTest {
    private fun createMockPayslip(
        dateStr: String,
        tax: Double = 5000.0,
        totalTaxPayable: Double = 0.0,
        monthNum: Int = 5
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = monthNum,
            monthName = "MonthName",
            dateStr = dateStr,
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(),
            deductions = Deductions(incomeTax = tax),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 120000.0, totalDeductions = tax, netRemittance = 120000.0 - tax),
            taxAndSavings = TaxAndSavings(
                totalTaxPayable = totalTaxPayable
            )
        )
    }

    @Test
    fun testAprilNewTaxCycleTrigger() {
        val current = createMockPayslip("04/2026", tax = 6000.0, totalTaxPayable = 72000.0, monthNum = 4)
        val auditor = TaxProjectionAuditor()
        val result = auditor.audit(current, null, emptyList())

        val anomaly = result.find { it.type == "TAX_PROJECTION" }
        assertTrue(anomaly != null)
        assertEquals(72000.0, anomaly.amount)
        assertTrue(anomaly.description.contains("New FY Tax Projection"))
    }

    @Test
    fun testTaxSpikeTriggerInMay() {
        val previous = createMockPayslip("04/2026", tax = 5000.0, monthNum = 4)
        val current = createMockPayslip("05/2026", tax = 8000.0, monthNum = 5)

        val auditor = TaxProjectionAuditor()
        val result = auditor.audit(current, previous, emptyList())

        val anomaly = result.find { it.type == "DEDUCTION_SPIKE" }
        assertTrue(anomaly != null)
        assertEquals(3000.0, anomaly.amount)
        assertTrue(anomaly.description.contains("spiked by"))
    }
}
