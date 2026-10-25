package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.part2.domain.RealPart2.number
import com.payslipmax.pdfparser.part2.domain.RealPart2.ref
import com.payslipmax.pdfparser.part2.domain.RealPart2.section
import com.payslipmax.pdfparser.part2.domain.RealPart2.text
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What content batch 2 ships (Part II plan, phase 6c), read from the real bundle: exactly CANCEL (p.226) and CANCELS (p.227), each in
 * group A on the page the lookup gives, each drafting without a hole when every answer is given, and none carrying markup. The batch
 * must be reachable from the layout (the Records cancellation case) without any screen naming a code.
 */
class Part2BatchTwoShipTest {
    private val batch = listOf("CANCEL", "CANCELS")
    private val templates get() = section.templates.filter { it.code in batch }.also { assertEquals(batch, it.map { t -> t.code }, "batch two ships CANCEL then CANCELS") }

    private val everything: Map<String, SlotValue> = mapOf("cancelled" to ref("0123/26", 6, 20), "cancelled_code" to text("GTDAL"), "cancelled_serial" to number(3))

    @Test
    fun eachTemplateCitesThePageTheLookupGivesAndSitsInGroupA() {
        val page = section.serials.flatMap { p -> p.codes.map { it to p } }.toMap()
        for (t in templates) {
            assertEquals(page.getValue(t.code).page, t.page, t.code)
            assertEquals("A", page.getValue(t.code).group, t.code)
        }
        assertEquals(listOf(226, 227), templates.map { it.page })
    }

    @Test
    fun everyTemplateDraftsWithoutAHoleWhenEveryAnswerIsGiven() {
        for (t in templates) {
            val unknown = t.slots.map { it.id }.filter { it !in everything }
            assertEquals(emptyList(), unknown, "${t.code}: add an answer for a new slot to this test")
            val row = rows(t.code)
            val printed = listOf(row.narrative, row.authority, row.certificate).plus(row.data).filterNotNull()
            assertTrue(printed.none { OccurrenceRenderer.BLANK in it }, "${t.code} left a blank")
            assertTrue(printed.none { '<' in it || '>' in it || "{" in it }, "${t.code} carries markup or an open placeholder")
        }
    }

    private fun rows(code: String) = RealPart2.rows(RealPart2.draft(OccurrenceRequest(code, everything))).single()

    @Test
    fun theLayoutHomesBothCodesInTheRecordsCancellationCaseWithAnOfferToDraftEach() {
        // why: the manual's own page names these two together; the officer finds them by the case and by "cancel" in search.
        val case = Part2LookupIndex(section).case("records_cancel")!!
        assertEquals(listOf("CANCEL", "CANCELS"), case.rows.map { it.draftCode })
    }

    @Test
    fun theTwoCancelCodesOfTheFamilyAreTheTwoTemplatesTheBatchShips() {
        assertEquals(CancelRules.CODES, templates.map { it.code }.toSet())
    }

    @Test
    fun noShippedWordIsOverTheShippedLimits() {
        val limits = checkNotNull(section.templateLimits)
        for (t in templates) {
            assertTrue(listOfNotNull(t.narrative, t.certificate).all { it.isNotBlank() && it.length <= limits.narrative }, t.code)
            assertTrue(t.slots.all { it.label.length <= limits.label && it.help.length <= limits.help }, t.code)
            assertTrue(t.slots.size <= limits.slots && t.links.size <= limits.links && t.rules.size <= limits.rules, t.code)
        }
    }
}
