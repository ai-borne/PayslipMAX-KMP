package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.logging.Logger

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
        cleaned = replaceWholeWordIgnoreCase(cleaned, word, " ")
    }
    return cleaned.replace(Regex("\\s+"), " ")
}

internal fun parseDate(
    cleanedFullText: String,
    filename: String,
): Pair<Int, Int> {
    // Matches 1-3 (anchored MM/YYYY, anchored Month YYYY, standalone MM/YYYY): shared with the
    // grammar-era detector via extractStatementPeriod, kept as a single source of truth.
    val period = com.payslipmax.pdfparser.parser.detection.extractStatementPeriod(cleanedFullText)
    if (period != null) {
        return Pair(period.month, period.year)
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
        officerName = officerName.split(Regex("\\b(?:A/C|Email|PAN|Basic|BPAY|CDA|tada|ta|laoKa|saM|For|rankpay|ledger|generalquery|contact|bankers|PRO)\\b", RegexOption.IGNORE_CASE))[0].trim()
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
    val panRegex = Regex("(?:PAN No|sqaayaI Kata saM#yaa/PAN No)\\s*:\\s*([A-Za-z*0-9]{8,})", RegexOption.IGNORE_CASE)
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
            val value = findKeyedNumber(cleanedFullText, key)
            if (value != null) {
                extractedTotals[term] = value
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
