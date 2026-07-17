package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnexpectedDebitAuditorTest {
    private fun createMockPayslip(
        debitRecovery: Double = 0.0,
        ticketRecovery: Double = 0.0,
    ): ParsedPayslip {
        val gross = 100000.0
        val deductions = debitRecovery + ticketRecovery
        return ParsedPayslip(
            file = "test.pdf",
            year = 2026,
            monthNum = 4,
            monthName = "April",
            dateStr = "04/2026",
            officer = Officer("Name", "Acc", "PAN"),
            earnings = Earnings(),
            deductions =
                Deductions(
                    recoveryOfDebits = debitRecovery,
                    ticketRecovery = ticketRecovery,
                ),
            ledgerBalances = LedgerBalances(),
            summary = PayslipSummary(grossPay = gross, totalDeductions = deductions, netRemittance = gross - deductions),
            taxAndSavings = null,
        )
    }

    @Test
    fun testNoDebitRecoveryReturnsEmpty() {
        val current = createMockPayslip()
        val auditor = UnexpectedDebitAuditor()
        val result = auditor.audit(current, null, emptyList())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testDebitRecoveryTriggered() {
        val current = createMockPayslip(debitRecovery = 5000.0)
        val auditor = UnexpectedDebitAuditor()
        val result = auditor.audit(current, null, emptyList())

        assertEquals(1, result.size)
        val anomaly = result.first()
        assertEquals("DEBIT_RECOVERY", anomaly.type)
        assertTrue(anomaly.description.contains("Unexpected deduction"))
        assertTrue(anomaly.description.contains("5.0%")) // 5000/100000 = 5%
    }
}
