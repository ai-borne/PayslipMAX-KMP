package com.payslipmax.pdfparser.insights.gemma

import com.payslipmax.pdfparser.logging.Logger
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The version manifest the client checks before deciding whether to (re)download the offline Gemma
 * model. Mirrors the backend `buildManifest` shape (functions/gemmaManifest.js). The Gemma
 * Terms-of-Use notice travels with it so the license redistribution requirement can be surfaced in
 * the UI at/before download.
 */
@Serializable
data class GemmaModelManifest(
    val version: String,
    val url: String,
    val sha256: String,
    val noticeText: String? = null,
    val noticeUrl: String? = null,
)

@Serializable
private data class ManifestEnvelope(
    val success: Boolean = false,
    val manifest: GemmaModelManifest? = null,
    val error: String? = null,
)

/**
 * Fetches the version manifest — the **single place** that knows "what version is current" (SSOT).
 * Reuses the same Ktor `HttpClient` shape as [com.payslipmax.pdfparser.insights.GeminiProxyService] and
 * attaches the interim shared-key header (Decision 3: a documented, temporary safeguard, weaker than
 * real Firebase App Check, until both apps are enrolled in Play Console / App Store Connect).
 *
 * The [client] must have JSON content negotiation installed (see di/Koin.kt).
 */
open class GemmaModelVersionManager(
    private val client: HttpClient = defaultManifestClient(),
    private val manifestUrl: String = DEFAULT_MANIFEST_URL,
    private val interimKey: String? = null,
) {
    open suspend fun fetchManifest(): Result<GemmaModelManifest> {
        return try {
            val envelope: ManifestEnvelope =
                client.get {
                    url(manifestUrl)
                    if (!interimKey.isNullOrEmpty()) {
                        headers { append(INTERIM_KEY_HEADER, interimKey) }
                    }
                }.body()

            if (envelope.success && envelope.manifest != null) {
                Result.success(envelope.manifest)
            } else {
                Result.failure(Exception(envelope.error ?: "Manifest fetch failed: unexpected response."))
            }
        } catch (e: Exception) {
            Logger.e("GemmaModelVersionManager", "fetchManifest failed calling $manifestUrl", e)
            Result.failure(e)
        }
    }

    companion object {
        /** Interim shared-key header validated server-side (matches functions/gemmaModelConfig.js). */
        const val INTERIM_KEY_HEADER = "x-gemma-cache-key"

        /** Firebase-derived endpoint path (see Phase 0 handoff: export name → path). */
        const val DEFAULT_MANIFEST_URL =
            "https://us-central1-payslip-app-475e1.cloudfunctions.net/gemmaModelManifest"

        /**
         * JSON-capable client for production use when no client is injected. Mirrors the content
         * negotiation config in di/Koin.kt (the download manager likewise self-provides its client).
         */
        private fun defaultManifestClient(): HttpClient =
            HttpClient {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            }
    }
}
