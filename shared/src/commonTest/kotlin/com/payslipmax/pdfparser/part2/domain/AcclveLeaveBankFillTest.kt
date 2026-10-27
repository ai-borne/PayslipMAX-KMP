package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.domain.LeaveEntry
import com.payslipmax.pdfparser.part2.domain.RuleContexts.date
import com.payslipmax.pdfparser.part2.domain.RuleContexts.number
import com.payslipmax.pdfparser.part2.leavebank.CrossCheck
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * What the year-end accumulation can fill from the Leave Bank and the officer's drafts (Part II plan, phase 8b; DECISIONS Round 30): the
 * previous total, the LTC days, the annual leave used in the year and (c) from them. Every figure is a suggestion; the rule reads only
 * `LeaveEntry` rows and request values. Synthetic figures.
 */
class AcclveLeaveBankFillTest {
    private val fill = checkNotNull(Part2Rules.shipped().fill("ACCLVE"))
    private val rows = listOf(accrued("2023-12-31", 30, 282), ltcEncashed("2024-04-26", 10), accrued("2024-12-31", 19, 301))

    private fun request(vararg values: Pair<String, SlotValue>) = OccurrenceRequest("ACCLVE", mapOf(*values))

    private fun leave(
        code: String,
        from: Int,
        to: Int,
        year: Int = 2024,
        month: Int = 7,
    ) = OccurrenceRequest(code, mapOf("leave_from" to date(month, from, year), "leave_to" to date(month, to, year), "year" to number(year)))

    private fun earlier(
        year: Int,
        previous: Int,
        ltc: Int = 0,
        accrued: Int = 30,
    ) = request("year" to number(year), "prev_total" to number(previous), "ltc_days" to number(ltc), "accrued" to number(accrued))

    private fun inputs(
        rows: List<LeaveEntry> = this.rows,
        printed: List<OccurrenceRequest> = emptyList(),
        drafted: List<OccurrenceRequest> = printed,
    ) = LeaveBankInputs(rows, printed, drafted)

    private fun filled(outcome: FillOutcome) = assertIs<FillOutcome.Values>(outcome).values

    @Test
    fun theFillBelongsToTheYearEndCodeAndWritesTheFourFigures() {
        assertEquals("ACCLVE", fill.code)
        assertEquals(setOf("prev_total", "ltc_days", "accrued", "al_used"), fill.slots)
    }

    @Test
    fun withoutTheLeaveYearNothingIsFilledAndTheOfficerIsToldWhy() {
        assertEquals(FillOutcome.NeedsYear, fill.fill(request(), inputs()))
        assertEquals(FillOutcome.NeedsYear, fill.fill(request("prev_total" to number(5)), inputs()), "other figures do not stand in for the year")
    }

    @Test
    fun theStatementGivesThePreviousTotalAndTheLtcDaysAndTheDraftsGiveTheLeaveUsedAndSoTheAccrual() {
        val values = filled(fill.fill(request("year" to number(2024)), inputs(printed = listOf(leave("GTDAL", 1, 12), leave("EXTNAL", 13, 17), leave("GTDAL", 1, 20, year = 2023)))))
        assertEquals(mapOf("prev_total" to number(282), "ltc_days" to number(10), "al_used" to number(17), "accrued" to number(30)), values, "12 + 5 days of 2024 only; min(30, 60 - 17)")
    }

    @Test
    fun theAccrualIsWhatIsLeftOfSixtyWhenMoreThanThirtyWereUsed() {
        val values = filled(fill.fill(request("year" to number(2024)), inputs(printed = listOf(leave("GTDAL", 1, 31), leave("EXTNAL", 1, 14, month = 8)))))
        assertEquals(number(45), values["al_used"])
        assertEquals(number(15), values["accrued"], "min(30, 60 - 45)")
        assertEquals(number(0), filled(fill.fill(request("year" to number(2024)), inputs(printed = listOf(leave("GTDAL", 1, 31), leave("GTDAL", 1, 31, month = 8)))))["accrued"], "62 days used leaves nothing, never below 0")
    }

    @Test
    fun onlyPrintedDraftsCountAsLeaveUsed() {
        // why: a draft never handed to the clerk is not leave taken yet (Round 30: the PRINTED drafts).
        val values = filled(fill.fill(request("year" to number(2024)), inputs(printed = emptyList(), drafted = listOf(leave("GTDAL", 1, 31)))))
        assertEquals(number(0), values["al_used"])
        assertEquals(number(30), values["accrued"])
    }

    @Test
    fun withNoStatementRowTheTotalOfTheOfficersOwnDraftForTheYearBeforeIsTheFallback() {
        val values = filled(fill.fill(request("year" to number(2024)), inputs(rows = emptyList(), drafted = listOf(earlier(2022, 400, accrued = 5), earlier(2023, 100, ltc = 10, accrued = 30), earlier(2023, 999, accrued = 1)))))
        assertEquals(number(120), values["prev_total"], "the newest draft of 2023: 100 - 10 + 30; the one of 2022 is not last year's")
    }

    @Test
    fun aRequestOfAnotherCodeIsNeverTakenForLastYearsAccumulation() {
        // why: only the year-end order's own figures are a carried-forward total; another order may use the same slot names.
        val lookalike = OccurrenceRequest("ENCASH", earlier(2023, 100).values)
        assertNull(filled(fill.fill(request("year" to number(2024)), inputs(rows = emptyList(), drafted = listOf(lookalike))))["prev_total"])
    }

    @Test
    fun aNegativeFigureIsLeftForTheOfficerLikeOneBeyondTheLimit() {
        val values = filled(fill.fill(request("year" to number(2024)), inputs(rows = listOf(accrued("2023-12-31", 30, -5)))))
        assertNull(values["prev_total"])
        assertEquals(number(0), filled(fill.fill(request("year" to number(2024)), inputs(rows = listOf(accrued("2023-12-31", 30, 0)))))["prev_total"], "zero is a figure")
    }

    @Test
    fun theStatementRowBeatsTheDraftAndAnUnfinishedDraftSuppliesNothing() {
        assertEquals(number(282), filled(fill.fill(request("year" to number(2024)), inputs(drafted = listOf(earlier(2023, 100)))))["prev_total"])
        val unfinished = request("year" to number(2023), "prev_total" to number(100))
        assertNull(filled(fill.fill(request("year" to number(2024)), inputs(rows = emptyList(), drafted = listOf(unfinished))))["prev_total"], "no total without all three figures")
    }

    @Test
    fun withNoStatementAtAllTheLtcDaysAndThePreviousTotalAreLeftForTheOfficer() {
        // why: "0 days encashed" would be a claim; without the statement the app does not know.
        val values = filled(fill.fill(request("year" to number(2024)), inputs(rows = emptyList())))
        assertEquals(setOf("al_used", "accrued"), values.keys)
    }

    @Test
    fun aFigureBeyondWhatTheFormTakesIsLeftForTheOfficerNotFilled() {
        // why: a typed number stops at 9,999 (Part2Limits); a statement figure past it is a misread, and a wrong total must not be filled quietly.
        val huge = listOf(accrued("2023-12-31", 30, Part2Limits.NUMBER_MAX + 1), ltcEncashed("2024-04-26", Part2Limits.NUMBER_MAX + 1))
        val values = filled(fill.fill(request("year" to number(2024)), inputs(rows = huge)))
        assertEquals(setOf("al_used", "accrued"), values.keys)
        assertEquals(number(Part2Limits.NUMBER_MAX), filled(fill.fill(request("year" to number(2024)), inputs(rows = listOf(accrued("2023-12-31", 30, Part2Limits.NUMBER_MAX)))))["prev_total"], "the limit itself is a figure")
    }

    @Test
    fun theNoteStatesWhatTheLeaveUsedAllowsAndSaysNothingUntilItIsTyped() {
        assertEquals(emptyList(), fill.notes(request(), rows))
        assertEquals(listOf(FillNote.AccrualLimit(used = 17, allowed = 30)), fill.notes(request("al_used" to number(17)), rows))
        assertEquals(listOf(FillNote.AccrualLimit(used = 45, allowed = 15)), fill.notes(request("al_used" to number(45)), rows))
        assertEquals(listOf(FillNote.AccrualLimit(used = 70, allowed = 0)), fill.notes(request("al_used" to number(70)), emptyList()))
    }

    @Test
    fun theCrossCheckComparesTheDraftedTotalWithTheStatementsRowOfThatYear() {
        val figures = arrayOf("year" to number(2024), "prev_total" to number(282), "ltc_days" to number(0), "accrued" to number(19))
        assertEquals(YearCheck(2024, CrossCheck.Matches(301)), fill.crossCheck(request(*figures), rows))
        assertEquals(YearCheck(2024, CrossCheck.Differs(drafted = 302, statement = 301)), fill.crossCheck(request(*figures.dropLast(1).toTypedArray(), "accrued" to number(20)), rows))
        assertEquals(YearCheck(2024, CrossCheck.NoStatement), fill.crossCheck(request(*figures), emptyList()))
        assertEquals(YearCheck(2030, CrossCheck.NoStatement), fill.crossCheck(request("year" to number(2030), "prev_total" to number(1), "ltc_days" to number(0), "accrued" to number(0)), rows), "no row for 2030")
    }

    @Test
    fun noCrossCheckIsMadeWhileAFigureOrTheYearIsMissing() {
        assertNull(fill.crossCheck(request("year" to number(2024), "prev_total" to number(282), "ltc_days" to number(0)), rows))
        assertNull(fill.crossCheck(request("prev_total" to number(282), "ltc_days" to number(0), "accrued" to number(19)), rows))
    }
}
