package com.payslipmax.pdfparser.guide.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * A raw braced name ("Rs {food_rate}") reads as a bug and undermines trust in every figure on the card. Cards carry none
 * (the bundle validator rejects them), and the "your figure" line comes from the bundle's figures.
 */
class CardTemplateTest {
    @Test
    fun aBraceIsRecognisedAsAPlaceholder() {
        assertTrue(CardTemplate.hasPlaceholder("Your amount: Rs {food_rate}/day"))
        assertFalse(CardTemplate.hasPlaceholder("Rs 1,200 a day"))
    }
}
