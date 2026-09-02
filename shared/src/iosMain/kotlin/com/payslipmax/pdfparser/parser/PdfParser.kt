@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.gemma.GemmaEngine
import com.payslipmax.pdfparser.insights.gemma.GemmaEngineConfig
import com.payslipmax.pdfparser.insights.gemma.resolveInstalledGemmaModelPath
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument

actual class PlatformPdfParser actual constructor() : PdfParser {
    actual override suspend fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> {
        return extractTokens(pdfBytes, password, filename).mapCatching { tokenized ->
            val gemmaEngine = buildGemmaEngine()
            GrammarAwareParser.parse(
                tokenized,
                filename,
                fallbackExtractor = gemmaEngine?.let { GemmaFallbackExtractor(gemmaEngine = it) },
                diagnosticExtractor = gemmaEngine?.let { GemmaDiagnosticExtractor(gemmaEngine = it) },
            ).getOrThrow()
        }
    }

    private fun buildGemmaEngine(): GemmaEngine? {
        return try {
            val modelPath = resolveInstalledGemmaModelPath() ?: return null
            GemmaEngine(GemmaEngineConfig(modelPath = modelPath))
        } catch (e: Throwable) {
            null
        }
    }

    actual override suspend fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            try {
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
                        return@withContext Result.failure(Exception("Failed to decrypt PDF: Incorrect password"))
                    }
                }

                Result.success(extractTokenized(pdfDoc))
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }
}
