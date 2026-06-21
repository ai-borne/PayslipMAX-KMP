package com.ssbmax.pdfparser.ui.screens

import com.ssbmax.pdfparser.database.LedgerRecordEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InsightsDeductionsWindowingTest {
    private fun record(
        year: Int,
        monthNum: Int,
        gross: Double = 100000.0,
        net: Double = 70000.0,
        tax: Double = 15000.0,
        dsop: Double = 10000.0,
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
        dsopSubscription = dsop,
        incomeTax = tax,
        netPay = net,
    )

    private val twelveMonths =
        (1..12).map { record(year = 2025, monthNum = it) } +
            (1..2).map { record(year = 2026, monthNum = it) }

    @Test
    fun trailingWindowEndsAtSelectedMonthNotLatest() {
        val selected = twelveMonths.first { it.year == 2025 && it.monthNum == 8 }
        val window = buildDeductionBars(twelveMonths, selected)
        assertEquals(6, window.size)
        assertEquals("Aug '25", window.last().label)
    }

    @Test
    fun windowShiftsWhenSelectionMovesBackward() {
        val laterSelected = twelveMonths.first { it.year == 2025 && it.monthNum == 10 }
        val earlierSelected = twelveMonths.first { it.year == 2025 && it.monthNum == 6 }
        val laterWindow = buildDeductionBars(twelveMonths, laterSelected)
        val earlierWindow = buildDeductionBars(twelveMonths, earlierSelected)
        assertTrue(laterWindow.map { it.label } != earlierWindow.map { it.label })
        assertEquals("Jun '25", earlierWindow.last().label)
    }

    @Test
    fun fewerThanSixRecordsReturnsAllAndCorrectTitleSuffix() {
        val shortHistory = (1..3).map { record(year = 2025, monthNum = it) }
        val selected = shortHistory.last()
        val window = buildDeductionBars(shortHistory, selected)
        assertEquals(3, window.size)
        assertEquals("3-Month Pay Breakdown", breakdownTitleFor(window.size))
    }

    @Test
    fun exactlySixReturnsFullTitle() {
        val sixHistory = (1..6).map { record(year = 2025, monthNum = it) }
        val selected = sixHistory.last()
        val window = buildDeductionBars(sixHistory, selected)
        assertEquals(6, window.size)
        assertEquals("6-Month Pay Breakdown", breakdownTitleFor(window.size))
    }

    @Test
    fun isSelectedFlagsOnlyTheMatchingRecord() {
        val selected = twelveMonths.first { it.year == 2025 && it.monthNum == 9 }
        val window = buildDeductionBars(twelveMonths, selected)
        val selectedBars = window.filter { it.isSelected }
        assertEquals(1, selectedBars.size)
        assertEquals("Sep '25", selectedBars.first().label)
    }

    @Test
    fun captionFormatsCorrectlyAcrossYearBoundary() {
        val selected = twelveMonths.first { it.year == 2026 && it.monthNum == 2 }
        val window = buildDeductionBars(twelveMonths, selected)
        assertEquals("Sep '25 – Feb '26", breakdownRangeCaption(window))
    }

    @Test
    fun singleRecordHistoryReturnsOneBarWithNonRedundantCaption() {
        val selected = record(year = 2025, monthNum = 5)
        val window = buildDeductionBars(listOf(selected), selected)
        assertEquals(1, window.size)
        assertEquals("1-Month Pay Breakdown", breakdownTitleFor(window.size))
        assertEquals("May '25", breakdownRangeCaption(window))
    }

    @Test
    fun segmentsSumToGrossPay() {
        val selected = record(year = 2025, monthNum = 5, gross = 100000.0, net = 70000.0, tax = 15000.0, dsop = 10000.0)
        val window = buildDeductionBars(listOf(selected, record(year = 2025, monthNum = 4)), selected)
        val bar = window.last()
        assertEquals(100000.0f, bar.net + bar.dsop + bar.tax + bar.other)
        assertEquals(5000.0f, bar.other)
    }
}
