package com.ssbmax.pdfparser.parser

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class GemmaFallbackExtraction(
    val earnings: Map<String, Double> = emptyMap(),
    val deductions: Map<String, Double> = emptyMap(),
)

/**
 * Safely parses structured JSON responses from Gemma offline fallback.
 */
object GemmaResponseParser {
    private val jsonInstance =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    fun parse(response: String): GemmaFallbackExtraction {
        return try {
            val cleaned = cleanMarkdownBlocks(response)
            val root = jsonInstance.parseToJsonElement(cleaned).jsonObject
            val earningsMap = extractSection(root["earnings"]?.jsonObject)
            val deductionsMap = extractSection(root["deductions"]?.jsonObject)
            GemmaFallbackExtraction(earnings = earningsMap, deductions = deductionsMap)
        } catch (e: Exception) {
            GemmaFallbackExtraction()
        }
    }

    private fun cleanMarkdownBlocks(raw: String): String {
        var text = raw.trim()
        if (text.startsWith("```json")) {
            text = text.removePrefix("```json").trim()
        } else if (text.startsWith("```")) {
            text = text.removePrefix("```").trim()
        }
        if (text.endsWith("```")) {
            text = text.removeSuffix("```").trim()
        }
        return text
    }

    private fun extractSection(obj: JsonObject?): Map<String, Double> {
        if (obj == null) return emptyMap()
        val map = mutableMapOf<String, Double>()
        for ((key, element) in obj.entries) {
            val amount = element.jsonPrimitive.doubleOrNull
            if (amount != null && amount > 0.0) {
                map[key] = amount
            }
        }
        return map
    }
}
