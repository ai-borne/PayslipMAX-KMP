package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CalendarDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * The narrative is the manual's own wording with the officer's answers dropped into its blanks, so every blank must be filled the way
 * HRMS prints it (DD/MM/YYYY, the number as typed) and a blank that cannot be filled must stay visibly blank instead of inventing a
 * value. The output is plain text: it is drawn and printed as text, never as markup.
 */
class OccurrenceRendererTest {
    private val answers: Map<String, SlotValue> =
        mapOf(
            "from" to SlotValue.DateValue(CalendarDate(2026, 7, 5)),
            "days" to SlotValue.NumberValue(62),
            "remark" to SlotValue.TextValue("  a few words  "),
            "grant" to SlotValue.RefValue(OrderRef("41/2026", CalendarDate(2026, 6, 30))),
        )

    private fun render(text: String) = OccurrenceRenderer.render(text, answers)

    @Test
    fun aDateIsPrintedDayMonthYearANumberAsTypedAndTextTrimmed() {
        assertEquals("from 05/07/2026 for 62 days: a few words", render("from {from} for {days} days: {remark}"))
    }

    @Test
    fun anOrderReferenceFillsItsNumberAndItsDateSeparately() {
        assertEquals("vide 41/2026 dated 30/06/2026", render("vide {grant.no} dated {grant.date}"))
    }

    @Test
    fun textWithNoPlaceholderIsUnchangedAndNeighbouringPlaceholdersBothFill() {
        assertEquals("Granted the thing", render("Granted the thing"))
        assertEquals("62/05/07/2026", render("{days}/{from}"))
    }

    @Test
    fun aBlankThatCannotBeFilledStaysVisiblyBlank() {
        // why: a missing published order number must print as a blank to fill in by hand, never as a guess (DECISIONS Round 10).
        val blank = OccurrenceRenderer.BLANK
        assertEquals("vide $blank dated $blank", OccurrenceRenderer.render("vide {grant.no} dated {grant.date}", emptyMap()))
        assertEquals("on $blank", OccurrenceRenderer.render("on {from}", emptyMap()))
        assertEquals("on $blank", render("on {nosuchslot}"), "an undeclared slot is blank, not an error")
    }

    @Test
    fun aValueOfTheWrongKindIsBlankNotCoerced() {
        val blank = OccurrenceRenderer.BLANK
        assertEquals("$blank", render("{grant}"), "a reference needs .no or .date")
        assertEquals("$blank", render("{from.no}"), "a date has no .no")
        assertEquals("$blank", render("{remark.date}"))
    }

    @Test
    fun theOutputIsPlainTextEvenIfMarkupGotThrough() {
        val hostile = mapOf("remark" to SlotValue.TextValue("<b>bold</b> & more"))
        val text = OccurrenceRenderer.render("Noted {remark} <i>now</i>", hostile)
        assertFalse('<' in text || '>' in text, text)
    }

    @Test
    fun anUnclosedBraceIsKeptLiterallyAndNeverThrows() {
        assertEquals("a {from", render("a {from"))
        assertEquals("a } b", render("a } b"))
    }

    @Test
    fun aReferenceForAColumnCellUsesTheSameFillingAsTheNarrative() {
        assertEquals("41/2026", OccurrenceRenderer.reference("grant.no", answers))
        assertEquals("05/07/2026", OccurrenceRenderer.reference("from", answers))
        assertEquals(OccurrenceRenderer.BLANK, OccurrenceRenderer.reference("grant.no", emptyMap()))
    }
}
