package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.insights.timeline.PayMonth
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The selected month and tab must survive the system recreating the app (low memory, "don't keep activities"),
 * or the user loses their place mid-audit. A saved list that is stale or corrupt must fall back to the
 * defaults rather than crash or open on a nonsense month.
 */
class PayAuditSavedStateTest {
    @Test
    fun roundTripsTheMonthAndTab() {
        val state = PayAuditSavedState(PayMonth(2026, 7), PayAuditTab.HISTORY)
        assertEquals(state, PayAuditSavedState.fromList(state.toList()))
    }

    @Test
    fun roundTripsNoMonthYet() {
        val state = PayAuditSavedState(null, PayAuditTab.PLAN_AHEAD)
        assertEquals(state, PayAuditSavedState.fromList(state.toList()))
    }

    @Test
    fun aCorruptOrFutureListFallsBackToTheDefaults() {
        assertEquals(PayAuditSavedState(), PayAuditSavedState.fromList(emptyList()))
        assertEquals(PayAuditSavedState(), PayAuditSavedState.fromList(listOf("x", 1)))
        assertEquals(PayAuditSavedState(null, PayAuditTab.THIS_MONTH), PayAuditSavedState.fromList(listOf(-1, 99)))
    }
}
