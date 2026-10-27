package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.guide.model.Part2Link
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Which orders carry the area re-grants (Part II plan, phase 9; owner, Round 35: every leave that returns the officer), and that the shipped
 * bundle is internally whole: every link lands on a template, no leave that does not return the officer carries one, and the one template
 * with the most links is the one the `links` limit was raised for (Phase 11 item 8).
 */
class Part2BatchFourScopeTest {
    private val section = RealPart2.section
    private val planner = LeaveFormPlanner(RealPart2.engine, Part2Rules.shipped())

    /** The leave grants that return the officer to the unit: the annual set without its extension, casual leave, and Chapter 5's grants. */
    private val inScope = listOf("GTDAL", "GTDALCF", "GTDCL", "GTDSPCL", "FURLEV", "MTRNLV", "SPMTRNLV", "GTDCCL", "EOL", "CHADPLVE", "PTRNLV", "PATLCHAD", "STDLEV", "STDLVAL", "STDLVFUR")
    private val outOfScope = listOf("EXTNAL", "EXTNFUR", "REJOINAL", "VOLREJ", "RECALL", "TERLEV")
    private val areaCodes = AreaRules.haaCodes + AreaRules.scciaCodes + AreaRules.fieldCodes + AreaRules.tranCodes + "STPTRAN"

    private fun link(
        code: String,
        page: Int,
        note: String,
        condition: String,
    ) = Part2Link(code, "manual", page, note, condition)

    private val expectedLinks: List<Part2Link> =
        AreaRules.haaCodes.map { link(it, 49, "ii", "haa_" + it.lowercase()) } +
            AreaRules.scciaCodes.map { link(it, 53, "2", "sccia_" + it.lowercase()) } +
            AreaRules.fieldCodes.map { link(it, 55, "2", "field_" + it.lowercase()) } +
            link("STPTRAN", 71, "v", "tran_stop") +
            AreaRules.tranCodes.map { link(it, 71, "iii", "tran_" + it.lowercase()) }

    private fun template(code: String) = checkNotNull(section.templates.firstOrNull { it.code == code }) { "no template $code" }

    @Test
    fun everyLeaveThatReturnsTheOfficerEndsWithTheFifteenAreaLinksCitedToTheManual() {
        for (code in inScope) assertEquals(expectedLinks, template(code).links.takeLast(15), code)
    }

    @Test
    fun noOtherTemplateLinksToAnAreaCode() {
        for (t in section.templates) {
            val linked = t.links.map { it.code }.filter { it in areaCodes }
            assertEquals(if (t.code in inScope) 15 else 0, linked.size, t.code)
        }
        for (code in outOfScope) assertTrue(template(code).links.none { it.code in areaCodes }, "$code does not return the officer on its own")
    }

    @Test
    fun theAnnualLeaveTemplatesHoldTheMostLinksAndTheLimitIsRaisedToExactlyThat() {
        // why: Phase 11 item 8 - GTDAL used all 16; the 15 area links raise it to 31 and the limit follows the longest real template (Round 12: no link-group feature).
        val most = section.templates.maxOf { it.links.size }
        assertEquals(31, most)
        assertEquals(listOf("GTDAL", "GTDALCF"), section.templates.filter { it.links.size == most }.map { it.code })
        assertEquals(most, section.templateLimits?.links, "the limit travels with the section and equals the longest real template")
    }

    @Test
    fun everyAreaTemplateExistsAndCitesTheAnnexureIIIPageOfItsRow() {
        val pages = mapOf("GTDHH11" to 48, "GTDHH31" to 49, "GTDHH32" to 49, "GTDCIAFD" to 52, "GTDCIAMF" to 52, "GTDCIAPC" to 52, "GTDHAFA" to 54, "GTDCFAA" to 55, "GTDCMFA" to 55)
        for ((code, page) in pages + areaCodes.filter { it.startsWith("GTDTRAN") || it == "STPTRAN" }.associateWith { 70 }) assertEquals(page, template(code).page, code)
        assertEquals(15, section.templates.count { it.code in areaCodes })
    }

    @Test
    fun everyLeaveInScopeDraftsTheSccAndTransportRegrantsWhenAskedWithTheFormsOwnFields() {
        // why: the form asks exactly what the orders print, so answering what it asks must always complete the slip for every leave that carries the links.
        for (code in inScope) {
            for (answer in listOf(AreaRules.ANSWER_SCCIA_PREFIX + "GTDCIAFD", AreaRules.ANSWER_TRAN_PREFIX + "GTDTRAN1")) {
                val request = OccurrenceRequest(code, emptyMap(), setOf(answer))
                val drafted = request.copy(values = longAbsence(planner.fields(request)))
                val withAreaFields = drafted.copy(values = longAbsence(planner.fields(drafted)))
                val result = RealPart2.engine.draft(LeaveEventInput(listOf(withAreaFields)))
                assertIs<DraftResult.Drafted>(result, "$code $answer: ${(result as? DraftResult.Refused)?.issues}")
                val printed = result.slip.orders.flatMap { it.occurrences }.map { it.code }
                assertTrue((if (answer.startsWith("sccia")) "GTDCIAFD" else "GTDTRAN1") in printed, "$code $answer: $printed")
            }
        }
    }

    /** Values for every field asked: dates from 25 June to 5 August so a whole month is covered, whatever the leave's own field order. */
    private fun longAbsence(fields: List<FormField>): Map<String, SlotValue> {
        val dates = mapOf("prefix_first" to CalendarDate(2026, 6, 24), "leave_from" to CalendarDate(2026, 6, 25), "leave_to" to CalendarDate(2026, 8, 5), "suffix_last" to CalendarDate(2026, 8, 6))
        return fields.associate { field ->
            field.slot.id to
                when (field.slot.kind) {
                    "date" -> SlotValue.DateValue(dates[field.slot.id] ?: CalendarDate(2026, 6, 20))
                    "number" -> SlotValue.NumberValue(1)
                    "ref" -> SlotValue.RefValue(OrderRef("N-1/26", CalendarDate(2026, 3, 2)))
                    "years" -> SlotValue.TextValue("2011 30; 2013 20")
                    else -> SlotValue.TextValue("plain")
                }
        }
    }
}
