package com.payslipmax.pdfparser.telemetry

/** What a failed PDF import records as a non-fatal: a fixed message, never the PDF library's own text (docs/Plan/14a P10). */
class PdfImportFailureEvent : Exception("pdf_import_failed")

/** What a failed Gemma model install records as a non-fatal: a fixed message, never the installer's own text. */
class GemmaInstallFailureEvent : Exception("gemma_install_failed")

/** The parser's own failure codes, the only words of an import failure that leave the device; anything else is [OTHER]. */
object ImportFailureReason {
    const val UNRECOGNIZED_GRAMMAR = "UNRECOGNIZED_GRAMMAR"
    const val PREFLIGHT = "PREFLIGHT_FAILED"
    const val NO_TEXT_TOKENS = "NO_TEXT_TOKENS"
    const val OTHER = "OTHER"

    fun of(message: String?): String =
        when {
            message == null -> OTHER
            message.contains("UNRECOGNIZED_GRAMMAR") -> UNRECOGNIZED_GRAMMAR
            message.contains("PdfPreFlightValidationFailed") -> PREFLIGHT
            message.contains("NO_TEXT_TOKENS") -> NO_TEXT_TOKENS
            else -> OTHER
        }
}

/**
 * Records a failed import with the exception's type and one of the parser's fixed codes, the stage and the file's size, and nothing the
 * exception said: its message can quote the file or a row (docs/Plan/14a P10).
 */
fun CrashReporter.recordPdfImportFailure(
    error: Throwable,
    fileSizeBytes: Int,
) {
    val reason = ImportFailureReason.of(error.message)
    recordException(
        PdfImportFailureEvent(),
        mapOf(
            "operation" to "pdf_import",
            "parser_stage" to if (reason == ImportFailureReason.UNRECOGNIZED_GRAMMAR) "preflight" else "grammar",
            "file_size_bytes" to fileSizeBytes.toString(),
            "error_reason" to reason,
            "error_type" to (error::class.simpleName ?: "unknown"),
        ),
    )
}

/** Records a failed Gemma install with a fixed message and no keys; the installer's own text stays out. */
fun CrashReporter.recordGemmaInstallFailure() {
    recordException(GemmaInstallFailureEvent(), null)
}
