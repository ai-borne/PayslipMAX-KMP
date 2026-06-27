@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.PDFKit.PDFDocument
import platform.PDFKit.PDFPage
import platform.PDFKit.PDFSelection

internal class IosLayoutScanner(
    private val pdfDoc: PDFDocument,
    private val tablePageIdx: Int,
    private val pageHeight: Double,
) {
    var yBpay: Double = pageHeight - 250.0
    var xDsop: Double = 150.0
    var yTotalCredit: Double = pageHeight - 700.0
    var xDetails: Double = 0.0
    var tableHeaderY: Double = 0.0

    fun scan() {
        fun findCoordinates(
            searchTerm: String,
            onSelection: (CValue<CGRect>) -> Unit,
        ) {
            val selections = pdfDoc.findString(searchTerm, withOptions = 1UL)
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

        var headerFound = false
        findCoordinates("Earnings") { rect ->
            if (!headerFound) {
                tableHeaderY = CGRectGetMinY(rect)
                headerFound = true
            }
        }
        findCoordinates("AAYA") { rect ->
            if (!headerFound) {
                tableHeaderY = CGRectGetMinY(rect)
                headerFound = true
            }
        }

        var yBpayFound = false
        findCoordinates("BPAY") { rect ->
            if (!yBpayFound) {
                yBpay = CGRectGetMinY(rect)
                yBpayFound = true
            }
        }
        findCoordinates("Basic Pay") { rect ->
            if (!yBpayFound) {
                yBpay = CGRectGetMinY(rect)
                yBpayFound = true
            }
        }

        findCoordinates("Total Credit") { rect -> yTotalCredit = CGRectGetMinY(rect) }
        findCoordinates("Gross Pay") { rect -> yTotalCredit = CGRectGetMinY(rect) }
        findCoordinates("Total Debit") { rect -> yTotalCredit = CGRectGetMinY(rect) }
        findCoordinates("Total Deductions") { rect -> yTotalCredit = CGRectGetMinY(rect) }
        findCoordinates("REMITTANCE") { rect ->
            val ry = CGRectGetMinY(rect)
            if (yTotalCredit == pageHeight - 700.0 || ry > yTotalCredit) {
                yTotalCredit = ry
            }
        }
        findCoordinates("Remittance") { rect ->
            val ry = CGRectGetMinY(rect)
            if (yTotalCredit == pageHeight - 700.0 || ry > yTotalCredit) {
                yTotalCredit = ry
            }
        }

        findCoordinates("DSOP") { rect ->
            val ry = CGRectGetMinY(rect)
            val rx = CGRectGetMinX(rect)
            if (ry in (yTotalCredit - 10.0)..(yBpay + 25.0)) {
                if (xDsop == 150.0 || rx < xDsop) xDsop = rx
            }
        }
        findCoordinates("AGIF") { rect ->
            val ry = CGRectGetMinY(rect)
            val rx = CGRectGetMinX(rect)
            if (ry in (yTotalCredit - 10.0)..(yBpay + 25.0)) {
                if (xDsop == 150.0 || rx < xDsop) xDsop = rx
            }
        }
        findCoordinates("ITAX") { rect ->
            val ry = CGRectGetMinY(rect)
            val rx = CGRectGetMinX(rect)
            if (ry in (yTotalCredit - 10.0)..(yBpay + 25.0)) {
                if (xDsop == 150.0 || rx < xDsop) xDsop = rx
            }
        }

        findCoordinates("DETAILS OF TRANSACTIONS") { rect ->
            val rx = CGRectGetMinX(rect)
            if (xDetails == 0.0 || rx < xDetails) xDetails = rx
        }
        findCoordinates("DETAILS OF DO2s") { rect ->
            val rx = CGRectGetMinX(rect)
            if (xDetails == 0.0 || rx < xDetails) xDetails = rx
        }
        findCoordinates("DETAILS OF") { rect ->
            val rx = CGRectGetMinX(rect)
            if (xDetails == 0.0 || rx < xDetails) xDetails = rx
        }
        findCoordinates("laona dona") { rect ->
            val rx = CGRectGetMinX(rect)
            if (xDetails == 0.0 || rx < xDetails) xDetails = rx
        }
        findCoordinates("loona dona") { rect ->
            val rx = CGRectGetMinX(rect)
            if (xDetails == 0.0 || rx < xDetails) xDetails = rx
        }
    }
}
