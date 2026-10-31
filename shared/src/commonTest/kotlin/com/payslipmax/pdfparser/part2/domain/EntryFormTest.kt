package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the form asks for exactly what the manual's Annexure III says this casualty needs (From always, To always offered, each Data
 * column only if used, required ones before Add) and invents no rule of its own. A date that is wrong, backwards or missing, a value
 * that is too long and a code the bundle does not know are refused here, before anything reaches storage.
 */
class EntryFormTest {
    private val index = Part2LookupIndex(SyntheticGuidePart2.lookup)
    private val form = assertNotNull(EntryForm.of(index, "FIRSTCODE"))

    private fun input(
        from: com.payslipmax.pdfparser.guide.preclaim.CalendarDate? = date(2026, 3, 1),
        to: com.payslipmax.pdfparser.guide.preclaim.CalendarDate? = null,
        data: List<String> = listOf("", "Ravi", "", ""),
        note: String = "",
    ) = EntryInput(from, to, data, note)

    @Test
    fun theFieldsAreFromAndOnlyTheColumnsTheOdpPrintsForTheCasualtyWithTheManualsCaptions() {
        assertEquals(
            listOf(
                EntryField(0, required = true, caption = "dd/mm/yyyy"),
                EntryField(2, required = false, caption = "prefix"),
                EntryField(3, required = true, caption = "name"),
            ),
            form.fields,
        )
        assertEquals(28, form.manualPage)
    }

    @Test
    fun aRequiredColumnWithNoPrintedCaptionStaysRequiredWithAnEmptyCaptionForTheScreenToPointToThePage() {
        val bare = Part2LookupIndex(SyntheticGuidePart2.lookup.copy(columns = SyntheticGuidePart2.bareColumns()))
        assertEquals(listOf(EntryField(0, true, "")), assertNotNull(EntryForm.of(bare, "FIRSTCODE")).fields)
    }

    private fun withTo(
        need: String,
        from: String = "required",
    ): EntryForm {
        fun column(n: String) = com.payslipmax.pdfparser.guide.model.Part2Column(n, if (n == "none") "" else "dd/mm/yyyy")
        val rows =
            SyntheticGuidePart2.columns.map {
                com.payslipmax.pdfparser.guide.model.Part2ColumnRow(it.serial, column(from), column(need), it.data)
            }
        return assertNotNull(EntryForm.of(Part2LookupIndex(SyntheticGuidePart2.lookup.copy(columns = rows)), "FIRSTCODE"))
    }

    @Test
    fun toIsAskedOnlyWhereTheOdpPrintsItAndIsRequiredOnlyWhereTheOdpSaysSo() {
        assertTrue(form.fields.none { it.position == 1 }, "this casualty prints no To column")
        assertEquals(EntryField(1, required = false, caption = "dd/mm/yyyy"), withTo("optional").fields[1])
        val required = withTo("required")
        assertEquals(mapOf(1 to EntryProblem.MISSING), required.check(input(to = null)))
        assertEquals(emptyMap(), required.check(input(to = date(2026, 3, 9))))
    }

    @Test
    fun aToTypedForACasualtyThatPrintsNoToColumnIsIgnoredAndNotStored() {
        assertEquals(emptyMap(), form.check(input(to = date(2026, 2, 30))), "an unused To is not judged")
        assertNull(assertNotNull(form.toEntry(input(to = date(2026, 3, 9)), "e1", null, 0, 0)).to)
    }

    @Test
    fun whereTheOdpPrintsNoFromColumnTheFormStillAsksAPlainRequiredDate() {
        val noFrom = withTo("none", from = "none")
        assertEquals(false, noFrom.fromPrinted)
        assertEquals(true, form.fromPrinted)
        assertEquals(EntryField(0, required = true, caption = ""), noFrom.fields.first())
        assertEquals(mapOf(0 to EntryProblem.MISSING), noFrom.check(input(from = null)))
    }

    @Test
    fun aCodeTheBundleDoesNotKnowHasNoForm() {
        assertNull(EntryForm.of(index, "NOSUCHCODE"))
    }

    @Test
    fun aCompleteInputPassesAndBecomesAnEntry() {
        val ok = input()
        assertEquals(emptyMap(), form.check(ok))
        val made = assertNotNull(form.toEntry(ok, "e1", null, 5L, 6L))
        assertEquals(Triple("e1", "FIRSTCODE", date(2026, 3, 1)), Triple(made.id, made.code, made.from))
        assertNull(made.to)
        assertEquals(listOf("", "Ravi", "", ""), made.data)
    }

    @Test
    fun aMissingFromAndAMissingRequiredColumnAreRefusedByPosition() {
        assertEquals(mapOf(0 to EntryProblem.MISSING), form.check(input(from = null)))
        assertEquals(mapOf(3 to EntryProblem.MISSING), form.check(input(data = listOf("", "   ", "", ""))))
        assertNull(form.toEntry(input(from = null), "e1", null, 0, 0))
    }

    @Test
    fun anOptionalColumnMayStayEmpty() {
        assertEquals(emptyMap(), form.check(input(data = listOf("", "x", "", ""))))
    }

    @Test
    fun aDateThatDoesNotExistIsRefusedAndFromNotAfterToIsKept() {
        assertEquals(mapOf(0 to EntryProblem.INVALID_DATE), form.check(input(from = date(2026, 2, 30))))
        assertEquals(mapOf(1 to EntryProblem.INVALID_DATE), withTo("optional").check(input(to = date(2026, 13, 1))))
        assertEquals(mapOf(0 to EntryProblem.AFTER_TO), withTo("optional").check(input(from = date(2026, 3, 10), to = date(2026, 3, 9))))
        assertEquals(emptyMap(), withTo("optional").check(input(from = date(2026, 3, 9), to = date(2026, 3, 9))), "the same day is not backwards")
    }

    @Test
    fun capsHold() {
        val long = "x".repeat(Part2Limits.TEXT_MAX + 1)
        assertEquals(mapOf(3 to EntryProblem.TOO_LONG), form.check(input(data = listOf("", long, "", ""))))
        assertEquals(mapOf(EntryPosition.NOTE to EntryProblem.TOO_LONG), form.check(input(note = long)))
        assertEquals(emptyMap(), form.check(input(note = "x".repeat(Part2Limits.TEXT_MAX))))
    }

    @Test
    fun aValueTypedInAColumnTheCasualtyDoesNotUseIsNotKept() {
        val made = assertNotNull(form.toEntry(input(data = listOf("", "Ravi", "stray", "stray")), "e1", null, 0, 0))
        assertEquals(listOf("", "Ravi", "", ""), made.data)
        assertTrue(form.fields.none { it.position == 4 })
    }
}
