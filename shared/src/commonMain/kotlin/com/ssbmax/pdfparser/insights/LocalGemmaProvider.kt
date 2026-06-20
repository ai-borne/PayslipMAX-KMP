package com.ssbmax.pdfparser.insights

class LocalGemmaProvider : AIInsightProvider {
    override suspend fun generateInsights(payload: PromptPayload): Result<String> {
        // Simulates Gemma-2B-IT local JSON output for offline execution
        val stubJson = """
        {
          "salaryChanges": [
            { "item": "Basic Pay", "change": "unchanged", "amount": 0.0 }
          ],
          "missingAllowances": [],
          "newDeductions": [],
          "riskAlerts": [
            { "observation": "Offline local audit completed.", "action": "Ensure settings match PCDA records." }
          ],
          "opportunities": [
            { "opportunity": "Provident Fund local milestone tracking", "action": "Check annual DSOP statement." }
          ],
          "actionRequired": []
        }
        """.trimIndent()
        return Result.success(stubJson)
    }
}
