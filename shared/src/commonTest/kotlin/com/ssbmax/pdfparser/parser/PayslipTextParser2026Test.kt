package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PayslipTextParser2026Test {
    @Test
    fun testParse2026Format() {
        val leftText =
            """
            BPAY (12A) 149000
            DA 98700
            MSP 15500
            RH12 21125
            TPTA 3600
            TPTADA 2160
            ARR-DA 9870
            ARR-TPTADA 216
            ETKT 1657
            """.trimIndent()

        val middleText =
            """
            ETKT 3056
            DSOP 40000
            AGIF 12500
            ITAX 50425
            EHCESS 2017
            """.trimIndent()

        val fullText =
            """
            04/2026  STATEMENT OF ACCOUNT FOR 04/2026
            Name: Officer Officer Officer A/C No: 16/111/206718K PAN No: AR*****90G
            kuula Aaya Gross Pay 301828 kuula kTaOtI Total Deductions 107998
            Net Remittance : Rs.1,93,830
            """.trimIndent()

        val result =
            PayslipTextParser.parse(
                leftColumnText = leftText,
                middleColumnText = middleText,
                fullText = fullText,
                filename = "04 Apr 2026.pdf",
            )
        assertTrue(result.isSuccess)
        val payslip = result.getOrNull()!!
        assertNotNull(payslip)

        println("--- rawEarnings ---")
        payslip.rawEarnings.forEach { (k, v) -> println("$k: $v") }
        println("--- rawDeductions ---")
        payslip.rawDeductions.forEach { (k, v) -> println("$k: $v") }

        // Assert values are parsed into structured maps
        assertEquals(149000.0, payslip.earnings.basicPay)
        assertEquals(98700.0, payslip.earnings.dearnessAllowance)
        assertEquals(15500.0, payslip.earnings.militaryServicePay)
        assertEquals(21125.0, payslip.earnings.riskHardshipAllowance)
        assertEquals(3600.0, payslip.earnings.transportAllowance)
        assertEquals(2160.0, payslip.earnings.transportAllowanceDa)
        assertEquals(9870.0, payslip.earnings.arrearsDa)
        assertEquals(216.0, payslip.earnings.arrearsTptaDa)
        assertEquals(1657.0, payslip.earnings.adjPayAndAllce)

        assertEquals(40000.0, payslip.deductions.dsopSubscription)
        assertEquals(12500.0, payslip.deductions.agif)
        assertEquals(50425.0, payslip.deductions.incomeTax)
        assertEquals(2017.0, payslip.deductions.educationCess)
        assertEquals(3056.0, payslip.deductions.ticketRecovery)

        // Totals assertions
        assertEquals(301828.0, payslip.summary.grossPay)
        assertEquals(107998.0, payslip.summary.totalDeductions)
        assertEquals(193830.0, payslip.summary.netRemittance)
    }
}
