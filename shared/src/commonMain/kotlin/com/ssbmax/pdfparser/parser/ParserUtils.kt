package com.ssbmax.pdfparser.parser

internal fun cleanCommasAndWhitespace(text: String): String {
    val cleaned = text.replace(Regex("(\\d),(\\d)")) { match ->
        match.groupValues[1] + match.groupValues[2]
    }
    return cleaned.replace(Regex("\\s+"), " ")
}

internal fun extractFromColumn(
    colText: String,
    creditMapping: Map<String, String>,
    debitMapping: Map<String, String>
): Map<String, Double> {
    val extracted = mutableMapOf<String, Double>()
    var workingCol = cleanCommasAndWhitespace(colText)
    val keys = (creditMapping.keys + debitMapping.keys).distinct().sortedByDescending { it.length }

    for (key in keys) {
        val escapedKey = Regex.escape(key)
        val pattern = Regex("(?<![a-zA-Z0-9])$escapedKey\\s*(?:\\([^)]+\\))?\\s*[:\\-–]?\\s*(?:Rs\\.?\\s*)?(\\d+)(?![a-zA-Z0-9])", RegexOption.IGNORE_CASE)
        var match = pattern.find(workingCol)
        while (match != null) {
            val value = match.groupValues[1].toDoubleOrNull() ?: 0.0
            extracted[key] = (extracted[key] ?: 0.0) + value
            workingCol = workingCol.replaceFirst(match.value, "MATCHED_VALUE")
            match = pattern.find(workingCol)
        }
    }
    return extracted
}
