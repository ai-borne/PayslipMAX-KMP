package com.payslipmax.pdfparser.guide.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Dearness allowance is revised twice a year, so a rate card is stale well before a year is out. The nudge appears
 * once the bundle's `rates_as_of` is [GuideStaleness.STALE_AFTER_MONTHS] or more months behind the clock, which is
 * injected, so these tests fix "now" and never read the device clock.
 */
class GuideStalenessTest {
    private val jan2026 = "2026-01"

    private fun millis(
        year: Int,
        month: Int,
        day: Int = 15,
    ): Long = GuideStaleness.epochDays(year, month, day) * 86_400_000L

    @Test
    fun theNudgeAppearsOnlyOnceTheThresholdIsReached() {
        val threshold = GuideStaleness.STALE_AFTER_MONTHS
        assertEquals(9, threshold, "changing the threshold is an owner decision; update the plan with it")

        assertFalse(GuideStaleness.isStale(jan2026, millis(2026, 9)), "8 months old")
        assertTrue(GuideStaleness.isStale(jan2026, millis(2026, 10)), "9 months old")
        assertTrue(GuideStaleness.isStale(jan2026, millis(2027, 3)))
    }

    @Test
    fun theMonthOfTheRatesAndEarlierMonthsAreNeverStale() {
        assertFalse(GuideStaleness.isStale(jan2026, millis(2026, 1, day = 1)))
        assertFalse(GuideStaleness.isStale(jan2026, millis(2025, 12)), "a clock behind the bundle is not stale")
    }

    @Test
    fun yearBoundariesCountInMonths() {
        assertFalse(GuideStaleness.isStale("2025-06", millis(2026, 2)), "8 months across a year end")
        assertTrue(GuideStaleness.isStale("2025-06", millis(2026, 3)), "9 months across a year end")
    }

    @Test
    fun theMonthComesFromTheClockInUtcIncludingLeapYears() {
        assertEquals(2024 to 2, GuideStaleness.yearMonthOf(millis(2024, 2, day = 29)))
        assertEquals(2026 to 12, GuideStaleness.yearMonthOf(millis(2026, 12, day = 31) + 86_399_999L))
        assertEquals(1970 to 1, GuideStaleness.yearMonthOf(0L))
        assertEquals(2027 to 1, GuideStaleness.yearMonthOf(millis(2027, 1, day = 1)))
    }

    @Test
    fun aMalformedDateIsNeverStaleSoTheNudgeCannotAppearByMistake() {
        for (bad in listOf("", "2026", "2026-13", "26-01", "abcd-ef")) {
            assertFalse(GuideStaleness.isStale(bad, millis(2030, 1)), "'$bad'")
        }
    }
}
