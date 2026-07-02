package com.ssbmax.pdfparser.parser

/**
 * Guards [TokenTableClassifier] against prose footer/disclaimer text ("This payslip is computer
 * generated...") that RowPairing can mis-pair with a stray nearby number (e.g. a page marker). No
 * legitimate PCDA line-item label is anywhere near this long — the longest known key in
 * [PayslipPatternConfig] is ~5 words / 25 chars — so an unmatched candidate this long is noise, not
 * a real (if unrecognized) earnings/deductions row.
 */
internal object RawLabelNoiseFilter {
    private const val MAX_UNMATCHED_LABEL_CHARS = 60
    private const val MAX_UNMATCHED_LABEL_WORDS = 8

    fun isProseNoise(normalizedLabel: String): Boolean =
        normalizedLabel.length > MAX_UNMATCHED_LABEL_CHARS || wordCount(normalizedLabel) > MAX_UNMATCHED_LABEL_WORDS

    private fun wordCount(text: String): Int = text.split(" ").count { it.isNotBlank() }
}
