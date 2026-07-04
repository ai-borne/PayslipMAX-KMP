package com.ssbmax.pdfparser.insights.gemma

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemmaModelVersionManagerTest {
    private fun jsonClient(engine: MockEngine) =
        HttpClient(engine) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        }

    private fun respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, "application/json")) }

    @Test
    fun fetchManifestParsesValidResponse() =
        runTest {
            val engine =
                respondJson(
                    """
                    {"success":true,"manifest":{"version":"v1","url":"https://cdn/x.litertlm",
                    "sha256":"abc123","noticeText":"Gemma terms","noticeUrl":"https://terms"}}
                    """.trimIndent(),
                )
            val manager = GemmaModelVersionManager(jsonClient(engine), interimKey = "secret")

            val result = manager.fetchManifest()

            assertTrue(result.isSuccess)
            val manifest = result.getOrThrow()
            assertEquals("v1", manifest.version)
            assertEquals("https://cdn/x.litertlm", manifest.url)
            assertEquals("abc123", manifest.sha256)
            assertEquals("Gemma terms", manifest.noticeText)
        }

    @Test
    fun fetchManifestAttachesInterimKeyHeader() =
        runTest {
            var sentKey: String? = null
            val engine =
                MockEngine { request ->
                    sentKey = request.headers[GemmaModelVersionManager.INTERIM_KEY_HEADER]
                    respond(
                        """{"success":true,"manifest":{"version":"v1","url":"u","sha256":"s"}}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            GemmaModelVersionManager(jsonClient(engine), interimKey = "my-key").fetchManifest()

            assertEquals("my-key", sentKey)
        }

    @Test
    fun fetchManifestOmitsHeaderWhenNoKey() =
        runTest {
            var sentKey: String? = "unset"
            val engine =
                MockEngine { request ->
                    sentKey = request.headers[GemmaModelVersionManager.INTERIM_KEY_HEADER]
                    respond(
                        """{"success":true,"manifest":{"version":"v1","url":"u","sha256":"s"}}""",
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            GemmaModelVersionManager(jsonClient(engine), interimKey = null).fetchManifest()

            assertNull(sentKey)
        }

    @Test
    fun fetchManifestFailsOnForbiddenInvalidKey() =
        runTest {
            // The server rejects a bad interim key with 403 + {success:false}; the manager surfaces
            // that as a failure rather than a bogus manifest.
            val engine =
                respondJson(
                    """{"success":false,"error":"Forbidden: invalid access key."}""",
                    HttpStatusCode.Forbidden,
                )
            val result = GemmaModelVersionManager(jsonClient(engine), interimKey = "wrong").fetchManifest()

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Forbidden") == true)
        }

    @Test
    fun fetchManifestFailsOnMalformedResponse() =
        runTest {
            val result =
                GemmaModelVersionManager(jsonClient(respondJson("this is not json {{{")))
                    .fetchManifest()

            assertTrue(result.isFailure)
        }
}
