package com.ssbmax.pdfparser.auth

/**
 * iOS actual implementation of AuthTokenProvider.
 *
 * Firebase Auth for iOS is not yet integrated in this phase.
 * Returns null → the Cloud Function will return 401, which the app
 * surfaces as an "AI unavailable" error until iOS auth is wired.
 *
 * TODO(Phase 2-iOS): integrate firebase-auth via SPM and return
 * Auth.auth().currentUser?.getIDTokenResult(forcingRefresh: true)
 */
actual class AuthTokenProvider actual constructor() {
    actual suspend fun getIdToken(): String? = null
}
