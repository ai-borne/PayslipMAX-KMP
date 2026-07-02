package com.ssbmax.pdfparser.engine

/**
 * Generic semantic token classifier that categorizes text tokens without domain-specific keywords.
 */
object SemanticTokenClassifier {
    fun classify(text: String): Pair<SemanticTokenType, Float> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return SemanticTokenType.Unknown to 0.0f

        return when {
            isAmount(trimmed) -> SemanticTokenType.Amount to 1.0f
            isMixedCode(trimmed) -> SemanticTokenType.MixedCode to 0.9f
            isMetadataTag(trimmed) -> SemanticTokenType.Metadata to 0.95f
            isPureLabel(trimmed) -> SemanticTokenType.Label to 0.85f
            else -> SemanticTokenType.Unknown to 0.5f
        }
    }

    private fun isAmount(text: String): Boolean {
        val cleaned = text.replace(",", "").trim()
        return Regex("^-?\\d+(\\.\\d+)?$").matches(cleaned)
    }

    private fun isMixedCode(text: String): Boolean {
        // Alphanumeric codes or dash-separated codes like RH12, ARR-DA, TPTADA, 16/000/000000X
        val hasDigits = text.any { it.isDigit() }
        val hasLetters = text.any { it.isLetter() }
        val hasSpecial = text.contains("-") || text.contains("/")
        return (hasDigits && hasLetters) || (hasLetters && hasSpecial && !text.contains(" "))
    }

    private fun isMetadataTag(text: String): Boolean {
        return text.endsWith(":") || text.endsWith(":-")
    }

    private fun isPureLabel(text: String): Boolean {
        return text.all { it.isLetter() || it.isWhitespace() || it == '.' || it == '&' || it == '(' || it == ')' }
    }
}
