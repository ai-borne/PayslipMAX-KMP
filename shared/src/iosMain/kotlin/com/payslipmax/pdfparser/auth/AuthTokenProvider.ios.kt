package com.payslipmax.pdfparser.auth

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * iOS actual implementation of AuthTokenProvider.
 *
 * Firebase Auth for iOS is integrated using a bridge delegate. The native iOS
 * Swift application registers its token retrieval callback, keeping the shared
 * Kotlin library free of direct compile-time binary dependencies on native Cocoa SDKs.
 */
actual class AuthTokenProvider actual constructor() {
    actual suspend fun getIdToken(): String? {
        val delegate = tokenProviderDelegate ?: return null
        return suspendCoroutine { continuation ->
            delegate { token ->
                continuation.resume(token)
            }
        }
    }

    companion object {
        /**
         * Delegate closure registered by Swift entry point.
         * Takes a completion block: (String?) -> Unit
         */
        var tokenProviderDelegate: (((String?) -> Unit) -> Unit)? = null
    }
}
