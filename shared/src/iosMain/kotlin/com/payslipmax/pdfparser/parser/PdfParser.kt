@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.gemma.GemmaEngine
import com.payslipmax.pdfparser.insights.gemma.GemmaEngineConfig
import com.payslipmax.pdfparser.insights.gemma.GemmaModelStorageManager
import com.payslipmax.pdfparser.insights.gemma.fileExistsAt
import com.payslipmax.pdfparser.insights.gemma.gemmaModelStorageDir
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument

actual class PlatformPdfParser actual constructor() : PdfParser {
    actual override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> {
        // Phase 4 cut-over: the token IR is now the primary path. The previous PDFKit column-crop
        // (extractTextSpatially + IosLayoutScanner) diverged per-platform and per-month; it is replaced
        // by the shared common engine (GrammarAwareParser), so iOS and Android parse identically.
        return extractTokens(pdfBytes, password, filename).mapCatching { tokenized ->
            // Same GemmaEngine instance backs both Tier 6 fallback and the Tier 6 diagnostic pass —
            // GemmaEngine opens a fresh stateless Conversation per call, so this is free of extra model-load cost.
            val gemmaEngine = buildGemmaEngine()
            GrammarAwareParser.parse(
                tokenized,
                filename,
                fallbackExtractor = gemmaEngine?.let { GemmaFallbackExtractor(gemmaEngine = it) },
                diagnosticExtractor = gemmaEngine?.let { GemmaDiagnosticExtractor(gemmaEngine = it) },
            ).getOrThrow()
        }
    }

    /**
     * Constructs the shared Tier 6 Gemma engine iff the active-slot model file is present on disk,
     * mirroring the Android [PlatformPdfParser] gating. Resolution goes through the shared
     * [GemmaModelStorageManager] SSOT (Phase 1) so both platforms load the exact same active slot; the
     * actual inference runs in the Swift LiteRT-LM bridge registered on [GemmaEngine.inferenceDelegate].
     * Returns null (Tier 6 stays on standby) when the model has not been downloaded, so a fresh install
     * never blocks on it.
     */
    private fun buildGemmaEngine(): GemmaEngine? {
        return try {
            val storageDir = gemmaModelStorageDir()
            if (storageDir.isEmpty()) return null
            val modelPath = "$storageDir/${GemmaModelStorageManager().getRecommendedModelFileName()}"
            if (!fileExistsAt(modelPath)) return null
            GemmaEngine(GemmaEngineConfig(modelPath = modelPath))
        } catch (e: Throwable) {
            null
        }
    }

    actual override fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip> {
        return try {
            val nsData =
                pdfBytes.usePinned { pinned ->
                    NSData.create(
                        bytes = pinned.addressOf(0),
                        length = pdfBytes.size.toULong(),
                    )
                }

            val pdfDoc = PDFDocument(data = nsData)
            if (pdfDoc.isEncrypted) {
                val success = pdfDoc.unlockWithPassword(password)
                if (!success) {
                    return Result.failure(Exception("Failed to decrypt PDF: Incorrect password"))
                }
            }

            Result.success(extractTokenized(pdfDoc))
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
