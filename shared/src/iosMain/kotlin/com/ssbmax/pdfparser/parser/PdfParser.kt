@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.logging.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxCropBox

actual class PlatformPdfParser actual constructor() : PdfParser {
    actual override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> {
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

            val pageCount = pdfDoc.pageCount.toInt()
            var tablePageIdx = 0
            for (i in 0 until pageCount) {
                val page = pdfDoc.pageAtIndex(i.toULong())
                val pageText = page?.string ?: ""
                if (pageText.lowercase().contains("bpay") || pageText.lowercase().contains("basic pay")) {
                    tablePageIdx = i
                    break
                }
            }

            val tablePage = pdfDoc.pageAtIndex(tablePageIdx.toULong()) ?: return Result.failure(Exception("Table page not found"))
            val pageBounds = tablePage.boundsForBox(kPDFDisplayBoxCropBox)
            val pageHeight = pageBounds.useContents { size.height }
            val pageWidth = pageBounds.useContents { size.width }

            val scanner = IosLayoutScanner(pdfDoc, tablePageIdx, pageHeight)
            scanner.scan()

            Logger.d("PlatformPdfParser", "Raw coordinates: yBpay=${scanner.yBpay}, xDsop=${scanner.xDsop}, yTotalCredit=${scanner.yTotalCredit}, xDetails=${scanner.xDetails}")

            var yMinVal = scanner.yTotalCredit + 2.0
            var yMaxVal = if (scanner.tableHeaderY > 0.0) kotlin.math.max(pageHeight - 180.0, scanner.tableHeaderY) else scanner.yBpay + 25.0
            if (yMaxVal <= yMinVal) {
                Logger.w("PlatformPdfParser", "Invalid Y bounds detected (yMaxVal: $yMaxVal <= yMinVal: $yMinVal). Applying safe fallbacks.")
                yMaxVal = pageHeight - 180.0
                yMinVal = pageHeight - 700.0
            }
            if (yMinVal < 0.0) yMinVal = 0.0
            if (yMaxVal > pageHeight) yMaxVal = pageHeight

            var xDsopVal = scanner.xDsop
            if (xDsopVal <= 50.0 || xDsopVal >= pageWidth) {
                Logger.w("PlatformPdfParser", "Invalid xDsopVal ($xDsopVal). Falling back to 150.0.")
                xDsopVal = 150.0
            }

            val calculatedBound = if (xDsopVal >= 250.0) (xDsopVal + 190.0) else 308.0
            val xRightBound = if (scanner.xDetails > xDsopVal + 20.0 && scanner.xDetails < calculatedBound) scanner.xDetails else calculatedBound

            Logger.d("PlatformPdfParser", "Final safe coordinates - yMinVal: $yMinVal, yMaxVal: $yMaxVal, xDsopVal: $xDsopVal, xRightBound: $xRightBound, pageWidth: $pageWidth, pageHeight: $pageHeight")

            val leftText =
                extractTextSpatially(
                    page = tablePage,
                    xMin = 0.0,
                    xMax = xDsopVal - 2.0,
                    yMin = yMinVal,
                    yMax = yMaxVal,
                )

            val middleText =
                extractTextSpatially(
                    page = tablePage,
                    xMin = xDsopVal - 2.0,
                    xMax = xRightBound,
                    yMin = yMinVal,
                    yMax = yMaxVal,
                )

            val textBuilder = StringBuilder()
            for (i in 0 until pageCount) {
                val page = pdfDoc.pageAtIndex(i.toULong())
                val pageText = page?.string ?: ""
                textBuilder.append(pageText).append("\n")
            }
            val flatText = textBuilder.toString()

            var taxText = ""
            var dsopText = ""
            for (i in 0 until pageCount) {
                val page = pdfDoc.pageAtIndex(i.toULong()) ?: continue
                val pageText = page.string ?: ""
                val pageTextLower = pageText.lowercase()

                if (taxText.isEmpty() && (
                        pageTextLower.contains("standard deduction") ||
                            pageTextLower.contains("taxable income") ||
                            pageTextLower.contains("tax payable") ||
                            pageTextLower.contains("income tax deducted")
                    )
                ) {
                    Logger.d("PlatformPdfParser", "Dynamically found Tax details on page: ${i + 1}")
                    val pBounds = page.boundsForBox(kPDFDisplayBoxCropBox)
                    val pHeight = pBounds.useContents { size.height }
                    val pWidth = pBounds.useContents { size.width }

                    val startIdx = pageTextLower.indexOf("income tax details").takeIf { it >= 0 } ?: 0
                    val endIdx1 = pageTextLower.indexOf("dsop fund").takeIf { it > startIdx } ?: pageText.length
                    val endIdx2 = pageTextLower.indexOf("loans & advances").takeIf { it > startIdx } ?: pageText.length
                    val endIdx3 = pageTextLower.indexOf("details of arrears").takeIf { it > startIdx } ?: pageText.length
                    val endIdx = minOf(endIdx1, endIdx2, endIdx3)

                    taxText = extractTextSpatially(page, 0.0, pWidth, 0.0, pHeight, startIdx, endIdx)
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
                    val pBounds = page.boundsForBox(kPDFDisplayBoxCropBox)
                    val pHeight = pBounds.useContents { size.height }
                    val pWidth = pBounds.useContents { size.width }

                    val startIdx = pageTextLower.indexOf("dsop fund").takeIf { it >= 0 } ?: 0
                    val endIdx1 = pageTextLower.indexOf("loans & advances").takeIf { it > startIdx } ?: pageText.length
                    val endIdx2 = pageTextLower.indexOf("details of arrears").takeIf { it > startIdx } ?: pageText.length
                    val endIdx = minOf(endIdx1, endIdx2)

                    dsopText = extractTextSpatially(page, 0.0, pWidth, 0.0, pHeight, startIdx, endIdx)
                }
            }
            if (dsopText.isEmpty()) {
                dsopText = taxText
            }

            Logger.d("PlatformPdfParser", "--- LEFT COLUMN TEXT ---\n$leftText")
            Logger.d("PlatformPdfParser", "--- MIDDLE COLUMN TEXT ---\n$middleText")

            PayslipTextParser.parse(
                leftColumnText = leftText,
                middleColumnText = middleText,
                fullText = flatText,
                taxPageText = taxText,
                dsopPageText = dsopText,
                filename = filename,
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
