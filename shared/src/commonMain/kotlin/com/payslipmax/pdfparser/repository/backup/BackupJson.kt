package com.payslipmax.pdfparser.repository.backup

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * The one reader (SSOT, F2, docs/Plan/14a P2) for everything a `.pcda` archive carries — the document, payslip payloads, corrections,
 * Guide notes, Part II rows and the page archive's trailer — and for the same payloads in the device database, so a row a later app
 * version wrote still reads. It tolerates what a later writer may do: add a key, write null where a default exists, use an enum value
 * this version does not know (the default is taken), or leave out a nullable field.
 *
 * Decode-only on purpose (owner decision, P2): writers keep the plain `Json` encoder, which writes every null, because shipped apps read
 * a payslip payload strictly and fail on a missing field (BackupWriterCompatibilityTest). Fields without a default: BackupDefaultsGuardTest.
 */
object BackupJson {
    @OptIn(ExperimentalSerializationApi::class)
    private val json =
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
            explicitNulls = false
        }

    fun <T> decode(
        deserializer: DeserializationStrategy<T>,
        text: String,
    ): T = json.decodeFromString(deserializer, text)

    inline fun <reified T> decode(text: String): T = decode(serializer<T>(), text)
}
