package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.TaxRegime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DefenceTaxExemptionEngineTest {
    private fun ledgerSummary(
        riskHardshipAllowance: Double = 0.0,
        fieldAllowance: Double = 0.0,
        dsop: Double = 0.0,
        agif: Double = 0.0,
        hra: Double = 0.0,
        basicPay: Double = 0.0,
        da: Double = 0.0,
    ): FyTaxLedgerSummary =
        FyTaxLedgerSummary(
            financialYear = "2026-27",
            assessmentYear = "2027-28",
            parsedMonthCount = 12,
            monthsElapsedInFy = 12,
            ytdGross = 0.0,
            ytdTaxDeducted = 0.0,
            ytdDsop = dsop,
            ytdAgif = agif,
            ytdFieldAllowance = fieldAllowance,
            ytdRiskHardshipAllowance = riskHardshipAllowance,
            ytdHra = hra,
            ytdBasicPay = basicPay,
            ytdDa = da,
            projectedAnnualGross = 0.0,
            projectedAnnualTaxDeducted = 0.0,
            projectedAnnualDsop = dsop,
            projectedAnnualAgif = agif,
            projectedAnnualFieldAllowance = fieldAllowance,
            projectedAnnualRiskHardshipAllowance = riskHardshipAllowance,
            projectedAnnualHra = hra,
            projectedAnnualBasicPay = basicPay,
            projectedAnnualDa = da,
            missingMonthNums = emptyList(),
            latestBasicPay = 0.0,
            latestDa = 0.0,
        )

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

    @Test
    fun testFieldAllowanceExemptionIsCappedNotPassedThroughUncapped() {
        // D8: RH12 @ Rs21,125/mo -> Rs2,53,500/yr received. Exemption must be capped to Rs50,400
        // (Rule 2BB "Highly Active Field Area" rate), not the uncapped received amount.
        val summary = ledgerSummary(riskHardshipAllowance = 253500.0)
        val result = DefenceTaxExemptionEngine.extractExemptions(summary)
        assertEquals(50400.0, result.fieldAllowanceExemption)
    }

    @Test
    fun testOldRegimeApr2026DeductionsMatchGoldStandard() {
        // Plan acceptance: "Old-regime deductions for Apr 2026 ~= Rs2,00,400" once (a) 80C is capped
        // at Rs1.5L (DSOP+AGIF already exceed it) and (b) the RH allowance is capped at Rs50,400
        // instead of the uncapped Rs2,53,500 -- no HRA on this payslip.
        val summary = ledgerSummary(riskHardshipAllowance = 253500.0, dsop = 480000.0, agif = 150000.0)
        val result = DefenceTaxExemptionEngine.extractExemptions(summary)
        assertEquals(200400.0, result.totalOldRegimeDeductions, 0.01)
    }

    @Test
    fun testNewRegimeZeroesOldRegimeSectionsWithExplicitReason() {
        // D10: a coincidental Rs0 (e.g. no DSOP contributions) must be indistinguishable in code from
        // "not available under your regime" only by checking the reason -- this test asserts the
        // reason itself exists, so it cannot pass on a coincidental zero. Proven by contrast: the same
        // summary under OLD regime yields a genuinely non-zero HRA exemption (see the assertion below).
        val summary =
            ledgerSummary(
                riskHardshipAllowance = 100000.0,
                dsop = 120000.0,
                agif = 60000.0,
                hra = 120000.0,
                basicPay = 800000.0,
                da = 200000.0,
            )
        val oldRegimeControl = DefenceTaxExemptionEngine.extractExemptions(summary, rentPaidAnnual = 180000.0, npsAnnual = 20000.0)
        assertTrue(oldRegimeControl.hraExemption > 0.0, "control: OLD regime must produce a genuinely non-zero HRA exemption")

        val result =
            DefenceTaxExemptionEngine.extractExemptions(
                summary,
                rentPaidAnnual = 180000.0,
                npsAnnual = 20000.0,
                activeRegime = TaxRegime.NEW,
            )

        assertEquals(0.0, result.sec80C.eligibleDeduction)
        assertEquals(0.0, result.sec80CCD1B.eligibleDeduction)
        assertEquals(0.0, result.hraExemption)
        assertEquals(0.0, result.fieldAllowanceExemption)
        assertEquals(0.0, result.totalOldRegimeDeductions)
        assertEquals(TaxRegime.NEW, result.regime)

        assertEquals(DefenceTaxExemptionEngine.REASON_NOT_AVAILABLE_NEW_REGIME, result.unavailableUnderRegime["sec80C"])
        assertEquals(DefenceTaxExemptionEngine.REASON_NOT_AVAILABLE_NEW_REGIME, result.unavailableUnderRegime["sec80CCD1B"])
        assertEquals(DefenceTaxExemptionEngine.REASON_NOT_AVAILABLE_NEW_REGIME, result.unavailableUnderRegime["hra"])
        assertEquals(DefenceTaxExemptionEngine.REASON_NOT_AVAILABLE_NEW_REGIME, result.unavailableUnderRegime["sec10Field"])
    }

    @Test
    fun testFieldCapConservativeAssumptionFlagPropagates() {
        // Open Item 2: must be disclosed on screen whenever the generic field bucket forced the
        // conservative fallback cap, but not when there's no field allowance to disclose an assumption about.
        val withField = DefenceTaxExemptionEngine.extractExemptions(ledgerSummary(fieldAllowance = 5000.0))
        assertTrue(withField.fieldCapIsConservativeAssumption)

        val withoutField = DefenceTaxExemptionEngine.extractExemptions(ledgerSummary(riskHardshipAllowance = 5000.0))
        assertFalse(withoutField.fieldCapIsConservativeAssumption)
    }

    @Test
    fun testOldRegimeHasNoUnavailableReasons() {
        val summary = ledgerSummary()
        val result = DefenceTaxExemptionEngine.extractExemptions(summary)
        assertEquals(emptyMap(), result.unavailableUnderRegime)
        assertNull(result.unavailableUnderRegime["sec80C"])
    }
}
