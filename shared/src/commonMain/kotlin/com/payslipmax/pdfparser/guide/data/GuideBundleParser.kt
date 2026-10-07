package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.guide.GuideLoadError
import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.model.GuideBundle
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * Turns the bundle text into a validated [GuideBundle], or an error state. Never throws: a broken bundle
 * must show an error with retry, never a crash or a blank tab. The version is read before the typed decode,
 * so a newer major is reported as such even when its shape no longer matches these models.
 */
object GuideBundleParser {
    /** Must equal `BUNDLE_VERSION` in `docs/Plan/rule_cards/tools/config.py`. */
    const val SUPPORTED_MAJOR = 1

    /** About 8x the 2026 bundle (252 KB); anything larger is not a bundle we built. */
    const val MAX_CHARS = 2_000_000

    fun parse(text: String): GuideLoadResult {
        if (text.length > MAX_CHARS) return GuideLoadResult.Failed(GuideLoadError.TOO_LARGE)
        val root = parseRoot(text) ?: return GuideLoadResult.Failed(GuideLoadError.MALFORMED)
        val version = (root["version"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
        val error =
            when {
                version == null -> GuideLoadError.MALFORMED
                version !in 1..SUPPORTED_MAJOR -> GuideLoadError.UNSUPPORTED_VERSION
                GuideBundleValidator.hasInternalFields(root) -> GuideLoadError.INVALID
                else -> null
            }
        if (error != null) return GuideLoadResult.Failed(error)
        val bundle = decode(root) ?: return GuideLoadResult.Failed(GuideLoadError.MALFORMED)
        return if (GuideBundleValidator.validate(bundle).isEmpty()) {
            GuideLoadResult.Loaded(bundle)
        } else {
            GuideLoadResult.Failed(GuideLoadError.INVALID)
        }
    }

    private fun parseRoot(text: String): JsonObject? =
        try {
            GuideJson.parseToJsonElement(text) as? JsonObject
        } catch (e: SerializationException) {
            null
        }

    private fun decode(root: JsonObject): GuideBundle? =
        try {
            GuideJson.decodeFromJsonElement(GuideBundle.serializer(), root)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}
