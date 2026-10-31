package com.payslipmax.pdfparser.utils

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.TimeZone
import kotlin.test.assertEquals

/**
 * WHY: "Printed on" and "edited" are days the officer lived, so the offset must be the device's zone at the instant asked about: India's
 * fixed +5:30 moves a late-evening print to the next day, and a daylight-saving zone has a different offset in winter and summer.
 */
class LocalTimeOffsetAndroidTest {
    private lateinit var original: TimeZone

    @Before
    fun remember() {
        original = TimeZone.getDefault()
    }

    @After
    fun restore() {
        TimeZone.setDefault(original)
    }

    @Test
    fun indiaIsFiveHoursThirtyEastAtAnyInstant() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"))

        assertEquals(19_800_000L, localUtcOffsetMillis(JANUARY))
        assertEquals(19_800_000L, localUtcOffsetMillis(JULY))
    }

    @Test
    fun aDaylightSavingZoneHasTheOffsetOfTheInstantAskedAbout() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"))

        assertEquals(-18_000_000L, localUtcOffsetMillis(JANUARY))
        assertEquals(-14_400_000L, localUtcOffsetMillis(JULY))
    }

    @Test
    fun utcIsZero() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))

        assertEquals(0L, localUtcOffsetMillis(JULY))
    }

    private companion object {
        const val JANUARY = 1_768_478_400_000L
        const val JULY = 1_784_116_800_000L
    }
}
