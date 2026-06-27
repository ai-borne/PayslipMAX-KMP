package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class PositionedTokenTest {
    @Test
    fun derivedEdgesAndCentersAreCorrect() {
        val token = PositionedToken(text = "BPAY", x = 10f, y = 20f, width = 40f, height = 8f)

        assertEquals(50f, token.right)
        assertEquals(28f, token.bottom)
        assertEquals(30f, token.centerX)
        assertEquals(24f, token.centerY)
    }

    @Test
    fun valueEqualityHoldsForIdenticalTokens() {
        val a = PositionedToken("123456", 1f, 2f, 3f, 4f)
        val b = PositionedToken("123456", 1f, 2f, 3f, 4f)

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }
}
