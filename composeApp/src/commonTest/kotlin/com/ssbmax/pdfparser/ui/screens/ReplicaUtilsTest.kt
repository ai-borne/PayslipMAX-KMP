package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReplicaUtilsTest {
    @Test
    fun `risk hardship label uses actual raw key not hardcoded RHA alias`() {
        val payslip =
            ParsedPayslip(
                file = "dec2025.pdf",
                year = 2025,
                monthNum = 12,
                monthName = "December",
                dateStr = "12/2025",
                officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
                earnings = Earnings(riskHardshipAllowance = 21125.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(21125.0, 0.0, 21125.0),
                taxAndSavings = null,
                rawEarnings = mapOf("RH12" to 21125.0),
                rawDeductions = emptyMap(),
            )

        val credits = getCreditsList(payslip)

        val rhEntry = credits.find { it.amount == 21125.0 }
        assertNotNull(rhEntry, "RH entry should exist in credits list")
        assertEquals(
            "RH12",
            rhEntry.code,
            "Label must be the actual PDF key 'RH12', not hardcoded 'RHA'",
        )
    }

    @Test
    fun testLegacyFallback() {
        val payslip =
            ParsedPayslip(
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
                rawDeductions = emptyMap(),
            )

        val credits = getCreditsList(payslip)
        val debits = getDebitsList(payslip)

        assertEquals(2, credits.size)
        assertEquals("BPAY", credits[0].code)
        assertEquals(140500.0, credits[0].amount)
        assertEquals("DA", credits[1].code)
        assertEquals(71760.0, credits[1].amount)

        assertEquals(2, debits.size)
        assertEquals("DSOP", debits[0].code)
        assertEquals(40000.0, debits[0].amount)
        assertEquals("LF", debits[1].code)
        assertEquals(878.0, debits[1].amount)
    }

    @Test
    fun testDynamicMappingWithCustomAndLedgerKeys() {
        val payslip =
            ParsedPayslip(
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
                rawEarnings =
                    mapOf(
                        "BPAY" to 140500.0,
                        "SPECIAL BONUS" to 15000.0,
                        "Op Cr Bal" to 71650.0,
                    ),
                rawDeductions =
                    mapOf(
                        "DSOP" to 40000.0,
                        "CUSTOM RECOVERY" to 2500.0,
                        "Cl. Cr. Bal." to 27119.0,
                    ),
            )

        val credits = getCreditsList(payslip)
        val debits = getDebitsList(payslip)

        assertEquals(2, credits.size)

        val bpayCredit = credits.first { it.code == "BPAY" }
        assertEquals(140500.0, bpayCredit.amount)
        assertTrue(bpayCredit.desc.contains("Core salary based on rank"))

        val customCredit = credits.first { it.code == "SPECIAL BONUS" }
        assertEquals(15000.0, customCredit.amount)
        assertEquals("Custom allowance or adjustment amount.", customCredit.desc)

        assertEquals(2, debits.size)

        val dsopDebit = debits.first { it.code == "DSOP" }
        assertEquals(40000.0, dsopDebit.amount)
        assertTrue(dsopDebit.desc.contains("Provident Fund"))

        val customDebit = debits.first { it.code == "CUSTOM RECOVERY" }
        assertEquals(2500.0, customDebit.amount)
        assertEquals("Custom deduction or recovery amount.", customDebit.desc)
    }

    @Test
    fun `ledger lines carry the SSOT field key used for confidence and corrections`() {
        val payslip =
            ParsedPayslip(
                file = "keys.pdf",
                year = 2024,
                monthNum = 1,
                monthName = "January",
                dateStr = "01/2024",
                officer = Officer("Officer Officer", "16/000/000000X", "AR*****90G"),
                earnings = Earnings(basicPay = 140500.0),
                deductions = Deductions(incomeTax = 25000.0),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(140500.0, 25000.0, 115500.0),
                taxAndSavings = null,
                rawEarnings = emptyMap(),
                rawDeductions = emptyMap(),
            )

        // Structured rows expose the domain field name, not the display code.
        assertEquals("basicPay", getCreditsList(payslip).first { it.code == "BPAY" }.fieldKey)
        assertEquals("incomeTax", getDebitsList(payslip).first { it.code == "ITAX" }.fieldKey)

        // Raw rows expose the raw label so corrections key against the same channel.
        val rawPayslip =
            payslip.copy(earnings = Earnings(), rawEarnings = mapOf("BONUS X" to 5000.0))
        assertEquals("BONUS X", getCreditsList(rawPayslip).first { it.code == "BONUS X" }.fieldKey)
    }
}
