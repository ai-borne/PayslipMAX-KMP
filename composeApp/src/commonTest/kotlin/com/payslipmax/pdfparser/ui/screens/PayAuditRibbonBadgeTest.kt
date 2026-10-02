package com.payslipmax.pdfparser.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * The Pay Audit ribbon's badge is the Insights-tab hook for the product's key feature: it must surface
 * real findings, and stay silent on a clean month rather than shout "0 findings" at a user who is fine.
 */
class PayAuditRibbonBadgeTest {
    @Test
    fun aCleanMonthShowsNoBadge() {
        assertNull(payAuditBadgeLabel(0))
    }

    @Test
    fun aNegativeCountIsTreatedAsCleanNotRenderedAsNonsense() {
        assertNull(payAuditBadgeLabel(-1))
    }

    @Test
    fun oneFindingIsSingular() {
        assertEquals("1 finding", payAuditBadgeLabel(1))
    }

    @Test
    fun severalFindingsArePlural() {
        assertEquals("3 findings", payAuditBadgeLabel(3))
    }
}
