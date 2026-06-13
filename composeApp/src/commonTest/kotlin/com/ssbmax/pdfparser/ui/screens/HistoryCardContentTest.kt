package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HistoryCardContentTest {
    @Test
    fun testCalculateTrendIncrease() {
        val july = createMockPayslip("07/2024", netRemittance = 100000.0)
        val august = createMockPayslip("08/2024", netRemittance = 105000.0)

        val trend = calculateTrend(august, listOf(july, august))
        assertNotNull(trend)
        assertTrue(trend.isIncrease)
        assertEquals(5.0, trend.percentageChange)
    }

    @Test
    fun testCalculateTrendDecrease() {
        val july = createMockPayslip("07/2024", netRemittance = 100000.0)
        val august = createMockPayslip("08/2024", netRemittance = 95000.0)

        val trend = calculateTrend(august, listOf(july, august))
        assertNotNull(trend)
        assertTrue(!trend.isIncrease)
        assertEquals(-5.0, trend.percentageChange)
    }

    @Test
    fun testCalculateTrendFirstItem() {
        val july = createMockPayslip("07/2024", netRemittance = 100000.0)

        val trend = calculateTrend(july, listOf(july))
        assertNull(trend)
    }

    @Test
    fun testFormatPercentage() {
        assertEquals("5%", formatPercentage(5.0))
        assertEquals("4.2%", formatPercentage(4.234))
        assertEquals("0.5%", formatPercentage(0.55))
    }

    @Test
    fun testCalculateTrendSortChronological() {
        val july = createMockPayslip("07/2024", netRemittance = 100000.0)
        val august = createMockPayslip("08/2024", netRemittance = 105000.0)

        // Pass august first in the list to verify it sorts chronologically
        val trend = calculateTrend(august, listOf(august, july))
        assertNotNull(trend)
        assertTrue(trend.isIncrease)
        assertEquals(5.0, trend.percentageChange)
    }

    @Test
    fun testCalculateTrendZeroPrevNet() {
        val july = createMockPayslip("07/2024", netRemittance = 0.0)
        val august = createMockPayslip("08/2024", netRemittance = 100000.0)

        val trend = calculateTrend(august, listOf(july, august))
        assertNull(trend) // should return null to prevent division by zero
    }

    @Test
    fun testCalculateTrendNoChange() {
        val july = createMockPayslip("07/2024", netRemittance = 100000.0)
        val august = createMockPayslip("08/2024", netRemittance = 100000.0)

        val trend = calculateTrend(august, listOf(july, august))
        assertNotNull(trend)
        assertTrue(trend.isZero)
        assertEquals(0.0, trend.percentageChange)
    }

    @Test
    fun testFormatPercentageZero() {
        assertEquals("0%", formatPercentage(0.0))
    }

    private fun createMockPayslip(
        dateStr: String,
        netRemittance: Double,
    ) =
        dateStr.split("/").let { split ->
            val month = split[0].toInt()
            val year = split[1].toInt()
            ParsedPayslip(
                file = "payslip_$dateStr.pdf", year = year, monthNum = month, monthName = "Month_$month", dateStr = dateStr,
                officer = Officer("Name", "Acc", "PAN"),
                earnings = Earnings(basicPay = 100000.0, dearnessAllowance = 50000.0),
                deductions = Deductions(dsopSubscription = 20000.0, incomeTax = 15000.0),
                ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
                summary = PayslipSummary(grossPay = 150000.0, totalDeductions = 35000.0, netRemittance = netRemittance),
                taxAndSavings = null,
            )
        }
}
