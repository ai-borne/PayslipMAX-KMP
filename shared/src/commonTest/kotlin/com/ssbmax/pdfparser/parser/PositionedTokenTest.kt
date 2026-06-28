package com.ssbmax.pdfparser.parser

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun defaultFontMetadataIsProvided() {
        val token = PositionedToken("BPAY", 10f, 20f, 40f, 8f)

        assertEquals(0f, token.fontSize)
        assertFalse(token.isBold)
    }

    @Test
    fun customFontMetadataIsStored() {
        val token = PositionedToken("HEADER", 10f, 20f, 40f, 8f, fontSize = 12f, isBold = true)

        assertEquals(12f, token.fontSize)
        assertTrue(token.isBold)
    }

    @Test
    fun jsonDeserializationWithMissingFontFieldsUsesDefaults() {
        val jsonStr = """{"text":"BPAY","x":10.0,"y":20.0,"width":40.0,"height":8.0}"""
        val decoded = Json.decodeFromString<PositionedToken>(jsonStr)

        assertEquals("BPAY", decoded.text)
        assertEquals(0f, decoded.fontSize)
        assertFalse(decoded.isBold)
    }
}
