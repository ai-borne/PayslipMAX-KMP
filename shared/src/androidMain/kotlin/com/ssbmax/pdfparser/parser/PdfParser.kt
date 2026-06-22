package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.logging.Logger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.ByteArrayInputStream

actual class PlatformPdfParser actual constructor() : PdfParser {
    actual override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
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
                    layoutScanner.getText(document)

                    // Extract full text of all pages for metadata parsing
                    val fullStripper = PDFTextStripper()
                    val fullText = fullStripper.getText(document) ?: ""

                    val page = document.getPage(tablePageIdx)
                    val originalCropBox = page.cropBox
                    val pageHeight = originalCropBox.height
                    val pageWidth = originalCropBox.width
                    val originX = originalCropBox.lowerLeftX
                    val originY = originalCropBox.lowerLeftY

                    var yStart = kotlin.math.min(180f, layoutScanner.bpayY - 5f)
                    var yEnd = layoutScanner.totalCreditY - 2f
                    var xSplit = layoutScanner.dsopX

                    Logger.d("PlatformPdfParser", "Found table on page: $tablePageIdx")
                    Logger.d(
                        "PlatformPdfParser",
                        "layoutScanner - bpayY: ${layoutScanner.bpayY}, totalCreditY: ${layoutScanner.totalCreditY}, dsopX: ${layoutScanner.dsopX}",
                    )
                    Logger.d(
                        "PlatformPdfParser",
                        "Page dimensions - width: $pageWidth, height: $pageHeight, originX: $originX, originY: $originY",
                    )
                    Logger.d("PlatformPdfParser", "Calculated coordinates - yStart: $yStart, yEnd: $yEnd, xSplit: $xSplit")

                    if (yStart < 0f) yStart = 0f
                    if (yEnd <= yStart) {
                        Logger.w("PlatformPdfParser", "Invalid Y bounds detected (yEnd: $yEnd <= yStart: $yStart). Applying safe fallbacks.")
                        yStart = 180f
                        yEnd = kotlin.math.max(700f, pageHeight - 20f)
                    }
                    if (xSplit <= 10f || xSplit >= pageWidth) {
                        Logger.w("PlatformPdfParser", "Invalid xSplit ($xSplit). Falling back to 150f.")
                        xSplit = 150f
                    }

                    Logger.d("PlatformPdfParser", "Final safe coordinates - yStart: $yStart, yEnd: $yEnd, xSplit: $xSplit")

                    // Extract Left Column (Credits) spatially
                    val leftStripper =
                        SpatialTextStripper(
                            xMin = 0f,
                            xMax = xSplit - 2f,
                            yMin = yStart,
                            yMax = yEnd,
                        )
                    leftStripper.startPage = tablePageIdx + 1
                    leftStripper.endPage = tablePageIdx + 1
                    Logger.d("PlatformPdfParser", "Starting left column spatial extraction...")
                    leftStripper.getText(document)
                    val leftText = leftStripper.getFilteredText()
                    Logger.d("PlatformPdfParser", "Finished left column spatial extraction:\n$leftText")

                    // Extract Middle Column (Debits) spatially
                    val xRightBound = if (layoutScanner.detailsX > xSplit - 2f) layoutScanner.detailsX else pageWidth
                    val middleStripper =
                        SpatialTextStripper(
                            xMin = xSplit - 2f,
                            xMax = xRightBound,
                            yMin = yStart,
                            yMax = yEnd,
                        )
                    middleStripper.startPage = tablePageIdx + 1
                    middleStripper.endPage = tablePageIdx + 1
                    Logger.d("PlatformPdfParser", "Starting middle column spatial extraction...")
                    middleStripper.getText(document)
                    val middleText = middleStripper.getFilteredText()
                    Logger.d("PlatformPdfParser", "Finished middle column spatial extraction:\n$middleText")

                    Logger.d("PlatformPdfParser", "Spatial extraction completed. Number of pages: ${document.numberOfPages}")

                    var taxText = ""
                    var dsopText = ""
                    for (i in 0 until document.numberOfPages) {
                        val pageStripper = PDFTextStripper()
                        pageStripper.startPage = i + 1
                        pageStripper.endPage = i + 1
                        val pageText = pageStripper.getText(document) ?: ""
                        val pageTextLower = pageText.lowercase()

                        if (taxText.isEmpty() && (
                                pageTextLower.contains("standard deduction") ||
                                    pageTextLower.contains("taxable income") ||
                                    pageTextLower.contains("tax payable") ||
                                    pageTextLower.contains("income tax deducted")
                            )
                        ) {
                            Logger.d("PlatformPdfParser", "Dynamically found Tax details on page: ${i + 1}")
                            taxText = pageText
                        }

                        if (dsopText.isEmpty() && (
                                pageTextLower.contains("dsop fund") ||
                                    (
                                        pageTextLower.contains("opening balance") &&
                                            pageTextLower.contains("closing balance") &&
                                            pageTextLower.contains("subscription")
                                    )
                            )
                        ) {
                            Logger.d("PlatformPdfParser", "Dynamically found DSOP details on page: ${i + 1}")
                            dsopText = pageText
                        }
                    }
                    if (dsopText.isEmpty()) {
                        dsopText = taxText
                    }

                    Logger.d("PlatformPdfParser", "Starting PayslipTextParser.parse...")
                    val parseResult =
                        PayslipTextParser.parse(
                            leftColumnText = leftText,
                            middleColumnText = middleText,
                            fullText = fullText,
                            taxPageText = taxText,
                            dsopPageText = dsopText,
                            filename = filename,
                        )
                    Logger.d("PlatformPdfParser", "Finished PayslipTextParser.parse. Success: ${parseResult.isSuccess}")
                    parseResult
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}

private class SpatialTextStripper(
    private val xMin: Float,
    private val xMax: Float,
    private val yMin: Float,
    private val yMax: Float,
) : PDFTextStripper() {
    private val extractedText = StringBuilder()

    init {
        sortByPosition = true
    }

    override fun writeString(
        text: String?,
        textPositions: MutableList<TextPosition>?,
    ) {
        if (text == null || textPositions == null || textPositions.isEmpty()) return

        val lineBuilder = StringBuilder()
        var lastX = 0f
        var lastW = 0f

        for (tp in textPositions) {
            val cx = tp.xDirAdj
            val cy = tp.yDirAdj

            if (cx in xMin..xMax && cy in yMin..yMax) {
                if (lineBuilder.isNotEmpty() && cx - (lastX + lastW) > 3f) {
                    lineBuilder.append(' ')
                }
                lineBuilder.append(tp.unicode)
                lastX = cx
                lastW = tp.widthDirAdj
            }
        }

        val trimmed = lineBuilder.toString().trim()
        if (trimmed.isNotEmpty()) {
            extractedText.append(trimmed).append("\n")
        }
    }

    fun getFilteredText(): String {
        return extractedText.toString().trim()
    }
}
