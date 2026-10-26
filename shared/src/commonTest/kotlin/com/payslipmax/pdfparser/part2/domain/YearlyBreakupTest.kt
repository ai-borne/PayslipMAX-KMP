package com.payslipmax.pdfparser.part2.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The year-by-year list of an encashment (p.140 and p.141 note 2: "year wise break up of Annual Leave accumulation during the entire
 * service"). It is typed by an officer and restored from a file, so every way it can be malformed must be refused, and the number it
 * holds must be a number the calendar and the cap arithmetic can use.
 */
class YearlyBreakupTest {
    private fun reason(text: String) = (YearlyBreakup.parse(text) as BreakupRead.Refused).reason

    private fun rows(text: String) = (YearlyBreakup.parse(text) as BreakupRead.Rows).rows

    @Test
    fun aListOfYearsAndDaysIsReadInOrderAndWrittenBackCanonically() {
        val read = rows("2011 30\n2013 30\n2020 20")
        assertEquals(listOf(YearDays(2011, 30), YearDays(2013, 30), YearDays(2020, 20)), read)
        assertEquals("2011 30\n2013 30\n2020 20", YearlyBreakup.format(read))
        assertEquals(80, YearlyBreakup.total(read))
        assertEquals(read, rows("  2011   30 \n\n2013 30\r\n2020 20\n"), "spaces, blank lines and CRLF are tolerated and dropped on the way back")
    }

    @Test
    fun aSemicolonEndsALineSoAOneLineTextBoxCanTakeTheWholeList() {
        assertEquals(listOf(YearDays(2011, 30), YearDays(2013, 20)), rows("2011 30; 2013 20"))
        assertEquals("2011 30\n2013 20", YearlyBreakup.format(rows("2011 30;2013 20;")), "written back with line breaks, a trailing semicolon dropped")
        assertEquals(DraftIssueReason.WRONG_KIND, reason("2011 30; 2011 20"), "the same rules apply across semicolons")
    }

    @Test
    fun theBoundsOfEachLineAndOfTheListAreInclusive() {
        assertEquals(listOf(YearDays(1950, 0), YearDays(2100, 9999)), rows("1950 0\n2100 9999"))
        val sixty = (0 until 60).joinToString("\n") { "${1960 + it} 1" }
        assertEquals(60, rows(sixty).size, "60 rows is the most")
        assertEquals(60, rows(sixty + "\n").size, "a final line break is still within the 600 characters")
        assertEquals(60, rows((0 until 60).joinToString("\n") { "${1960 + it} 9999" } + "\n").size, "exactly 600 characters, as the storage bound allows")
        assertEquals(DraftIssueReason.TOO_LONG, reason((0 until 61).joinToString("\n") { "${1960 + it} 1" }), "a 61st row")
    }

    @Test
    fun aListNothingCanReadIsRefusedWithTheReasonTheFormWords() {
        assertEquals(DraftIssueReason.NOT_ANSWERED, reason(""))
        assertEquals(DraftIssueReason.NOT_ANSWERED, reason("  \n \n"))
        for (bad in listOf("abc", "2011", "2011 30 5", "-2011 30", "2011 -5", "2011 3x", "2011,30", "٢٠١١ ٣٠")) {
            assertEquals(DraftIssueReason.WRONG_KIND, reason(bad), bad)
        }
        assertEquals(DraftIssueReason.WRONG_KIND, reason("2013 30\n2011 30"), "descending")
        assertEquals(DraftIssueReason.WRONG_KIND, reason("2011 30\n2011 20"), "the same year twice")
    }

    @Test
    fun aYearOutsideTheCalendarOrAHugeDayCountIsOutOfRange() {
        assertEquals(DraftIssueReason.OUT_OF_RANGE, reason("1949 5"))
        assertEquals(DraftIssueReason.OUT_OF_RANGE, reason("2101 5"))
        assertEquals(DraftIssueReason.OUT_OF_RANGE, reason("2011 10000"))
        assertEquals(DraftIssueReason.OUT_OF_RANGE, reason("2011 99999999999999999999"), "a number too big for an Int is refused, never wrapped")
    }

    @Test
    fun markupAndOverlongTextAreRefusedBeforeAnythingIsRead() {
        assertEquals(DraftIssueReason.MARKUP, reason("2011 <b>"))
        assertEquals(DraftIssueReason.MARKUP, reason("2011 30>"))
        assertEquals(DraftIssueReason.TOO_LONG, reason("2011 30\n" + " ".repeat(Part2Limits.BREAKUP_TEXT_MAX)))
    }
}
