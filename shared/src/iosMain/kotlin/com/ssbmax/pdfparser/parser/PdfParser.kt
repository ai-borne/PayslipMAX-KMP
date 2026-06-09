@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.useContents
import platform.Foundation.NSData
import platform.Foundation.create
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.PDFKit.PDFSelection
import platform.PDFKit.PDFDisplayBoxCropBox
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake

actual class PlatformPdfParser actual constructor() : PdfParser {

    actual override fun decryptAndParse(pdfBytes: ByteArray, password: String): Result<ParsedPayslip> {
        return try {
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
            val pageBounds = tablePage.boundsForBox(PDFDisplayBoxCropBox)
            val pageHeight = pageBounds.useContents { size.height }

            var yBpay = pageHeight - 250.0
            var xDsop = 150.0
            var yTotalCredit = pageHeight - 700.0

            fun findCoordinates(searchTerm: String, onSelection: (CGRect) -> Unit) {
                val selections = pdfDoc.findString(searchTerm, withOptions = 1UL) // 1UL is NSCaseInsensitiveSearch
                if (selections != null) {
                    for (selObj in selections) {
                        val selection = selObj as? PDFSelection ?: continue
                        val pagesList = selection.pages ?: continue
                        for (pageObj in pagesList) {
                            val page = pageObj as? PDFPage ?: continue
                            if (page.label == tablePage.label) {
                                val rect = selection.boundsForPage(page)
                                onSelection(rect)
                            }
                        }
                    }
                }
            }

            findCoordinates("BPAY") { rect ->
                yBpay = rect.useContents { origin.y }
            }
            findCoordinates("Basic Pay") { rect ->
                yBpay = rect.useContents { origin.y }
            }

            findCoordinates("DSOP") { rect ->
                val rx = rect.useContents { origin.x }
                if (xDsop == 150.0 || rx < xDsop) {
                    xDsop = rx
                }
            }
            findCoordinates("AGIF") { rect ->
                val rx = rect.useContents { origin.x }
                if (xDsop == 150.0 || rx < xDsop) {
                    xDsop = rx
                }
            }
            findCoordinates("ITAX") { rect ->
                val rx = rect.useContents { origin.x }
                if (xDsop == 150.0 || rx < xDsop) {
                    xDsop = rx
                }
            }

            findCoordinates("Total Credit") { rect ->
                yTotalCredit = rect.useContents { origin.y }
            }
            findCoordinates("Gross Pay") { rect ->
                yTotalCredit = rect.useContents { origin.y }
            }
            findCoordinates("Total Debit") { rect ->
                yTotalCredit = rect.useContents { origin.y }
            }
            findCoordinates("Total Deductions") { rect ->
                yTotalCredit = rect.useContents { origin.y }
            }

            val yMin = yTotalCredit + 2.0
            val yMax = yBpay + 25.0
            val colHeight = yMax - yMin

            val leftRect = CGRectMake(
                x = 0.0,
                y = yMin,
                width = xDsop - 2.0,
                height = colHeight
            )
            val leftSelection = tablePage.selectionForRect(leftRect)
            val leftText = leftSelection?.string ?: ""

            val middleRect = CGRectMake(
                x = xDsop - 2.0,
                y = yMin,
                width = 310.0 - (xDsop - 2.0),
                height = colHeight
            )
            val middleSelection = tablePage.selectionForRect(middleRect)
            val middleText = middleSelection?.string ?: ""

            val textBuilder = StringBuilder()
            for (i in 0 until pageCount) {
                val page = pdfDoc.pageAtIndex(i.toULong())
                val pageText = page?.string ?: ""
                textBuilder.append(pageText).append(" ")
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

            PayslipTextParser.parse(
                leftColumnText = leftText,
                middleColumnText = middleText,
                fullText = flatText,
                taxPageText = taxText,
                dsopPageText = dsopText,
                filename = "payslip.pdf"
            )
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }
}
