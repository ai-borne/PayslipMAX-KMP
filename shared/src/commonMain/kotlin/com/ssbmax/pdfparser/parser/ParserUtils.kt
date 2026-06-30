package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.*
import com.ssbmax.pdfparser.logging.Logger

internal fun cleanCommasAndWhitespace(text: String): String {
    val cleaned =
        text.replace(Regex("(\\d),(\\d)")) { match ->
            match.groupValues[1] + match.groupValues[2]
        }
    return cleaned.replace(Regex("\\s+"), " ")
}

internal fun negateHindiTransliterations(text: String): String {
    var cleaned = text
    for (word in PayslipPatternConfig.hindiTransliterations) {
        cleaned = cleaned.replace(Regex("(?<![a-zA-Z0-9])${Regex.escape(word)}(?![a-zA-Z0-9])", RegexOption.IGNORE_CASE), " ")
    }
    return cleaned.replace(Regex("\\s+"), " ")
}

internal fun cleanPreservingNewlines(text: String): String {
    var cleaned =
        text.replace(Regex("(\\d),(\\d)")) { match ->
            match.groupValues[1] + match.groupValues[2]
        }
    for (word in PayslipPatternConfig.hindiTransliterations) {
        cleaned = cleaned.replace(Regex("(?<![a-zA-Z0-9])${Regex.escape(word)}(?![a-zA-Z0-9])", RegexOption.IGNORE_CASE), " ")
    }
    return cleaned.split('\n').map { it.replace(Regex("[ \\t]+"), " ").trim() }.joinToString("\n")
}

internal fun stripNotesAndDescriptions(text: String): String {
    val lines = text.split('\n')
    val filteredLines =
        lines.filterNot { line ->
            val trimmed = line.trim()
            trimmed.contains(Regex("^\\d+\\s*\\.\\s*(?:Recovery|Credit|Refund|Rent|Bill|LF)\\b", RegexOption.IGNORE_CASE)) ||
                trimmed.contains(Regex("^Rent Bill", RegexOption.IGNORE_CASE)) ||
                trimmed.contains(Regex("^Recovery of", RegexOption.IGNORE_CASE)) ||
                trimmed.contains(Regex("^Credit of", RegexOption.IGNORE_CASE)) ||
                trimmed.contains(Regex("^Refund of", RegexOption.IGNORE_CASE))
        }
    return filteredLines.joinToString("\n")
}

private fun truncateFooterAndTotals(cleanedText: String): String {
    var tableText = cleanedText
    val footerIndicators =
        listOf(
            "Note: This is a system",
            "Note: This is system",
            "Note : This is a system",
            "Note: This is a system generated document",
            "Note: This is system generated document",
        )
    for (indicator in footerIndicators) {
        val idx = cleanedText.indexOf(indicator, ignoreCase = true)
        if (idx >= 0) {
            tableText = cleanedText.substring(0, idx)
            break
        }
    }
    val endOfTableIndicators =
        listOf(
            "Total Credit",
            "Total Debit",
            "Total Deductions",
            "Gross Pay",
            "Net Remittance",
            "REMITTANCE",
        )
    for (indicator in endOfTableIndicators) {
        val idx = tableText.indexOf(indicator, ignoreCase = true)
        if (idx >= 0) {
            tableText = tableText.substring(0, idx)
        }
    }
    return tableText
}

internal fun splitCreditDebitSections(cleanedText: String): Triple<String, String, Boolean> {
    val tableText = truncateFooterAndTotals(cleanedText)
    val debitOnlyAnchors =
        listOf(
            "DSOPF Subn", "DSOPF", "DSOP", "AGIF", "Incm Tax", "ITAX",
            "Educ Cess", "EHCESS", "Educ. Cess", "Op Dr Bal",
            "OP Bal(-)", "Cl. Cr. Bal.", "Clos Bal(+)", "R/o Of /Drs",
        )
    val caseSensitiveAnchors = listOf("LF", "FUR")
    var splitIdx = tableText.length
    var found = false
    for (anchor in debitOnlyAnchors) {
        val idx = tableText.indexOf(anchor, ignoreCase = true)
        if (idx in 1 until splitIdx) {
            val context = tableText.substring(kotlin.math.max(0, idx - 8), idx).lowercase()
            if (context.contains("ref")) continue
            splitIdx = idx
            found = true
        }
    }
    for (anchor in caseSensitiveAnchors) {
        val idx = tableText.indexOf(anchor, ignoreCase = false)
        if (idx in 1 until splitIdx) {
            val context = tableText.substring(kotlin.math.max(0, idx - 8), idx).lowercase()
            if (context.contains("ref")) continue
            splitIdx = idx
            found = true
        }
    }
    val creditSection = tableText.substring(0, splitIdx)
    val debitSection = if (found) tableText.substring(splitIdx) else ""
    return Triple(creditSection, debitSection, found)
}

internal fun extractFromColumn(
    colText: String,
    creditMapping: Map<String, String>,
    debitMapping: Map<String, String>,
): Map<String, Double> {
    val extracted = mutableMapOf<String, Double>()
    var workingCol = cleanCommasAndWhitespace(colText).replace(Regex("[^a-zA-Z0-9\\s()/.&-]"), " ")
    val keys = (creditMapping.keys + debitMapping.keys).distinct().sortedByDescending { it.length }

    for (key in keys) {
        val escapedKey = Regex.escape(key)
        val pattern =
            Regex(
                "(?<![a-zA-Z0-9])$escapedKey\\s*(?:\\([^)]+\\))?\\s*(?:[:–]|\\-\\s+)?\\s*(?:Rs\\.?\\s*)?(-?\\d+)(?![a-zA-Z0-9])",
                RegexOption.IGNORE_CASE,
            )
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

internal fun parseDate(
    cleanedFullText: String,
    filename: String,
): Pair<Int, Int> {
    // Match 1: STATEMENT OF ACCOUNT FOR MM/YYYY
    val dateMatch = Regex("STATEMENT OF ACCOUNT FOR (\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE).find(cleanedFullText)
    if (dateMatch != null) {
        return Pair(
            dateMatch.groupValues[1].toIntOrNull() ?: 1,
            dateMatch.groupValues[2].toIntOrNull() ?: 2024,
        )
    }

    // Match 2: STATEMENT OF ACCOUNT FOR [Month] [YYYY]
    val stmtMonthMatch = Regex("STATEMENT OF ACCOUNT FOR\\s+([A-Za-z]+)\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(cleanedFullText)
    if (stmtMonthMatch != null) {
        val monthStr = stmtMonthMatch.groupValues[1].lowercase()
        val mNum = PayslipPatternConfig.monthMap[monthStr] ?: 1
        val yVal = stmtMonthMatch.groupValues[2].toIntOrNull() ?: 2024
        return Pair(mNum, yVal)
    }

    // Match 3: standalone MM/YYYY in the text
    val standaloneMatch = Regex("\\b(0[1-9]|1[0-2])/(\\d{4})\\b").find(cleanedFullText)
    if (standaloneMatch != null) {
        return Pair(
            standaloneMatch.groupValues[1].toIntOrNull() ?: 1,
            standaloneMatch.groupValues[2].toIntOrNull() ?: 2024,
        )
    }

    // Match 4: Filename fallback
    val fileMonthMatch = Regex("(?:^|\\d+\\s+)([a-zA-Z]+)", RegexOption.IGNORE_CASE).find(filename)
    val fileYearMatch = Regex("(\\d{4})").find(filename)
    val mNum = fileMonthMatch?.groupValues?.get(1)?.lowercase()?.let { PayslipPatternConfig.monthMap[it] } ?: 1

    val yVal =
        if (fileYearMatch != null) {
            fileYearMatch.groupValues[1].toIntOrNull() ?: 2024
        } else {
            val year2dMatch = Regex("(\\d{2})\\.pdf$", RegexOption.IGNORE_CASE).find(filename)
            if (year2dMatch != null) {
                2000 + (year2dMatch.groupValues[1].toIntOrNull() ?: 24)
            } else {
                2024
            }
        }
    return Pair(mNum, yVal)
}

private fun extractOfficerName(text: String): String {
    val nameRegex = Regex("(?:Name|naama/Name)\\s*:\\s*([A-Za-z\\s]+)", RegexOption.IGNORE_CASE)
    var officerName = nameRegex.find(text)?.groupValues?.get(1)?.trim() ?: ""
    if (officerName.isEmpty()) {
        val fallbackNameRegex = Regex("PAN No\\s*[:\\-–]?\\s*([A-Za-z\\s]+)", RegexOption.IGNORE_CASE)
        officerName = fallbackNameRegex.find(text)?.groupValues?.get(1)?.trim() ?: ""
    }
    if (officerName.isNotEmpty()) {
        Logger.d("ParserUtils", "officerName before split: '$officerName'")
        officerName = officerName.split(Regex("\\b(?:A/C|Email|PAN|Basic|BPAY|CDA|tada|ta|laoKa|saM|For|rankpay|ledger|generalquery|contact|bankers)\\b", RegexOption.IGNORE_CASE))[0].trim()
        Logger.d("ParserUtils", "officerName after split: '$officerName'")
        if (officerName.endsWith(" A", ignoreCase = true)) {
            officerName = officerName.substring(0, officerName.length - 2).trim()
        }
    }
    return officerName
}

private fun extractAccountNumber(text: String): String {
    val acRegex = Regex("(?:A/C No|CDA A/C NO|laoKa saM#yaa /A/C No)\\s*[:\\-–]?\\s*([^\\s]+)", RegexOption.IGNORE_CASE)
    val fallbackAcRegex = Regex("([0-9]{2,}/[0-9]{2,}/[0-9]{5,}[A-Z]?)", RegexOption.IGNORE_CASE)
    var accountNo = fallbackAcRegex.find(text)?.groupValues?.get(1)?.trim() ?: ""
    if (accountNo.isEmpty()) {
        accountNo = acRegex.find(text)?.groupValues?.get(1)?.trim() ?: ""
    }
    if (accountNo.isEmpty()) {
        accountNo = "16/000/000000X"
    }
    if (accountNo.endsWith("PAN")) {
        accountNo = accountNo.removeSuffix("PAN").trim()
    }
    if (accountNo.startsWith(":")) {
        accountNo = accountNo.removePrefix(":").trim()
    }
    return accountNo
}

private fun extractPanNumber(text: String): String {
    val panRegex = Regex("(?:PAN No|sqaayaI Kata saM#yaa/PAN No)\\s*:\\s*([^\\s]+)", RegexOption.IGNORE_CASE)
    var panNo = panRegex.find(text)?.groupValues?.get(1)?.trim() ?: ""
    if (panNo.isEmpty()) {
        val fallbackPanRegex = Regex("([A-Z]{2}[*\\d]{7}[A-Z])", RegexOption.IGNORE_CASE)
        panNo = fallbackPanRegex.find(text)?.groupValues?.get(1)?.trim() ?: ""
    }
    if (panNo.isEmpty()) {
        panNo = "AR*****90G"
    }
    return panNo
}

internal fun parseOfficer(
    cleanedFullText: String,
    monthNum: Int,
    year: Int,
): Officer {
    val officerName = extractOfficerName(cleanedFullText)
    val accountNo = extractAccountNumber(cleanedFullText)
    val panNo = extractPanNumber(cleanedFullText)
    return Officer(name = officerName, accountNo = accountNo, pan = panNo)
}

internal fun parseTotals(cleanedFullText: String): Triple<Double, Double, Double> {
    val totalsMapping =
        mapOf(
            "Gross Pay" to listOf("kuula Aaya Gross Pay", "kula Aaya Gross Pay", "kuula Aaya", "kula Aaya", "Gross Pay", "Total Credit"),
            "Total Deductions" to listOf("kuula kTaOtI Total Deductions", "kula kTaOtI Total Deductions", "kuula kTaOtI", "kula kTaOtI", "Total Deductions", "Total Debit"),
            "Net Remittance" to listOf("Net Remittance", "REMITTANCE", "REMITANCE", "inavala p`oiYat Qana/Net Remittance", "inavala p`oiYat Qana"),
        )

    val extractedTotals = mutableMapOf<String, Double>()
    for ((term, keys) in totalsMapping) {
        for (key in keys) {
            val escapedKey = Regex.escape(key)
            val pattern = Regex("(?<![a-zA-Z0-9])$escapedKey(?![a-zA-Z0-9])\\s*[:\\-–]?\\s*(?:Rs\\.?\\s*)?(\\d+)", RegexOption.IGNORE_CASE)
            val match = pattern.find(cleanedFullText)
            if (match != null) {
                extractedTotals[term] = match.groupValues[1].toDoubleOrNull() ?: 0.0
                break
            }
        }
    }

    // PDFKit artifact: labels serialised before amounts → "REMITTANCE … Total Debit 400000 27119 581007".
    // Standard loop captures 400000 (remittance) as deductions; recover via label-block pattern.
    if ((extractedTotals["Net Remittance"] ?: 0.0) == 0.0) {
        val blockMatch =
            Regex("""(?i)REMITTANCE[^0-9]+Total\s+Debit\s+(\d+)(?:\s+\d+)*\s+(\d+)""")
                .find(cleanedFullText)
        if (blockMatch != null) {
            extractedTotals["Net Remittance"] = blockMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            extractedTotals["Total Deductions"] = blockMatch.groupValues[2].toDoubleOrNull() ?: 0.0
        }
    }

    return Triple(
        extractedTotals["Gross Pay"] ?: 0.0,
        extractedTotals["Total Deductions"] ?: 0.0,
        extractedTotals["Net Remittance"] ?: 0.0,
    )
}
