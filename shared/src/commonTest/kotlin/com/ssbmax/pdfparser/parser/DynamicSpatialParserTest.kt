package com.ssbmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicSpatialParserTest {
    @Test
    fun testDynamicSpatialParserKeyValueExtraction() {
        val sampleText = """
            BPAY (12A) 144700
            DA 84906
            CC to bankers 11923
            to 31/08/2024
        """.trimIndent()
        
        val pairs = DynamicSpatialParser.extractDynamicPairs(sampleText)
        assertEquals(144700.0, pairs["BPAY"])
        assertEquals(84906.0, pairs["DA"])
        assertEquals(11923.0, pairs["CC to bankers"])
        // standalone "to" should be blocked
        assertTrue(pairs["to"] == null)

        val sampleTextWithHeaders = """
            ivavarNa
            Description
            raiSa
            Amount
            BPAY (12A) 144700
            DA 84906
        """.trimIndent()
        val cleanedText = cleanPreservingNewlines(sampleTextWithHeaders)
        val pairsWithHeaders = DynamicSpatialParser.extractDynamicPairs(cleanedText)
        assertEquals(144700.0, pairsWithHeaders["BPAY"])
        assertEquals(84906.0, pairsWithHeaders["DA"])
        assertTrue(pairsWithHeaders["Description Amount BPAY"] == null)
    }

    @Test
    fun testSmartDynamicKeyExclusions() {
        val text = """
            Basic Pay 144700
            DA 84906
            uula 109308
            D DSOP 40000
            Agra WATER 3552
            ROUS NEW YEAR 2026
            ARR-DA 14040
            ETKT 1117
        """.trimIndent()

        val pairs = DynamicSpatialParser.extractDynamicPairs(text)
        
        // Assert that valid hardcoded keys are kept
        assertEquals(144700.0, pairs["Basic Pay"])
        assertEquals(84906.0, pairs["DA"])
        
        // Assert that valid dynamic keys (matching the strict format) are kept
        assertEquals(14040.0, pairs["ARR-DA"])
        assertEquals(1117.0, pairs["ETKT"])
        
        // Assert that spurious keys are ignored
        assertTrue(pairs["uula"] == null)
        assertTrue(pairs["D DSOP"] == null)
        assertTrue(pairs["Agra WATER"] == null)
        assertTrue(pairs["ROUS NEW YEAR"] == null)
    }

    @Test
    fun testHindiTransliterationNegation() {
        val rawText = "kuula Basic Pay 144700 kula DA 84906 uula 123"
        val negated = negateHindiTransliterations(rawText)
        assertTrue(!negated.contains("kuula", ignoreCase = true))
        assertTrue(!negated.contains("kula", ignoreCase = true))
        assertTrue(!negated.contains("uula", ignoreCase = true))
        assertTrue(negated.contains("Basic Pay"))
        assertTrue(negated.contains("DA"))
    }

    @Test
    fun testBlocklistFiltering() {
        val text = """
            gross pay 12345
            Total Credit 54321
            Basic Pay 144700
        """.trimIndent()
        val pairs = DynamicSpatialParser.extractDynamicPairs(text)
        assertTrue(pairs["gross pay"] == null)
        assertTrue(pairs["Total Credit"] == null)
        assertEquals(144700.0, pairs["Basic Pay"])
    }

    @Test
    fun testEdgeCasesInDynamicSpatialParser() {
        val text = """
            A 100
            123 200
            BPAY 144700
            - 300
        """.trimIndent()
        val pairs = DynamicSpatialParser.extractDynamicPairs(text)
        assertTrue(pairs["A"] == null)
        assertTrue(pairs["123"] == null)
        assertTrue(pairs["-"] == null)
        assertEquals(144700.0, pairs["BPAY"])
    }
}
