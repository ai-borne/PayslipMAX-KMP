package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The dates and words a row needs that the officer never types as such (Part II plan, phase 2b): the first day away is the earlier of
 * the first prefix day and the leave start, the last day away the later of the leave end and the last suffix day, the rejoining day the
 * day after; a prefix or suffix may be absent ('Nil'); a column may join words and dates; an input may exist only for a check. The
 * template is invented (the engine knows no code), so these pin how the engine uses a template, not what the manual says.
 */
class Part2ComputedSlotsTest {
    private fun date(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = SlotValue.DateValue(CalendarDate(year, month, day))

    private fun slot(
        id: String,
        kind: String,
        vararg between: String,
        ifMissing: String? = null,
        forRule: String? = null,
    ) = Part2Slot(id, kind, "Label $id", "Help for $id.", between.toList(), ifMissing, forRule)

    private fun need(slot: String) = Part2Column("required", slot)

    private val template =
        Part2Template(
            code = GRANT,
            rev = "b0000001",
            page = 28,
            narrative = "Away {away_from} to {last_away}, prefix {prefix_first}, suffix {suffix_last}, back {rejoin_on}",
            slots =
                listOf(
                    slot("prefix_first", "date", ifMissing = "Nil"),
                    slot("leave_from", "date"),
                    slot("leave_to", "date"),
                    slot("suffix_last", "date", ifMissing = "Nil"),
                    slot("away_from", "earliest", "prefix_first", "leave_from"),
                    slot("last_away", "latest", "leave_to", "suffix_last"),
                    slot("rejoin_on", "next_day", "last_away"),
                    slot("days", "days", "leave_from", "leave_to"),
                    slot("earlier", "number", forRule = CAP_DAYS),
                ),
            from = need("away_from"),
            to = need("last_away"),
            data = listOf(Part2Column("required", text = "{days} days from {leave_from}"), Part2Column("optional", "prefix_first"), Part2Column("none"), Part2Column("none")),
            rules = listOf(CAP_DAYS),
        )

    private val engine = Part2DraftEngine(SyntheticPart2Templates.lookup.copy(templates = listOf(template)), SyntheticPart2Templates.registry())

    private val base: Map<String, SlotValue> = mapOf("leave_from" to date(7, 5), "leave_to" to date(9, 4))

    private fun row(vararg extra: Pair<String, SlotValue>): Part2Occurrence {
        val result = engine.draft(LeaveEventInput(listOf(OccurrenceRequest(GRANT, base + extra))))
        return assertIs<DraftResult.Drafted>(result).slip.orders.single().occurrences.single()
    }

    private fun refused(vararg extra: Pair<String, SlotValue>): List<DraftIssue> =
        assertIs<DraftResult.Refused>(engine.draft(LeaveEventInput(listOf(OccurrenceRequest(GRANT, base + extra))))).issues

    @Test
    fun theFirstDayAwayIsTheEarlierOfThePrefixAndTheLeaveStart() {
        assertEquals(CalendarDate(2026, 7, 1), row("prefix_first" to date(7, 1)).from)
        assertEquals(CalendarDate(2026, 7, 5), row().from, "no prefix: the leave start")
        assertEquals(CalendarDate(2026, 7, 5), row("prefix_first" to date(7, 9)).from, "a prefix dated after the start never moves the start later")
    }

    @Test
    fun theLastDayAwayIsTheLaterOfTheLeaveEndAndTheSuffixAndTheRejoiningDayFollowsIt() {
        val suffixed = row("suffix_last" to date(9, 7))
        assertEquals(CalendarDate(2026, 9, 7), suffixed.to)
        assertEquals("Away 05/07/2026 to 07/09/2026, prefix Nil, suffix 07/09/2026, back 08/09/2026", suffixed.narrative)
        val plain = row()
        assertEquals(CalendarDate(2026, 9, 4), plain.to)
        assertEquals("Away 05/07/2026 to 04/09/2026, prefix Nil, suffix Nil, back 05/09/2026", plain.narrative)
    }

    @Test
    fun theRejoiningDayRollsOverTheEndOfTheYear() {
        val year = row("leave_to" to date(12, 31))
        assertEquals("Away 05/07/2026 to 31/12/2026, prefix Nil, suffix Nil, back 01/01/2027", year.narrative)
    }

    @Test
    fun anAnswerLeftOutPrintsItsIfMissingWordAndAnOptionalColumnStaysEmpty() {
        assertEquals(listOf("62 days from 05/07/2026", "", "", ""), row().data)
        assertEquals("01/07/2026", row("prefix_first" to date(7, 1)).data[1])
    }

    @Test
    fun aColumnJoinsWordsAndAnswersAndAnUnansweredPartIsRefusedByName() {
        assertEquals("62 days from 05/07/2026", row().data[0])
        val typedDays = template.copy(slots = template.slots.filter { it.id != "days" } + slot("days", "number"))
        val bare = Part2DraftEngine(SyntheticPart2Templates.lookup.copy(templates = listOf(typedDays)), SyntheticPart2Templates.registry())
        val missing = bare.draft(LeaveEventInput(listOf(OccurrenceRequest(GRANT, base))))
        assertEquals(listOf(DraftIssue(GRANT, "days", DraftIssueReason.NOT_ANSWERED)), assertIs<DraftResult.Refused>(missing).issues)
    }

    @Test
    fun aSlotOnlyACheckReadsIsNeverRequiredButIsHeldToTheSameBounds() {
        assertEquals(emptyList(), refused("earlier" to SlotValue.NumberValue(-1)).filter { it.slot != "earlier" })
        assertEquals(listOf(DraftIssue(GRANT, "earlier", DraftIssueReason.OUT_OF_RANGE)), refused("earlier" to SlotValue.NumberValue(Part2Limits.NUMBER_MAX + 1)))
        assertEquals("Away 05/07/2026 to 04/09/2026, prefix Nil, suffix Nil, back 05/09/2026", row("earlier" to SlotValue.NumberValue(10)).narrative)
    }

    @Test
    fun aComputedDateWithNoDateToWorkFromIsRefusedOnItsOwnId() {
        val noLeave = engine.draft(LeaveEventInput(listOf(OccurrenceRequest(GRANT, emptyMap()))))
        val slots = assertIs<DraftResult.Refused>(noLeave).issues.map { it.slot }
        assertEquals(true, "leave_from" in slots && "last_away" in slots, "got $slots")
        assertEquals(true, slots.none { it == "prefix_first" || it == "suffix_last" || it == "earlier" }, "an optional answer is never asked for")
    }

    @Test
    fun theDayAfterTheLastAllowedDayIsRefusedNotInvented() {
        val issues = refused("leave_to" to date(12, 31, 2100), "leave_from" to date(7, 5, 2100))
        assertEquals(listOf(DraftIssue(GRANT, "rejoin_on", DraftIssueReason.BAD_DATE)), issues)
    }

    @Test
    fun aBadDateIsRefusedOnTheSlotAndNothingIsComputedFromIt() {
        val issues = refused("prefix_first" to date(2, 30))
        assertEquals(listOf(DraftIssue(GRANT, "prefix_first", DraftIssueReason.BAD_DATE)), issues, "the bad date is blamed once, not again on every date worked from it")
    }

    @Test
    fun theDaysCountStillRunsBothDaysCounted() {
        assertEquals("62 days from 05/07/2026", row().data[0], "the leave notes' own example: 05/07 to 04/09 is 62 days, not 60")
        assertEquals(DraftIssueReason.DATES_REVERSED, refused("leave_to" to date(7, 1)).first { it.slot == "leave_to" }.reason)
    }
}
