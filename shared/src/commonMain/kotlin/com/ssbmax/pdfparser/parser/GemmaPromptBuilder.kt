package com.ssbmax.pdfparser.parser

/**
 * Constructs lightweight, deterministic prompts for Tier 6 Gemma fallback extraction.
 */
object GemmaPromptBuilder {
    private val sampleEarningsKeys =
        listOf(
            "basicPay", "dearnessAllowance", "militaryServicePay", "transportAllowance",
            "rationMoney", "dressAllowance", "specialForcesPay", "fieldAllowance", "specialAllowance",
        )

    private val sampleDeductionsKeys =
        listOf(
            "dsopSubscription",
            "agif",
            "incomeTax",
            "educationCess",
            "licenseFee",
            "furnitureRent",
            "waterCharges",
            "electricityCharges",
        )

    fun buildPrompt(
        rawEarnings: Map<String, Double>,
        rawDeductions: Map<String, Double>,
    ): String {
        val rawEarnStr = formatMap(rawEarnings)
        val rawDedStr = formatMap(rawDeductions)
        val earnKeysStr = sampleEarningsKeys.joinToString(", ")
        val dedKeysStr = sampleDeductionsKeys.joinToString(", ")

        return """
            Map unresolved military payslip items to standardized keys.
            Raw Earnings: [$rawEarnStr]
            Allowed Earnings Keys: [$earnKeysStr]
            Raw Deductions: [$rawDedStr]
            Allowed Deductions Keys: [$dedKeysStr]
            Respond ONLY with JSON format:
            {"earnings": {"key": amount}, "deductions": {"key": amount}}
            """.trimIndent()
    }

    private fun formatMap(map: Map<String, Double>): String {
        if (map.isEmpty()) return "None"
        return map.entries.joinToString("; ") { "${it.key}: ${it.value}" }
    }
}
