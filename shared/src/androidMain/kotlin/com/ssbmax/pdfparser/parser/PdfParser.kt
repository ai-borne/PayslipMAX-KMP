package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

actual class PlatformPdfParser actual constructor() : PdfParser {

    actual override fun decryptAndParse(pdfBytes: ByteArray, password: String): Result<ParsedPayslip> {
        return try {
            // Get the application context dynamically using reflection
            val application = Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? android.content.Context
                
            if (application != null) {
                com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(application)
            }

            ByteArrayInputStream(pdfBytes).use { inputStream ->
                PDDocument.load(inputStream, password).use { document ->
                    if (document.isEncrypted) {
                        // PDFBox loads decrypted document directly if password is correct,
                        // otherwise it throws an exception during PDDocument.load().
                        // No additional unlock is required.
                    }
                    val stripper = PDFTextStripper()
                    val fullText = stripper.getText(document) ?: ""
                    PayslipTextParser.parse(fullText, "payslip.pdf")
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
