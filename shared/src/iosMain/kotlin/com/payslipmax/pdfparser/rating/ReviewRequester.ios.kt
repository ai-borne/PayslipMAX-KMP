package com.payslipmax.pdfparser.rating

import platform.StoreKit.SKStoreReviewController
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * `SKStoreReviewController.requestReview()` requires the main thread — dispatched explicitly here
 * rather than assuming the caller (a suspend function's coroutine context) is already on it.
 */
actual fun requestReview() {
    dispatch_async(dispatch_get_main_queue()) {
        SKStoreReviewController.requestReview()
    }
}
