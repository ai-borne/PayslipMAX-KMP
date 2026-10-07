package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.model.GuideBundle

/** The Claim Guide data the UI reads. Loads the bundled rule cards on first use, never at app launch. */
interface GuideRepository {
    suspend fun load(): GuideLoadResult
}

/** Reads the raw bundle text. Each platform reads the same bundled file; see `GuideModule`. */
fun interface GuideBundleSource {
    suspend fun readText(): String
}

sealed interface GuideLoadResult {
    data class Loaded(
        val bundle: GuideBundle,
    ) : GuideLoadResult

    /** Only [error] may leave the device (as a telemetry code); never card text or ids. */
    data class Failed(
        val error: GuideLoadError,
    ) : GuideLoadResult
}

enum class GuideLoadError {
    READ_FAILED,
    TOO_LARGE,
    MALFORMED,
    UNSUPPORTED_VERSION,
    INVALID,
}
