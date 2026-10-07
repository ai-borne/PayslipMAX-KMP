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
 * A personal card carries bullets such as "Level {level} = Rs {food_rate}/day". Shown raw, a braced name reads
 * as a bug and undermines trust in every figure on the card, so those bullets go to the "your figure" slot that
 * phase E6 fills from the profile, and the rest of the card stays complete without them.
 */
class CardTemplateTest {
    private val bundle = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(SyntheticGuideBundle.JSON)).bundle
    private val index = GuideIndex(bundle)

    @Test
    fun placeholderBulletsMoveToTheFigureSlotAndAreNeverVisible() {
        val card = index.card(SyntheticGuideBundle.PERSONAL_CARD)!!.copy(key = listOf("Hours away decide it", "Your amount: Level {level} = Rs {food_rate}/day"))

        val body = CardTemplate.body(card)

        assertEquals(listOf("Hours away decide it"), body.key)
        assertEquals(listOf("Your amount: Level {level} = Rs {food_rate}/day"), body.figureTemplates)
        assertFalse((body.key + body.attach + body.watch).any(CardTemplate::hasPlaceholder))
    }

    @Test
    fun placeholdersInAnySectionAreCollectedInCardOrder() {
        val card = index.card(SyntheticGuideBundle.PERSONAL_CARD)!!.copy(key = listOf("K {a}"), attach = listOf("Form"), watch = listOf("W {b}", "Plain"))

        val body = CardTemplate.body(card)

        assertEquals(listOf("K {a}", "W {b}"), body.figureTemplates)
        assertEquals(emptyList(), body.key, "a key section of only placeholders is left empty, so the UI hides it")
        assertEquals(listOf("Form"), body.attach)
        assertEquals(listOf("Plain"), body.watch)
    }

    @Test
    fun aCardWithoutPlaceholdersIsShownAsAuthored() {
        val card = index.card(SyntheticGuideBundle.AMENDED_CARD)!!

        val body = CardTemplate.body(card)

        assertEquals(card.key, body.key)
        assertEquals(card.attach, body.attach)
        assertEquals(card.watch, body.watch)
        assertTrue(body.figureTemplates.isEmpty())
    }
}
