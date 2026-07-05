package com.payslipmax.pdfparser.parser

import kotlinx.serialization.json.Json

/**
 * Safely parses the Tier 6 diagnostic response from Gemma into a [DiagnosticSuggestion].
 *
 * Structural anti-autofix guard: deserialization is strict (`ignoreUnknownKeys = false`), so
 * a response smuggling in an extra key (e.g. a disguised "correctedValue") fails the whole
 * parse to `null` rather than silently stripping the offending key.
 */
object GemmaDiagnosticResponseParser {
    private const val MAX_REASON_LENGTH = 200

    private val jsonInstance =
        Json {
            ignoreUnknownKeys = false
            isLenient = true
        }

    fun parse(
        response: String,
        allowedFieldKeys: Set<String>,
    ): DiagnosticSuggestion? {
        return try {
            val cleaned = GemmaMarkdownCleaner.clean(response)
            val suggestion = jsonInstance.decodeFromString(DiagnosticSuggestion.serializer(), cleaned)
            if (suggestion.fieldKey !in allowedFieldKeys) {
                null
            } else {
                suggestion.copy(reason = suggestion.reason.take(MAX_REASON_LENGTH))
            }
        } catch (e: Exception) {
            null
        }
    }
}
