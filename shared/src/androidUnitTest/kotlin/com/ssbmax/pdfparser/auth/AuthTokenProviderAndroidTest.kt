package com.ssbmax.pdfparser.auth

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FakeFirebaseAuthWrapper : FirebaseAuthWrapper {
    override var currentUserExists: Boolean = false
    var signInCalledCount = 0
    var signInShouldThrow = false
    var tokenToReturn: String? = "mock-token"
    var tokenShouldThrow = false
    var forceRefreshPassed: Boolean? = null

    override suspend fun signInAnonymously() {
        if (signInShouldThrow) {
            throw Exception("Sign in failed")
        }
        signInCalledCount++
        currentUserExists = true
    }

    override suspend fun getUserIdToken(forceRefresh: Boolean): String? {
        if (tokenShouldThrow) {
            throw Exception("Token fetch failed")
        }
        forceRefreshPassed = forceRefresh
        return tokenToReturn
    }
}

class AuthTokenProviderAndroidTest {

    private lateinit var fakeWrapper: FakeFirebaseAuthWrapper

    @Before
    fun setUp() {
        fakeWrapper = FakeFirebaseAuthWrapper()
        AuthTokenProvider.authWrapper = fakeWrapper
    }

    @After
    fun tearDown() {
        AuthTokenProvider.authWrapper = DefaultFirebaseAuthWrapper()
    }

    @Test
    fun testGetIdToken_whenUserAlreadySignedIn_returnsTokenWithoutSigningInAgain() = runTest {
        fakeWrapper.currentUserExists = true
        fakeWrapper.tokenToReturn = "existing-token"

        val provider = AuthTokenProvider()
        val token = provider.getIdToken()

        assertEquals("existing-token", token)
        assertEquals(0, fakeWrapper.signInCalledCount)
        assertEquals(true, fakeWrapper.forceRefreshPassed)
    }

    @Test
    fun testGetIdToken_whenUserNotSignedIn_signsInAnonymouslyAndReturnsToken() = runTest {
        fakeWrapper.currentUserExists = false
        fakeWrapper.tokenToReturn = "new-token"

        val provider = AuthTokenProvider()
        val token = provider.getIdToken()

        assertEquals("new-token", token)
        assertEquals(1, fakeWrapper.signInCalledCount)
        assertEquals(true, fakeWrapper.forceRefreshPassed)
    }

    @Test
    fun testGetIdToken_whenSignInThrows_returnsNullGracefully() = runTest {
        fakeWrapper.currentUserExists = false
        fakeWrapper.signInShouldThrow = true

        val provider = AuthTokenProvider()
        val token = provider.getIdToken()

        assertNull(token)
    }

    @Test
    fun testGetIdToken_whenTokenFetchThrows_returnsNullGracefully() = runTest {
        fakeWrapper.currentUserExists = true
        fakeWrapper.tokenShouldThrow = true

        val provider = AuthTokenProvider()
        val token = provider.getIdToken()

        assertNull(token)
    }
}
