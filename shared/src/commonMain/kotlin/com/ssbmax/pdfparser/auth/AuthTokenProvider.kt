package com.ssbmax.pdfparser.auth

/**
 * AuthTokenProvider
 *
 * SSOT for the Firebase ID token used to authenticate calls to the
 * generateInsights Cloud Function.
 *
 * The `expect` declaration lives in commonMain so the domain and repository
 * layers can depend on this abstraction with zero Android imports.
 * Platform-specific `actual` implementations provide the real token.
 */
expect class AuthTokenProvider() {
    /**
     * Returns the current user's Firebase ID token, or null if the user is
     * not authenticated (anonymous / guest mode — function may still respond
     * but the token will be missing and the server will return 401).
     *
     * Suspends until the token is fetched. Throws on network error.
     */
    suspend fun getIdToken(): String?
}
