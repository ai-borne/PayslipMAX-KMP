package com.payslipmax.pdfparser.insights

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RetirementCalculatorEngineTest {
    private val tol = 0.5

    @Test
    fun retiringPensionIsHalfOfReckonableEmoluments() {
        assertEquals(57750.0, RetirementCalculatorEngine.retiringPension(basicPay = 100000.0, militaryServicePay = 15500.0), tol)
        assertEquals(58000.0, RetirementCalculatorEngine.retiringPension(basicPay = 100000.0, militaryServicePay = 15500.0, classPay = 500.0), tol)
    }

    @Test
    fun netMonthlyPensionCalculatesDearnessReliefOnFullUncommutedPension() {
        // Basic Pension = 50000. Commuted 50% -> Residual pension = 25000.
        // DR @ 50% on FULL 50000 pension = 25000.
        // Total Net Monthly Payout = 25000 + 25000 = 50000.
        val netMonthly =
            RetirementCalculatorEngine.calculateNetMonthlyPension(
                basicPension = 50000.0,
                commuteFraction = 0.50,
                daPercentage = 50.0,
            )
        assertEquals(50000.0, netMonthly, tol)
    }

    @Test
    fun gratuityIsQuarterEmolumentsPerSixMonthlyPeriod() {
        assertEquals(1_300_000.0, RetirementCalculatorEngine.retirementGratuity(100000.0, 30000.0, 20.0), tol)
    }

    @Test
    fun gratuityIsCappedAt16Point5TimesEmoluments() {
        assertEquals(1_650_000.0, RetirementCalculatorEngine.retirementGratuity(100000.0, 0.0, 40.0), tol)
    }

    @Test
    fun gratuityIsCappedAtAbsoluteCeiling() {
        assertEquals(2_500_000.0, RetirementCalculatorEngine.retirementGratuity(200000.0, 50000.0, 33.0), tol)
    }

    @Test
    fun commutationFactorCoversAges20To67() {
        assertEquals(9.136, RetirementCalculatorEngine.commutationFactor(36))
        assertEquals(8.678, RetirementCalculatorEngine.commutationFactor(54))
        assertEquals(8.287, RetirementCalculatorEngine.commutationFactor(60))
        assertEquals(7.431, RetirementCalculatorEngine.commutationFactor(67))
        assertNull(RetirementCalculatorEngine.commutationFactor(19))
        assertNull(RetirementCalculatorEngine.commutationFactor(68))
    }

    @Test
    fun commutedLumpSumUsesPensionFractionTwelveAndFactor() {
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
    fun leaveEncashmentExcludesMilitaryServicePayPerPcdaRule() {
        // (100000 + 30000)/30 * 300 = 1,300,000 (MSP excluded!)
        assertEquals(1_300_000.0, RetirementCalculatorEngine.leaveEncashment(100000.0, 30000.0, 300), tol)
        // days beyond 300 are capped
        assertEquals(
            RetirementCalculatorEngine.leaveEncashment(100000.0, 30000.0, 300),
            RetirementCalculatorEngine.leaveEncashment(100000.0, 30000.0, 400),
            tol,
        )
    }

    @Test
    fun disabilityPensionIsNonTaxableThirtyPercentOfEmolumentsScaled() {
        val emoluments = 100000.0 // BP + MSP + CL PAY
        // 100% disability -> 30,000/mo
        assertEquals(30000.0, RetirementCalculatorEngine.calculateDisabilityPension(emoluments, 100), tol)
        // 50% disability -> 15,000/mo
        assertEquals(15000.0, RetirementCalculatorEngine.calculateDisabilityPension(emoluments, 50), tol)
        // <20% disability -> 0.0
        assertEquals(0.0, RetirementCalculatorEngine.calculateDisabilityPension(emoluments, 15), tol)
    }

    @Test
    fun agifMaturityDeductsExtendedInsuranceCover() {
        // 1,000,000 - 160,000 (extended cover) = 840,000
        assertEquals(840000.0, RetirementCalculatorEngine.calculateAgifMaturity(1000000.0, 0.0), tol)
    }

    @Test
    fun commutationMatrixGeneratesThreeScenarios() {
        val matrix = RetirementCalculatorEngine.calculateCommutationMatrix(basicPension = 50000.0, ageNextBirthday = 54, daPercentage = 50.0)
        assertEquals(3, matrix.size)

        val scenario0 = matrix[0]
        assertEquals(0.0, scenario0.fraction)
        assertEquals(0.0, scenario0.lumpSum)
        assertEquals(75000.0, scenario0.netMonthlyPayout) // 50000 pension + 25000 DR

        val scenario50 = matrix[2]
        assertEquals(0.50, scenario50.fraction)
        assertTrue(scenario50.lumpSum > 0.0)
        assertEquals(50000.0, scenario50.netMonthlyPayout) // 25000 residual + 25000 DR
        assertTrue(scenario50.breakEvenRoiPercent > 0.0)
    }

    @Test
    fun zeroOrNegativeInputsProduceZero() {
        assertEquals(0.0, RetirementCalculatorEngine.retirementGratuity(100000.0, 0.0, 0.0), tol)
        assertEquals(0.0, RetirementCalculatorEngine.leaveEncashment(100000.0, 0.0, 0), tol)
        assertTrue(RetirementCalculatorEngine.commutedLumpSum(0.0, 0.5, 8.287) == 0.0)
    }
}
