package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.EXTENSION
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NOTE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

/**
 * The officer types these answers, so they are bounded, plain text and real dates before they reach a template: a draft is refused
 * with the field named, and the refusal never carries what was typed (it may be logged by nothing, but must not be able to be).
 */
class Part2DraftInputTest {
    private val engine = Part2DraftEngine(SyntheticPart2Templates.lookup, SyntheticPart2Templates.registry())

    private fun date(
        month: Int,
        day: Int,
        year: Int = 2026,
    ) = SlotValue.DateValue(CalendarDate(year, month, day))

    private val grant: Map<String, SlotValue> =
        mapOf("leave_from" to date(7, 5), "leave_to" to date(9, 4), "year" to SlotValue.NumberValue(2026))
    private val note: Map<String, SlotValue> = mapOf("noted_on" to date(7, 5), "remark" to SlotValue.TextValue("fine"))

    private fun issues(
        code: String,
        values: Map<String, SlotValue>,
        lastOrder: OrderRef? = null,
    ): List<DraftIssue> = assertIs<DraftResult.Refused>(engine.draft(LeaveEventInput(listOf(OccurrenceRequest(code, values)), lastOrder))).issues

    private fun only(
        code: String,
        values: Map<String, SlotValue>,
    ) = issues(code, values).single()

    @Test
    fun aRequiredAnswerThatIsMissingIsNamed() {
        assertEquals(DraftIssue(GRANT, "year", DraftIssueReason.NOT_ANSWERED), only(GRANT, grant - "year"))
        assertEquals(DraftIssue(NOTE, "remark", DraftIssueReason.NOT_ANSWERED), only(NOTE, note - "remark"))
    }

    @Test
    fun blankTextCountsAsMissing() {
        assertEquals(DraftIssue(NOTE, "remark", DraftIssueReason.NOT_ANSWERED), only(NOTE, note + ("remark" to SlotValue.TextValue("   "))))
    }

    @Test
    fun aDateThatDoesNotExistIsRefusedNotRolledOver() {
        for (bad in listOf(CalendarDate(2026, 2, 30), CalendarDate(2026, 13, 1), CalendarDate(1999, 12, 31), CalendarDate(2101, 1, 1))) {
            assertEquals(DraftIssue(GRANT, "leave_from", DraftIssueReason.BAD_DATE), only(GRANT, grant + ("leave_from" to SlotValue.DateValue(bad))), "$bad")
        }
    }

    @Test
    fun aRangeThatEndsBeforeItStartsIsRefusedOnTheEndDate() {
        assertEquals(DraftIssue(GRANT, "leave_to", DraftIssueReason.DATES_REVERSED), only(GRANT, grant + ("leave_to" to date(7, 4))))
    }

    @Test
    fun anAnswerOfTheWrongKindIsRefusedNotCoerced() {
        assertEquals(DraftIssue(GRANT, "year", DraftIssueReason.WRONG_KIND), only(GRANT, grant + ("year" to SlotValue.TextValue("2026"))))
        assertEquals(DraftIssue(NOTE, "noted_on", DraftIssueReason.WRONG_KIND), only(NOTE, note + ("noted_on" to SlotValue.NumberValue(5))))
    }

    @Test
    fun aNumberOutsideTheAllowedRangeIsRefused() {
        assertEquals(DraftIssue(GRANT, "year", DraftIssueReason.OUT_OF_RANGE), only(GRANT, grant + ("year" to SlotValue.NumberValue(-1))))
        assertEquals(DraftIssue(GRANT, "year", DraftIssueReason.OUT_OF_RANGE), only(GRANT, grant + ("year" to SlotValue.NumberValue(Part2Limits.NUMBER_MAX + 1))))
        assertEquals(emptyList(), issuesOrEmpty(GRANT, grant + ("year" to SlotValue.NumberValue(Part2Limits.NUMBER_MAX))))
    }

    @Test
    fun textIsBoundedAndPlain() {
        val long = SlotValue.TextValue("x".repeat(Part2Limits.TEXT_MAX + 1))
        assertEquals(DraftIssue(NOTE, "remark", DraftIssueReason.TOO_LONG), only(NOTE, note + ("remark" to long)))
        assertEquals(emptyList(), issuesOrEmpty(NOTE, note + ("remark" to SlotValue.TextValue("x".repeat(Part2Limits.TEXT_MAX)))))
        for (markup in listOf("<b>bold</b>", "a > b", "<script>")) {
            assertEquals(DraftIssue(NOTE, "remark", DraftIssueReason.MARKUP), only(NOTE, note + ("remark" to SlotValue.TextValue(markup))), markup)
        }
    }

    @Test
    fun anOrderReferenceIsBoundedPlainAndDated() {
        val extension = mapOf("ext_from" to date(9, 5), "new_to" to date(9, 30))

        fun ref(
            no: String,
            at: CalendarDate = CalendarDate(2026, 6, 30),
        ) = extension + ("grant" to SlotValue.RefValue(OrderRef(no, at)))
        assertEquals(DraftIssue(EXTENSION, "grant", DraftIssueReason.NOT_ANSWERED), only(EXTENSION, ref("  ")))
        assertEquals(DraftIssue(EXTENSION, "grant", DraftIssueReason.TOO_LONG), only(EXTENSION, ref("1".repeat(Part2Limits.ORDER_NO_MAX + 1))))
        assertEquals(DraftIssue(EXTENSION, "grant", DraftIssueReason.MARKUP), only(EXTENSION, ref("<1>")))
        assertEquals(DraftIssue(EXTENSION, "grant", DraftIssueReason.BAD_DATE), only(EXTENSION, ref("41/2026", CalendarDate(2026, 2, 30))))
        assertEquals(emptyList(), issuesOrEmpty(EXTENSION, ref("41/2026")))
    }

    @Test
    fun theLastOrderTheOfficerTypedIsHeldToTheSameBounds() {
        val bad = OrderRef("<x>", CalendarDate(2026, 6, 1))
        assertEquals(DraftIssue("", "last_order", DraftIssueReason.MARKUP), issues(GRANT, grant, bad).single())
        assertEquals(DraftIssue("", "last_order", DraftIssueReason.BAD_DATE), issues(GRANT, grant, OrderRef("1", CalendarDate(2026, 2, 30))).single())
    }

    @Test
    fun anEventCannotHoldMoreRequestsThanTheLimit() {
        val many = List(Part2Limits.REQUESTS_MAX + 1) { OccurrenceRequest(GRANT, grant) }
        assertEquals(listOf(DraftIssue("", "", DraftIssueReason.TOO_MANY)), assertIs<DraftResult.Refused>(engine.draft(LeaveEventInput(many))).issues)
    }

    @Test
    fun everyProblemIsReportedAtOnceNotJustTheFirst() {
        val found = issues(GRANT, grant - "year" + ("leave_to" to date(7, 4)))
        assertEquals(setOf("year", "leave_to"), found.map { it.slot }.toSet())
    }

    @Test
    fun aRefusalNeverCarriesWhatWasTyped() {
        val secret = "TypedSecret77"
        val found = issues(NOTE, note + ("remark" to SlotValue.TextValue("<$secret>")))
        assertFalse(found.toString().contains(secret), "$found")
    }

    private fun issuesOrEmpty(
        code: String,
        values: Map<String, SlotValue>,
    ): List<DraftIssue> = (engine.draft(LeaveEventInput(listOf(OccurrenceRequest(code, values)))) as? DraftResult.Refused)?.issues.orEmpty()
}
