package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals

class RedactionSanitizerTest {

    @Test
    fun testPiiRedaction() {
        val original = ParsedPayslip(
            file = "/users/sunil/payslips/2026-SunilPawar-May.pdf",
            year = 2026,
            monthNum = 5,
            monthName = "May",
            dateStr = "05/2026",
            officer = Officer(
                name = "SUNIL PAWAR",
                accountNo = "1002345098",
                pan = "ABCDE1234F"
            ),
            earnings = Earnings(basicPay = 105300.0),
            deductions = Deductions(dsopSubscription = 12000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = 186000.0, totalDeductions = 15000.0, netRemittance = 171000.0),
            taxAndSavings = null
        )

        val redacted = RedactionSanitizer.redact(original)

        assertEquals("payslip.pdf", redacted.file)
        assertEquals("[OFFICER_NAME_REDACTED]", redacted.officer.name)
        assertEquals("[ACCOUNT_NO_REDACTED]", redacted.officer.accountNo)
        assertEquals("[PAN_REDACTED]", redacted.officer.pan)

        assertEquals(original.year, redacted.year)
        assertEquals(original.monthNum, redacted.monthNum)
        assertEquals(original.earnings.basicPay, redacted.earnings.basicPay)
        assertEquals(original.summary.grossPay, redacted.summary.grossPay)
    }
}
