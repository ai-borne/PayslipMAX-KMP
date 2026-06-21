package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsTrendWindowingTest {
    private fun record(
        year: Int,
        monthNum: Int,
        gross: Double = 1000.0,
        net: Double = 800.0,
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
        grossPay = gross,
        dsopSubscription = 0.0,
        incomeTax = 0.0,
        netPay = net,
    )

    private val twelveMonths =
        (1..12).map { record(year = 2025, monthNum = it) } +
            (1..2).map { record(year = 2026, monthNum = it) }

    @Test
    fun trailingWindowEndsAtSelectedMonthNotLatest() {
        val selected = twelveMonths.first { it.year == 2025 && it.monthNum == 8 }
        val window = buildTrailingWindow(twelveMonths, selected)
        assertEquals(6, window.size)
        assertEquals("Aug '25", window.last().label)
    }

    @Test
    fun windowShiftsWhenSelectionMovesBackward() {
        val laterSelected = twelveMonths.first { it.year == 2025 && it.monthNum == 10 }
        val earlierSelected = twelveMonths.first { it.year == 2025 && it.monthNum == 6 }
        val laterWindow = buildTrailingWindow(twelveMonths, laterSelected)
        val earlierWindow = buildTrailingWindow(twelveMonths, earlierSelected)
        assertTrue(laterWindow.map { it.label } != earlierWindow.map { it.label })
        assertEquals("Jun '25", earlierWindow.last().label)
    }

    @Test
    fun fewerThanSixRecordsReturnsAllAndCorrectTitleSuffix() {
        val shortHistory = (1..3).map { record(year = 2025, monthNum = it) }
        val selected = shortHistory.last()
        val window = buildTrailingWindow(shortHistory, selected)
        assertEquals(3, window.size)
        assertEquals("3-Month Trend", trendTitleFor(window.size))
    }

    @Test
    fun exactlySixReturnsFullTitle() {
        val sixHistory = (1..6).map { record(year = 2025, monthNum = it) }
        val selected = sixHistory.last()
        val window = buildTrailingWindow(sixHistory, selected)
        assertEquals(6, window.size)
        assertEquals("6-Month Salary Trend", trendTitleFor(window.size))
    }

    @Test
    fun isSelectedFlagsOnlyTheMatchingRecord() {
        val selected = twelveMonths.first { it.year == 2025 && it.monthNum == 9 }
        val window = buildTrailingWindow(twelveMonths, selected)
        val selectedPoints = window.filter { it.isSelected }
        assertEquals(1, selectedPoints.size)
        assertEquals("Sep '25", selectedPoints.first().label)
    }

    @Test
    fun captionFormatsCorrectlyAcrossYearBoundary() {
        val selected = twelveMonths.first { it.year == 2026 && it.monthNum == 2 }
        val window = buildTrailingWindow(twelveMonths, selected)
        assertEquals("Sep '25 – Feb '26", trendRangeCaption(window))
    }
}
