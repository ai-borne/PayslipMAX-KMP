package com.payslipmax.pdfparser.insights

import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals

class DsopRoomCalculatorTest {
    private fun payslip(
        year: Int,
        month: Int,
        dsop: Double,
    ) = ParsedPayslip(
        file = "t.pdf",
        year = year,
        monthNum = month,
        monthName = "",
        dateStr = "$month/$year",
        officer = Officer("N", "A", "P"),
        earnings = Earnings(basicPay = 100000.0),
        deductions = Deductions(dsopSubscription = dsop),
        ledgerBalances = LedgerBalances(),
        summary = PayslipSummary(0.0, 0.0, 0.0),
        taxAndSavings = null,
    )

    @Test
    fun sumsOnlyTheCurrentFinancialYearsSubscriptions() {
        val current = payslip(2026, 6, 40000.0)
        // Same financial year (Apr 2026-Mar 2027): Apr and May count; Mar 2026 is the previous FY and is excluded.
        val history =
            listOf(
                payslip(2026, 4, 40000.0),
                payslip(2026, 5, 40000.0),
                payslip(2026, 3, 40000.0),
            )

        val room = DsopRoomCalculator.calculate(current, history)

        assertEquals("FY 2026-27", room.financialYearLabel)
        assertEquals(120000.0, room.subscribedYtd)
        assertEquals(380000.0, room.roomLeft)
    }

    @Test
    fun aJanuaryPayslipBelongsToTheFinancialYearThatStartedTheAprilBefore() {
        val current = payslip(2027, 1, 45000.0)
        val history = listOf(payslip(2026, 4, 45000.0))

        val room = DsopRoomCalculator.calculate(current, history)

        assertEquals("FY 2026-27", room.financialYearLabel)
        assertEquals(90000.0, room.subscribedYtd)
    }

    @Test
    fun roomLeftNeverGoesBelowZero() {
        val current = payslip(2026, 4, 600000.0)

        val room = DsopRoomCalculator.calculate(current, emptyList())

        assertEquals(0.0, room.roomLeft)
    }

    @Test
    fun aDuplicateMonthIsNotDoubleCounted() {
        val current = payslip(2026, 6, 40000.0)
        val history = listOf(payslip(2026, 6, 40000.0)) // same month re-imported

        val room = DsopRoomCalculator.calculate(current, history)

        assertEquals(40000.0, room.subscribedYtd)
    }

    @Test
    fun currentTakesPrecedenceOverAStaleDuplicateInHistory() {
        // The stored history still holds the original, uncorrected figure for June; the freshly re-parsed
        // "current" payslip for the same month carries the corrected one (P7-06).
        val current = payslip(2026, 6, 45000.0)
        val history = listOf(payslip(2026, 6, 40000.0))

        val room = DsopRoomCalculator.calculate(current, history)

        assertEquals(45000.0, room.subscribedYtd)
    }
}
