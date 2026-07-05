package com.ssbmax.pdfparser.utils

import kotlin.test.Test
import kotlin.test.assertEquals

class FormatUtilsTest {
    @Test
    fun formatIndianGroupedHandlesThousandsAndLakhBoundaries() {
        assertEquals("500", FormatUtils.formatIndianGrouped(500.0))
        assertEquals("5,500", FormatUtils.formatIndianGrouped(5500.0))
        assertEquals("1,23,456", FormatUtils.formatIndianGrouped(123456.0))
        assertEquals("12,34,567", FormatUtils.formatIndianGrouped(1234567.0))
    }

    @Test
    fun formatIndianCompactFallsBackBelowOneLakh() {
        assertEquals("5,500", FormatUtils.formatIndianCompact(5500.0))
    }

    @Test
    fun formatIndianCompactAbbreviatesAtOneLakhWithoutTrailingDot() {
        // Regression: the old `lakhs.toString().take(4)` truncated "100.0" to "100." for exact values.
        assertEquals("1.0L", FormatUtils.formatIndianCompact(100000.0))
        assertEquals("10.0L", FormatUtils.formatIndianCompact(1000000.0))
    }

    @Test
    fun formatIndianCompactRoundsToOneDecimal() {
        assertEquals("12.3L", FormatUtils.formatIndianCompact(1234567.0))
    }
}
