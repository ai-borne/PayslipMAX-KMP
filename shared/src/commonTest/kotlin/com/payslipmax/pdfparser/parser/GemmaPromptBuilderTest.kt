package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertTrue

class GemmaPromptBuilderTest {
    @Test
    fun testBuildPromptIncludesRawItemsAndValidKeys() {
        val rawEarnings = mapOf("SPL ALLW" to 1500.0)
        val rawDeductions = mapOf("MISC REC" to 300.0)

        val prompt = GemmaPromptBuilder.buildPrompt(rawEarnings, rawDeductions)

        assertTrue(prompt.contains("SPL ALLW: 1500.0"))
        assertTrue(prompt.contains("MISC REC: 300.0"))
        assertTrue(prompt.contains("earnings"))
        assertTrue(prompt.contains("deductions"))
        assertTrue(prompt.contains("basicPay"))
        assertTrue(prompt.contains("dsopSubscription"))
    }

    @Test
    fun testBuildPromptEmptyRawMaps() {
        val prompt = GemmaPromptBuilder.buildPrompt(emptyMap(), emptyMap())
        assertTrue(prompt.isNotEmpty())
        assertTrue(prompt.contains("earnings"))
        assertTrue(prompt.contains("deductions"))
    }
}
