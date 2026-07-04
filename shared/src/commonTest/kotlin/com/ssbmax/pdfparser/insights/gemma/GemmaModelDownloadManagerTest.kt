package com.ssbmax.pdfparser.insights.gemma

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FakeModelSink : ModelSink {
    val received = mutableListOf<Byte>()
    var closed = false

    override fun append(bytes: ByteArray) {
        received.addAll(bytes.toList())
    }

    override fun close() {
        closed = true
    }
}

class GemmaModelDownloadManagerTest {
    @Test
    fun testDownloadSuccessEmitsProgressAndSuccess() =
        runTest {
            val mockData = ByteArray(1024 * 10) { 1 } // 10 KB dummy payload
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = mockData,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentLength, mockData.size.toString()),
                    )
                }
            val httpClient = HttpClient(mockEngine)
            val fakeSink = FakeModelSink()
            val downloadManager = GemmaModelDownloadManager(httpClient, sinkFactory = { fakeSink })

            val states = mutableListOf<DownloadStatus>()
            downloadManager.downloadModel(
                url = "https://example.com/gemma.task",
                destinationPath = "test_gemma.task",
            ).toList(states)

            assertTrue(states.first() is DownloadStatus.Idle)
            assertTrue(states.any { it is DownloadStatus.Downloading })
            val lastState = states.last()
            assertTrue(lastState is DownloadStatus.Success, "Expected Success but got $lastState")

            // The regression this test guards: downloadModel used to discard every downloaded byte
            // and still emit Success, so a "successful" download produced no usable model file.
            assertEquals(mockData.size, fakeSink.received.size)
            assertTrue(fakeSink.closed)
        }

    @Test
    fun testDownloadAttachesProvidedHeaders() =
        runTest {
            // Callers may attach the interim shared-key header; the download manager forwards it
            // verbatim without taking on any auth responsibility of its own.
            var sentHeader: String? = null
            val mockEngine =
                MockEngine { request ->
                    sentHeader = request.headers["x-gemma-cache-key"]
                    respond(
                        content = ByteArray(8) { 1 },
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentLength, "8"),
                    )
                }
            val downloadManager =
                GemmaModelDownloadManager(HttpClient(mockEngine), sinkFactory = { FakeModelSink() })

            downloadManager.downloadModel(
                url = "https://example.com/gemma.litertlm",
                destinationPath = "test_gemma.litertlm",
                headers = mapOf("x-gemma-cache-key" to "abc"),
            ).toList(mutableListOf())

            assertEquals("abc", sentHeader)
        }

    @Test
    fun testDownloadFailureEmitsError() =
        runTest {
            val mockEngine =
                MockEngine { request ->
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound,
                    )
                }
            val httpClient = HttpClient(mockEngine)
            val downloadManager = GemmaModelDownloadManager(httpClient)

            val states = mutableListOf<DownloadStatus>()
            downloadManager.downloadModel(
                url = "https://example.com/missing.task",
                destinationPath = "test_gemma.task",
            ).toList(states)

            val lastState = states.last()
            assertTrue(lastState is DownloadStatus.Error, "Expected Error but got $lastState")
        }
}
