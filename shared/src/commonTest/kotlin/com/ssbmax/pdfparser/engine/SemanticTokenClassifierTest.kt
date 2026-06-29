package com.ssbmax.pdfparser.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class SemanticTokenClassifierTest {
    @Test
    fun testClassifyAmount() {
        val (type1, conf1) = SemanticTokenClassifier.classify("144700")
        assertEquals(SemanticTokenType.Amount, type1)
        assertEquals(1.0f, conf1)

        val (type2, _) = SemanticTokenClassifier.classify("40,000")
        assertEquals(SemanticTokenType.Amount, type2)

        val (type3, _) = SemanticTokenClassifier.classify("-1500.50")
        assertEquals(SemanticTokenType.Amount, type3)
    }

    @Test
    fun testClassifyMixedCode() {
        val (type1, _) = SemanticTokenClassifier.classify("RH12")
        assertEquals(SemanticTokenType.MixedCode, type1)

        val (type2, _) = SemanticTokenClassifier.classify("ARR-DA")
        assertEquals(SemanticTokenType.MixedCode, type2)

        val (type3, _) = SemanticTokenClassifier.classify("16/000/000000X")
        assertEquals(SemanticTokenType.MixedCode, type3)
    }

    @Test
    fun testClassifyLabelAndMetadata() {
        val (type1, _) = SemanticTokenClassifier.classify("Basic Pay")
        assertEquals(SemanticTokenType.Label, type1)

        val (type2, _) = SemanticTokenClassifier.classify("PAN No:")
        assertEquals(SemanticTokenType.Metadata, type2)
    }
}
