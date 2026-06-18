package com.ssbmax.pdfparser.ui.components

import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.ui.theme.TransparencyStrings
import kotlin.test.Test
import kotlin.test.assertTrue

class TransparencyDialogTest {

    @Test
    fun testFormatPayslipTextRaw() {
        val original = ParsedPayslip(
            file = "payslip.pdf",
            year = 2026,
            monthNum = 5,
            monthName = "May",
            dateStr = "05/2026",
            officer = Officer(
                name = "SUNIL PAWAR",
                accountNo = "1002345098",
                pan = "ABCDE1234F",
            ),
            earnings = Earnings(
                basicPay = 100000.0,
                dearnessAllowance = 50000.0
            ),
            deductions = Deductions(
                dsopSubscription = 10000.0,
                incomeTax = 5000.0
            ),
            ledgerBalances = LedgerBalances(
                closingCreditBalance = 200000.0
            ),
            summary = PayslipSummary(
                grossPay = 150000.0,
                totalDeductions = 15000.0,
                netRemittance = 135000.0
            ),
            taxAndSavings = null
        )

        val formattedRaw = formatPayslipText(original, isSanitized = false)

        assertTrue(formattedRaw.contains("${TransparencyStrings.labelPerson}SUNIL PAWAR"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelPanNumber}ABCDE1234F"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelCdaAccount}1002345098"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelFilename}payslip.pdf"))
        
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelBasicPay}₹100000"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelDA}₹50000"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelCredits}₹150000"))
        
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelDSOP}₹10000"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelITax}₹5000"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelDebits}₹15000"))
        
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelNetRemittance}₹135000"))
        assertTrue(formattedRaw.contains("${TransparencyStrings.labelDsopBalance}₹200000"))
    }

    @Test
    fun testFormatPayslipTextSanitized() {
        val original = ParsedPayslip(
            file = "payslip.pdf",
            year = 2026,
            monthNum = 5,
            monthName = "May",
            dateStr = "05/2026",
            officer = Officer(
                name = "[OFFICER_NAME_REDACTED]",
                accountNo = "[ACCOUNT_NO_REDACTED]",
                pan = "[PAN_REDACTED]",
            ),
            earnings = Earnings(
                basicPay = 100000.0,
                dearnessAllowance = 50000.0
            ),
            deductions = Deductions(
                dsopSubscription = 10000.0,
                incomeTax = 5000.0
            ),
            ledgerBalances = LedgerBalances(
                closingCreditBalance = 200000.0
            ),
            summary = PayslipSummary(
                grossPay = 150000.0,
                totalDeductions = 15000.0,
                netRemittance = 135000.0
            ),
            taxAndSavings = null
        )

        val formattedSanitized = formatPayslipText(original, isSanitized = true)

        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelPerson}${TransparencyStrings.labelAnonymous}"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelPanNumber}[PAN_REDACTED]"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelCdaAccount}[ACCOUNT_NO_REDACTED]"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelFilename}payslip.pdf"))
        
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelBasicPay}₹100000"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelDA}₹50000"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelCredits}₹150000"))
        
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelDSOP}₹10000"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelITax}₹5000"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelDebits}₹15000"))
        
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelNetRemittance}₹135000"))
        assertTrue(formattedSanitized.contains("${TransparencyStrings.labelDsopBalance}₹200000"))
    }
}
