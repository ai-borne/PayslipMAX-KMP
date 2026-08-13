package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.billing.PurchaseResult
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Guards the upgrade sheet's reaction to every [PurchaseResult]: a successful purchase must close the
 * sheet, while a cancelled/pending/failed attempt must leave it open so the user isn't left wondering
 * whether payment went through. Before this was extracted, the sheet dismissed unconditionally on tap,
 * discarding the result and giving the user zero feedback on failure.
 */
class PurchaseSheetOutcomeTest {
    @Test
    fun successDismissesTheSheet() {
        assertEquals(PurchaseSheetOutcome.Dismiss, purchaseSheetOutcome(PurchaseResult.Success()))
    }

    @Test
    fun userCancelledStaysOpenWithNoError() {
        assertEquals(PurchaseSheetOutcome.StayOpen, purchaseSheetOutcome(PurchaseResult.UserCancelled))
    }

    @Test
    fun pendingStaysOpenWithNoError() {
        assertEquals(PurchaseSheetOutcome.StayOpen, purchaseSheetOutcome(PurchaseResult.Pending))
    }

    @Test
    fun errorStaysOpenAndSurfacesTheMessage() {
        val outcome = purchaseSheetOutcome(PurchaseResult.Error("Package yearly unavailable"))
        check(outcome is PurchaseSheetOutcome.ShowError)
        assertEquals("Purchase failed: Package yearly unavailable", outcome.message)
    }
}
