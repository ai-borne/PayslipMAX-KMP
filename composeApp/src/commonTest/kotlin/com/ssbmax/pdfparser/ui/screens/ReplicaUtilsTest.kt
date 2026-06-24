package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReplicaUtilsTest {

    @Test
    fun testLegacyFallback() {
        val payslip = ParsedPayslip(
            file = "legacy.pdf",
            year = 2024,
            monthNum = 1,
            monthName = "January",
            dateStr = "01/2024",
            officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
            earnings = Earnings(basicPay = 140500.0, dearnessAllowance = 71760.0),
            deductions = Deductions(dsopSubscription = 40000.0, licenseFee = 878.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(212260.0, 40878.0, 171382.0),
            taxAndSavings = null,
            rawEarnings = emptyMap(),
            rawDeductions = emptyMap()
        )

        val credits = getCreditsList(payslip)
        val debits = getDebitsList(payslip)

        assertEquals(2, credits.size)
        assertEquals("BPAY", credits[0].first)
        assertEquals(140500.0, credits[0].second)
        assertEquals("DA", credits[1].first)
        assertEquals(71760.0, credits[1].second)

        assertEquals(2, debits.size)
        assertEquals("DSOP", debits[0].first)
        assertEquals(40000.0, debits[0].second)
        assertEquals("LF", debits[1].first)
        assertEquals(878.0, debits[1].second)
    }

    @Test
    fun testDynamicMappingWithCustomAndLedgerKeys() {
        val payslip = ParsedPayslip(
            file = "dynamic.pdf",
            year = 2024,
            monthNum = 1,
            monthName = "January",
            dateStr = "01/2024",
            officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
            earnings = Earnings(),
            deductions = Deductions(),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(212260.0, 40878.0, 171382.0),
            taxAndSavings = null,
            rawEarnings = mapOf(
                "BPAY" to 140500.0,
                "SPECIAL BONUS" to 15000.0,
                "Op Cr Bal" to 71650.0  // Ledger balance, should be excluded
            ),
            rawDeductions = mapOf(
                "DSOP" to 40000.0,
                "CUSTOM RECOVERY" to 2500.0,
                "Cl. Cr. Bal." to 27119.0 // Ledger balance, should be excluded
            )
        )

        val credits = getCreditsList(payslip)
        val debits = getDebitsList(payslip)

        // Excludes Op Cr Bal
        assertEquals(2, credits.size)
        
        // Assert Standard Key BPAY has premium description
        val bpayCredit = credits.first { it.first == "BPAY" }
        assertEquals(140500.0, bpayCredit.second)
        assertTrue(bpayCredit.third.contains("Core salary based on rank"))

        // Assert Custom Key SPECIAL BONUS has generic description
        val customCredit = credits.first { it.first == "SPECIAL BONUS" }
        assertEquals(15000.0, customCredit.second)
        assertEquals("Custom allowance or adjustment amount.", customCredit.third)

        // Excludes Cl. Cr. Bal.
        assertEquals(2, debits.size)

        // Assert Standard Key DSOP has premium description
        val dsopDebit = debits.first { it.first == "DSOP" }
        assertEquals(40000.0, dsopDebit.second)
        assertTrue(dsopDebit.third.contains("Provident Fund"))

        // Assert Custom Key CUSTOM RECOVERY has generic description
        val customDebit = debits.first { it.first == "CUSTOM RECOVERY" }
        assertEquals(2500.0, customDebit.second)
        assertEquals("Custom deduction or recovery amount.", customDebit.third)
    }
}
