package com.ssbmax.pdfparser.parser

object DynamicSpatialParser {
    private val blocklist get() = PayslipPatternConfig.blocklist
    private val sentenceWords get() = PayslipPatternConfig.sentenceWords
    private val invalidEntireKeys get() = PayslipPatternConfig.invalidEntireKeys

    fun cleanAndJoinColumns(colText: String): String {
        val lines = colText.split("\n")
        val auditedLines = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue

            val isPurelyNumeric = trimmed.matches(Regex("^[-\\d,\\s.()]+$"))
            if (isPurelyNumeric) {
                auditedLines.add(trimmed)
                continue
            }

            val cleanedTag = trimmed.replace(Regex("\\(\\w+\\)"), " ")
            val workingLine = cleanedTag.replace(Regex("[^a-zA-Z\\s]"), " ").trim()
            val lower = workingLine.lowercase()
            if (lower.isEmpty() || lower.length == 1) continue

            val words = lower.split(Regex("\\s+"))
            if (lower in invalidEntireKeys || words.any { it in blocklist || it in sentenceWords }) {
                continue
            }
            auditedLines.add(trimmed)
        }

        val joined = mutableListOf<String>()
        var i = 0
        while (i < auditedLines.size) {
            val current = auditedLines[i]
            if (i < auditedLines.size - 1) {
                val next = auditedLines[i + 1]
                val currentHasDigits = current.any { it.isDigit() }
                val nextHasDigits = next.any { it.isDigit() }
                if (!currentHasDigits && nextHasDigits) {
                    joined.add("$current $next")
                    i += 2
                    continue
                }
            }
            joined.add(current)
            i++
        }
        return joined.joinToString(" ")
    }

    fun extractDynamicPairs(colText: String): Map<String, Double> {
        val cleanedText = cleanAndJoinColumns(colText)
        val extracted = mutableMapOf<String, Double>()
        val pattern = Regex("\\b([A-Za-z/().&-]{1,15}(?:\\s+[A-Za-z/().&-]{1,15}){0,2}\\d*)\\s+(-?\\d+)\\b")

        val cleanedTag = cleanedText.replace(Regex("\\(\\w+\\)"), " ")
        val cleanVal = cleanCommasAndWhitespace(cleanedTag)
        val workingLine = cleanVal.replace(Regex("[^a-zA-Z0-9\\s()/.&-]"), " ")

        val matches = pattern.findAll(workingLine)
        for (match in matches) {
            val key = match.groupValues[1].trim()
            val valStr = match.groupValues[2]

            if (key.length < 2 || key.matches(Regex("^\\d+$"))) continue

            val isHardcoded = key in PayslipPatternConfig.creditKeysMapping.keys || key in PayslipPatternConfig.debitKeysMapping.keys
            if (!isHardcoded) {
                if (!key.matches(Regex("^[A-Z0-9\\-/.]+$")) || key.length < 2) continue
            }

            val keyLower = key.lowercase()
            if (keyLower in blocklist || keyLower in invalidEntireKeys) continue
            if (!key.any { it.isLetter() }) continue

            val wordsInKey = Regex("\\b[a-z]+\\b").findAll(keyLower).map { it.value }.toList()
            if (wordsInKey.any { it in blocklist || it in sentenceWords }) continue

            val value = valStr.toDoubleOrNull() ?: 0.0
            extracted[key] = (extracted[key] ?: 0.0) + value
        }
        return extracted
    }

    fun extractDynamicEarningsAndDeductions(
        isSplit: Boolean,
        leftColumnText: String,
        middleColumnText: String,
        fullText: String,
        cleanedLeftText: String,
        cleanedMiddleText: String,
        cleanedFullText: String,
    ): Pair<Map<String, Double>, Map<String, Double>> {
        val dynamicEarnings: Map<String, Double>
        val dynamicDeductions: Map<String, Double>
        if (isSplit) {
            if (leftColumnText != middleColumnText) {
                dynamicEarnings = extractDynamicPairs(cleanedLeftText)
                dynamicDeductions = extractDynamicPairs(cleanedMiddleText)
            } else {
                val (creditSectionText, debitSectionText, _) = splitCreditDebitSections(cleanedFullText)
                dynamicEarnings = extractDynamicPairs(creditSectionText)
                dynamicDeductions = extractDynamicPairs(debitSectionText)
            }
        } else {
            dynamicEarnings = extractDynamicPairs(cleanedFullText)
            dynamicDeductions = extractDynamicPairs(cleanedFullText)
        }
        return Pair(dynamicEarnings, dynamicDeductions)
    }
}
