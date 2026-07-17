package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GemmaDiagnosticPromptBuilderTest {
    @Test
    fun testBuildPromptIncludesTotalsResidualAndFieldKeys() {
        val earnings = mapOf("basicPay" to 50000.0, "dearnessAllowance" to 20000.0)
        val deductions = mapOf("incomeTax" to 5000.0)

        val prompt =
            GemmaDiagnosticPromptBuilder.buildPrompt(
                earnings = earnings,
                deductions = deductions,
                grossPay = 70000.0,
                totalDeductions = 5000.0,
                netRemittance = 65000.0,
                residual = 128370.0,
            )

        assertTrue(prompt.contains("basicPay: 50000.0"))
        assertTrue(prompt.contains("dearnessAllowance: 20000.0"))
        assertTrue(prompt.contains("incomeTax: 5000.0"))
        assertTrue(prompt.contains("70000.0"))
        assertTrue(prompt.contains("5000.0"))
        assertTrue(prompt.contains("65000.0"))
        assertTrue(prompt.contains("128370.0"))
        assertTrue(prompt.contains("fieldKey"))
        assertTrue(prompt.contains("reason"))
    }

    @Test
    fun testBuildPromptNeverProposesCorrectedValue() {
        val prompt =
            GemmaDiagnosticPromptBuilder.buildPrompt(
                earnings = mapOf("basicPay" to 50000.0),
                deductions = emptyMap(),
                grossPay = 50000.0,
                totalDeductions = 0.0,
                netRemittance = 40000.0,
                residual = 10000.0,
            )

        assertTrue(prompt.contains("Never propose a corrected value"))
        assertFalse(prompt.contains("correctedValue"))
    }

    @Test
    fun testBuildPromptContainsNoPii() {
        val prompt =
            GemmaDiagnosticPromptBuilder.buildPrompt(
                earnings = mapOf("basicPay" to 50000.0),
                deductions = mapOf("incomeTax" to 5000.0),
                grossPay = 50000.0,
                totalDeductions = 5000.0,
                netRemittance = 45000.0,
                residual = 0.0,
            )

        assertFalse(prompt.contains("name", ignoreCase = true))
        assertFalse(prompt.contains("account", ignoreCase = true))
        assertFalse(prompt.contains("PAN", ignoreCase = false))
    }

    @Test
    fun testBuildPromptEmptyMapsEdgeCase() {
        val prompt =
            GemmaDiagnosticPromptBuilder.buildPrompt(
                earnings = emptyMap(),
                deductions = emptyMap(),
                grossPay = 0.0,
                totalDeductions = 0.0,
                netRemittance = 0.0,
                residual = 0.0,
            )

        assertTrue(prompt.isNotEmpty())
        assertTrue(prompt.contains("None"))
    }
}
