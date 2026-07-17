package com.payslipmax.pdfparser.parser.detection

/**
 * Identifies the document grammar family for PCDA(O) payslips.
 * Replaces abstract versioning with domain-descriptive identifiers.
 */
enum class GrammarFamily {
    /** 2014 Monolithic text statement of account */
    PCDA_LEGACY_STATEMENT,

    /** 2015–2017 Early dual-column format */
    PCDA_EARLY_DUAL_COL,

    /** 2018–Oct 2023 7th CPC key-value text grid */
    PCDA_TRANSITIONAL_7TH_CPC,

    /** Nov 2023–Feb 2025 Standard reconstructed spatial grid */
    PCDA_MODERN_GRID,

    /** Mar 2025+ Extended multi-container grid */
    PCDA_EXTENDED_GRID,

    /** Unclassified or unrecognized document layout */
    UNKNOWN,
}
