package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2Link
import com.payslipmax.pdfparser.guide.model.Part2Slot
import com.payslipmax.pdfparser.guide.model.Part2Template
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * WHY: a template's `rev` is how a printed draft tells that the manual's wording changed since it was handed over. The tool computes it
 * (first 8 hex of the SHA-256 of the shipped template as compact, key-sorted JSON); the app recomputes it on every load so a template
 * whose words changed without its rev changing is refused. The two must agree to the byte on every platform, so the expected value
 * below was produced by `part2_templates.rev()` on the same content (quotes, a backslash, accented and dash characters included).
 */
class Part2TemplateRevTest {
    private val sample =
        Part2Template(
            code = "SAMPLE",
            rev = "00000000",
            page = 12,
            narrative = "Granted \"x\" \\ backé – {a.no} dated {a.date}",
            slots =
                listOf(
                    Part2Slot("a", "ref", "L é", "H", ifMissing = "Nil"),
                    Part2Slot("d", "days", "D", "H", between = listOf("x", "y")),
                ),
            from = Part2Column("required", slot = "a"),
            to = Part2Column("none"),
            data = listOf(Part2Column("optional", text = "{a.no}"), Part2Column("none"), Part2Column("none"), Part2Column("none")),
            certificate = "CERTé",
            links = listOf(Part2Link("OTHER", "manual", page = 3, note = "2", condition = "r1"), Part2Link("P", "practice")),
        )

    @Test
    fun theRevisionIsTheOneThePythonToolComputesForTheSameContent() {
        assertEquals("40f38033", Part2TemplateRev.of(sample))
    }

    @Test
    fun theRevisionDoesNotDependOnTheRevisionItself() {
        assertEquals(Part2TemplateRev.of(sample), Part2TemplateRev.of(sample.copy(rev = "ffffffff")))
    }

    @Test
    fun anyChangeToAShippedWordChangesTheRevision() {
        val base = Part2TemplateRev.of(sample)
        assertNotEquals(base, Part2TemplateRev.of(sample.copy(narrative = sample.narrative + ".")))
        assertNotEquals(base, Part2TemplateRev.of(sample.copy(certificate = null)))
        assertNotEquals(base, Part2TemplateRev.of(sample.copy(page = 13)))
        assertNotEquals(base, Part2TemplateRev.of(sample.copy(slots = sample.slots.reversed())))
        assertNotEquals(base, Part2TemplateRev.of(sample.copy(links = sample.links.take(1))))
        assertNotEquals(base, Part2TemplateRev.of(sample.copy(rules = listOf("r1"))))
    }

    @Test
    fun controlCharactersAreEscapedAsThePythonToolEscapesThemAndOthersAreLeftAlone() {
        // why: json.dumps(ensure_ascii=False) writes \n \t \r \b \f and \u00xx for the rest below a space, and leaves DEL and U+2028 as they are.
        val controls =
            Part2Template(
                code = "CTL",
                rev = "00000000",
                page = 1,
                narrative = "a\nb\tc\rd\be\u000Cf\u0001g\u001fh\u007fi\u2028j",
                slots = emptyList(),
                from = Part2Column("none"),
                to = Part2Column("none"),
                data = listOf(Part2Column("none")),
            )
        assertEquals("4d97995f", Part2TemplateRev.of(controls))
    }
}
