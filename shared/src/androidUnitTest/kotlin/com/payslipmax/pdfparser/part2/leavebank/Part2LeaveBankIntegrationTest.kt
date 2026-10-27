package com.payslipmax.pdfparser.part2.leavebank

import com.payslipmax.pdfparser.domain.LeaveAccumulation
import com.payslipmax.pdfparser.insights.LeaveBankCalculator
import com.payslipmax.pdfparser.part2.domain.AcclveRules
import com.payslipmax.pdfparser.part2.domain.DraftResult
import com.payslipmax.pdfparser.part2.domain.FillNote
import com.payslipmax.pdfparser.part2.domain.FillOutcome
import com.payslipmax.pdfparser.part2.domain.LeaveBankInputs
import com.payslipmax.pdfparser.part2.domain.LeaveEventInput
import com.payslipmax.pdfparser.part2.domain.OccurrenceRequest
import com.payslipmax.pdfparser.part2.domain.RealPart2
import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import com.payslipmax.pdfparser.part2.domain.YearCheck
import com.payslipmax.pdfparser.part2.domain.YearlyBreakup
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.accrued
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.ltcEncashed
import com.payslipmax.pdfparser.testing.SyntheticLeaveAccumulation.payslip
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The cross-check of the year-end order against the Leave Bank (Part II plan, phase 8, DECISIONS Round 5 and 30): ACCLVE of year Y is
 * compared with the statement of year Y+1, found through the REAL `LeaveBankCalculator` over a synthetic December payslip, and the draft
 * comes from the REAL shipped template. A mismatch shows both figures and changes nothing; the break-up of an encashment comes from
 * the same rows. Synthetic figures only.
 */
class Part2LeaveBankIntegrationTest {
    private val statement =
        LeaveAccumulation(
            listOf(accrued("2008-12-31", 7, 7), accrued("2010-12-31", 5, 12), accrued("2023-12-31", 30, 282), ltcEncashed("2024-04-26", 10), accrued("2024-12-31", 19, 291)),
        )
    private val entries = LeaveBankFigures.entriesOf(LeaveBankCalculator.summarize(listOf(payslip(2025, 12, statement), payslip(2026, 3))))

    private fun acclve(
        accrued: Int,
        previous: Int,
        ltc: Int,
    ) = OccurrenceRequest("ACCLVE", mapOf("year" to number(2024), "as_on" to date(12, 31, 2024), "prev_total" to number(previous), "ltc_days" to number(ltc), "accrued" to number(accrued)))

    private fun slip(request: OccurrenceRequest) = assertIs<DraftResult.Drafted>(RealPart2.engine.draft(LeaveEventInput(listOf(request)))).slip

    @Test
    fun theRealCalculatorHandsOverTheStatementRowsAndTheSuggestionFillsTheRealTemplate() {
        assertEquals(5, entries.size, "the latest statement's rows as printed")
        val suggestion = LeaveBankFigures.suggest(2024, entries, null, alUsed = 0)
        assertEquals(282, suggestion.previousTotal)
        assertEquals(10, suggestion.ltcDays)

        val request = acclve(accrued = 19, previous = checkNotNull(suggestion.previousTotal), ltc = suggestion.ltcDays)
        assertEquals(291, AcclveRules.totalOf(request), "282 - 10 + 19")
        assertEquals("291", RealPart2.rows(slip(request)).single().data[1])
        assertEquals(CrossCheck.Matches(291), LeaveBankFigures.crossCheck(2024, checkNotNull(AcclveRules.totalOf(request)), entries))
    }

    @Test
    fun aMismatchShowsBothFiguresAndTheSlipIsExactlyWhatTheOfficerTyped() {
        val typed = acclve(accrued = 20, previous = 282, ltc = 10)
        val before = slip(typed)

        val check = LeaveBankFigures.crossCheck(2024, checkNotNull(AcclveRules.totalOf(typed)), entries)

        assertEquals(CrossCheck.Differs(drafted = 292, statement = 291), check)
        assertEquals(before, slip(typed), "the check changes nothing: the slip still carries the officer's 292")
        assertEquals("292", RealPart2.rows(slip(typed)).single().data[1])
    }

    @Test
    fun theStatementOfTheYearAfterIsNotHeldYetSoNothingIsCompared() {
        assertEquals(CrossCheck.NoStatement, LeaveBankFigures.crossCheck(2025, 300, entries))
    }

    @Test
    fun theEncashmentBreakUpComesFromTheSameRowsWithItsGapsNamedAndFillsTheRealTemplate() {
        val rows = LeaveBankFigures.breakup(entries)
        assertEquals(listOf(2008, 2010, 2023, 2024), rows.map { it.year })
        assertEquals((2009..2009).toList() + (2011..2022).toList(), LeaveBankFigures.missingYears(rows), "the years the statement has no row for, for the officer to type")

        val request = OccurrenceRequest("ENCASH", mapOf("breakup" to text(YearlyBreakup.format(rows))))
        val row = RealPart2.rows(slip(request)).single()
        assertEquals("61", row.data[1], "7 + 5 + 30 + 19")
        assertEquals("Allowed to encash 61 days of Annual Leave on retirement. Year-wise break up of Annual Leave accumulation: 2008 7, 2010 5, 2023 30, 2024 19, Total 61", row.narrative)
    }

    private val fillOf = { code: String -> checkNotNull(com.payslipmax.pdfparser.part2.domain.Part2Rules.shipped().fill(code)) }

    private fun leave(
        code: String,
        from: Int,
        to: Int,
    ) = OccurrenceRequest(code, mapOf("leave_from" to date(7, from, 2024), "leave_to" to date(7, to, 2024), "year" to number(2024)))

    @Test
    fun theFilledFiguresDraftTheRealTemplateAndTheCrossCheckAgreesWithTheStatement() {
        // why: the whole 8b path with no screen: the real summary's rows and the printed leave in, the slip and the check out.
        val fill = fillOf("ACCLVE")
        val inputs = LeaveBankInputs(entries, printed = listOf(leave("GTDAL", 1, 31), leave("EXTNAL", 1, 10)), drafted = emptyList())
        val values = (fill.fill(OccurrenceRequest("ACCLVE", mapOf("year" to number(2024))), inputs) as FillOutcome.Values).values
        assertEquals(mapOf("prev_total" to number(282), "ltc_days" to number(10), "al_used" to number(41), "accrued" to number(19)), values, "31 + 10 days used: min(30, 60 - 41) = 19")

        val request = OccurrenceRequest("ACCLVE", values + mapOf("year" to number(2024), "as_on" to date(12, 31, 2024)))
        assertEquals("291", RealPart2.rows(slip(request)).single().data[1], "282 - 10 + 19")
        assertEquals(YearCheck(2024, CrossCheck.Matches(291)), fill.crossCheck(request, entries))
        assertEquals(emptyList(), fill.notes(request, entries).filterNot { it is FillNote.AccrualLimit })
    }

    @Test
    fun theFilledBreakUpDraftsTheRealEncashmentAndNamesTheYearsTheStatementLacks() {
        val fill = fillOf("ENCASH")
        val values = (fill.fill(OccurrenceRequest("ENCASH"), LeaveBankInputs(entries, emptyList(), emptyList())) as FillOutcome.Values).values
        val request = OccurrenceRequest("ENCASH", values)
        assertEquals("61", RealPart2.rows(slip(request)).single().data[1])
        val notes = fill.notes(request, entries)
        assertEquals(FillNote.MissingYears((2009..2009).toList() + (2011..2022).toList()), notes[0])
        assertEquals(FillNote.StatementTotal(statement = 291, breakup = 61), notes[1], "the statement's own running total, never recomputed")
    }

    @Test
    fun everySlotAFillWritesIsAnAnswerSlotOfItsRealTemplate() {
        // why: a fill that names a slot the template does not have would fill nothing, silently.
        for (code in listOf("ACCLVE", "ENCASH")) {
            val fill = fillOf(code)
            val template = checkNotNull(RealPart2.engine.template(code))
            val answerable = template.slots.filter { it.kind in setOf("number", "years") }.map { it.id }.toSet()
            assertEquals(fill.slots, fill.slots.intersect(answerable), "$code: every slot it fills is a typed number or a year list")
        }
    }
}
