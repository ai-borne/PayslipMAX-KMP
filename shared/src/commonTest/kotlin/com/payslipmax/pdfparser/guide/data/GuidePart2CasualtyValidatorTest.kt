package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2AboutPage
import com.payslipmax.pdfparser.guide.model.Part2Column
import com.payslipmax.pdfparser.guide.model.Part2ColumnRow
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.guide.model.Part2RelatedLink
import com.payslipmax.pdfparser.guide.model.Part2RelatedRow
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The Annexure III columns, the 30 plain-English pages and the "usually published with" links are what the Bank's entry form and the
 * casualty page are built from. A damaged section must refuse to load rather than ask the officer for a field the order never carries,
 * hide one it does, or show markup. These rechecks mirror `part2_columns_check.py`, `part2_about_check.py` and `part2_related_check.py`.
 */
class GuidePart2CasualtyValidatorTest {
    private val valid: GuideBundle = SyntheticGuideBundle.withPart2()
    private val section: Part2Lookup = SyntheticGuidePart2.lookup

    private fun GuideBundle.with(edit: (Part2Lookup) -> Part2Lookup) = copy(part2 = edit(section))

    private fun Part2Lookup.editRow(
        serial: String,
        edit: (Part2ColumnRow) -> Part2ColumnRow,
    ) = copy(columns = columns.map { if (it.serial == serial) edit(it) else it })

    private fun Part2Lookup.editPage(
        id: String,
        edit: (Part2AboutPage) -> Part2AboutPage,
    ) = copy(about = about.map { if (it.id == id) edit(it) else it })

    private fun problems(bundle: GuideBundle) = GuideBundleValidator.validate(bundle)

    private fun assertProblem(
        bundle: GuideBundle,
        vararg fragments: String,
    ) {
        val found = problems(bundle)
        assertTrue(found.any { line -> fragments.all { it in line } }, "expected a problem with ${fragments.toList()}, got $found")
    }

    @Test
    fun theSyntheticSectionIsValid() {
        assertEquals(emptyList(), problems(valid))
    }

    @Test
    fun aSectionWithNoCasualtyKeysStillLoadsAndIsValid() {
        // why: the casualty data is additive (the section stays version 1), so a bundle built before it must keep opening the Guide.
        val old = valid.with { it.copy(columns = emptyList(), about = emptyList(), related = emptyList()) }
        assertEquals(emptyList(), problems(old))
        val json = GuideJson.encodeToString(GuideBundle.serializer(), old)
        assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(json))
    }

    @Test
    fun theCasualtyDataSurvivesAJsonRoundTripWithTheWhenKeyIntact() {
        val json = GuideJson.encodeToString(GuideBundle.serializer(), valid)
        assertTrue("\"when\":" in json && "whenSeen" !in json, "the shipped key is when, never the Kotlin property name")
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(json))
        assertEquals(section.columns, loaded.bundle.part2?.columns)
        assertEquals(section.about, loaded.bundle.part2?.about)
        assertEquals(section.related, loaded.bundle.part2?.related)
    }

    @Test
    fun aBrokenColumnMakesTheWholeBundleInvalid() {
        val broken = valid.with { it.editRow("1.1") { r -> r.copy(from = Part2Column("maybe", "x")) } }
        assertEquals(GuideLoadResult.Failed(GuideLoadError.INVALID), GuideBundleParser.parse(GuideJson.encodeToString(GuideBundle.serializer(), broken)))
    }

    @Test
    fun everySerialHasExactlyOneRowOnceColumnsArePresent() {
        assertProblem(valid.with { it.copy(columns = it.columns.drop(1)) }, "columns", "serial 1.1", "no columns")
        assertProblem(valid.with { it.copy(columns = it.columns + it.columns.first()) }, "columns 1.1", "duplicate")
        assertProblem(valid.with { it.editRow("1.1") { r -> r.copy(serial = "99.9") } }, "columns", "99.9", "unknown serial")
    }

    @Test
    fun aNeedMustBeRequiredOptionalOrNone() {
        assertProblem(valid.with { it.editRow("1.1") { r -> r.copy(from = Part2Column("maybe", "x")) } }, "columns 1.1", "from", "need")
        assertProblem(valid.with { it.editRow("1.1") { r -> r.copy(to = Part2Column("", "")) } }, "columns 1.1", "to", "need")
    }

    @Test
    fun aNoneColumnCarriesNoCaption() {
        assertProblem(valid.with { it.editRow("1.1") { r -> r.copy(to = Part2Column("none", "Not required")) } }, "columns 1.1", "none", "caption")
    }

    @Test
    fun exactlyFourDataColumnsAreRequired() {
        assertProblem(valid.with { it.editRow("1.1") { r -> r.copy(data = r.data.drop(1)) } }, "columns 1.1", "data", "four")
    }

    @Test
    fun captionsArePlainAndBoundedByTheLimitTheSectionCarries() {
        assertProblem(valid.with { it.editRow("1.1") { r -> r.copy(from = Part2Column("required", "<b>")) } }, "columns 1.1", "angle brackets")
        assertProblem(valid.with { it.editRow("1.1") { r -> r.copy(from = Part2Column("required", "x".repeat(41))) } }, "columns 1.1", "over 40")
        assertProblem(valid.with { it.copy(limits = it.limits.copy(caption = 0)) }, "limits", "caption")
    }

    @Test
    fun anAboutPageIsIdentifiedByItsFirstSerialAndNamesRealSerials() {
        assertProblem(valid.with { it.editPage("1.1") { p -> p.copy(id = "1.2") } }, "about 1.2", "id")
        assertProblem(valid.with { it.editPage("1.1") { p -> p.copy(serials = listOf("1.1", "99.9")) } }, "about 1.1", "unknown serial 99.9")
        assertProblem(valid.with { it.editPage("1.1") { p -> p.copy(serials = listOf("1.1", "3.1")) } }, "about", "serial 3.1", "twice")
    }

    @Test
    fun theFourPartsAreNotEmptyPlainAndBounded() {
        assertProblem(valid.with { it.editPage("1.1") { p -> p.copy(what = " ") } }, "about 1.1", "what is empty")
        assertProblem(valid.with { it.editPage("1.1") { p -> p.copy(whenSeen = "a <i> b") } }, "about 1.1", "when", "angle brackets")
        assertProblem(valid.with { it.editPage("1.1") { p -> p.copy(ready = "x".repeat(61)) } }, "about 1.1", "ready", "over 60")
        assertProblem(valid.with { it.editPage("1.1") { p -> p.copy(watch = ">") } }, "about 1.1", "watch", "angle brackets")
    }

    @Test
    fun aRelatedLinkNamesRealDifferentCodesWithTheRightEvidence() {
        fun related(vararg links: Part2RelatedLink) = valid.with { it.copy(related = listOf(Part2RelatedRow("FIRSTCODE", links.toList()))) }
        assertProblem(valid.with { it.copy(related = listOf(Part2RelatedRow("NOPE", emptyList()))) }, "related NOPE", "unknown code")
        assertProblem(related(Part2RelatedLink("NOPE", "practice")), "related FIRSTCODE", "NOPE", "unknown code")
        assertProblem(related(Part2RelatedLink("FIRSTCODE", "practice")), "related FIRSTCODE", "itself")
        assertProblem(related(Part2RelatedLink("SECONDCODE", "practice"), Part2RelatedLink("SECONDCODE", "practice")), "SECONDCODE", "repeats")
        assertProblem(related(Part2RelatedLink("SECONDCODE", "manual")), "SECONDCODE", "manual", "page")
        assertProblem(related(Part2RelatedLink("SECONDCODE", "manual", 9999)), "SECONDCODE", "manual", "page")
        assertProblem(related(Part2RelatedLink("SECONDCODE", "practice", 5)), "SECONDCODE", "practice", "page")
        assertProblem(related(Part2RelatedLink("SECONDCODE", "rumour")), "SECONDCODE", "basis")
        assertProblem(valid.with { it.copy(related = it.related + it.related.first()) }, "related FIRSTCODE", "twice")
        assertProblem(valid.with { it.copy(related = listOf(Part2RelatedRow("FIRSTCODE", emptyList()))) }, "related FIRSTCODE", "no links")
    }

    @Test
    fun aProblemNeverRepeatsWhatTheOfficerWouldRead() {
        // why: problems are for tests and must never travel into a log or crash key (TelemetrySanitizer rule).
        val secret = "SecretCaption"
        val bundle =
            valid.with {
                it.editRow("1.1") { r -> r.copy(from = Part2Column("required", "$secret <")) }
                    .editPage("1.1") { p -> p.copy(what = "$secret >") }
            }
        val found = problems(bundle)
        assertTrue(found.isNotEmpty())
        assertFalse(found.any { secret in it }, "problems name ids and fields only: $found")
    }
}
