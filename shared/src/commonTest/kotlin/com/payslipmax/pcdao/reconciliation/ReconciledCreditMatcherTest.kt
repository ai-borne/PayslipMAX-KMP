package com.payslipmax.pcdao.reconciliation

import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReconciledCreditMatcherTest {
    private val matcher = ReconciledCreditMatcher()

    private fun createBasePayslip(
        earnings: Earnings = Earnings(),
        rawEarnings: Map<String, Double> = emptyMap(),
    ): ParsedPayslip =
        ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = 8,
            monthName = "August",
            dateStr = "31-08-2026",
            officer = Officer("Col Rathore", "IC12345", "ABCDE1234F"),
            earnings = earnings,
            deductions = com.payslipmax.pdfparser.domain.Deductions(),
            ledgerBalances = com.payslipmax.pdfparser.domain.LedgerBalances(),
            summary = PayslipSummary(grossPay = 0.0, totalDeductions = 0.0, netRemittance = 0.0),
            taxAndSavings = null,
            rawEarnings = rawEarnings,
        )

    @Test
    fun testExtractStructuredEarningsCredits() {
        val slip =
            createBasePayslip(
                earnings =
                    Earnings(
                        transportAllowance = 7200.0,
                        transportAllowanceDa = 4320.0,
                        houseRentAllowance = 44700.0,
                        childrenEducationAllowance = 33750.0,
                        riskHardshipAllowance = 21125.0,
                        nonPracticingAllowance = 29800.0,
                        technicalAllowance = 4500.0,
                        specialForcesPay = 13125.0,
                    ),
            )

        assertEquals(11520.0, matcher.getCreditedAmount(slip, "TPTA"))
        assertEquals(44700.0, matcher.getCreditedAmount(slip, "HRA_SPR"))
        assertEquals(2812.50, matcher.getCreditedAmount(slip, "CEA"))
        assertEquals(21125.0, matcher.getCreditedAmount(slip, "HAFAA"))
        assertEquals(21125.0, matcher.getCreditedAmount(slip, "CFAA"))
        assertEquals(21125.0, matcher.getCreditedAmount(slip, "SIACHEN"))
        assertEquals(29800.0, matcher.getCreditedAmount(slip, "NPA"))
        assertEquals(4500.0, matcher.getCreditedAmount(slip, "TECHNICAL_PAY"))
        assertEquals(13125.0, matcher.getCreditedAmount(slip, "PARACHUTE_ALLOWANCE"))
    }

    @Test
    fun testExtractRawEarningsFallbackCredits() {
        val slip =
            createBasePayslip(
                rawEarnings =
                    mapOf(
                        "RH12" to 21125.0,
                        "SDA" to 14900.0,
                        "ISDA" to 23840.0,
                        "TRGALW" to 17880.0,
                        "LVELTC" to 49666.0,
                        "CTG" to 119200.0,
                        "TECI" to 3000.0,
                        "SPCDO" to 10500.0,
                    ),
            )

        assertEquals(21125.0, matcher.getCreditedAmount(slip, "HAFAA"))
        assertEquals(14900.0, matcher.getCreditedAmount(slip, "SDA"))
        assertEquals(23840.0, matcher.getCreditedAmount(slip, "ISDA"))
        assertEquals(17880.0, matcher.getCreditedAmount(slip, "TRAINING_ALLOWANCE"))
        assertEquals(49666.0, matcher.getCreditedAmount(slip, "LTC_ENCASHMENT"))
        assertEquals(119200.0, matcher.getCreditedAmount(slip, "CTG"))
        assertEquals(3000.0, matcher.getCreditedAmount(slip, "TECHNICAL_PAY"))
        assertEquals(10500.0, matcher.getCreditedAmount(slip, "PARACHUTE_ALLOWANCE"))
    }

    @Test
    fun testCeaCreditDetectionAcrossStructuredAndRaw() {
        val slipWithoutCea = createBasePayslip()
        assertFalse(matcher.hasCeaCredit(slipWithoutCea))

        val slipWithMonthlyCea = createBasePayslip(earnings = Earnings(childrenEducationAllowance = 33750.0))
        assertTrue(matcher.hasCeaCredit(slipWithMonthlyCea))

        val slipWithArrearsCea = createBasePayslip(earnings = Earnings(arrearsCea = 33750.0))
        assertTrue(matcher.hasCeaCredit(slipWithArrearsCea))

        val slipWithRawCea = createBasePayslip(rawEarnings = mapOf("ARR-CEA" to 33750.0))
        assertTrue(matcher.hasCeaCredit(slipWithRawCea))

        val slipWithHostel = createBasePayslip(rawEarnings = mapOf("HOSTEL" to 81000.0))
        assertTrue(matcher.hasCeaCredit(slipWithHostel))
    }
}
