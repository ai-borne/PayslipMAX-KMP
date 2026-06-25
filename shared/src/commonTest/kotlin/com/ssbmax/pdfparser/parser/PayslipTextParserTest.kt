package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PayslipTextParserTest {
    @Test
    fun testParse2024Format() {
        val mockText =
            """
            01/2024  kI laoKa ivavarNaI  / STATEMENT OF ACCOUNT FOR 01/2024
            Name: Officer Officer Officer A/C No - 16/000/000000X PAN No: AR*****90G
            Aaya / EARNINGS (`) kTaOtI / DEDUCTIONS (`) laona dona ka ivavarNa / DETAILS OF TRANSACTIONS
            ivavarNa Description raiSa Amount ivavarNa Description raiSa Amount
            BPAY 140500
            DA 71760
            MSP 15500
            TPTA 3600
            TPTADA 1656
            FUR 392
            LF 878
            DSOP 40000
            AGIF 10000
            ITAX 40521
            EHCESS 1621
            1.  Recovery of LF  from 01/01/2024 to 31/01/2024. (Bill No RL/04/8713/.  ) 878
            2.  Recovery of FUR  from 01/01/2024 to 31/01/2024. (Bill No RL/04/8713/.  ) 392
            kuula Aaya Gross Pay 233016 kuula kTaOtI Total Deductions 93412
            Net Remittance : Rs.1,39,604 (One Lakh Thirty Nine Thousand Six Hundred Four  only)
            
            Assessment Year 2024-2025. Period 01/04/2023 to 31/03/2024
            1. Gross Salary upto 31/01/2024 2547493
            2. HRA Amount 0
            3. Gross Salary upto 31/01/2024 excluding HRA 2547493
            4. Estimated future Salary upto  31/03/2024 233016
            5. Income from Other Source/House Property 0
            6. Total Taxable Income 2780509
            7. Deduction 0
            8. Standard Deduction 50000
            9. Net Taxable Income 2730510
            10. Total Tax Payable 519153
            11. Income Tax Deducted 478631
            12. Ed. Cess Deducted 19061
            13. Surcharge Deducted 0
            Opening Balance 670282 Subscription 400000 Refund 0 Misc Adj 0 Withdrawal 0 Closing Balance 1070282
            """.trimIndent()

        val result = PayslipTextParser.parse(mockText, "01 Jan 2024.pdf")

        assertTrue(result.isSuccess)
        val payslip = result.getOrNull()!!
        assertNotNull(payslip)

        assertEquals(2024, payslip.year)
        assertEquals(1, payslip.monthNum)
        assertEquals("January", payslip.monthName)
        assertEquals("01/2024", payslip.dateStr)

        assertEquals("Officer Officer Officer", payslip.officer.name)
        assertEquals("16/000/000000X", payslip.officer.accountNo)
        assertEquals("AR*****90G", payslip.officer.pan)

        assertEquals(140500.0, payslip.earnings.basicPay)
        assertEquals(71760.0, payslip.earnings.dearnessAllowance)
        assertEquals(15500.0, payslip.earnings.militaryServicePay)
        assertEquals(3600.0, payslip.earnings.transportAllowance)
        assertEquals(1656.0, payslip.earnings.transportAllowanceDa)

        assertEquals(40000.0, payslip.deductions.dsopSubscription)
        assertEquals(10000.0, payslip.deductions.agif)
        assertEquals(40521.0, payslip.deductions.incomeTax)
        assertEquals(1621.0, payslip.deductions.educationCess)
        assertEquals(878.0, payslip.deductions.licenseFee)
        assertEquals(392.0, payslip.deductions.furnitureRent)

        assertEquals(233016.0, payslip.summary.grossPay)
        assertEquals(93412.0, payslip.summary.totalDeductions)
        assertEquals(139604.0, payslip.summary.netRemittance)

        val tax = payslip.taxAndSavings!!
        assertNotNull(tax)
        assertEquals(2547493.0, tax.grossSalaryYtd)
        assertEquals(2780509.0, tax.totalTaxableIncome)
        assertEquals(50000.0, tax.standardDeduction)
        assertEquals(2730510.0, tax.netTaxableIncome)
        assertEquals(519153.0, tax.totalTaxPayable)
        assertEquals(478631.0, tax.taxDeductedYtd)
        assertEquals(19061.0, tax.cessDeductedYtd)

        val dsop = tax.dsopFund!!
        assertNotNull(dsop)
        assertEquals(670282.0, dsop.openingBalance)
        assertEquals(400000.0, dsop.subscriptionYtd)
        assertEquals(0.0, dsop.refundYtd)
        assertEquals(0.0, dsop.miscAdjYtd)
        assertEquals(0.0, dsop.withdrawalYtd)
        assertEquals(1070282.0, dsop.closingBalance)
    }

    @Test
    fun testParse2025FormatWithColumnSplitAndDuplicateKeys() {
        val leftText =
            """
            BPAY (12A) 144200
            DA 76426
            MSP 15500
            TPTA 7200
            TPTADA 3816
            RH12 3450
            ARR-RH12 3450
            ARR-RH12 3450
            A/o Pay & Allce 2000
            """.trimIndent()

        val middleText =
            """
            DSOP 25000
            AGIF 5000
            ITAX 40521
            EHCESS 1621
            LF 878
            FUR 392
            """.trimIndent()

        val fullText =
            """
            01/2025  kI laoKa ivavarNaI  / STATEMENT OF ACCOUNT FOR 01/2025
            Name: Officer Officer Officer A/C No - 16/000/000000X PAN No: AR*****90G
            kuula Aaya Gross Pay 256042 kuula kTaOtI Total Deductions 32891
            Net Remittance : Rs.2,23,151
            """.trimIndent()

        val result =
            PayslipTextParser.parse(
                leftColumnText = leftText,
                middleColumnText = middleText,
                fullText = fullText,
                filename = "01 January 2025.pdf",
            )

        assertTrue(result.isSuccess)
        val payslip = result.getOrNull()!!
        assertNotNull(payslip)

        assertEquals(2025, payslip.year)
        assertEquals(1, payslip.monthNum)
        assertEquals("01/2025", payslip.dateStr)

        assertEquals("Officer Officer Officer", payslip.officer.name)
        assertEquals("16/000/000000X", payslip.officer.accountNo)
        assertEquals("AR*****90G", payslip.officer.pan)

        assertEquals(144200.0, payslip.earnings.basicPay)
        assertEquals(76426.0, payslip.earnings.dearnessAllowance)
        assertEquals(15500.0, payslip.earnings.militaryServicePay)
        assertEquals(7200.0, payslip.earnings.transportAllowance)
        assertEquals(3816.0, payslip.earnings.transportAllowanceDa)
        assertEquals(0.0, payslip.earnings.rationMoney)
        assertEquals(3450.0, payslip.earnings.riskHardshipAllowance)
        assertEquals(6900.0, payslip.earnings.arrearsRiskHardship)
        assertEquals(2000.0, payslip.earnings.adjPayAndAllce)

        assertEquals(25000.0, payslip.deductions.dsopSubscription)
        assertEquals(5000.0, payslip.deductions.agif)
        assertEquals(40521.0, payslip.deductions.incomeTax)
        assertEquals(1621.0, payslip.deductions.educationCess)
        assertEquals(878.0, payslip.deductions.licenseFee)
        assertEquals(392.0, payslip.deductions.furnitureRent)

        assertEquals(256042.0, payslip.summary.grossPay)
        assertEquals(32891.0, payslip.summary.totalDeductions)
        assertEquals(223151.0, payslip.summary.netRemittance)
    }

    @Test
    fun testParseWithRenamedFileHistoricalCorrections() {
        // Mock a statement for April 2022 (04/2022)
        val mockText =
            """
            04/2022  kI laoKa ivavarNaI  / STATEMENT OF ACCOUNT FOR 04/2022
            Name: Officer Officer Officer A/C No - 16/000/000000X PAN No: AR*****90G
            Aaya / EARNINGS (`) kTaOtI / DEDUCTIONS (`) laona dona ka ivavarNa / DETAILS OF TRANSACTIONS
            BPAY 130000
            DA 40000
            MSP 15500
            kuula Aaya Gross Pay 185500 kuula kTaOtI Total Deductions 0
            Net Remittance : Rs.1,85,500
            """.trimIndent()

        // Pass a totally different filename (not "04 April 2022.pdf")
        val result = PayslipTextParser.parse(mockText, "my_custom_renamed_payslip.pdf")
        assertTrue(result.isSuccess)
        val payslip = result.getOrNull()!!

        // Assert that the corrections (BPAY + 14, DA + 29, MSP + 24) are applied
        // since the parser extracts April 2022 from the content.
        assertEquals(130014.0, payslip.earnings.basicPay)
        assertEquals(40029.0, payslip.earnings.dearnessAllowance)
        assertEquals(15524.0, payslip.earnings.militaryServicePay)
    }

    @Test
    fun testRegex() {
        val text = "kula Aaya Gross Pay 337772 kula kTaOtI Total Deductions 259772"
        val cleaned = negateHindiTransliterations(cleanCommasAndWhitespace(text))
        println("cleaned: '$cleaned'")
        val (gross, ded, net) = parseTotals(cleaned)
        println("[TEST RESULT] gross: $gross, ded: $ded")
    }

    @Test
    fun testNetTaxableIncomeParsingAndCapping() {
        // Test Case 1: Simple parenthetical formula (6 - 7 - 8)
        val text1 =
            """
            1. Gross Salary upto 30/04/2026 586894
            6. Total Taxable Income 3487744
            8. Standard Deduction 75000
            9. Net Taxable Income (6 - 7 - 8) 3412740
            10. Total Tax Payable 603822
            """.trimIndent()
        val res1 = parseTaxAndSavings(text1, null, text1)
        assertNotNull(res1)
        assertEquals(3412740.0, res1.netTaxableIncome)
        // Verify early-month capping: since 603822 > 586894, totalTaxPayable is capped to 586894 * 0.30 = 176068.2 (rounded to 176068.0)
        assertEquals(176068.0, res1.totalTaxPayable)

        // Test Case 2: Nested parenthetical formula
        val text2 =
            """
            Gross Salary upto 31/01/2024 2547493
            Total Taxable Income 2780509
            Standard Deduction 50000
            Net Taxable Income ((Sl.No. 6 + Sl.No. 7) - (Sl.No. 8)) 2730510
            Total Tax Payable 519153
            """.trimIndent()
        val res2 = parseTaxAndSavings(text2, null, text2)
        assertNotNull(res2)
        assertEquals(2730510.0, res2.netTaxableIncome)
        // No capping should apply since 519153 < 2547493
        assertEquals(519153.0, res2.totalTaxPayable)
    }

    @Test
    fun testDynamicSpatialParserKeyValueExtraction() {
        val sampleText = """
            BPAY (12A) 144700
            DA 84906
            CC to bankers 11923
            to 31/08/2024
        """.trimIndent()
        
        val pairs = DynamicSpatialParser.extractDynamicPairs(sampleText)
        assertEquals(144700.0, pairs["BPAY"])
        assertEquals(84906.0, pairs["DA"])
        assertEquals(11923.0, pairs["CC to bankers"])
        // standalone "to" should be blocked
        assertTrue(pairs["to"] == null)

        val sampleTextWithHeaders = """
            ivavarNa
            Description
            raiSa
            Amount
            BPAY (12A) 144700
            DA 84906
        """.trimIndent()
        val cleanedText = cleanPreservingNewlines(sampleTextWithHeaders)
        val pairsWithHeaders = DynamicSpatialParser.extractDynamicPairs(cleanedText)
        assertEquals(144700.0, pairsWithHeaders["BPAY"])
        assertEquals(84906.0, pairsWithHeaders["DA"])
        assertTrue(pairsWithHeaders["Description Amount BPAY"] == null)
    }


    @Test
    fun testMathematicalReconciliationFailure() {
        val mockText =
            """
            01/2024  STATEMENT OF ACCOUNT FOR 01/2024
            Name: Officer Officer Officer A/C No - 16/000/000000X PAN No: AR*****90G
            BPAY 140500
            DA 71760
            DSOP 40000
            kuula Aaya Gross Pay 212260 kuula kTaOtI Total Deductions 40000
            Net Remittance : Rs.130000
            """.trimIndent()

        val result = PayslipTextParser.parse(mockText, "01 Jan 2024.pdf")
        assertTrue(result.isFailure)
        val msg = result.exceptionOrNull()?.message ?: ""
        assertTrue(msg.contains("reconciliation check failed"), "Error message should complain about reconciliation failure, got: '$msg'")
    }
}
