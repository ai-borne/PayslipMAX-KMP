package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PayslipTextParser2022Test {
    @Test
    fun testParse2022Format() {
        val mockText =
            """
            01/2022
             STATEMENT OF ACCOUNT FOR 01/2022
            CDA A/C NO : 16/110/206718K
            NAME  : SUNIL SURESH PAWAR
            Basic Pay 132400
            DA 45849
            MSP 15500
            Tpt Allc 9432
            DSOPF Subn 7944
            AGIF 5000
            Incm Tax 31199
            Educ Cess 1564
            L Fee 748
            Fur 326
            Total Credit 203181 Total Debit 203181
            REMITTANCE 156400
            """.trimIndent()

        val result = PayslipTextParser.parse(mockText, "01 January 2022.pdf")

        assertTrue(result.isSuccess)
        val payslip = result.getOrNull()!!
        assertNotNull(payslip)

        assertEquals(2022, payslip.year)
        assertEquals(1, payslip.monthNum)
        assertEquals("01/2022", payslip.dateStr)

        assertEquals("SUNIL SURESH PAWAR", payslip.officer.name)
        assertEquals("16/110/206718K", payslip.officer.accountNo)

        // Earnings assertions
        assertEquals(132400.0, payslip.earnings.basicPay)
        assertEquals(45849.0, payslip.earnings.dearnessAllowance)
        assertEquals(15500.0, payslip.earnings.militaryServicePay)
        assertEquals(9432.0, payslip.earnings.transportAllowance)

        // Deductions assertions
        assertEquals(7944.0, payslip.deductions.dsopSubscription)
        assertEquals(5000.0, payslip.deductions.agif)
        assertEquals(31199.0, payslip.deductions.incomeTax)
        assertEquals(1564.0, payslip.deductions.educationCess)
        assertEquals(748.0, payslip.deductions.licenseFee)
        assertEquals(326.0, payslip.deductions.furnitureRent)

        // Totals assertions
        assertEquals(203181.0, payslip.summary.grossPay)
        assertEquals(46781.0, payslip.summary.totalDeductions)
        assertEquals(156400.0, payslip.summary.netRemittance)
    }
}
