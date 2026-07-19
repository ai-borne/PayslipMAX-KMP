package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals

class DefenceTaxExemptionEngineTest {
    @Test
    fun test80CLimitAndHeadroom() {
        // DSOP 10k/mo = 1.2L/yr, AGIF 5k/mo = 60k/yr -> Total 1.8L
        // Cap is 1.5L. Headroom = 0.
        val res = DefenceTaxExemptionEngine.compute80CUsage(annualDsop = 120000.0, annualAgif = 60000.0)
        assertEquals(180000.0, res.totalClaimed)
        assertEquals(150000.0, res.eligibleDeduction)
        assertEquals(0.0, res.headroom)

        // DSOP 5k/mo = 60k/yr, AGIF 2k/mo = 24k/yr -> Total 84k
        // Headroom = 1.5L - 84k = 66,000.
        val resUnder = DefenceTaxExemptionEngine.compute80CUsage(annualDsop = 60000.0, annualAgif = 24000.0)
        assertEquals(84000.0, resUnder.totalClaimed)
        assertEquals(84000.0, resUnder.eligibleDeduction)
        assertEquals(66000.0, resUnder.headroom)
    }

    @Test
    fun testHraExemptionFormula() {
        // HRA Received = 1.2L/yr (10k/mo). Rent Paid = 1.8L/yr (15k/mo). Basic+DA = 10L/yr.
        // Rule 1: Actual HRA = 1,20,000
        // Rule 2: Rent Paid - 10% Basic+DA = 1.8L - 1.0L = 80,000
        // Rule 3: 40% Basic+DA = 4,00,000
        // Min = 80,000.
        val exemption =
            DefenceTaxExemptionEngine.computeHraExemption(
                annualHraReceived = 120000.0,
                annualRentPaid = 180000.0,
                annualBasicPlusDa = 1000000.0,
                isMetro = false,
            )
        assertEquals(80000.0, exemption)
    }

    @Test
    fun testNps80CCD1BHeadroom() {
        val npsRes = DefenceTaxExemptionEngine.compute80CCD1BUsage(currentAnnualNps = 20000.0)
        assertEquals(20000.0, npsRes.eligibleDeduction)
        assertEquals(30000.0, npsRes.headroom)
    }
}
