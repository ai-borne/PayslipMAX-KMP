package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * What the encashment can fill from the Leave Bank (Part II plan, phase 8b): the year-by-year break-up from the statement's accumulation
 * rows, and what the form says about the break-up the officer holds: the years with no line and a difference from the statement's total.
 * Synthetic figures.
 */
class EncashLeaveBankFillTest {
    private val fill = checkNotNull(Part2Rules.shipped().fill("ENCASH"))
    private val rows = listOf(accrued("2008-12-31", 7, 7), accrued("2010-12-31", 5, 12), ltcEncashed("2023-05-01", 5), accrued("2023-12-31", 30, 282), accrued("2024-12-31", 19, 301))

    private fun request(text: String? = null) = OccurrenceRequest("ENCASH", listOfNotNull(text?.let { "breakup" to SlotValue.TextValue(it) }).toMap())

    private fun inputs(rows: List<com.payslipmax.pdfparser.domain.LeaveEntry> = this.rows) = LeaveBankInputs(rows, emptyList(), emptyList())

    @Test
    fun theFillBelongsToTheEncashmentCodeAndWritesTheBreakUpOnly() {
        assertEquals("ENCASH", fill.code)
        assertEquals(setOf("breakup"), fill.slots)
    }

    @Test
    fun theBreakUpIsTheStatementsAccumulationRowsOneLineAYearOldestFirstAndIgnoresLtcRows() {
        val values = assertIs<FillOutcome.Values>(fill.fill(request(), inputs())).values
        assertEquals(mapOf("breakup" to SlotValue.TextValue("2008 7\n2010 5\n2023 30\n2024 19")), values)
        assertEquals(listOf(2008, 2010, 2023, 2024), (YearlyBreakup.parse((values.getValue("breakup") as SlotValue.TextValue).text) as BreakupRead.Rows).rows.map { it.year }, "what it writes the form can read back")
    }

    @Test
    fun withNoAccumulationRowNothingIsKnown() {
        assertEquals(FillOutcome.NothingKnown, fill.fill(request(), inputs(emptyList())))
        assertEquals(FillOutcome.NothingKnown, fill.fill(request(), inputs(listOf(ltcEncashed("2023-05-01", 5)))), "an LTC row is not an accumulation")
    }

    @Test
    fun theYearsWithNoLineAreNamedFromWhatIsInTheBoxSoATypedYearClearsItsName() {
        val twoYears = "2008 7\n2012 5"
        assertEquals(listOf(FillNote.MissingYears(listOf(2009, 2010, 2011)), FillNote.StatementTotal(statement = 301, breakup = 12)), fill.notes(request(twoYears), rows))
        assertEquals(listOf(FillNote.MissingYears(listOf(2009, 2011)), FillNote.StatementTotal(301, 17)), fill.notes(request("2008 7; 2010 5; 2012 5"), rows))
    }

    @Test
    fun aBreakUpThatAddsToTheStatementsTotalOrHasNoGapsNeedsNoNote() {
        assertEquals(emptyList(), fill.notes(request("2023 282\n2024 19"), rows), "no gap and 282 + 19 is the statement's 301")
        assertEquals(listOf(FillNote.MissingYears(listOf(2009))), fill.notes(request("2008 7\n2010 294"), rows), "the total matches, the gap stays named")
    }

    @Test
    fun withNoStatementOnlyTheGapsAreNamedAndNothingIsSaidOfAnEmptyOrUnreadableBox() {
        assertEquals(listOf(FillNote.MissingYears(listOf(2009))), fill.notes(request("2008 7; 2010 5"), emptyList()))
        assertEquals(emptyList(), fill.notes(request(), rows))
        assertEquals(emptyList(), fill.notes(request("  "), rows))
        assertEquals(emptyList(), fill.notes(request("2008 seven"), rows), "a break-up the engine refuses is named on its own field, not here")
        assertEquals(emptyList(), fill.notes(OccurrenceRequest("ENCASH", mapOf("breakup" to number(5))), rows), "not a text answer")
    }

    @Test
    fun thisCodeHasNoCrossCheck() {
        assertNull(fill.crossCheck(request("2008 7"), rows))
    }
}
