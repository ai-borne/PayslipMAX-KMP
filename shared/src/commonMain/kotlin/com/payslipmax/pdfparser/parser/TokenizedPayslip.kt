package com.payslipmax.pdfparser.parser

/**
 * The new platform-independent IR contract (Phase 2): both Android and iOS emit an identical
 * [TokenizedPayslip] so that all grid reconstruction, row pairing, classification, and reconciliation
 * can live in common code and be unit-tested without a device.
 *
 * Tokens are pre-grouped per *classified page* — table, tax, and DSOP — rather than cropped into
 * `leftColumnText` / `middleColumnText` strings by guessed geometry. This permanently removes the
 * historical iOS/Android divergence and the brittle per-month geometry patches.
 *
 * All token coordinates follow the single top-down convention documented on [PositionedToken]
 * (origin top-left, y growing downward); the iOS adapter normalizes PDFKit's bottom-up bounds before
 * the tokens reach common code.
 *
 * The old string pipeline ([PayslipTextParser.parse]) remains the default path until the Phase 4
 * cut-over; this contract is populated and validated independently in the interim.
 */
data class TokenizedPayslip(
    /** Word tokens from the earnings/deductions table page (contains BPAY / Basic Pay). */
    val tableTokens: List<PositionedToken>,
    /** Word tokens from the income-tax details page, empty if not present. */
    val taxTokens: List<PositionedToken>,
    /** Word tokens from the DSOP-fund page; falls back to the tax page when no distinct DSOP page. */
    val dsopTokens: List<PositionedToken>,
    /** Concatenated plain text of all pages, retained for metadata (officer, dates) parsing. */
    val fullText: String,
)
