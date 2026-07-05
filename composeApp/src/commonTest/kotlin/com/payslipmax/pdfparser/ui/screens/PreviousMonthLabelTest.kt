package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private fun record(
    year: Int,
    monthNum: Int,
) = LedgerRecordEntity(
    dateStr = "$monthNum/$year",
    year = year,
    monthNum = monthNum,
    basicPay = 0.0,
    dearnessAllowance = 0.0,
    militaryServicePay = 0.0,
    transportAllowance = 0.0,
    transportAllowanceDa = 0.0,
    houseRentAllowance = 0.0,
    grossPay = 0.0,
    dsopSubscription = 0.0,
    incomeTax = 0.0,
    netPay = 0.0,
)

class PreviousMonthLabelTest {
    @Test
    fun testNullPreviousRecordReturnsNull() {
        assertNull(previousMonthLabel(null))
    }

    @Test
    fun testNormalMonthReturnsMonthName() {
        assertEquals("March", previousMonthLabel(record(year = 2026, monthNum = 3)))
    }

    @Test
    fun testDecemberRolloverReturnsDecember() {
        assertEquals("December", previousMonthLabel(record(year = 2025, monthNum = 12)))
    }
}
