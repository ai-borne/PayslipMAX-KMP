package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.Part2Case
import com.payslipmax.pdfparser.guide.model.Part2CodePage
import com.payslipmax.pdfparser.guide.model.Part2Lookup
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The Part II lookup is what an officer reads a code's name and group from, so a damaged section must refuse to load rather than
 * show a wrong group (the group decides which orders may share a slip) or a code that two pages claim. These rechecks mirror
 * `part2_codes_check.py` and `part2_nav.py`, which enforced the same rules when the bundle was built.
 */
class GuidePart2LookupValidatorTest {
    private val valid: GuideBundle = SyntheticGuideBundle.withPart2()
    private val section: Part2Lookup = SyntheticGuidePart2.lookup

    private fun GuideBundle.with(edit: (Part2Lookup) -> Part2Lookup) = copy(part2 = edit(section))

    private fun Part2Lookup.editSerial(
        serial: String,
        edit: (Part2CodePage) -> Part2CodePage,
    ) = copy(serials = serials.map { if (it.serial == serial) edit(it) else it })

    private fun Part2Lookup.editCase(
        id: String,
        edit: (Part2Case) -> Part2Case,
    ) = copy(ribbons = ribbons.map { r -> r.copy(cases = r.cases.map { if (it.id == id) edit(it) else it }) })

    private fun problems(bundle: GuideBundle) = GuideBundleValidator.validate(bundle)

    private fun assertProblem(
        bundle: GuideBundle,
        vararg fragments: String,
    ) {
        val found = problems(bundle)
        assertTrue(found.any { line -> fragments.all { it in line } }, "expected a problem with ${fragments.toList()}, got $found")
    }

    @Test
    fun theSyntheticSectionIsValidAndABundleWithoutOneIsToo() {
        assertEquals(emptyList(), problems(valid))
        assertEquals(emptyList(), problems(valid.copy(part2 = null)))
    }

    @Test
    fun anOldBundleWithNoPart2KeyStillLoads() {
        // why: the section is additive (BUNDLE_VERSION stays 1), so a bundle built before Part II must keep opening the Guide.
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON))
        assertNull(loaded.bundle.part2)
    }

    @Test
    fun aBundleCarryingTheSectionLoadsAndKeepsOrder() {
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(GuideJson.encodeToString(GuideBundle.serializer(), valid)))
        val part2 = loaded.bundle.part2
        assertEquals(listOf("leave", "records"), part2?.ribbons?.map { it.id })
        assertEquals(listOf("1.1", "1.2", "2.1", "3.1", "3.27", "3.28"), part2?.serials?.map { it.serial })
        assertNull(part2?.serials?.first { it.serial == "3.28" }?.group, "an absent group stays absent, never defaults to one")
    }

    @Test
    fun aBrokenSectionMakesTheWholeBundleInvalidRatherThanShowingWrongData() {
        val broken = valid.with { it.editSerial("1.1") { s -> s.copy(group = "Z") } }
        val result = GuideBundleParser.parse(GuideJson.encodeToString(GuideBundle.serializer(), broken))
        assertEquals(GuideLoadResult.Failed(GuideLoadError.INVALID), result)
    }

    @Test
    fun anUnknownSectionVersionIsRefused() {
        assertProblem(valid.with { it.copy(version = 2) }, "part2", "version")
    }

    @Test
    fun anUnknownGroupLetterIsRefused() {
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(group = "Z") } }, "serial 1.1", "group")
    }

    @Test
    fun theGroupTableMustBeExactlyTheFiveLettersWithReadableMeanings() {
        assertProblem(valid.with { it.copy(groups = it.groups - "CM") }, "groups must be exactly")
        assertProblem(valid.with { it.copy(groups = it.groups + ("D" to "Never defined")) }, "groups must be exactly")
        assertProblem(valid.with { it.copy(groups = it.groups + ("A" to " ")) }, "group A", "meaning is empty")
    }

    @Test
    fun aDuplicateCodeOrSerialIsRefused() {
        assertProblem(valid.with { it.editSerial("1.2") { s -> s.copy(codes = listOf("FIRSTCODE")) } }, "duplicate code FIRSTCODE")
        assertProblem(valid.with { it.editSerial("1.2") { s -> s.copy(codes = listOf("SECONDCODE", "SECONDCODE")) } }, "duplicate code SECONDCODE")
        assertProblem(valid.with { it.copy(serials = it.serials + it.serials.first()) }, "serial 1.1", "duplicate serial")
    }

    @Test
    fun anEmptyOrOverlongOrBracketedNameIsRefused() {
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(name = "  ") } }, "serial 1.1", "name is empty")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(name = "x".repeat(301)) } }, "serial 1.1", "name is over 300")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(name = "Granted <b>") } }, "serial 1.1", "angle brackets")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(name = "Granted >") } }, "serial 1.1", "angle brackets")
    }

    @Test
    fun aNullGroupIsAllowedForTheTwoSerialsTheManualLeavesBlankAndNowhereElse() {
        // why: "Group not printed in ODP-2026" (Round 8). The group decides the para 7.1 order split, so none is ever inferred.
        assertEquals(setOf("3.28", "11.8"), GuidePart2LookupValidator.NO_GROUP_SERIALS)
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(group = null) } }, "serial 1.1", "group")
        assertProblem(valid.with { it.editSerial("3.28") { s -> s.copy(group = "A") } }, "serial 3.28", "no group")
    }

    @Test
    fun malformedSerialsCodesChaptersAndPagesAreRefused() {
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(serial = "1.x") } }, "chapter.number")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(chapter = 2) } }, "serial 1.1", "chapter")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(codes = emptyList()) } }, "serial 1.1", "no codes")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(codes = listOf("lower")) } }, "serial 1.1", "capital letters or digits")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(codes = listOf("A")) } }, "serial 1.1", "capital letters or digits")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(codes = listOf("A/B")) } }, "serial 1.1", "capital letters or digits")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(page = 0) } }, "serial 1.1", "page")
        assertProblem(valid.with { it.editSerial("1.1") { s -> s.copy(page = 401) } }, "serial 1.1", "page")
    }

    @Test
    fun theLimitsMustBePositiveBecauseTheValidatorReadsThemFromTheSection() {
        assertProblem(valid.with { it.copy(limits = it.limits.copy(text = 0)) }, "limits must be above zero")
    }

    @Test
    fun ribbonAndCaseCountsAreHeldToTheLimitsTheSectionCarries() {
        assertProblem(valid.with { it.copy(limits = it.limits.copy(ribbonsMin = 3, ribbonsMax = 3)) }, "2 ribbons", "allowed 3 to 3")
        assertProblem(valid.with { it.copy(limits = it.limits.copy(casesPerRibbon = 1)) }, "ribbon leave", "2 cases", "max 1")
        assertProblem(valid.with { it.copy(limits = it.limits.copy(caseSerialsMin = 3)) }, "case leave_one", "2 serials", "allowed 3 to 4")
        assertProblem(valid.with { it.copy(limits = it.limits.copy(caseSerialsMax = 1, caseSerialsMin = 1)) }, "case leave_one", "allowed 1 to 1")
    }

    @Test
    fun titlesAreBoundedPlainAndNotEmpty() {
        val long = "one two three four five six seven eight"
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(title = long) } }, "case leave_one", "title", "over 7 words")
        assertProblem(valid.with { it.copy(ribbons = it.ribbons.map { r -> r.copy(title = long) }) }, "ribbon leave", "title", "over 7 words")
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(title = "A <i> case") } }, "case leave_one", "angle brackets")
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(title = " ") } }, "case leave_one", "title is empty")
    }

    @Test
    fun idsMustBeWellFormedAndUnique() {
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(id = "Bad Id") } }, "id is malformed")
        assertProblem(valid.with { it.editCase("records_one") { c -> c.copy(id = "leave_one") } }, "duplicate part2 case id leave_one")
        assertProblem(valid.with { it.copy(ribbons = it.ribbons.map { r -> r.copy(id = "leave") }) }, "duplicate part2 ribbon id leave")
    }

    @Test
    fun everySerialIsHomedExactlyOnceAndOnlyRealSerialsAreListed() {
        assertProblem(valid.with { it.editCase("leave_two") { c -> c.copy(serials = listOf("2.1", "1.1")) } }, "serial 1.1", "homed twice")
        assertProblem(valid.with { it.editCase("leave_two") { c -> c.copy(serials = listOf("2.1", "99.9")) } }, "unknown serial 99.9")
        assertProblem(valid.with { it.editCase("leave_two") { c -> c.copy(serials = listOf("2.1", "3.27")) } }, "serial 3.1", "no home")
    }

    @Test
    fun anAlsoLinkMustPointAtARealCodeHomedElsewhere() {
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(also = listOf("99.9")) } }, "also 99.9", "unknown serial")
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(also = listOf("1.1")) } }, "also 1.1", "homed in this case")
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(also = listOf("3.1", "3.1")) } }, "also 3.1", "repeats")
    }

    @Test
    fun aLookupMarkMustNameASerialOfThatCase() {
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(lookup = listOf("3.27")) } }, "lookup 3.27", "not in the case")
        assertProblem(valid.with { it.editCase("leave_one") { c -> c.copy(lookup = listOf("1.1", "1.1")) } }, "lookup 1.1", "repeats")
    }

    @Test
    fun aProblemNeverRepeatsWhatTheOfficerWouldRead() {
        // why: the section is shown to every user and problems are for tests; a name or title must never travel into a log or crash key.
        val secret = "SecretNarrative"
        val bundle =
            valid.with { it.editSerial("1.1") { s -> s.copy(name = "$secret <") }.editCase("leave_one") { c -> c.copy(title = "$secret >") } }
        val found = problems(bundle)
        assertTrue(found.isNotEmpty())
        assertFalse(found.any { secret in it }, "problems name ids and fields only: $found")
    }
}
