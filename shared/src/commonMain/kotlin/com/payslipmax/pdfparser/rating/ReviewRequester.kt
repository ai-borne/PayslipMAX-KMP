package com.payslipmax.pdfparser.rating

/**
 * Triggers the platform-native in-app review prompt (Play In-App Review on Android,
 * `SKStoreReviewController` on iOS). Both are OS-rendered dialogs with no copy or layout this app
 * controls, and both self-throttle server-side — a call here is a request, not a guarantee.
 */
expect fun requestReview()
