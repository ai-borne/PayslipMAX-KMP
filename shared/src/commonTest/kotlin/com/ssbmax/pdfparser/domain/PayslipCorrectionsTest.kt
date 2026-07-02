package com.ssbmax.pdfparser.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PayslipCorrectionsTest {
    private fun sample(): ParsedPayslip =
        ParsedPayslip(
            file = "apr2024.pdf",
            year = 2024,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2024",
            officer = Officer("Officer Name", "00/000/000000X", "AB*****00C"),
            earnings = Earnings(basicPay = 132400.0, dearnessAllowance = 66200.0),
            deductions = Deductions(dsopSubscription = 40000.0, incomeTax = 25000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 198600.0, totalDeductions = 65000.0, netRemittance = 133600.0),
            taxAndSavings = null,
            rawEarnings = mapOf("UNKWN" to 1200.0),
            rawDeductions = mapOf("RECOV" to 800.0),
            fieldConfidence = mapOf("basicPay" to 0.5f, "UNKWN" to 0.4f),
            needsReview = true,
        )

    @Test
    fun emptyCorrectionsReturnsSameInstance() {
        val payslip = sample()
        assertSame(payslip, payslip.applyCorrections(emptyMap()))
    }

    @Test
    fun correctsStandardizedEarningsField() {
        val corrected = sample().applyCorrections(mapOf("basicPay" to 132414.0))
        assertEquals(132414.0, corrected.earnings.basicPay)
        // unrelated fields untouched
        assertEquals(66200.0, corrected.earnings.dearnessAllowance)
    }

    @Test
    fun correctsStandardizedDeductionsField() {
        val corrected = sample().applyCorrections(mapOf("incomeTax" to 24000.0))
        assertEquals(24000.0, corrected.deductions.incomeTax)
    }

    @Test
    fun correctsRawEarningsAndDeductionsByLabel() {
        val corrected = sample().applyCorrections(mapOf("UNKWN" to 1500.0, "RECOV" to 900.0))
        assertEquals(1500.0, corrected.rawEarnings["UNKWN"])
        assertEquals(900.0, corrected.rawDeductions["RECOV"])
    }

    @Test
    fun unknownKeyIsIgnored() {
        val corrected = sample().applyCorrections(mapOf("notAField" to 999.0))
        assertTrue("notAField" !in corrected.rawEarnings)
        assertTrue("notAField" !in corrected.rawDeductions)
    }

    @Test
    fun doesNotMutateOriginalAndLeavesPrintedTotalsUntouched() {
        val original = sample()
        val corrected = original.applyCorrections(mapOf("basicPay" to 1.0))
        assertNotSame(original, corrected)
        assertEquals(132400.0, original.earnings.basicPay, "original must be unchanged")
        // Printed totals are authoritative and must not be recomputed by a line-item correction.
        assertEquals(original.summary, corrected.summary)
    }
}
