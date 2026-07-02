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
import kotlin.test.assertTrue

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
            val downloadManager = GemmaModelDownloadManager(httpClient)

            val states = mutableListOf<DownloadStatus>()
            downloadManager.downloadModel(
                url = "https://example.com/gemma.task",
                destinationPath = "test_gemma.task",
            ).toList(states)

            assertTrue(states.first() is DownloadStatus.Idle)
            assertTrue(states.any { it is DownloadStatus.Downloading })
            val lastState = states.last()
            assertTrue(lastState is DownloadStatus.Success, "Expected Success but got $lastState")
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
