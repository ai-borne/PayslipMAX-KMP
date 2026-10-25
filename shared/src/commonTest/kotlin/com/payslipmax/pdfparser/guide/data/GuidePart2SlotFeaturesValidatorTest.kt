package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.data.SealedSyntheticPart2.sealed
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.CAP_DAYS
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.GRANT_QUOTED
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NOTE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Kotlin recheck of the template features content batch 1 needed (Part II plan, phase 2b), mirroring `test_part2_template_calc.py`:
 * computed date slots, optional answers, check-only slots and composite column text. A template that wired any of them wrongly would
 * print a wrong date or a hole in the order a clerk types, so a bundle carrying one must refuse to load. Invented content only.
 */
class GuidePart2SlotFeaturesValidatorTest {
    private val section: Part2Lookup = SealedSyntheticPart2.lookup

    private fun slot(
        id: String,
        kind: String,
        vararg between: String,
        ifMissing: String? = null,
        forRule: String? = null,
    ) = Part2Slot(id, kind, "Label $id", "Help for $id.", between.toList(), ifMissing, forRule)

    private val dates = listOf(slot("d1", "date"), slot("d2", "date"))

    private fun problems(edit: (Part2Template) -> Part2Template): List<String> {
        val edited = section.copy(templates = section.templates.map { if (it.code == NOTE) edit(it).sealed() else it })
        return GuideBundleValidator.validate(SyntheticPart2Templates.bundle().copy(part2 = edited))
    }

    private fun withSlots(
        extra: List<Part2Slot>,
        narrative: String? = null,
        rules: List<String>? = null,
    ): List<String> =
        problems { t -> t.copy(slots = t.slots + extra, narrative = narrative ?: t.narrative, rules = rules ?: t.rules) }

    private fun assertProblem(
        found: List<String>,
        fragment: String,
    ) = assertTrue(found.any { fragment in it }, "expected '$fragment', got $found")

    @Test
    fun earliestLatestAndNextDayAreValidAndCountAsUsingTheirInputs() {
        val slots = dates + listOf(slot("first", "earliest", "d1", "d2"), slot("last", "latest", "d1", "d2"), slot("after", "next_day", "last"))
        assertEquals(emptyList(), withSlots(slots, "Noted {noted_on} as {remark} from {first} to {last}, back {after}"))
    }

    @Test
    fun aComputedSlotMayNameTheFromAndToColumns() {
        val slots = dates + listOf(slot("first", "earliest", "d1", "d2"), slot("after", "next_day", "first"))
        assertEquals(emptyList(), problems { it.copy(slots = it.slots + slots, from = Part2Column("required", "first"), to = Part2Column("required", "after")) })
    }

    @Test
    fun aComputedSlotMustNameDeclaredDateValuedSlotsInTheRightNumber() {
        val text = "Noted {noted_on} as {remark} {calc}"
        assertProblem(withSlots(dates + slot("calc", "earliest", "d1", "ghost"), text), "slot calc: between must name 2 to 4 different date slots")
        assertProblem(withSlots(dates + slot("calc", "latest", "d1", "remark"), text), "slot calc: between must name 2 to 4 different date slots")
        assertProblem(withSlots(dates + slot("calc", "earliest", "d1"), text), "slot calc: between must name 2 to 4 different date slots")
        assertProblem(withSlots(dates + slot("calc", "earliest", "d1", "d1"), text), "slot calc: between must name 2 to 4 different date slots")
        assertProblem(withSlots(dates + slot("calc", "next_day", "d1", "d2"), text), "slot calc: between must name one date slot")
    }

    @Test
    fun aComputedSlotMayOnlyReadSlotsDeclaredBeforeItSoNoLoopIsPossible() {
        val loop = listOf(slot("early", "next_day", "later"), slot("later", "next_day", "early"))
        assertProblem(withSlots(dates + loop, "Noted {noted_on} as {remark} {early} {later}"), "slot early: between must name one date slot")
    }

    @Test
    fun onlyDaysAndComputedSlotsHaveBetweenAndTheKindListNamesTheComputedKinds() {
        assertProblem(problems { t -> t.copy(slots = t.slots.map { if (it.id == "remark") it.copy(between = listOf("noted_on")) else it }) }, "slot remark: only a days or computed slot has between")
        assertProblem(problems { t -> t.copy(slots = t.slots.map { if (it.id == "remark") it.copy(kind = "money") else it }) }, "slot remark: kind must be date, number, text, days, ref, earliest, latest or next_day")
    }

    @Test
    fun ifMissingBelongsToADateNumberOrTextSlotAndIsPlainAndShort() {
        for (kind in listOf("date", "number", "text")) {
            assertEquals(emptyList(), withSlots(listOf(slot("extra", kind, ifMissing = "Nil")), "Noted {noted_on} as {remark} with {extra}"), kind)
        }
        val limit = SyntheticPart2Templates.limits.note
        assertProblem(withSlots(dates + slot("extra", "earliest", "d1", "d2", ifMissing = "Nil"), "Noted {noted_on} as {remark} {extra}"), "slot extra: if_missing is only for a date, number or text slot")
        assertProblem(withSlots(listOf(slot("extra", "ref", ifMissing = "Nil")), "Noted {noted_on} as {remark} {extra.no}"), "slot extra: if_missing is only for a date, number or text slot")
        assertProblem(withSlots(listOf(slot("extra", "date", ifMissing = "<b>")), "Noted {noted_on} as {remark} {extra}"), "slot extra: if_missing has angle brackets")
        assertProblem(withSlots(listOf(slot("extra", "date", ifMissing = "")), "Noted {noted_on} as {remark} {extra}"), "slot extra: if_missing is empty")
        assertProblem(withSlots(listOf(slot("extra", "date", ifMissing = "x".repeat(limit + 1))), "Noted {noted_on} as {remark} {extra}"), "slot extra: if_missing is over $limit characters")
    }

    @Test
    fun aSlotForARuleTheTemplateRunsNeedsNoOtherUseAndOneForAnotherRuleIsRefused() {
        val earlier = slot("earlier", "number", forRule = CAP_DAYS)
        assertEquals(emptyList(), withSlots(listOf(earlier), rules = listOf(CAP_DAYS)))
        assertProblem(withSlots(listOf(slot("earlier", "number", forRule = GRANT_QUOTED)), rules = listOf(CAP_DAYS)), "slot earlier: for names a rule the template does not run")
        assertProblem(withSlots(listOf(earlier)), "slot earlier: for names a rule the template does not run")
        assertProblem(withSlots(listOf(slot("calc", "next_day", "noted_on", forRule = CAP_DAYS)), rules = listOf(CAP_DAYS)), "slot calc: for is only for a date, number or text slot")
    }

    @Test
    fun aColumnMayJoinWordsAndSlotsButTakesASlotOrTextNeverBoth() {
        assertEquals(emptyList(), problems { it.copy(data = listOf(Part2Column("required", text = "{remark} on {noted_on}")) + it.data.drop(1)) })
        assertEquals(emptyList(), problems { it.copy(data = listOf(Part2Column("required", text = "R1H1")) + it.data.drop(1)) }, "a fixed word needs no slot")
        assertProblem(problems { it.copy(data = listOf(Part2Column("required", "remark", "x {remark}")) + it.data.drop(1)) }, "column data1 names a slot and text, use one")
        assertProblem(problems { it.copy(data = listOf(Part2Column("none", text = "x {remark}")) + it.data.drop(1)) }, "column data1 is not required but has text")
        assertProblem(problems { it.copy(from = Part2Column("required", text = "{noted_on}")) }, "column from must name a date slot, not text")
    }

    @Test
    fun columnTextIsHeldToThePlaceholderRulesTheTextLimitAndPlainText() {
        fun withText(text: String) = problems { it.copy(data = listOf(Part2Column("required", text = text)) + it.data.drop(1)) }
        assertProblem(withText("{remark"), "column data1 has a malformed placeholder")
        assertProblem(withText("{remark} {ghost}"), "slot ghost used but not declared")
        assertProblem(withText("<b>{remark}</b>"), "column data1 has angle brackets")
        assertProblem(withText("{remark} " + "x".repeat(SyntheticPart2Templates.limits.narrative)), "column data1 is over ${SyntheticPart2Templates.limits.narrative} characters")
    }
}
