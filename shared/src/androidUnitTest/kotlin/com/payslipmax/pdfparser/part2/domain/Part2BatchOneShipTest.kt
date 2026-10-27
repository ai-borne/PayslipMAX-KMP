package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RealPart2.date
import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.section
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What content batch 1 ships (Part II plan, phase 2b), read from the real bundle: exactly the 24 templates the plan names, each citing
 * the page the lookup gives for its code, each drafting without a hole when every answer is given, and none carrying markup.
 */
class Part2BatchOneShipTest {
    private val annualSet = listOf("GTDAL", "GTDALCF", "EXTNAL", "GTDCL", "GTDSPCL", "REJOINAL", "VOLREJ", "RECALL")
    private val rationFour = listOf("SORS", "GTDRSHNA", "TORS", "STPRSHNA")
    private val rh = listOf("STPRH") + AreaRules.rhCodes

    /** One plausible answer for every slot id the batch declares. Computed slots (days, first and last day away, rejoining) are worked out. */
    private val everything: Map<String, SlotValue> =
        mapOf(
            "leave_from" to date(7, 6), "leave_to" to date(8, 4), "prefix_first" to date(7, 4), "suffix_last" to date(8, 6),
            "year" to number(2026), "earlier_al" to number(10), "retire_on" to date(12, 1), "reason" to text("a reason"), "leave_type" to text("annual leave"),
            "leave_kind" to text("annual leave"), "rejoined_on" to date(8, 7), "recalled_on" to date(7, 20), "cancel_from" to date(7, 21),
            "cancel_to" to date(8, 4), "station" to text("ZZ01"), "corps" to text("Alpha"), "serial" to text("12"),
            "grant" to ref("0123/26", 6, 20), "carry_order" to ref("0050/25", 12, 1, 2025), "notif" to ref("N-17/2026", 3, 2),
        )

    /** The shipped templates, never an empty list: a loop over nothing would pass whatever the content. */
    private val templates get() = section.templates.filter { it.code in annualSet + rationFour + rh }.also { assertEquals(24, it.size, "batch one ships 24 templates") }

    private val byCode get() = templates.associateBy { it.code }

    @Test
    fun theBundleShipsExactlyTheTwentyFourBatchOneTemplates() {
        val batchOne = annualSet + rationFour + rh
        assertEquals(batchOne.sorted(), section.templates.map { it.code }.filter { it in batchOne }.sorted())
        assertEquals(24, section.templates.count { it.code in batchOne })
    }

    @Test
    fun eachTemplateCitesThePageTheLookupGivesForItsCode() {
        // why: the template page and the code page must agree, or the officer is sent to a page that does not hold the row.
        val lookupPage = section.serials.flatMap { page -> page.codes.map { it to page.page } }.toMap()
        for (template in templates) assertEquals(lookupPage.getValue(template.code), template.page, template.code)
    }

    @Test
    fun everyTemplateDraftsWithoutAHoleWhenEveryAnswerIsGiven() {
        val engine = Part2DraftEngine(section, Part2Rules.shipped())
        for (template in templates) {
            val unknown = template.slots.map { it.id }.filter { it !in everything && it !in COMPUTED }
            assertEquals(emptyList(), unknown, "${template.code}: add an answer for a new slot to this test")
            val result = engine.draft(LeaveEventInput(listOf(OccurrenceRequest(template.code, everything))))
            val slip = (result as? DraftResult.Drafted)?.slip ?: error("${template.code} was refused: $result")
            val row = slip.orders.flatMap { it.occurrences }.first { it.code == (template.print ?: template.code) }
            val printed = listOf(row.narrative, row.authority, row.certificate).plus(row.data)
            assertTrue(printed.filterNotNull().none { OccurrenceRenderer.BLANK in it && "{" in it }, "${template.code} left a placeholder open")
            assertTrue(printed.filterNotNull().none { '<' in it || '>' in it }, "${template.code} carries markup")
        }
    }

    @Test
    fun noShippedTemplateWordIsEmptyOrOverTheShippedLimits() {
        val limits = checkNotNull(section.templateLimits)
        for (t in templates) {
            val texts = listOfNotNull(t.narrative, t.authority, t.certificate) + t.data.mapNotNull { it.text }
            assertTrue(texts.all { it.isNotBlank() && it.length <= limits.narrative }, t.code)
            assertTrue(t.slots.all { it.label.length <= limits.label && it.help.length <= limits.help }, t.code)
            assertTrue(t.slots.size <= limits.slots && t.links.size <= limits.links && t.rules.size <= limits.rules, t.code)
        }
    }

    @Test
    fun onlyTheGrantsLinkAndEveryLinkCitesItsBasis() {
        for (t in templates) {
            for (link in t.links) {
                assertTrue(link.basis == "manual" && link.page != null || link.basis == "practice" && link.page == null, "${t.code} -> ${link.code}")
            }
        }
        val linking = templates.filter { it.links.isNotEmpty() }.map { it.code }.toSet()
        assertEquals(setOf("GTDAL", "GTDALCF", "GTDCL", "GTDSPCL"), linking)
        val area = AreaRules.haaCodes + AreaRules.scciaCodes + AreaRules.fieldCodes + "STPTRAN" + AreaRules.tranCodes
        assertEquals(listOf("SORS", "GTDRSHNA", "STPRH", "TORS", "STPRSHNA") + AreaRules.rhCodes + area, byCode.getValue("GTDAL").links.map { it.code })
        assertEquals(listOf("SORS", "GTDRSHNA", "TORS", "STPRSHNA") + area, byCode.getValue("GTDCL").links.map { it.code }, "ration and the area re-grants on casual leave, never R&H")
    }

    private companion object {
        val COMPUTED = setOf("days", "cancel_days", "away_from", "last_away", "rejoin_on")
    }
}
