package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.database.LedgerRecordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Locks [buildPayTrendPoints], the pure trailing-window helper behind [PayTrendChart]'s net-pay/tax
 * lines and tappable month rows — a wrong window would show the wrong months or the wrong "selected" tap target.
 */
class PayTrendChartLogicTest {
    private fun record(
        year: Int,
        monthNum: Int,
        netPay: Double = 70_000.0,
        incomeTax: Double = 15_000.0,
    ) = LedgerRecordEntity(
        dateStr = "${monthNum.toString().padStart(2, '0')}/$year",
        year = year,
        monthNum = monthNum,
        basicPay = 0.0,
        dearnessAllowance = 0.0,
        militaryServicePay = 0.0,
        transportAllowance = 0.0,
        transportAllowanceDa = 0.0,
        houseRentAllowance = 0.0,
        grossPay = 100_000.0,
        dsopSubscription = 10_000.0,
        incomeTax = incomeTax,
        netPay = netPay,
    )

    private val twelveMonths = (1..12).map { record(year = 2025, monthNum = it) }

    @Test
    fun `window ends at the selected month, not the latest`() {
        val selected = twelveMonths.first { it.monthNum == 8 }
        val points = buildPayTrendPoints(twelveMonths, selected)
        assertEquals(6, points.size)
        assertEquals("Aug '25", points.last().label)
    }

    @Test
    fun `fewer than the window size returns all available records`() {
        val shortHistory = (1..3).map { record(year = 2025, monthNum = it) }
        val points = buildPayTrendPoints(shortHistory, shortHistory.last())
        assertEquals(3, points.size)
    }

    @Test
    fun `each point carries the record's net pay and income tax, not swapped`() {
        val selected = record(year = 2025, monthNum = 5, netPay = 82_000.0, incomeTax = 12_000.0)
        val point = buildPayTrendPoints(listOf(selected), selected).single()
        assertEquals(82_000.0, point.netPay)
        assertEquals(12_000.0, point.incomeTax)
    }

    @Test
    fun `selected index resolves to the point matching the selected record's dateStr`() {
        val selected = twelveMonths.first { it.monthNum == 9 }
        val points = buildPayTrendPoints(twelveMonths, selected)
        val selectedIndex = points.indexOfFirst { it.dateStr == selected.dateStr }
        assertTrue(selectedIndex >= 0)
        assertEquals("Sep '25", points[selectedIndex].label)
    }
}
