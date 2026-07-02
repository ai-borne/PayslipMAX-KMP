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
    for (i in 0 until pageCount) {
        val text = pdfDoc.pageAtIndex(i.toULong())?.string ?: ""
        if (tableIdx < 0 && PageClassifier.isTablePage(text)) tableIdx = i
        if (taxIdx < 0 && PageClassifier.isTaxPage(text)) taxIdx = i
        if (dsopIdx < 0 && PageClassifier.isDsopPage(text)) dsopIdx = i
    }
    if (tableIdx < 0) tableIdx = 0
    if (dsopIdx < 0) dsopIdx = taxIdx

    val tableTokens = pageTokens(pdfDoc, tableIdx)
    val taxTokens = if (taxIdx >= 0) pageTokens(pdfDoc, taxIdx) else emptyList()
    val dsopTokens = if (dsopIdx >= 0) pageTokens(pdfDoc, dsopIdx) else emptyList()

    // Combine all pages' page.string (officer info + multi-page totals) with token reading order
    // (correct TRANSITIONAL label→amount adjacency). For TRANSITIONAL, page.string has labels before
    // amounts (non-adjacent, no regex match), so parseTotals falls through to the token-based match.
    val allPagesRawText =
        (0 until pageCount).joinToString("\n") { i ->
            pdfDoc.pageAtIndex(i.toULong())?.string ?: ""
        }
    val tokenPageSets =
        buildList {
            add(tableTokens)
            if (taxIdx >= 0 && taxIdx != tableIdx) add(taxTokens)
            if (dsopIdx >= 0 && dsopIdx != tableIdx && dsopIdx != taxIdx) add(dsopTokens)
        }
    val tokenText = tokenPageSets.joinToString("\n") { TokenText.readingOrder(it) }
    val fullText = "$allPagesRawText\n$tokenText"

    return TokenizedPayslip(
        tableTokens = tableTokens,
        taxTokens = taxTokens,
        dsopTokens = dsopTokens,
        fullText = fullText,
    )
}

private fun pageTokens(
    pdfDoc: PDFDocument,
    pageIdx: Int,
): List<PositionedToken> {
    val page = pdfDoc.pageAtIndex(pageIdx.toULong()) ?: return emptyList()
    val pageHeight = page.boundsForBox(kPDFDisplayBoxCropBox).useContents { size.height }
    return extractPageTokens(page, pageHeight)
}
