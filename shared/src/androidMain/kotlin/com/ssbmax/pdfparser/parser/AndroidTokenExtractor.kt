package com.ssbmax.pdfparser.parser

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper

/**
 * Android side of the Phase 2 token IR: classifies pages via [PageClassifier] (the SSOT shared with
 * iOS) and emits word tokens per classified page using [TokenScanner]. No geometry cropping — the
 * un-cropped IR feeds the common grid engine in later phases. Kept as top-level helpers so the
 * platform [PlatformPdfParser] stays comfortably under the 300-line limit.
 */
internal fun extractTokenized(document: PDDocument): TokenizedPayslip {
    var tableIdx = -1
    var taxIdx = -1
    var dsopIdx = -1
    for (i in 0 until document.numberOfPages) {
        val text = singlePageText(document, i)
        if (tableIdx < 0 && PageClassifier.isTablePage(text)) tableIdx = i
        if (taxIdx < 0 && PageClassifier.isTaxPage(text)) taxIdx = i
        if (dsopIdx < 0 && PageClassifier.isDsopPage(text)) dsopIdx = i
    }
    if (tableIdx < 0) tableIdx = 0
    if (dsopIdx < 0) dsopIdx = taxIdx

    val fullText = PDFTextStripper().getText(document) ?: ""
    val tokenized =
        TokenizedPayslip(
            tableTokens = scanPageTokens(document, tableIdx),
            taxTokens = if (taxIdx >= 0) scanPageTokens(document, taxIdx) else emptyList(),
            dsopTokens = if (dsopIdx >= 0) scanPageTokens(document, dsopIdx) else emptyList(),
            fullText = fullText,
        )

    val allTokens = tokenized.tableTokens + tokenized.taxTokens + tokenized.dsopTokens
    println("=== STAGE 0: RAW PDF EXTRACTION (ANDROID PDFBox) ===")
    println("total character count extracted: ${fullText.length}")
    println("total token count: ${allTokens.size}")
    println("page count: ${document.numberOfPages}")
    allTokens.forEachIndexed { idx, t ->
        val pg = if (idx < tokenized.tableTokens.size) 1 else 2
        println("TOKEN #$idx\ntext=${t.text}\npage=$pg\nx=${t.x}\ny=${t.y}\nw=${t.width}\nh=${t.height}\nfont=N/A\nsize=${t.fontSize}")
    }
    println("=====================================================")
    return tokenized
}

private fun singlePageText(
    document: PDDocument,
    pageIdx: Int,
): String {
    val stripper = PDFTextStripper()
    stripper.startPage = pageIdx + 1
    stripper.endPage = pageIdx + 1
    return stripper.getText(document) ?: ""
}

private fun scanPageTokens(
    document: PDDocument,
    pageIdx: Int,
): List<PositionedToken> {
    val scanner = TokenScanner()
    scanner.startPage = pageIdx + 1
    scanner.endPage = pageIdx + 1
    scanner.getText(document)
    return scanner.tokens()
}
