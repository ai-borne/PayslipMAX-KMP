package com.ssbmax.pdfparser.insights

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NetworkErrorMapperTest {

    @Test
    fun testSocketTimeoutMapping() {
        val exception = SocketTimeoutException("Socket timeout has expired [url=https://test.com, socket_timeout=unknown] ms")
        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("The server is taking too long to respond. Please check your internet connection and try again.", message)
    }

    @Test
    fun testConnectTimeoutMapping() {
        val exception = ConnectTimeoutException("https://test.com", null)
        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("The server is taking too long to respond. Please check your internet connection and try again.", message)
    }

    @Test
    fun testHttpRequestTimeoutMapping() {
        val exception = HttpRequestTimeoutException("https://test.com", null)
        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("The server is taking too long to respond. Please check your internet connection and try again.", message)
    }

    @Test
    fun testResponseExceptionUnauthorized() = runTest {
        val mockEngine = MockEngine { _ ->
            respond("Unauthorized", status = HttpStatusCode.Unauthorized)
        }
        val client = HttpClient(mockEngine) {
            expectSuccess = true
        }
        val exception = try {
            client.get("https://test.com")
            throw AssertionError("Should have failed")
        } catch (e: Exception) {
            e
        }

        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("Unauthorized access. Please log in again.", message)
    }

    @Test
    fun testResponseExceptionNotFound() = runTest {
        val mockEngine = MockEngine { _ ->
            respond("Not Found", status = HttpStatusCode.NotFound)
        }
        val client = HttpClient(mockEngine) {
            expectSuccess = true
        }
        val exception = try {
            client.get("https://test.com")
            throw AssertionError("Should have failed")
        } catch (e: Exception) {
            e
        }

        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("The requested service was not found.", message)
    }

    @Test
    fun testResponseExceptionInternalServerError() = runTest {
        val mockEngine = MockEngine { _ ->
            respond("Internal Server Error", status = HttpStatusCode.InternalServerError)
        }
        val client = HttpClient(mockEngine) {
            expectSuccess = true
        }
        val exception = try {
            client.get("https://test.com")
            throw AssertionError("Should have failed")
        } catch (e: Exception) {
            e
        }

        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("Our servers are experiencing issues. Please try again later.", message)
    }

    @Test
    fun testUnknownHostExceptionMapping() {
        val exception = Exception("java.net.UnknownHostException: Unable to resolve host test.com: No address associated with hostname")
        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("Cannot connect to the server. Please check your internet connection and try again.", message)
    }

    @Test
    fun testGenericTimeoutExceptionMapping() {
        val exception = Exception("Something timed out here")
        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("The request timed out. Please try again.", message)
    }

    @Test
    fun testGenericFallbackExceptionMapping() {
        val exception = Exception("Random unexpected database failure")
        val message = NetworkErrorMapper.getUserFriendlyMessage(exception)
        assertEquals("Unable to generate AI insights due to a connection issue. Please check your network and try again.", message)
    }
}
