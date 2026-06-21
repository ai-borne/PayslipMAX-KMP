package com.ssbmax.pdfparser.auth

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Unit tests for the common-side AuthTokenProvider contract.
 *
 * WHY: Verifies that the expect/actual contract is correct in the common
 * compilation unit. The iOS actual always returns null; the Android actual
 * is covered by instrumentation tests in the Android module.
 *
 * Business contract under test:
 *   - getIdToken() is a suspend function (safe to call from a coroutine)
 *   - It never throws; it returns null on failure / no-user state
 */
class AuthTokenProviderTest {
    @Test
    fun `getIdToken returns null when no user is signed in`() =
        runTest {
            // The commonTest compilation resolves to the iOS actual which always
            // returns null — this tests the contract that null is a valid response
            // and never throws.
            val provider = AuthTokenProvider()
            val token = provider.getIdToken()
            assertNull(token, "Token must be null when no Firebase user is present")
        }

    @Test
    fun `getIdToken does not throw on repeated calls`() =
        runTest {
            val provider = AuthTokenProvider()
            // Call twice — must not throw on either call
            provider.getIdToken()
            provider.getIdToken()
        }
}
