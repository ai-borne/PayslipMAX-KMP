package com.payslipmax.pdfparser.billing

actual fun provideBillingManager(): BillingManager = IosBillingManager()
