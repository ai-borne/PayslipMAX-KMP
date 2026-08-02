package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TdsRunwayEngineTest {
    @Test
    fun testTdsRunwayCalculationNoSpike() {
        // YTD TDS paid = 40,000 across 8 months (5,000/mo). Total Annual Tax Liability = 60,000.
        // Remaining Tax = 20,000 across 4 remaining months = 5,000/mo projected.
        // Current Monthly TDS = 5,000. Spike = 0%.
        val res =
            TdsRunwayEngine.computeTdsRunway(
                ytdTdsDeducted = 40000.0,
                parsedMonthCount = 8,
                totalAnnualTaxLiability = 60000.0,
                currentMonthlyTds = 5000.0,
            )

        assertEquals(4, res.remainingMonths)
        assertEquals(20000.0, res.remainingTaxPayable)
        assertEquals(5000.0, res.projectedMonthlyTds)
        assertFalse(res.hasTdsSpikeWarning)
    }

    @Test
    fun testJanMarTdsSpikeWarningTrigger() {
        // YTD TDS paid = 30,000 across 9 months (3,333/mo). Total Annual Tax Liability = 1,10,000.
        // Remaining Tax = 80,000 across 3 remaining months = 26,666/mo projected!
        // Current Monthly TDS = 3,333. Jump from 3.3k to 26.6k is >30% -> Spike Warning!
        val res =
            TdsRunwayEngine.computeTdsRunway(
                ytdTdsDeducted = 30000.0,
                parsedMonthCount = 9,
                totalAnnualTaxLiability = 110000.0,
                currentMonthlyTds = 3333.0,
            )

        assertEquals(3, res.remainingMonths)
        assertEquals(80000.0, res.remainingTaxPayable)
        assertTrue(res.projectedMonthlyTds > 26000.0)
        assertTrue(res.hasTdsSpikeWarning)
    }
}
