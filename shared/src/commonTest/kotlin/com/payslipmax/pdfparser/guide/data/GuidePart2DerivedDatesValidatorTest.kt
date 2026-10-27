package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.data.SealedSyntheticPart2.sealed
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2RuleDecl
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates.NOTE
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The Kotlin recheck of `DerivedDates` in `test_part2_derived_slots.py` (Part II plan, phase 9): the Transport Allowance rows are dated by
 * whole calendar months, which no earliest, latest or next_day slot can say, so a derive rule works the date out and From or To may name
 * a derived slot. Any other kind of slot, or a part of a reference, is still refused. Invented content only.
 */
class GuidePart2DerivedDatesValidatorTest {
    private val section: Part2Lookup = SealedSyntheticPart2.lookup
    private val deriveRule = Part2RuleDecl("sum_it", "derive", null, "manual", 133, "(a)")
    private val inputs = listOf(Part2Slot("first", "number", "Label first", "Help first."), Part2Slot("second", "number", "Label second", "Help second."))
    private val total = Part2Slot("total", "derived", "Label total", "Help total.", by = "sum_it", reads = listOf("first", "second"))

    private fun problems(edit: (Part2Template) -> Part2Template): List<String> {
        val edited =
            section.copy(
                rules = section.rules + deriveRule,
                templates = section.templates.map { if (it.code == NOTE) edit(it.copy(slots = it.slots + inputs + total, narrative = "Noted {noted_on} as {remark} {first} {second}")).sealed() else it },
            )
        return GuideBundleValidator.validate(SyntheticPart2Templates.bundle().copy(part2 = edited))
    }

    @Test
    fun fromAndToMayNameADerivedSlot() {
        assertEquals(emptyList(), problems { it.copy(from = Part2Column("required", "total")) }, "from")
        assertEquals(emptyList(), problems { it.copy(to = Part2Column("required", "total")) }, "to")
    }

    @Test
    fun fromAndToStillRefuseAnyOtherKindOfSlotAndAPartOfAReference() {
        val numberInFrom = problems { it.copy(from = Part2Column("required", "first")) }
        assertTrue(numberInFrom.any { "column from must name a date slot" in it }, "$numberInFrom")
        val numberInTo = problems { it.copy(to = Part2Column("required", "first")) }
        assertTrue(numberInTo.any { "column to must name a date slot" in it }, "$numberInTo")
        val part = problems { it.copy(from = Part2Column("required", "total.date")) }
        assertTrue(part.any { "column from must name a date slot" in it }, "$part")
    }
}
