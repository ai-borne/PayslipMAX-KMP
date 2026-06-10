package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

actual class PlatformPdfParser actual constructor() : PdfParser {
    actual override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
    ): Result<ParsedPayslip> {
        return try {
            try {
                val application =
                    Class.forName("android.app.ActivityThread")
                        .getMethod("currentApplication")
                        .invoke(null) as? android.content.Context

                if (application != null) {
                    com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(application)
                }
            } catch (e: Exception) {
                // Ignore classloader reflection failures in desktop unit test runs
            }

            ByteArrayInputStream(pdfBytes).use { inputStream ->
                PDDocument.load(inputStream, password).use { document ->
                    // Find the table page index containing BPAY or Basic Pay
                    var tablePageIdx = 0
                    for (i in 0 until document.numberOfPages) {
                        val singlePageStripper = PDFTextStripper()
                        singlePageStripper.startPage = i + 1
                        singlePageStripper.endPage = i + 1
                        val text = singlePageStripper.getText(document) ?: ""
                        if (text.lowercase().contains("bpay") || text.lowercase().contains("basic pay")) {
                            tablePageIdx = i
                            break
                        }
                    }

                    // Extract coordinates from table page
                    val layoutScanner = LayoutScanner()
                    layoutScanner.startPage = tablePageIdx + 1
                    layoutScanner.endPage = tablePageIdx + 1
                    val fullText = layoutScanner.getText(document) ?: ""

                    val page = document.getPage(tablePageIdx)
                    val originalCropBox = page.cropBox
                    val pageHeight = originalCropBox.height
                    val pageWidth = originalCropBox.width
                    val originX = originalCropBox.lowerLeftX
                    val originY = originalCropBox.lowerLeftY

                    var yStart = kotlin.math.min(180f, layoutScanner.bpayY - 5f)
                    var yEnd = layoutScanner.totalCreditY - 2f
                    var xSplit = layoutScanner.dsopX

                    println("[PdfParserDebug] Found table on page: $tablePageIdx")
                    println(
                        "[PdfParserDebug] layoutScanner - bpayY: ${layoutScanner.bpayY}, totalCreditY: ${layoutScanner.totalCreditY}, dsopX: ${layoutScanner.dsopX}",
                    )
                    println(
                        "[PdfParserDebug] Page dimensions - width: $pageWidth, height: $pageHeight, originX: $originX, originY: $originY",
                    )
                    println("[PdfParserDebug] Calculated coordinates - yStart: $yStart, yEnd: $yEnd, xSplit: $xSplit")

                    if (yStart < 0f) yStart = 0f
                    if (yEnd <= yStart) {
                        println("[PdfParserWarning] Invalid Y bounds detected (yEnd: $yEnd <= yStart: $yStart). Applying safe fallbacks.")
                        yStart = 180f
                        yEnd = kotlin.math.max(700f, pageHeight - 20f)
                    }
                    if (xSplit <= 10f || xSplit >= pageWidth) {
                        println("[PdfParserWarning] Invalid xSplit ($xSplit). Falling back to 150f.")
                        xSplit = 150f
                    }

                    println("[PdfParserDebug] Final safe coordinates - yStart: $yStart, yEnd: $yEnd, xSplit: $xSplit")

                    // Crop Left Column (Credits)
                    val leftRect =
                        com.tom_roush.pdfbox.pdmodel.common.PDRectangle(
                            originX,
                            originY + (pageHeight - yEnd),
                            xSplit - 2f,
                            yEnd - yStart,
                        )
                    page.cropBox = leftRect
                    val leftStripper = PDFTextStripper()
                    leftStripper.startPage = tablePageIdx + 1
                    leftStripper.endPage = tablePageIdx + 1
                    println("[PdfParserDebug] Starting left column text extraction...")
                    val leftText = leftStripper.getText(document) ?: ""
                    println("[PdfParserDebug] Finished left column text extraction. Length: ${leftText.length}")

                    // Crop Middle Column (Debits)
                    val xRightBound = if (layoutScanner.detailsX > xSplit - 2f) layoutScanner.detailsX else pageWidth
                    val middleRect =
                        com.tom_roush.pdfbox.pdmodel.common.PDRectangle(
                            originX + xSplit - 2f,
                            originY + (pageHeight - yEnd),
                            kotlin.math.max(10f, xRightBound - (xSplit - 2f)),
                            yEnd - yStart,
                        )
                    page.cropBox = middleRect
                    val middleStripper = PDFTextStripper()
                    middleStripper.startPage = tablePageIdx + 1
                    middleStripper.endPage = tablePageIdx + 1
                    println("[PdfParserDebug] Starting middle column text extraction...")
                    val middleText = middleStripper.getText(document) ?: ""
                    println("[PdfParserDebug] Finished middle column text extraction. Length: ${middleText.length}")

                    // Restore original crop box
                    page.cropBox = originalCropBox
                    println("[PdfParserDebug] Original crop box restored. Number of pages: ${document.numberOfPages}")

                    // Extract Page 3 and Page 4/3 text
                    var taxText = ""
                    if (document.numberOfPages >= 3) {
                        val taxStripper = PDFTextStripper()
                        taxStripper.startPage = 3
                        taxStripper.endPage = 3
                        println("[PdfParserDebug] Starting Page 3 text extraction...")
                        taxText = taxStripper.getText(document) ?: ""
                        println("[PdfParserDebug] Finished Page 3 text extraction. Length: ${taxText.length}")
                    }

                    var dsopText = ""
                    if (document.numberOfPages >= 4) {
                        val dsopStripper = PDFTextStripper()
                        dsopStripper.startPage = 4
                        dsopStripper.endPage = 4
                        println("[PdfParserDebug] Starting Page 4 text extraction...")
                        dsopText = dsopStripper.getText(document) ?: ""
                        println("[PdfParserDebug] Finished Page 4 text extraction. Length: ${dsopText.length}")
                    } else if (document.numberOfPages >= 3) {
                        dsopText = taxText
                    }

                    println("[PdfParserDebug] Starting PayslipTextParser.parse...")
                    val parseResult =
                        PayslipTextParser.parse(
                            leftColumnText = leftText,
                            middleColumnText = middleText,
                            fullText = fullText,
                            taxPageText = taxText,
                            dsopPageText = dsopText,
                            filename = "payslip.pdf",
                        )
                    println("[PdfParserDebug] Finished PayslipTextParser.parse. Success: ${parseResult.isSuccess}")
                    parseResult
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
