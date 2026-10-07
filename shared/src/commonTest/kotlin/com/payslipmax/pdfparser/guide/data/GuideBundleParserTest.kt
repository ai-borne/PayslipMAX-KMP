package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import com.payslipmax.pdfparser.guide.model.GuideChip
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * The parser is the gate between a file in the app package and what a user reads. A newer bundle must
 * still load when it only adds fields, a breaking one must give an error state rather than a crash or a
 * half-read card, and internal reviewer data must never be accepted even if a bad build ships it.
 */
class GuideBundleParserTest {
    private fun loaded(text: String = SyntheticGuideBundle.JSON): GuideBundle =
        assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle

    private fun failure(text: String): GuideLoadError = assertIs<GuideLoadResult.Failed>(GuideBundleParser.parse(text)).error

    @Test
    fun syntheticBundleLoadsWithEveryField() {
        val bundle = loaded()

        assertEquals(SyntheticGuideBundle.RATES_AS_OF, bundle.ratesAsOf)
        assertEquals(13, bundle.cards.size)
        assertEquals(listOf("travel", "pay"), bundle.nav.map { it.id })
        assertEquals("Who qualifies", bundle.facets["Q"])
        val personal = bundle.cards.first { it.id == SyntheticGuideBundle.PERSONAL_CARD }
        assertEquals("level:food_rate", personal.personal)
        assertEquals(listOf(GuideChip.RATES), personal.trustChips)
        assertEquals(12, bundle.limits.bulletWords)
    }

    @Test
    fun roundTripKeepsTheBundleIntact() {
        val bundle = loaded()

        assertEquals(bundle, loaded(GuideJson.encodeToString(GuideBundle.serializer(), bundle)))
    }

    @Test
    fun anUnknownFieldIsIgnoredSoAnAdditiveBundleStillLoads() {
        val text =
            SyntheticGuideBundle.JSON.replace("\"version\":1,", "\"version\":1,\"search_aliases\":{\"a\":\"b\"},")
                .replace("\"id\":\"RB-T5\",", "\"id\":\"RB-T5\",\"audio\":\"x.mp3\",")

        assertEquals(loaded().cards, loaded(text).cards)
    }

    @Test
    fun anUnknownChipMapsToUnknownInsteadOfFailingTheLoad() {
        val text = SyntheticGuideBundle.JSON.replace("\"chips\":[\"AMENDED\"]", "\"chips\":[\"AMENDED\",\"SEASONAL\"]")

        val card = loaded(text).cards.first { it.id == SyntheticGuideBundle.AMENDED_CARD }
        assertEquals(listOf(GuideChip.AMENDED, GuideChip.UNKNOWN), card.trustChips)
    }

    @Test
    fun aNewerMajorVersionIsAnErrorStateNotACrash() {
        assertEquals(
            GuideLoadError.UNSUPPORTED_VERSION,
            failure(SyntheticGuideBundle.JSON.replace("\"version\":1,", "\"version\":${GuideBundleParser.SUPPORTED_MAJOR + 1},")),
        )
        assertEquals(GuideLoadError.UNSUPPORTED_VERSION, failure(SyntheticGuideBundle.JSON.replace("\"version\":1,", "\"version\":0,")))
    }

    @Test
    fun malformedInputIsRejected() {
        assertEquals(GuideLoadError.MALFORMED, failure(""))
        assertEquals(GuideLoadError.MALFORMED, failure("[1,2]"))
        assertEquals(GuideLoadError.MALFORMED, failure(SyntheticGuideBundle.JSON.dropLast(5)))
        assertEquals(GuideLoadError.MALFORMED, failure(SyntheticGuideBundle.JSON.replace("\"version\":1,", "")))
        assertEquals(GuideLoadError.MALFORMED, failure(SyntheticGuideBundle.JSON.replace("\"version\":1,", "\"version\":\"one\",")))
        // A required field missing from a card.
        assertEquals(GuideLoadError.MALFORMED, failure(SyntheticGuideBundle.JSON.replace("\"facet\":\"L\",\"nav\":\"pay-hra\"", "\"nav\":\"pay-hra\"")))
    }

    @Test
    fun oversizedInputIsRejectedBeforeParsing() {
        val padded = SyntheticGuideBundle.JSON + " ".repeat(GuideBundleParser.MAX_CHARS)

        assertEquals(GuideLoadError.TOO_LARGE, failure(padded))
    }

    @Test
    fun internalReviewerFieldsAreNeverAccepted() {
        assertEquals(
            GuideLoadError.INVALID,
            failure(SyntheticGuideBundle.JSON.replace("\"id\":\"RB-T2\",", "\"id\":\"RB-T2\",\"open\":[\"reviewer note\"],")),
        )
        assertEquals(
            GuideLoadError.INVALID,
            failure(SyntheticGuideBundle.JSON.replace("\"id\":\"RB-T2\",", "\"id\":\"RB-T2\",\"from\":[\"SS-T001\"],")),
        )
    }

    @Test
    fun aStructurallyInvalidBundleIsRejected() {
        // Valid JSON, valid types, but a case lists a card that does not exist.
        assertEquals(GuideLoadError.INVALID, failure(SyntheticGuideBundle.JSON.replace("\"RB-T9\",\"RB-T10\"", "\"RB-T9\",\"RB-T10\",\"RB-NOPE\"")))
    }
}
