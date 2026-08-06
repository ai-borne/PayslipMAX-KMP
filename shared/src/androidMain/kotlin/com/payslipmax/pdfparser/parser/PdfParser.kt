package com.payslipmax.pdfparser.parser

import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.insights.gemma.GemmaEngine
import com.payslipmax.pdfparser.logging.Logger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

/**
 * Intermediate text representation extracted from a payslip PDF. Exposed so the opt-in
 * corpus-capture utility can persist the exact inputs the parser sees (see Phase 0 regression net).
 */
data class ExtractedPayslipTexts(
    val leftColumnText: String,
    val middleColumnText: String,
    val fullText: String,
    val taxPageText: String,
    val dsopPageText: String,
)

actual class PlatformPdfParser actual constructor() : PdfParser {
    actual override suspend fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> {
        return extractTokens(pdfBytes, password, filename).mapCatching { tokenized ->
            Logger.d("PlatformPdfParser", "Starting GrammarAwareParser.parse...")
            val gemmaEngine =
                try {
                    val modelPath = com.payslipmax.pdfparser.insights.gemma.resolveInstalledGemmaModelPath()
                    if (modelPath != null) {
                        Logger.d("PlatformPdfParser", "Gemma model asset pack installed. Initializing GemmaEngine...")
                        val config = com.payslipmax.pdfparser.insights.gemma.GemmaEngineConfig(modelPath = modelPath)
                        val engine = GemmaEngine(config)
                        Logger.d("PlatformPdfParser", "GemmaEngine initialized successfully! isInitialized=${engine.isInitialized}")
                        engine
                    } else {
                        Logger.d("PlatformPdfParser", "Gemma model asset pack not yet installed.")
                        null
                    }
                } catch (e: Throwable) {
                    Logger.e("PlatformPdfParser", "Failed to initialize GemmaEngine", e)
                    null
                }
            val fallbackExtractor = gemmaEngine?.let { GemmaFallbackExtractor(gemmaEngine = it) }
            val diagnosticExtractor = gemmaEngine?.let { GemmaDiagnosticExtractor(gemmaEngine = it) }
            val parseResult =
                GrammarAwareParser.parse(
                    tokenized,
                    filename,
                    fallbackExtractor = fallbackExtractor,
                    diagnosticExtractor = diagnosticExtractor,
                )
            Logger.d("PlatformPdfParser", "Finished GrammarAwareParser.parse. Success: ${parseResult.isSuccess}")
            parseResult.getOrThrow()
        }
    }

    actual override suspend fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            try {
                initResourceLoader()
                ByteArrayInputStream(pdfBytes).use { inputStream ->
                    PDDocument.load(inputStream, password).use { document ->
                        Result.success(extractTokenized(document))
                    }
                }
            } catch (e: Throwable) {
                Result.failure(e)
            }
        }

    /**
     * Decrypts a payslip PDF and runs only the platform text-extraction stage (no parsing).
     * Behavior-preserving extraction of the logic previously inlined in [decryptAndParse]; both
     * paths now share it so captured fixtures reflect the exact production inputs.
     */
    fun decryptAndExtractTexts(
        pdfBytes: ByteArray,
        password: String,
    ): Result<ExtractedPayslipTexts> {
        return try {
            initResourceLoader()
            ByteArrayInputStream(pdfBytes).use { inputStream ->
                PDDocument.load(inputStream, password).use { document ->
                    Result.success(extractTexts(document))
                }
            }
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    private fun initResourceLoader() {
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
    }

    private fun findTablePageIdx(document: PDDocument): Int {
        for (i in 0 until document.numberOfPages) {
            val singlePageStripper = PDFTextStripper()
            singlePageStripper.startPage = i + 1
            singlePageStripper.endPage = i + 1
            val text = singlePageStripper.getText(document) ?: ""
            if (text.lowercase().contains("bpay") || text.lowercase().contains("basic pay")) {
                return i
            }
        }
        return 0
    }

    private fun extractTaxAndDsopTexts(document: PDDocument): Pair<String, String> {
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
        return Pair(taxText, dsopText)
    }

    private fun extractTexts(document: PDDocument): ExtractedPayslipTexts {
        val tablePageIdx = findTablePageIdx(document)

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

        var yStart = if (layoutScanner.tableHeaderY > 0f) kotlin.math.min(180f, layoutScanner.tableHeaderY) else kotlin.math.min(180f, layoutScanner.bpayY - 5f)
        var yEnd = layoutScanner.totalCreditY - 2f
        var xSplit = layoutScanner.dsopX

        Logger.d("PlatformPdfParser", "Found table on page: $tablePageIdx")
        Logger.d(
            "PlatformPdfParser",
            "layoutScanner - bpayY: ${layoutScanner.bpayY}, totalCreditY: ${layoutScanner.totalCreditY}, dsopX: ${layoutScanner.dsopX}, detailsX: ${layoutScanner.detailsX}",
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
        if (xSplit <= 50f || xSplit >= pageWidth) {
            Logger.w("PlatformPdfParser", "Invalid xSplit ($xSplit). Falling back to 150f.")
            xSplit = 150f
        }

        val calculatedBound = if (xSplit >= 250f) (xSplit + 190f) else 308.0f
        val xRightBound = if (layoutScanner.detailsX > xSplit + 20f && layoutScanner.detailsX < calculatedBound) layoutScanner.detailsX else calculatedBound

        Logger.d("PlatformPdfParser", "Final safe coordinates - yStart: $yStart, yEnd: $yEnd, xSplit: $xSplit, xRightBound: $xRightBound")

        // Extract Left & Middle Columns using TableGridExtractor
        val gridExtractor =
            TableGridExtractor(
                yTableStart = yStart,
                yTableEnd = yEnd,
                xSplit = xSplit,
                xRightBound = xRightBound,
            )
        gridExtractor.startPage = tablePageIdx + 1
        gridExtractor.endPage = tablePageIdx + 1
        Logger.d("PlatformPdfParser", "Starting table grid row extraction...")
        gridExtractor.getText(document)
        val (leftText, middleText) = gridExtractor.extractColumnTexts()
        Logger.d("PlatformPdfParser", "Finished left column row extraction:\n$leftText")
        Logger.d("PlatformPdfParser", "Finished middle column row extraction:\n$middleText")

        val (taxText, dsopText) = extractTaxAndDsopTexts(document)

        return ExtractedPayslipTexts(
            leftColumnText = leftText,
            middleColumnText = middleText,
            fullText = fullText,
            taxPageText = taxText,
            dsopPageText = dsopText,
        )
    }
}
