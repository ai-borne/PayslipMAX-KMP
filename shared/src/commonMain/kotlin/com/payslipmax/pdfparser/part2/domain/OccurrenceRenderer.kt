package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.calendar.CivilCalendar
import com.payslipmax.pdfparser.guide.data.Part2Placeholders

/**
 * Fills a template's blanks (`{slot}`, `{slot.no}`, `{slot.date}`) with the officer's answers. Dates print DD/MM/YYYY as HRMS does and
 * text as typed, trimmed. A blank that cannot be filled (nothing answered, the wrong kind of answer, an undeclared slot) prints as
 * [BLANK], never as a guess, so the officer sees what is still to be filled by hand. The output is plain text: angle brackets are dropped
 * even though the engine already refuses them. Plain scans, no regex, so it stays linear on Kotlin/Native.
 */
object OccurrenceRenderer {
    /** A blank to fill in by hand, for example a published order number not yet known. */
    const val BLANK = "____"

    fun render(
        text: String,
        answers: Map<String, SlotValue>,
    ): String {
        val out = StringBuilder()
        var at = 0
        while (at < text.length) {
            val open = text.indexOf('{', at)
            val close = if (open < 0) -1 else text.indexOf('}', open + 1)
            if (open < 0 || close < 0) {
                out.append(text, at, text.length)
                break
            }
            val inner = text.substring(open + 1, close)
            if (inner.isEmpty() || '{' in inner) {
                out.append(text, at, open + 1)
                at = open + 1
            } else {
                out.append(text, at, open).append(reference(inner, answers))
                at = close + 1
            }
        }
        return plain(out.toString())
    }

    /** The value a placeholder or a column names (`slot`, `slot.no`, `slot.date`) as printed text, or [BLANK]. */
    fun reference(
        ref: String,
        answers: Map<String, SlotValue>,
    ): String {
        val parsed = Part2Placeholders.parse(ref) ?: return BLANK
        val value = answers[parsed.slot]
        val printed =
            when {
                value is SlotValue.RefValue -> refPart(value.ref, parsed.part)
                parsed.part != null -> null
                value is SlotValue.DateValue -> CivilCalendar.format(value.date)
                value is SlotValue.NumberValue -> value.number.toString()
                value is SlotValue.TextValue -> value.text.trim()
                else -> null
            }
        return plain(printed.orEmpty()).ifEmpty { BLANK }
    }

    private fun refPart(
        ref: OrderRef,
        part: String?,
    ): String? =
        when (part) {
            Part2Placeholders.NO -> ref.orderNo.trim()
            Part2Placeholders.DATE -> CivilCalendar.format(ref.date)
            else -> null
        }

    private fun plain(text: String): String = text.filter { it != '<' && it != '>' }
}
