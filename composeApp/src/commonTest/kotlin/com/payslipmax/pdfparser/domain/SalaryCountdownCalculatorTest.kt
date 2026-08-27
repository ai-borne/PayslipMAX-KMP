package com.payslipmax.pdfparser.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SalaryCountdownCalculatorTest {
    @Test
    fun test31DayMonthCalculation() {
        // e.g. August 27th, 2026 -> August 31st (4 days remaining)
        val result =
            SalaryCountdownCalculator.calculateCountdown(
                year = 2026,
                month = 8,
                day = 27,
            )

        assertEquals(4, result.daysRemaining)
        assertEquals("31 Aug", result.paydayDateFormatted)
        assertFalse(result.isPaydayToday)
        assertEquals(27f / 31f, result.progressRatio)
    }

    @Test
    fun test30DayMonthCalculation() {
        // e.g. September 1st, 2026 -> September 30th (29 days remaining)
        val result =
            SalaryCountdownCalculator.calculateCountdown(
                year = 2026,
                month = 9,
                day = 1,
            )

        assertEquals(29, result.daysRemaining)
        assertEquals("30 Sep", result.paydayDateFormatted)
        assertFalse(result.isPaydayToday)
        assertEquals(1f / 30f, result.progressRatio)
    }

    @Test
    fun testLeapYearFebruaryCalculation() {
        // 2024 is a leap year -> Feb has 29 days
        val result =
            SalaryCountdownCalculator.calculateCountdown(
                year = 2024,
                month = 2,
                day = 15,
            )

        assertEquals(14, result.daysRemaining)
        assertEquals("29 Feb", result.paydayDateFormatted)
        assertFalse(result.isPaydayToday)
        assertEquals(15f / 29f, result.progressRatio)
    }

    @Test
    fun testNonLeapYearFebruaryCalculation() {
        // 2025 is not a leap year -> Feb has 28 days
        val result =
            SalaryCountdownCalculator.calculateCountdown(
                year = 2025,
                month = 2,
                day = 15,
            )

        assertEquals(13, result.daysRemaining)
        assertEquals("28 Feb", result.paydayDateFormatted)
        assertFalse(result.isPaydayToday)
        assertEquals(15f / 28f, result.progressRatio)
    }

    @Test
    fun testPaydayToday() {
        // August 31st (same day) -> 0 days remaining, isPaydayToday = true
        val result =
            SalaryCountdownCalculator.calculateCountdown(
                year = 2026,
                month = 8,
                day = 31,
            )

        assertEquals(0, result.daysRemaining)
        assertEquals("31 Aug", result.paydayDateFormatted)
        assertTrue(result.isPaydayToday)
        assertEquals(1.0f, result.progressRatio)
    }

    @Test
    fun testOneDayLeft() {
        // August 30th -> 1 day remaining
        val result =
            SalaryCountdownCalculator.calculateCountdown(
                year = 2026,
                month = 8,
                day = 30,
            )

        assertEquals(1, result.daysRemaining)
        assertEquals("31 Aug", result.paydayDateFormatted)
        assertFalse(result.isPaydayToday)
    }

    @Test
    fun testDaysFromEpochDateCalculation() {
        // 2026-08-27 timestamp approximation
        // Epoch day calculation: 2026-08-27
        val (year, month, day) = SalaryCountdownCalculator.epochMillisToDate(1787788800000L) // around Aug 2026
        assertTrue(year >= 2026)
        assertTrue(month in 1..12)
        assertTrue(day in 1..31)
    }
}
