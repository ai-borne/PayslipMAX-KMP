package com.payslipmax.pdfparser.part2.print

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural guards on the print code (Part II plan, Phase 5), beside `Part2DraftSourceTest` in composeApp. The slip is the one thing the
 * officer hands to another person, and it goes through the system print dialog only: no share sheet, no file another app can reach, no
 * clipboard, no network, no log, no payslip and no identity. These scan every source set of the print folder.
 */
class Part2PrintSourceTest {
    private val folders = listOf("commonMain", "androidMain", "iosMain").map { File("src/$it/kotlin/com/payslipmax/pdfparser/part2/print") }

    private fun files() = folders.flatMap { it.listFiles { f -> f.extension == "kt" }.orEmpty().toList() }.sortedBy { it.name }

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theFoldersAreWhereTheTestExpectsThemAndHoldBothPlatformsPrinters() {
        assertTrue(folders.all { it.isDirectory }, "run from the shared module: ${folders.map { it.absolutePath }}")
        for (expected in listOf("SlipLayout.kt", "SlipPrinter.kt", "SlipDocument.kt", "AndroidSlipPrinter.kt", "SlipPrintAdapter.kt", "IosSlipPrinter.kt", "IosSlipPdf.kt")) {
            assertTrue(expected in files().map { it.name }, "$expected is missing")
        }
    }

    @Test
    fun noPrintFileSharesSendsWritesAFileOrTouchesTheClipboardOrTheNetwork() {
        val forbidden =
            listOf(
                "sharePdf", "ACTION_SEND", "FileProvider", "Intent", "startActivity", "startIntentSender", "Clipboard", "ClipData",
                "UIActivityViewController", "presentIosShare", "UIPasteboard", "File(", "FileOutputStream", "cacheDir", "filesDir", "getExternal",
                "NSTemporaryDirectory", "writeToFile", "NSFileManager", "HttpURLConnection", "NSURLSession", "WebView",
            )
        for (file in files()) {
            val found = forbidden.filter { it in withoutComments(file.readText()) }
            assertEquals(emptyList(), found, "${file.name} uses something the print path must not: the slip goes to the system print dialog and nowhere else")
        }
    }

    @Test
    fun noPrintFileLogsReportsOrTouchesAPayslipOrAnIdentity() {
        val forbidden = listOf("Logger", "CrashReporter", "TelemetrySanitizer", "println(", "printStackTrace", "Log.", "Payslip", "Officer", "panNumber", "accountNumber", "Regex(", "toRegex")
        for (file in files()) {
            val found = forbidden.filter { it in withoutComments(file.readText()) }
            assertEquals(emptyList(), found, "${file.name} must not log or report the slip, and the slip has no identity")
        }
    }

    @Test
    fun theDocumentTypesHideTheirContentWhenPrintedAsText() {
        val text =
            SlipDocument("MARKER_TITLE", "MARKER_LAST", "MARKER_NOTICE", listOf(SlipDocumentOrder("MARKER_ORDER", listOf(SlipDocumentRow("MARKER_CODE", "MARKER_NARRATIVE", listOf("MARKER_DETAIL"))))), "MARKER_LABEL").toString() +
                SlipDocumentOrder("MARKER_ORDER", emptyList()) + SlipDocumentRow("MARKER_CODE", "MARKER_NARRATIVE", listOf("MARKER_DETAIL"))

        assertTrue("MARKER" !in text, text)
    }
}
