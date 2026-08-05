package com.payslipmax.pdfparser.billing

/**
 * Single Source of Truth (SSOT) data model for In-App Purchase execution results.
 */
sealed class PurchaseResult {
    data class Success(val purchaseToken: String = "") : PurchaseResult()

    data object UserCancelled : PurchaseResult()

    data object Pending : PurchaseResult()

    data class Error(val message: String) : PurchaseResult()
}
