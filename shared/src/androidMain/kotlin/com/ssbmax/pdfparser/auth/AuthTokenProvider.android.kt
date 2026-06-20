package com.ssbmax.pdfparser.auth

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * Android actual implementation of AuthTokenProvider.
 *
 * Fetches the Firebase ID token for the currently signed-in user.
 * Force-refreshes every call (forceRefresh = true) so the token
 * is always valid when it reaches the Cloud Function (tokens expire in 1h).
 *
 * Returns null when:
 *  - No user is signed in (anonymous / first-launch)
 *  - Firebase Auth is not yet initialized (should not happen with google-services.json)
 *
 * The Cloud Function returns HTTP 401 when the token is null or invalid.
 */
interface FirebaseAuthWrapper {
    val currentUserExists: Boolean
    suspend fun signInAnonymously()
    suspend fun getUserIdToken(forceRefresh: Boolean): String?
}

class DefaultFirebaseAuthWrapper : FirebaseAuthWrapper {
    override val currentUserExists: Boolean
        get() = Firebase.auth.currentUser != null

    override suspend fun signInAnonymously() {
        Firebase.auth.signInAnonymously().await()
    }

    override suspend fun getUserIdToken(forceRefresh: Boolean): String? {
        return Firebase.auth.currentUser
            ?.getIdToken(forceRefresh)
            ?.await()
            ?.token
    }
}

actual class AuthTokenProvider actual constructor() {
    actual suspend fun getIdToken(): String? =
        try {
            if (!authWrapper.currentUserExists) {
                authWrapper.signInAnonymously()
            }
            authWrapper.getUserIdToken(forceRefresh = true)
        } catch (e: Exception) {
            null
        }

    companion object {
        var authWrapper: FirebaseAuthWrapper = DefaultFirebaseAuthWrapper()
    }
}

