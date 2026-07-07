package com.payslipmax.pdfparser.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModelCorrectionTest {
    private fun createBasePayslip(): ParsedPayslip {
        return ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = 7,
            monthName = "July",
            dateStr = "07/2026",
            officer = Officer("test", "CDA123", "PAN123"),
            earnings = Earnings(basicPay = 140000.0, dearnessAllowance = 90000.0),
            deductions = Deductions(incomeTax = 40000.0),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(230000.0, 40000.0, 190000.0),
            taxAndSavings = null,
            rawEarnings = mapOf("UNKNOWN_ALLOWANCE" to 2000.0),
            rawDeductions = mapOf("UNKNOWN_DEBIT" to 500.0),
        )
    }

    @Test
    fun testDeleteStructuredCorrectionZeroesValue() {
        val base = createBasePayslip()
        val corrections =
            listOf(
                SingleCorrection(
                    fieldKey = "basicPay",
                    codeHead = "BPAY",
                    amount = 0.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.DELETED,
                    originalAmount = 140000.0,
                    originalCodeHead = "BPAY",
                    timestamp = 123456L,
                ),
            )

        val result = base.applyCorrections(corrections)
        assertEquals(0.0, result.earnings.basicPay)
        // Check that other fields are unaffected
        assertEquals(90000.0, result.earnings.dearnessAllowance)
    }

    @Test
    fun testDeleteRawCorrectionRemovesKey() {
        val base = createBasePayslip()
        val corrections =
            listOf(
                SingleCorrection(
                    fieldKey = "UNKNOWN_ALLOWANCE",
                    codeHead = "UNKNOWN_ALLOWANCE",
                    amount = 0.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.DELETED,
                    originalAmount = 2000.0,
                    originalCodeHead = "UNKNOWN_ALLOWANCE",
                    timestamp = 123456L,
                ),
            )

        val result = base.applyCorrections(corrections)
        assertNull(result.rawEarnings["UNKNOWN_ALLOWANCE"])
    }

    @Test
    fun testEditStructuredCorrectionUpdatesValue() {
        val base = createBasePayslip()
        val corrections =
            listOf(
                SingleCorrection(
                    fieldKey = "dearnessAllowance",
                    codeHead = "DA",
                    amount = 95000.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.EDITED,
                    originalAmount = 90000.0,
                    originalCodeHead = "DA",
                    timestamp = 123456L,
                ),
            )

        val result = base.applyCorrections(corrections)
        assertEquals(95000.0, result.earnings.dearnessAllowance)
    }

    @Test
    fun testAddEarningStructuredCorrectionUpdatesValue() {
        val base = createBasePayslip()
        // CEA matches standard field childrenEducationAllowance
        val corrections =
            listOf(
                SingleCorrection(
                    fieldKey = "childrenEducationAllowance",
                    codeHead = "CEA",
                    amount = 4500.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.ADDED,
                    originalAmount = null,
                    originalCodeHead = null,
                    timestamp = 123456L,
                ),
            )

        val result = base.applyCorrections(corrections)
        assertEquals(4500.0, result.earnings.childrenEducationAllowance)
        assertTrue(result.rawEarnings.containsKey("UNKNOWN_ALLOWANCE"))
    }

    @Test
    fun testAddEarningRawCorrectionAppendsKey() {
        val base = createBasePayslip()
        val corrections =
            listOf(
                SingleCorrection(
                    fieldKey = "NEW_CUSTOM_EARNING",
                    codeHead = "CUSTOM_EARN",
                    amount = 5000.0,
                    category = EntryCategory.EARNING,
                    type = CorrectionType.ADDED,
                    originalAmount = null,
                    originalCodeHead = null,
                    timestamp = 123456L,
                ),
            )

        val result = base.applyCorrections(corrections)
        assertEquals(5000.0, result.rawEarnings["CUSTOM_EARN"])
    }
}
