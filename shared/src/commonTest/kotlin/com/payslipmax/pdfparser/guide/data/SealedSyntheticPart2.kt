package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2Template
import com.payslipmax.pdfparser.testing.SyntheticPart2Templates

/**
 * The invented templates with each revision recomputed (`Part2TemplateRev`), for the validator suites: the shared fixtures keep readable
 * revisions for the tests that print them, but the validator now refuses a revision that is not its template's own.
 */
internal object SealedSyntheticPart2 {
    fun Part2Template.sealed(): Part2Template = copy(rev = Part2TemplateRev.of(this))

    val lookup: Part2Lookup = SyntheticPart2Templates.lookup.let { lookup -> lookup.copy(templates = lookup.templates.map { it.sealed() }) }

    fun bundle(): GuideBundle = SyntheticPart2Templates.bundle().copy(part2 = lookup)
}
