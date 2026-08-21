package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.billing.PurchaseResult
import com.payslipmax.pdfparser.ui.theme.AppStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PremiumUpgradeBottomSheetTest {
    @Test
    fun purchaseSheetOutcome_dismisses_on_success() {
        val result = PurchaseResult.Success("tok_123")
        val outcome = purchaseSheetOutcome(result)
        assertEquals(PurchaseSheetOutcome.Success(AppStrings.statusPurchaseSuccess), outcome)
    }

    @Test
    fun purchaseSheetOutcome_stays_open_on_cancel_and_pending() {
        assertEquals(PurchaseSheetOutcome.StayOpen, purchaseSheetOutcome(PurchaseResult.UserCancelled))
        assertEquals(PurchaseSheetOutcome.StayOpen, purchaseSheetOutcome(PurchaseResult.Pending))
    }

    @Test
    fun purchaseSheetOutcome_shows_error_on_failure() {
        val outcome = purchaseSheetOutcome(PurchaseResult.Error("Card declined"))
        assertTrue(outcome is PurchaseSheetOutcome.ShowError)
        assertEquals("${AppStrings.statusPurchaseFailed}Card declined", outcome.message)
    }

    @Test
    fun restoreSheetOutcome_dismisses_on_success() {
        val result = PurchaseResult.Success("tok_123")
        val outcome = restoreSheetOutcome(result)
        assertEquals(PurchaseSheetOutcome.Success(AppStrings.statusRestorePurchasesSuccess), outcome)
    }

    @Test
    fun restoreSheetOutcome_stays_open_on_cancel_and_pending() {
        assertEquals(PurchaseSheetOutcome.StayOpen, restoreSheetOutcome(PurchaseResult.UserCancelled))
        assertEquals(PurchaseSheetOutcome.StayOpen, restoreSheetOutcome(PurchaseResult.Pending))
    }

    @Test
    fun restoreSheetOutcome_shows_error_on_failure() {
        val outcome = restoreSheetOutcome(PurchaseResult.Error("No active verified subscription found"))
        assertTrue(outcome is PurchaseSheetOutcome.ShowError)
        assertEquals("${AppStrings.statusRestorePurchasesFailed}No active verified subscription found", outcome.message)
    }
}
