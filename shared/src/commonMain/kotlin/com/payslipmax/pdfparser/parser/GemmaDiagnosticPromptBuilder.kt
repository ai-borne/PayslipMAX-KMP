package com.payslipmax.pdfparser.parser

/**
 * Constructs the Tier 6 diagnostic prompt asking Gemma to identify the single field
 * most likely mis-extracted when Tier 7 arithmetic reconciliation fails. Diagnosis only —
 * the prompt explicitly forbids proposing a corrected value.
 */
object GemmaDiagnosticPromptBuilder {
    fun buildPrompt(
        earnings: Map<String, Double>,
        deductions: Map<String, Double>,
        grossPay: Double,
        totalDeductions: Double,
        netRemittance: Double,
        residual: Double,
    ): String {
        val earningsStr = formatMap(earnings)
        val deductionsStr = formatMap(deductions)

        return """
            A military payslip failed arithmetic reconciliation. Identify at most ONE field
            most likely mis-extracted and explain why in prose (amounts allowed in the explanation).
            Never propose a corrected value. Never invent a field not listed below.
            Earnings: [$earningsStr]
            Deductions: [$deductionsStr]
            Gross Pay: $grossPay
            Total Deductions: $totalDeductions
            Net Remittance: $netRemittance
            Residual Mismatch: $residual
            Respond ONLY with JSON format:
            {"fieldKey": "<one of the keys above>", "reason": "<prose explanation>"}
            If no field is suspect, respond with: {}
            """.trimIndent()
    }

    private fun formatMap(map: Map<String, Double>): String {
        if (map.isEmpty()) return "None"
        return map.entries.joinToString("; ") { "${it.key}: ${it.value}" }
    }
}
