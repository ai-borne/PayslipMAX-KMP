@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
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
            GrammarAwareParser.parse(tokenized, filename).getOrThrow()
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
