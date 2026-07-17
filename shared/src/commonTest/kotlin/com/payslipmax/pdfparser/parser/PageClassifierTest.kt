package com.payslipmax.pdfparser.parser

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Phase 2: the shared page classifier is the SSOT both platform token adapters use, so it must agree
 * on table / tax / DSOP regardless of casing and across the variations seen in the corpus.
 */
class PageClassifierTest {
    @Test
    fun `table page detected from BPAY or Basic Pay case-insensitively`() {
        assertTrue(PageClassifier.isTablePage("Earnings BPAY 56100 DA 9876"))
        assertTrue(PageClassifier.isTablePage("basic pay and allowances"))
        assertFalse(PageClassifier.isTablePage("Income Tax Details for the year"))
    }

    @Test
    fun `tax page detected from any income-tax landmark`() {
        assertTrue(PageClassifier.isTaxPage("Standard Deduction 50000"))
        assertTrue(PageClassifier.isTaxPage("TAXABLE INCOME 1200000"))
        assertTrue(PageClassifier.isTaxPage("Tax Payable 90000"))
        assertTrue(PageClassifier.isTaxPage("Income Tax Deducted 7500"))
        assertFalse(PageClassifier.isTaxPage("DSOP FUND opening balance"))
    }

    @Test
    fun `dsop page detected from DSOP fund or the balance triad`() {
        assertTrue(PageClassifier.isDsopPage("DSOP FUND details"))
        assertTrue(
            PageClassifier.isDsopPage("Opening Balance 100 Subscription 50 Closing Balance 150"),
        )
        // Balance triad must be complete — a lone opening balance is not enough.
        assertFalse(PageClassifier.isDsopPage("Opening Balance 100 only"))
        assertFalse(PageClassifier.isDsopPage("Earnings BPAY 56100"))
    }
}
