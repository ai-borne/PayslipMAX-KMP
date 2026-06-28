package com.ssbmax.pdfparser.parser

import com.ssbmax.pdfparser.domain.ParsedPayslip
import com.ssbmax.pdfparser.insights.gemma.GemmaEngine
import com.ssbmax.pdfparser.logging.Logger
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream

/**
 * Intermediate text representation extracted from a payslip PDF, before [PayslipTextParser]
 * turns it into a [ParsedPayslip]. Exposed so the opt-in corpus-capture utility can persist the
 * exact inputs the parser sees (see Phase 0 regression net).
 */
data class ExtractedPayslipTexts(
    val leftColumnText: String,
    val middleColumnText: String,
    val fullText: String,
    val taxPageText: String,
    val dsopPageText: String,
)

actual class PlatformPdfParser actual constructor() : PdfParser {
    actual override fun decryptAndParse(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<ParsedPayslip> {
        // Phase 4 cut-over: the token IR is now the primary path. Grid reconstruction, row pairing,
        // classification and the arithmetic confidence solver all live in common code (PayslipTokenParser),
        // so Android and iOS share one device-independent engine instead of diverging column crops.
        return extractTokens(pdfBytes, password, filename).mapCatching { tokenized ->
            Logger.d("PlatformPdfParser", "Starting PayslipTokenParser.parse...")
            val fallbackExtractor =
                try {
                    val context = Class.forName("android.app.ActivityThread").getMethod("currentApplication").invoke(null) as? android.content.Context
                    val modelFile = if (context != null) java.io.File(context.filesDir, "gemma-3-1b-it-int4.task") else null
                    if (modelFile != null && modelFile.exists()) {
                        Logger.d("PlatformPdfParser", "Gemma model binary detected (${modelFile.length()} bytes). Initializing GemmaEngine...")
                        val config = com.ssbmax.pdfparser.insights.gemma.GemmaEngineConfig(modelPath = modelFile.absolutePath)
                        val engine = GemmaEngine(config)
                        Logger.d("PlatformPdfParser", "GemmaEngine initialized successfully! isInitialized=${engine.isInitialized}")
                        GemmaFallbackExtractor(gemmaEngine = engine)
                    } else {
                        Logger.d("PlatformPdfParser", "Gemma model file not found in filesDir.")
                        null
                    }
                } catch (e: Throwable) {
                    Logger.e("PlatformPdfParser", "Failed to initialize GemmaEngine", e)
                    null
                }
            val parseResult = PayslipTokenParser.parse(tokenized, filename, fallbackExtractor = fallbackExtractor)
            Logger.d("PlatformPdfParser", "Finished PayslipTokenParser.parse. Success: ${parseResult.isSuccess}")
            parseResult.getOrThrow()
        }
    }

    actual override fun extractTokens(
        pdfBytes: ByteArray,
        password: String,
        filename: String,
    ): Result<TokenizedPayslip> {
        return try {
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

    private fun extractTexts(document: PDDocument): ExtractedPayslipTexts {
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

        return ExtractedPayslipTexts(
            leftColumnText = leftText,
            middleColumnText = middleText,
            fullText = fullText,
            taxPageText = taxText,
            dsopPageText = dsopText,
        )
    }
}
