package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Section10CapPolicyTest {
    @Test
    fun testRiskHardshipCappedAtHighlyActiveFieldRate() {
        // D8: RH12 @ Rs21,125/mo received -> Rs2,53,500/yr uncapped. Rule 2BB caps the "Special
        // Compensatory (Highly Active Field Area) Allowance" category (all RH11-RH33/SICHA collapse
        // here) at Rs4,200/mo -> Rs50,400/yr.
        val result = Section10CapPolicy.capExemptions(annualFieldAllowanceReceived = 0.0, annualRiskHardshipReceived = 253500.0)
        assertEquals(50400.0, result.cappedRiskHardshipAmount)
        assertEquals(50400.0, result.exempt)
        assertTrue(result.exempt < 253500.0)
    }

    @Test
    fun testFieldAllowanceUsesConservativeCapAndFlagsAssumption() {
        // Open Item 2: the generic "FD" bucket can't be resolved to a specific field-area tier from
        // parsed data alone -- apply the lowest (Modified Field Area, Rs1,000/mo -> Rs12,000/yr) cap.
        val result = Section10CapPolicy.capExemptions(annualFieldAllowanceReceived = 50000.0, annualRiskHardshipReceived = 0.0)
        assertEquals(12000.0, result.cappedFieldAmount)
        assertTrue(result.isConservativeAssumption)
    }

    @Test
    fun testNoAssumptionFlagWhenNoFieldAllowanceReceived() {
        val result = Section10CapPolicy.capExemptions(annualFieldAllowanceReceived = 0.0, annualRiskHardshipReceived = 10000.0)
        assertFalse(result.isConservativeAssumption)
    }

    @Test
    fun testReceivedBelowCapPassesThroughUncapped() {
        val result = Section10CapPolicy.capExemptions(annualFieldAllowanceReceived = 0.0, annualRiskHardshipReceived = 20000.0)
        assertEquals(20000.0, result.cappedRiskHardshipAmount)
    }
}
