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

    actual override fun decryptAndParse(pdfBytes: ByteArray, password: String): Result<ParsedPayslip> {
        return try {
            // Pin the Kotlin ByteArray to get a native address for NSData creation
            val nsData = pdfBytes.usePinned { pinned ->
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = pdfBytes.size.toULong()
                )
            }

            val pdfDoc = PDFDocument(data = nsData)
            if (pdfDoc.isEncrypted) {
                val success = pdfDoc.unlockWithPassword(password)
                if (!success) {
                    return Result.failure(Exception("Failed to decrypt PDF: Incorrect password"))
                }
            }

            // Extract text from all pages
            val pageCount = pdfDoc.pageCount.toInt()
            val textBuilder = StringBuilder()
            
            for (i in 0 until pageCount) {
                val page = pdfDoc.pageAtIndex(i.toULong())
                val pageText = page?.string ?: ""
                textBuilder.append(pageText).append(" ")
            }

            val flatText = textBuilder.toString()
            
            // We use a dummy filename since we will parse dates from text contents
            PayslipTextParser.parse(flatText, "payslip.pdf")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
