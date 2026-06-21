package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarriedQuartersRiskAuditorTest {
    private fun createMockPayslip(
        dateStr: String,
        hra: Double = 0.0,
        licenseFee: Double = 0.0,
        furnitureRent: Double = 0.0,
        monthNum: Int = 1,
        year: Int = 2026,
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "test.pdf",
            year = year,
            monthNum = monthNum,
            monthName = "MonthName",
            dateStr = dateStr,
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(houseRentAllowance = hra),
            deductions = Deductions(licenseFee = licenseFee, furnitureRent = furnitureRent),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 100000.0, totalDeductions = 10000.0, netRemittance = 90000.0),
            taxAndSavings = null,
        )
    }

    @Test
    fun testNoRiskIfHraIsActive() {
        val current = createMockPayslip("03/2026", hra = 20000.0)
        val auditor = MarriedQuartersRiskAuditor()
        val result = auditor.audit(current, null, emptyList())
        assertTrue(result.isEmpty(), "Should not report risk if HRA is actively drawn")
    }

    @Test
    fun testRiskTriggeredAfter3MonthsOfZeroDeductions() {
        val p1 = createMockPayslip("01/2026", hra = 0.0, licenseFee = 0.0, furnitureRent = 0.0, monthNum = 1)
        val p2 = createMockPayslip("02/2026", hra = 0.0, licenseFee = 0.0, furnitureRent = 0.0, monthNum = 2)
        val current = createMockPayslip("03/2026", hra = 0.0, licenseFee = 0.0, furnitureRent = 0.0, monthNum = 3)

        val auditor = MarriedQuartersRiskAuditor()
        val result = auditor.audit(current, p2, listOf(p1, p2))

        assertEquals(1, result.size)
        val anomaly = result.first()
        assertEquals("RENT_RECOVERY_RISK", anomaly.type)
        assertTrue(anomaly.description.contains("Quarters Rent Recovery Risk"))
        assertEquals(12000.0, anomaly.amount) // 3 months * 4000.0
    }
}
