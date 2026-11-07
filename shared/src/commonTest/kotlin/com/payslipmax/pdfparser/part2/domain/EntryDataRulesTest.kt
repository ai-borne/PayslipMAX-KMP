package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the manual's captions are the only type information the bundle carries. A box it asks as dd/mm/yyyy must take the same picker as
 * From and To, and a day count must follow the From-To span, so an officer does not retype what the dates already say. A running total
 * is never the span, so it is left alone. These rules decide that from the caption alone, so one wrong match changes what the officer sees.
 */
class EntryDataRulesTest {
    private val gtdalFields =
        listOf(
            EntryField(Part2ColumnPosition.FROM, true, "dd/mm/yyyy"),
            EntryField(Part2ColumnPosition.TO, true, "dd/mm/yyyy"),
            EntryField(Part2ColumnPosition.DATA_FIRST, false, "prefix dd/mm/yyyy"),
            EntryField(Part2ColumnPosition.DATA_FIRST + 1, false, "suffix dd/mm/yyyy"),
            EntryField(Part2ColumnPosition.DATA_FIRST + 2, true, "(No of days of AL)"),
            EntryField(Part2ColumnPosition.DATA_FIRST + 3, true, "Year (yyyy)"),
        )
    private val gtdal = EntryForm("GTDAL", gtdalFields, manualPage = 114)

    private fun input(
        from: CalendarDate?,
        to: CalendarDate?,
        data: List<String> = listOf("", "", "", "2026"),
    ) = EntryInput(from, to, data, "")

    @Test
    fun aCaptionAskingForDdMmYyyyIsADateWhereverItSitsAndInAnyCase() {
        assertTrue(EntryDataRules.isDate("prefix dd/mm/yyyy"))
        assertTrue(EntryDataRules.isDate("Part II Order dd/mm/yyyy"))
        assertTrue(EntryDataRules.isDate("date of increment (dd/mm/yyyy)"))
        assertTrue(EntryDataRules.isDate("Prefix DD/MM/YYYY"))
        assertFalse(EntryDataRules.isDate("prefix"))
        assertFalse(EntryDataRules.isDate("Year (yyyy)"))
    }

    @Test
    fun aDayCountIsAnyDaysCaptionExceptARunningTotalAndADateBox() {
        assertTrue(EntryDataRules.isDayCount("(No of days of AL)"))
        assertTrue(EntryDataRules.isDayCount("No. of days of CCL/ 1st/ 2nd spell."))
        assertTrue(EntryDataRules.isDayCount("… (days)"))
        assertFalse(EntryDataRules.isDayCount("total No of days AL debited"), "a total is not the span, the officer types it")
        assertFalse(EntryDataRules.isDayCount("Total No. of days of CCL availed (including current leave)"))
        assertFalse(EntryDataRules.isDayCount("Part II Order dd/mm/yyyy"), "a date with the word days in it is still a date")
        assertFalse(EntryDataRules.isDayCount("daysworth of pay"), "the word must stand alone")
    }

    @Test
    fun parseReadsOnlyARealDayInDdMmYyyy() {
        assertEquals(CalendarDate(2026, 11, 1), EntryDataRules.parse("01/11/2026"))
        assertEquals(CalendarDate(2026, 11, 1), EntryDataRules.parse(" 01/11/2026 "))
        assertNull(EntryDataRules.parse("31/04/2026"), "31 April is not a day")
        assertNull(EntryDataRules.parse("1/11/2026"), "the picker always writes two digits")
        assertNull(EntryDataRules.parse("2026-11-01"))
        assertNull(EntryDataRules.parse("0a/11/2026"))
        assertNull(EntryDataRules.parse(""))
    }

    @Test
    fun aDateBoxHoldingTypedTextThatIsNotARealDateIsRefusedBeforeSaving() {
        val problems = gtdal.check(input(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 2), listOf("31/04/2026", "", "2", "2026")))
        assertEquals(EntryProblem.INVALID_DATE, problems[Part2ColumnPosition.DATA_FIRST])
    }

    @Test
    fun aDayCountFollowsTheFromToSpanWithBothEndsCounted() {
        val before = input(CalendarDate(2026, 11, 1), null)
        val after = before.copy(to = CalendarDate(2026, 11, 2))
        assertEquals("2", gtdal.withSpanDays(before, after).data[2])
    }

    @Test
    fun aDayCountTracksANewSpanWhileItStillHoldsTheOldOne() {
        val before = input(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 2), listOf("", "", "2", "2026"))
        val after = before.copy(to = CalendarDate(2026, 11, 5))
        assertEquals("5", gtdal.withSpanDays(before, after).data[2])
    }

    @Test
    fun aDayCountTheOfficerTypedIsNeverOverwrittenByADateChange() {
        val before = input(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 2), listOf("", "", "7", "2026"))
        val after = before.copy(to = CalendarDate(2026, 11, 5))
        assertEquals("7", gtdal.withSpanDays(before, after).data[2], "the officer's own count stands")
    }

    @Test
    fun aBoxEditThatLeavesTheDatesAloneLeavesTheDayCountAlone() {
        val before = input(CalendarDate(2026, 11, 1), CalendarDate(2026, 11, 2), listOf("", "", "2", "2026"))
        val cleared = before.copy(data = listOf("", "", "", "2026"))
        assertEquals("", gtdal.withSpanDays(before, cleared).data[2], "clearing the box to retype must not snap the span back")
    }

    @Test
    fun aDayCountStaysEmptyUntilBothDatesAreSet() {
        val before = input(null, null)
        assertEquals("", gtdal.withSpanDays(before, before.copy(from = CalendarDate(2026, 11, 1))).data[2])
    }

    @Test
    fun aFormWithoutADayCountBoxLeavesTheInputAlone() {
        val noDays = EntryForm("GTDAL", gtdalFields.filterNot { it.position == Part2ColumnPosition.DATA_FIRST + 2 }, manualPage = 114)
        val before = input(CalendarDate(2026, 11, 1), null)
        val after = before.copy(to = CalendarDate(2026, 11, 2))
        assertEquals(after, noDays.withSpanDays(before, after))
    }
}
