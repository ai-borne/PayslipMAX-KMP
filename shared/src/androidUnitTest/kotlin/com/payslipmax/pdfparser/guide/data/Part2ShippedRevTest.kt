package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.part2.domain.RealPart2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** WHY: the Kotlin recompute and the Python tool must agree on every template the app really ships, not only on one sample. */
class Part2ShippedRevTest {
    @Test
    fun everyShippedTemplatesRevisionIsWhatTheAppRecomputes() {
        val templates = RealPart2.section.templates
        assertTrue(templates.size >= 24, "the shipped batch is there: ${templates.size}")
        assertEquals(templates.map { it.rev }, templates.map { Part2TemplateRev.of(it) })
    }
}
