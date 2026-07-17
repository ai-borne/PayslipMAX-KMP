package com.payslipmax.pdfparser.parser.detection

import com.payslipmax.pdfparser.parser.PayslipPatternConfig

/**
 * Extracts the printed statement period from payslip full text, or returns null when none can be
 * confidently found. Unlike [com.payslipmax.pdfparser.parser.parseDate] this never guesses from a
 * filename or defaults to a fallback year — grammar-era selection must be able to tell "no date
 * found" apart from "a date was found", so it knows when to fall back to text-signature detection.
 *
 * Match order mirrors [com.payslipmax.pdfparser.parser.parseDate]: the anchored "STATEMENT OF ACCOUNT
 * FOR ..." phrase is tried before a bare standalone MM/YYYY scan, so an unrelated date elsewhere in
 * the document (e.g. a DSOP loan due-date) doesn't get mistaken for the statement period.
 */
internal fun extractStatementPeriod(fullText: String): StatementPeriod? {
    val numericAnchor = Regex("STATEMENT OF ACCOUNT FOR (\\d{2})/(\\d{4})", RegexOption.IGNORE_CASE).find(fullText)
    if (numericAnchor != null) {
        val month = numericAnchor.groupValues[1].toIntOrNull()
        val year = numericAnchor.groupValues[2].toIntOrNull()
        if (month != null && year != null) return StatementPeriod(month, year)
    }

    val namedAnchor =
        Regex("STATEMENT OF ACCOUNT FOR\\s+([A-Za-z]+)\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(fullText)
    if (namedAnchor != null) {
        val month = PayslipPatternConfig.monthMap[namedAnchor.groupValues[1].lowercase()]
        val year = namedAnchor.groupValues[2].toIntOrNull()
        if (month != null && year != null) return StatementPeriod(month, year)
    }

    val standalone = Regex("\\b(0[1-9]|1[0-2])/(\\d{4})\\b").find(fullText)
    if (standalone != null) {
        val month = standalone.groupValues[1].toIntOrNull()
        val year = standalone.groupValues[2].toIntOrNull()
        if (month != null && year != null) return StatementPeriod(month, year)
    }

    return null
}
