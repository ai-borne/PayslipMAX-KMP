package com.payslipmax.pdfparser.parser

/**
 * Single source of truth for classifying a PDF page from its plain text, used by both platform token
 * adapters (Phase 2) so Android and iOS agree on which page is the table / tax / DSOP page.
 *
 * The keyword sets mirror the landmark detection the legacy string pipeline performs inline; keeping
 * them here as one pure, common, unit-testable function removes the cross-platform drift that used to
 * make iOS and Android disagree on page selection.
 */
object PageClassifier {
    /** Earnings/deductions table page — the one carrying BPAY / Basic Pay. */
    fun isTablePage(pageText: String): Boolean {
        val t = pageText.lowercase()
        return t.contains("bpay") || t.contains("basic pay")
    }

    /** Income-tax details page. */
    fun isTaxPage(pageText: String): Boolean {
        val t = pageText.lowercase()
        return t.contains("standard deduction") ||
            t.contains("taxable income") ||
            t.contains("tax payable") ||
            t.contains("income tax deducted")
    }

    /** DSOP-fund page. */
    fun isDsopPage(pageText: String): Boolean {
        val t = pageText.lowercase()
        return t.contains("dsop fund") ||
            (
                t.contains("opening balance") &&
                    t.contains("closing balance") &&
                    t.contains("subscription")
            )
    }
}
