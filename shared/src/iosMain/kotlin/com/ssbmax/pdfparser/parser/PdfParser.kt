@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.PDFKit.PDFSelection
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

            var yBpay = pageHeight - 250.0
            var xDsop = 150.0
            var yTotalCredit = pageHeight - 700.0
            var xDetails = 0.0

            fun findCoordinates(
                searchTerm: String,
                onSelection: (CValue<CGRect>) -> Unit,
            ) {
                val selections = pdfDoc.findString(searchTerm, withOptions = 1UL) // 1UL is NSCaseInsensitiveSearch
                selections?.forEach { selObj ->
                    val selection = selObj as? PDFSelection ?: return@forEach
                    val pagesList = selection.pages ?: return@forEach
                    for (pageObj in pagesList) {
                        val page = pageObj as? PDFPage ?: continue
                        val idx = pdfDoc.indexForPage(page).toInt()
                        if (idx == tablePageIdx) {
                            val rect = selection.boundsForPage(page)
                            onSelection(rect)
                        }
                    }
                }
            }

            findCoordinates("BPAY") { rect ->
                yBpay = CGRectGetMinY(rect)
            }
            findCoordinates("Basic Pay") { rect ->
                yBpay = CGRectGetMinY(rect)
            }

            findCoordinates("DSOP") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDsop == 150.0 || rx < xDsop) {
                    xDsop = rx
                }
            }
            findCoordinates("AGIF") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDsop == 150.0 || rx < xDsop) {
                    xDsop = rx
                }
            }
            findCoordinates("ITAX") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDsop == 150.0 || rx < xDsop) {
                    xDsop = rx
                }
            }

            findCoordinates("DETAILS OF TRANSACTIONS") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDetails == 0.0 || rx < xDetails) {
                    xDetails = rx
                }
            }
            findCoordinates("DETAILS OF DO2s") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDetails == 0.0 || rx < xDetails) {
                    xDetails = rx
                }
            }
            findCoordinates("DETAILS OF") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDetails == 0.0 || rx < xDetails) {
                    xDetails = rx
                }
            }
            findCoordinates("laona dona") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDetails == 0.0 || rx < xDetails) {
                    xDetails = rx
                }
            }
            findCoordinates("loona dona") { rect ->
                val rx = CGRectGetMinX(rect)
                if (xDetails == 0.0 || rx < xDetails) {
                    xDetails = rx
                }
            }

            findCoordinates("Total Credit") { rect ->
                yTotalCredit = CGRectGetMinY(rect)
            }
            findCoordinates("Gross Pay") { rect ->
                yTotalCredit = CGRectGetMinY(rect)
            }
            findCoordinates("Total Debit") { rect ->
                yTotalCredit = CGRectGetMinY(rect)
            }
            findCoordinates("Total Deductions") { rect ->
                yTotalCredit = CGRectGetMinY(rect)
            }

            println("[PdfParserDebug] Raw coordinates: yBpay=$yBpay, xDsop=$xDsop, yTotalCredit=$yTotalCredit, xDetails=$xDetails")

            var yMinVal = yTotalCredit + 2.0
            var yMaxVal = yBpay + 25.0
            if (yMaxVal <= yMinVal) {
                println("[PdfParserWarning] Invalid Y bounds detected (yMaxVal: $yMaxVal <= yMinVal: $yMinVal). Applying safe fallbacks.")
                yMaxVal = pageHeight - 180.0
                yMinVal = pageHeight - 700.0
            }
            if (yMinVal < 0.0) yMinVal = 0.0
            if (yMaxVal > pageHeight) yMaxVal = pageHeight

            var xDsopVal = xDsop
            if (xDsopVal <= 10.0 || xDsopVal >= pageWidth) {
                xDsopVal = 150.0
            }

            val xRightBound = if (xDetails > xDsopVal - 2.0) xDetails else pageWidth

            println("[PdfParserDebug] Final safe coordinates - yMinVal: $yMinVal, yMaxVal: $yMaxVal, xDsopVal: $xDsopVal, xRightBound: $xRightBound, pageWidth: $pageWidth, pageHeight: $pageHeight")
            println("[PdfParserDebug] --- RAW TABLE PAGE STRING ---\n${tablePage.string}")

            val leftText = extractTextSpatially(
                page = tablePage,
                xMin = 0.0,
                xMax = xDsopVal - 2.0,
                yMin = yMinVal,
                yMax = yMaxVal
            )

            val middleText = extractTextSpatially(
                page = tablePage,
                xMin = xDsopVal - 2.0,
                xMax = xRightBound,
                yMin = yMinVal,
                yMax = yMaxVal
            )

            val textBuilder = StringBuilder()
            for (i in 0 until pageCount) {
                val page = pdfDoc.pageAtIndex(i.toULong())
                val pageText = if (i == tablePageIdx && page != null) {
                    val bounds = page.boundsForBox(kPDFDisplayBoxCropBox)
                    val w = bounds.useContents { size.width }
                    val h = bounds.useContents { size.height }
                    extractTextSpatially(page, 0.0, w, 0.0, h)
                } else {
                    page?.string ?: ""
                }
                textBuilder.append(pageText).append("\n")
            }
            val flatText = textBuilder.toString()

            var taxText = ""
            if (pageCount >= 3) {
                taxText = pdfDoc.pageAtIndex(2UL)?.string ?: ""
            }

            var dsopText = ""
            if (pageCount >= 4) {
                dsopText = pdfDoc.pageAtIndex(3UL)?.string ?: ""
            } else if (pageCount >= 3) {
                dsopText = taxText
            }

            println("[PdfParserDebug] --- LEFT COLUMN TEXT ---\n$leftText")
            println("[PdfParserDebug] --- MIDDLE COLUMN TEXT ---\n$middleText")
            println("[PdfParserDebug] --- FLAT TEXT ---\n$flatText")

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
