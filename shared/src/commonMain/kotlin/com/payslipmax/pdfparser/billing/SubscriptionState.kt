package com.payslipmax.pdfparser.billing

/**
 * Domain model representing the current user's entitlement state.
 */
sealed class SubscriptionState {
    data class Active(
        val expirationTimestampMs: Long = 0L,
        val autoRenewing: Boolean = true,
    ) : SubscriptionState()

    data object Inactive : SubscriptionState()

    data object Unknown : SubscriptionState()
}
