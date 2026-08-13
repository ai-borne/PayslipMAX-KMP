package com.payslipmax.pdfparser.billing

/**
 * The RevenueCat public SDK key, safe to embed in client code (RevenueCat's own guidance — same
 * "public, not secret" class as a Firebase apiKey in google-services.json). Currently the Phase 0
 * RevenueCat Test Store key for both platforms; swap for the real per-platform public keys once
 * Play Console/App Store Connect are linked in RevenueCat, before Phase 5 device verification.
 */
expect fun revenueCatApiKey(): String
