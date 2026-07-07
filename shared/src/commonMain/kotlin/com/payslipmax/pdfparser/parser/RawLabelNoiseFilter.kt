package com.payslipmax.pdfparser.parser

/**
 * Guards [TokenTableClassifier] against prose footer/disclaimer text ("This payslip is computer
 * generated...") that RowPairing can mis-pair with a stray nearby number (e.g. a page marker). No
 * legitimate PCDA line-item label is anywhere near this long — the longest known key in
 * [PayslipPatternConfig] is ~5 words / 25 chars — so an unmatched candidate this long is noise, not
 * a real (if unrecognized) earnings/deductions row.
 *
 * Short prose survives the length check though — e.g. printed seasonal greetings ("Happy New Year
 * 2026", "Merry Christmas", "Wishing you a prosperous Diwali") are only 3-6 words, but their trailing
 * year/date figure is exactly the kind of stray number RowPairing mis-pairs as an amount. Real PCDA
 * codes are terse abbreviations and never contain natural-language function words, so a label with
 * two or more common English stopwords is prose regardless of length.
 */
internal object RawLabelNoiseFilter {
    private const val MAX_UNMATCHED_LABEL_CHARS = 60
    private const val MAX_UNMATCHED_LABEL_WORDS = 8
    private const val MIN_STOPWORDS_FOR_PROSE = 2

    private val ENGLISH_STOPWORDS =
        setOf(
            "a", "an", "the", "and", "for", "to", "of", "on", "in", "with", "you", "your", "we",
            "happy", "merry", "wish", "wishes", "wishing", "greetings", "prosperous", "new", "year",
            "years", "birthday", "festival", "festive", "season", "diwali", "christmas", "eid",
            "holi", "holiday", "regards", "best",
        )

    fun isProseNoise(normalizedLabel: String): Boolean =
        normalizedLabel.length > MAX_UNMATCHED_LABEL_CHARS ||
            wordCount(normalizedLabel) > MAX_UNMATCHED_LABEL_WORDS ||
            stopwordCount(normalizedLabel) >= MIN_STOPWORDS_FOR_PROSE

    private fun wordCount(text: String): Int = text.split(" ").count { it.isNotBlank() }

    private fun stopwordCount(text: String): Int = text.split(" ").count { it in ENGLISH_STOPWORDS }
}
