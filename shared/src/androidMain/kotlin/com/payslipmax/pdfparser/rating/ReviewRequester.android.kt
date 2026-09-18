package com.payslipmax.pdfparser.rating

import android.app.Activity
import com.google.android.play.core.review.ReviewManagerFactory
import com.payslipmax.pdfparser.crypto.ContextHolder

/**
 * `ReviewManager.launchReviewFlow` needs an `Activity`, which nothing in `shared` exposes today
 * (`ContextHolder.context` is Application-scoped only). Mirrors the
 * `AndroidGemmaBaseModelInstaller.confirmationHandler` bridge: `MainActivity.onCreate` sets
 * [activityProvider] once, and this Activity-agnostic module reads it lazily at call time.
 */
object ReviewActivityBridge {
    var activityProvider: (() -> Activity?)? = null
}

actual fun requestReview() {
    val context = ContextHolder.context ?: return
    val activity = ReviewActivityBridge.activityProvider?.invoke() ?: return
    if (activity.isFinishing || activity.isDestroyed) return

    val manager = ReviewManagerFactory.create(context)
    val request = manager.requestReviewFlow()
    request.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            manager.launchReviewFlow(activity, task.result)
        }
        // Silently no-op on failure — Play throttles/declines internally; no user-facing error needed.
    }
}
