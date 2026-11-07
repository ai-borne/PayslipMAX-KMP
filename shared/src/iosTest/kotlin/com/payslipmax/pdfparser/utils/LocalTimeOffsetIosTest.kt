package com.payslipmax.pdfparser.utils

import platform.Foundation.NSTimeZone
import platform.Foundation.defaultTimeZone
import platform.Foundation.setDefaultTimeZone
import platform.Foundation.timeZoneWithName
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** WHY: the same as the Android twin: the offset is the device zone's at the instant asked about, so India is +5:30 and New York changes with daylight saving. */
class LocalTimeOffsetIosTest {
    private lateinit var original: NSTimeZone

    @BeforeTest
    fun remember() {
        original = NSTimeZone.defaultTimeZone
    }

    @AfterTest
    fun restore() {
        NSTimeZone.setDefaultTimeZone(original)
    }

    private fun zone(name: String) = NSTimeZone.setDefaultTimeZone(NSTimeZone.timeZoneWithName(name)!!)

    @Test
    fun indiaIsFiveHoursThirtyEastAtAnyInstant() {
        zone("Asia/Kolkata")

        assertEquals(19_800_000L, localUtcOffsetMillis(JANUARY))
        assertEquals(19_800_000L, localUtcOffsetMillis(JULY))
    }

    @Test
    fun aDaylightSavingZoneHasTheOffsetOfTheInstantAskedAbout() {
        zone("America/New_York")

        assertEquals(-18_000_000L, localUtcOffsetMillis(JANUARY))
        assertEquals(-14_400_000L, localUtcOffsetMillis(JULY))
    }

    private companion object {
        const val JANUARY = 1_768_478_400_000L
        const val JULY = 1_784_116_800_000L
    }
}
