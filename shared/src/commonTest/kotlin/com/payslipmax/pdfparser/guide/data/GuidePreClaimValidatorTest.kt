package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideOptionGroup
import com.payslipmax.pdfparser.guide.model.GuidePreClaim
import com.payslipmax.pdfparser.guide.model.GuidePreClaimField
import com.payslipmax.pdfparser.guide.model.GuidePreClaimMove
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import com.payslipmax.pdfparser.testing.SyntheticGuidePreClaim
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The pre-claim worksheet is data an officer files a real advance from, so a broken section must refuse to load rather than
 * draw a worksheet with a dead rule link, an empty document list or a label that overflows. These rechecks mirror
 * `preclaim.py`, which enforced the same limits when the bundle was built.
 */
class GuidePreClaimValidatorTest {
    private val valid: GuideBundle = SyntheticGuideBundle.withPreClaim()
    private val section: GuidePreClaim = SyntheticGuidePreClaim.preClaim

    private fun GuideBundle.with(edit: (GuidePreClaim) -> GuidePreClaim) = copy(preclaim = edit(section))

    private fun GuidePreClaim.editMove(
        id: String,
        edit: (GuidePreClaimMove) -> GuidePreClaimMove,
    ) = copy(moves = moves.mapValues { (key, move) -> if (key == id) edit(move) else move })

    private fun GuidePreClaim.editField(
        move: String,
        field: String,
        edit: (GuidePreClaimField) -> GuidePreClaimField,
    ) = editMove(move) { m -> m.copy(fields = m.fields.map { if (it.id == field) edit(it) else it }) }

    private fun GuidePreClaim.editGroup(
        id: String,
        edit: (GuideOptionGroup) -> GuideOptionGroup,
    ) = copy(options = options.mapValues { (key, group) -> if (key == id) edit(group) else group })

    private fun assertProblem(
        bundle: GuideBundle,
        vararg fragments: String,
    ) {
        val found = GuideBundleValidator.validate(bundle)
        assertTrue(found.any { line -> fragments.all { it in line } }, "expected a problem with $fragments, got $found")
    }

    @Test
    fun theSyntheticSectionIsValidAndABundleWithoutOneIsToo() {
        assertEquals(emptyList(), GuideBundleValidator.validate(valid))
        assertEquals(emptyList(), GuideBundleValidator.validate(valid.copy(preclaim = null)))
    }

    @Test
    fun aBundleCarryingTheSectionLoadsAndKeepsFieldOrder() {
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(GuideJson.encodeToString(GuideBundle.serializer(), valid)))
        assertEquals(listOf("account", "sanction_id"), loaded.bundle.preclaim?.moves?.getValue("PD")?.fields?.map { it.id })
    }

    @Test
    fun aBrokenSectionMakesTheBundleInvalidRatherThanDrawingABadWorksheet() {
        val broken = valid.with { it.editMove("PD") { m -> m.copy(documents = emptyList()) } }
        val result = GuideBundleParser.parse(GuideJson.encodeToString(GuideBundle.serializer(), broken))
        assertEquals(GuideLoadResult.Failed(GuideLoadError.INVALID), result)
    }

    @Test
    fun aMoveTheAppDoesNotKnowIsRefused() {
        assertProblem(valid.with { it.copy(moves = it.moves + ("XX" to it.moves.getValue("PD"))) }, "unknown move", "XX")
    }

    @Test
    fun aMoveNeedsFieldsDocumentsAndATitle() {
        assertProblem(valid.with { it.editMove("TD") { m -> m.copy(documents = emptyList()) } }, "TD", "documents")
        assertProblem(valid.with { it.editMove("TD") { m -> m.copy(fields = emptyList()) } }, "TD", "fields")
        assertProblem(valid.with { it.editMove("TD") { m -> m.copy(title = " ") } }, "TD", "title")
    }

    @Test
    fun aFieldNeedsALabelWithinTheLengthLimitAndNoMarkup() {
        assertProblem(valid.with { it.editField("PD", "account") { f -> f.copy(label = "") } }, "account", "label")
        assertProblem(valid.with { it.editField("PD", "account") { f -> f.copy(label = "x".repeat(61)) } }, "account", "label")
        assertProblem(valid.with { it.editField("PD", "account") { f -> f.copy(label = "<b>A</b>") } }, "account", "angle brackets")
        assertProblem(valid.with { it.editField("LTC", "fare") { f -> f.copy(hint = "x".repeat(161)) } }, "fare", "hint")
    }

    @Test
    fun anUnknownKindOrSourceIsRefused() {
        assertProblem(valid.with { it.editField("PD", "account") { f -> f.copy(kind = "slider") } }, "account", "kind")
        assertProblem(valid.with { it.editField("PD", "account") { f -> f.copy(source = "magic") } }, "account", "source")
    }

    @Test
    fun aTextLimitIsOnlyForTextFieldsAndWithinBounds() {
        assertProblem(valid.with { it.editField("PD", "sanction_id") { f -> f.copy(maxChars = 201) } }, "sanction_id", "max_chars")
        assertProblem(valid.with { it.editField("LTC", "fare") { f -> f.copy(maxChars = 10) } }, "fare", "max_chars")
    }

    @Test
    fun anUnverifiedFieldMustSayWhatIsUnconfirmed() {
        assertProblem(valid.with { it.editField("TD", "tick") { f -> f.copy(hint = "") } }, "tick", "hint")
    }

    @Test
    fun aChoiceNamesAnExistingGroupAndOtherKindsNameNone() {
        assertProblem(valid.with { it.editField("TD", "rule") { f -> f.copy(options = "nope") } }, "rule", "nope")
        assertProblem(valid.with { it.editField("LTC", "fare") { f -> f.copy(options = "mode") } }, "fare", "options")
    }

    @Test
    fun aPickerOptionNeedsALiveCardAndAPlainGroupNamesNone() {
        fun card(value: String) = valid.with { it.editGroup("rule") { g -> g.copy(options = g.options.map { o -> o.copy(card = value) }) } }
        assertProblem(card(""), "rule", "needs a card")
        assertProblem(card("RB-NOPE"), "rule", "RB-NOPE")
        assertProblem(valid.with { it.editGroup("mode") { g -> g.copy(options = g.options.map { o -> o.copy(card = "RB-T9") }) } }, "mode", "card")
        val replaced = valid.copy(cards = valid.cards.map { if (it.id == "RB-T9") it.copy(replacedBy = "RB-T10", until = "2026-01-01") else it })
        assertProblem(replaced, "rule", "replaced")
    }

    @Test
    fun aPickerOptionsShortLineIsOptionalAtMostThreeWordsAndPlainText() {
        fun short(value: String) = valid.with { it.editGroup("mode") { g -> g.copy(options = g.options.map { o -> o.copy(short = value) }) } }
        assertEquals(emptyList(), GuideBundleValidator.validate(short("")))
        assertEquals(emptyList(), GuideBundleValidator.validate(short("Course or exam")))
        assertProblem(short("A course or an exam"), "mode", "short", "words")
        assertProblem(short("<b>Course</b>"), "mode", "short", "angle brackets")
    }

    @Test
    fun duplicateIdsAreRefused() {
        assertProblem(valid.with { it.editMove("PD") { m -> m.copy(fields = m.fields + m.fields.first()) } }, "PD", "duplicate", "account")
        assertProblem(valid.with { it.editGroup("mode") { g -> g.copy(options = g.options + g.options.first()) } }, "mode", "duplicate")
    }

    @Test
    fun theUploadRuleMustDescribeOnePdfWithPositiveBounds() {
        assertProblem(valid.with { it.copy(upload = it.upload.copy(files = 2)) }, "upload", "files")
        assertProblem(valid.with { it.copy(upload = it.upload.copy(moreThanKb = 0)) }, "upload", "more_than_kb")
        assertProblem(valid.with { it.copy(upload = it.upload.copy(lessThanMb = -1)) }, "upload", "less_than_mb")
        assertProblem(valid.with { it.copy(upload = it.upload.copy(format = "DOCX")) }, "upload", "format")
    }

    @Test
    fun theLimitsComeFromTheSectionSoOneEditMovesBothLanguages() {
        val tight = valid.with { it.copy(limits = it.limits.copy(label = 5)) }
        assertProblem(tight, "label", "over 5")
        assertProblem(valid.with { it.copy(limits = it.limits.copy(hint = 0)) }, "limits")
    }

    @Test
    fun problemsNameIdsOnlyNeverTheWorksheetText() {
        val secret = "My own sentence"
        val found = GuideBundleValidator.validate(valid.with { it.editField("PD", "account") { f -> f.copy(label = "<$secret") } })
        assertTrue(found.isNotEmpty() && found.none { secret in it }, "a problem line must not echo the text: $found")
    }
}
