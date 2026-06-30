@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.ssbmax.pdfparser.parser

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.PDFKit.PDFDocument
import platform.PDFKit.kPDFDisplayBoxCropBox

/**
 * iOS side of the Phase 2 token IR: classifies pages via [PageClassifier] (the SSOT shared with
 * Android) and emits word tokens per classified page using [extractPageTokens], normalizing PDFKit's
 * bottom-up bounds to the common top-down convention. Kept as a top-level helper so the platform
 * [PlatformPdfParser] stays well under the 300-line limit.
 */
internal fun extractTokenized(pdfDoc: PDFDocument): TokenizedPayslip {
    val pageCount = pdfDoc.pageCount.toInt()
    var tableIdx = -1
    var taxIdx = -1
    var dsopIdx = -1
    val fullText = StringBuilder()
    for (i in 0 until pageCount) {
        val text = pdfDoc.pageAtIndex(i.toULong())?.string ?: ""
        fullText.append(text).append("\n")
        if (tableIdx < 0 && PageClassifier.isTablePage(text)) tableIdx = i
        if (taxIdx < 0 && PageClassifier.isTaxPage(text)) taxIdx = i
        if (dsopIdx < 0 && PageClassifier.isDsopPage(text)) dsopIdx = i
    }
    if (tableIdx < 0) tableIdx = 0
    if (dsopIdx < 0) dsopIdx = taxIdx

    val fullStr = fullText.toString()
    val tokenized =
        TokenizedPayslip(
            tableTokens = pageTokens(pdfDoc, tableIdx),
            taxTokens = if (taxIdx >= 0) pageTokens(pdfDoc, taxIdx) else emptyList(),
            dsopTokens = if (dsopIdx >= 0) pageTokens(pdfDoc, dsopIdx) else emptyList(),
            fullText = fullStr,
        )

    return tokenized
}

private fun pageTokens(
    pdfDoc: PDFDocument,
    pageIdx: Int,
): List<PositionedToken> {
    val page = pdfDoc.pageAtIndex(pageIdx.toULong()) ?: return emptyList()
    val pageHeight = page.boundsForBox(kPDFDisplayBoxCropBox).useContents { size.height }
    return extractPageTokens(page, pageHeight)
}
