package com.payslipmax.pdfparser.parser

/**
 * Strips markdown code-fence wrappers (```json ... ``` or ``` ... ```) that Gemma
 * sometimes wraps its JSON responses in. Shared by all Gemma response parsers.
 */
internal object GemmaMarkdownCleaner {
    fun clean(raw: String): String {
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
}
