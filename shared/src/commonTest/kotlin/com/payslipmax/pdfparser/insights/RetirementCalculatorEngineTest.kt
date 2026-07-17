package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetirementCalculatorEngineTest {
    private val tol = 0.5

    @Test
    fun retiringPensionIsHalfOfBasicPlusMsp() {
        assertEquals(57750.0, RetirementCalculatorEngine.retiringPension(basicPay = 100000.0, militaryServicePay = 15500.0), tol)
    }

    @Test
    fun gratuityIsQuarterEmolumentsPerSixMonthlyPeriod() {
        // emoluments = 100000 + 30000 = 130000; 20 yrs -> 40 periods; 0.25*130000*40 = 1,300,000 (below both caps).
        assertEquals(1_300_000.0, RetirementCalculatorEngine.retirementGratuity(100000.0, 30000.0, 20.0), tol)
    }

    @Test
    fun gratuityIsCappedAt16Point5TimesEmoluments() {
        // emoluments = 100000; 40 yrs -> 80 periods; raw = 2,000,000 but 16.5x cap = 1,650,000 (< ₹25L).
        assertEquals(1_650_000.0, RetirementCalculatorEngine.retirementGratuity(100000.0, 0.0, 40.0), tol)
    }

    @Test
    fun gratuityIsCappedAtAbsoluteCeiling() {
        // emoluments = 250000; 33 yrs -> 66 periods; raw = 16.5*250000 = 4,125,000 -> ceiling ₹25,00,000.
        assertEquals(2_500_000.0, RetirementCalculatorEngine.retirementGratuity(200000.0, 50000.0, 33.0), tol)
    }

    @Test
    fun commutationFactorKnownForOfficerBandNullOutside() {
        assertEquals(8.287, RetirementCalculatorEngine.commutationFactor(60))
        assertEquals(8.194, RetirementCalculatorEngine.commutationFactor(61))
        assertNull(RetirementCalculatorEngine.commutationFactor(50), "factor must not be fabricated outside the sourced band")
        assertNull(RetirementCalculatorEngine.commutationFactor(70))
    }

    @Test
    fun commutedLumpSumUsesPensionFractionTwelveAndFactor() {
        // 57750 * 0.5 * 12 * 8.287 = 346500 * 8.287 = 2,871,445.5
        assertEquals(2_871_445.5, RetirementCalculatorEngine.commutedLumpSum(57750.0, 0.5, 8.287), 1.0)
    }

    @Test
    fun commutedFractionIsCappedAtHalf() {
        val at60Pct = RetirementCalculatorEngine.commutedLumpSum(57750.0, 0.6, 8.287)
        val at50Pct = RetirementCalculatorEngine.commutedLumpSum(57750.0, 0.5, 8.287)
        assertEquals(at50Pct, at60Pct, tol, "armed-forces commutation is capped at 50%")
    }

    @Test
    fun residualPensionIsWhatRemainsAfterCommuting() {
        assertEquals(28875.0, RetirementCalculatorEngine.residualPension(57750.0, 0.5), tol)
    }

    @Test
    fun leaveEncashmentIsPayPer30TimesDaysCappedAt300() {
        // (100000 + 30000)/30 * 300 = 1,300,000
        assertEquals(1_300_000.0, RetirementCalculatorEngine.leaveEncashment(100000.0, 30000.0, 300), tol)
        // days beyond 300 are capped
        assertEquals(
            RetirementCalculatorEngine.leaveEncashment(100000.0, 30000.0, 300),
            RetirementCalculatorEngine.leaveEncashment(100000.0, 30000.0, 400),
            tol,
        )
    }

    @Test
    fun zeroOrNegativeInputsProduceZero() {
        assertEquals(0.0, RetirementCalculatorEngine.retirementGratuity(100000.0, 0.0, 0.0), tol)
        assertEquals(0.0, RetirementCalculatorEngine.leaveEncashment(100000.0, 0.0, 0), tol)
        assertTrue(RetirementCalculatorEngine.commutedLumpSum(0.0, 0.5, 8.287) == 0.0)
    }
}
