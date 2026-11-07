package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals

/** A guide card's "as of" label is shown to the user; anything that is not a real year and month must pass through unchanged, never crash or invent a month. */
class GuideStringsYearMonthTest {
    @Test
    fun `first and last month format`() {
        assertEquals("Jan 2026", GuideStrings.yearMonth("2026-01"))
        assertEquals("Dec 2026", GuideStrings.yearMonth("2026-12"))
    }

    @Test
    fun `month 0 and 13 are returned as they came`() {
        assertEquals("2026-00", GuideStrings.yearMonth("2026-00"))
        assertEquals("2026-13", GuideStrings.yearMonth("2026-13"))
    }

    @Test
    fun `non month values are returned as they came`() {
        assertEquals("2026", GuideStrings.yearMonth("2026"))
        assertEquals("2026-ab", GuideStrings.yearMonth("2026-ab"))
        assertEquals("26-05", GuideStrings.yearMonth("26-05"))
        assertEquals("", GuideStrings.yearMonth(""))
    }
}
