package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

actual class PlatformPdfParser actual constructor() : PdfParser {

    actual override fun decryptAndParse(pdfBytes: ByteArray, password: String): Result<ParsedPayslip> {
        return try {
            try {
                val application = Class.forName("android.app.ActivityThread")
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
                    val originX = originalCropBox.lowerLeftX
                    val originY = originalCropBox.lowerLeftY

                    val yStart = kotlin.math.min(180f, layoutScanner.bpayY - 5f)
                    val yEnd = layoutScanner.totalCreditY - 2f
                    val xSplit = layoutScanner.dsopX

                    // Crop Left Column (Credits)
                    val leftRect = com.tom_roush.pdfbox.pdmodel.common.PDRectangle(
                        originX,
                        originY + (pageHeight - yEnd),
                        xSplit - 2f,
                        yEnd - yStart
                    )
                    page.cropBox = leftRect
                    val leftStripper = PDFTextStripper()
                    leftStripper.startPage = tablePageIdx + 1
                    leftStripper.endPage = tablePageIdx + 1
                    val leftText = leftStripper.getText(document) ?: ""

                    // Crop Middle Column (Debits)
                    val middleRect = com.tom_roush.pdfbox.pdmodel.common.PDRectangle(
                        originX + xSplit - 2f,
                        originY + (pageHeight - yEnd),
                        310f - (xSplit - 2f),
                        yEnd - yStart
                    )
                    page.cropBox = middleRect
                    val middleStripper = PDFTextStripper()
                    middleStripper.startPage = tablePageIdx + 1
                    middleStripper.endPage = tablePageIdx + 1
                    val middleText = middleStripper.getText(document) ?: ""

                    // Restore original crop box
                    page.cropBox = originalCropBox

                    // Extract Page 3 and Page 4/3 text
                    var taxText = ""
                    if (document.numberOfPages >= 3) {
                        val taxStripper = PDFTextStripper()
                        taxStripper.startPage = 3
                        taxStripper.endPage = 3
                        taxText = taxStripper.getText(document) ?: ""
                    }

                    var dsopText = ""
                    if (document.numberOfPages >= 4) {
                        val dsopStripper = PDFTextStripper()
                        dsopStripper.startPage = 4
                        dsopStripper.endPage = 4
                        dsopText = dsopStripper.getText(document) ?: ""
                    } else if (document.numberOfPages >= 3) {
                        dsopText = taxText
                    }

                    PayslipTextParser.parse(
                        leftColumnText = leftText,
                        middleColumnText = middleText,
                        fullText = fullText,
                        taxPageText = taxText,
                        dsopPageText = dsopText,
                        filename = "payslip.pdf"
                    )
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
