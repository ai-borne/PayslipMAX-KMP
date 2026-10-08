package com.payslipmax.pdfparser.guide.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import com.payslipmax.pdfparser.testing.SyntheticGuideBundle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * A raw braced name ("Rs {food_rate}") reads as a bug and undermines trust in every figure on the card. Cards carry none
 * (the bundle validator rejects them), and the "your figure" line comes from the bundle's figures, so the body is the
 * authored sections as they are.
 */
class CardTemplateTest {
    private val bundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle
    private val index = GuideIndex(bundle)

    @Test
    fun aCardBodyIsItsSectionsAsAuthored() {
        val card = index.card(SyntheticGuideBundle.AMENDED_CARD)!!

        val body = CardTemplate.body(card)

        assertEquals(card.key, body.key)
        assertEquals(card.attach, body.attach)
        assertEquals(card.watch, body.watch)
    }

    @Test
    fun aBraceIsRecognisedAsAPlaceholder() {
        assertTrue(CardTemplate.hasPlaceholder("Your amount: Rs {food_rate}/day"))
        assertFalse(CardTemplate.hasPlaceholder("Rs 1,200 a day"))
    }
}
