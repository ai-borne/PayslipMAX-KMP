package com.payslipmax.pdfparser.billing

/**
 * Expect platform factory for providing platform-specific [BillingManager].
 */
expect fun provideBillingManager(): BillingManager
