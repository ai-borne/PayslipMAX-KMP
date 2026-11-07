package com.payslipmax.pdfparser.repository

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security evidence for sharing one order page (plan 13, Phase 8), read from the sources because the property is "this code does not
 * exist": the share code logs and reports nothing, builds no message from a path, reads no file whole, names the one file in one place,
 * creates no other file, and reaches the share sheet only through the two platform calls. File lists are explicit so a moved source
 * fails loudly instead of making a scan vacuous.
 */
class PageShareSecurityContractTest {
    // Comment lines go, by line, so that a MIME literal or a KDoc sentence cannot hide code from the scans.
    private fun clean(file: File): String =
        file.readLines().map { it.trim() }.filterNot { it.startsWith("/*") || it.startsWith("*") || it.startsWith("//") }.joinToString("\n") { it.substringBefore(" // ") }

    private val common = "src/commonMain/kotlin/com/payslipmax/pdfparser/repository/PageShare.kt"
    private val android = "src/androidMain/kotlin/com/payslipmax/pdfparser/repository/AndroidPageShare.kt"
    private val ios = "src/iosMain/kotlin/com/payslipmax/pdfparser/repository/IosPageShare.kt"
    private val shareCode = listOf(common, android, ios).associateWith { clean(File(it)) }

    @Test
    fun theScansReadTheRealFilesSoTheyAreNotVacuous() {
        assertTrue(shareCode.values.all { it.length > 300 })
    }

    @Test
    fun noShareCodeLogsPrintsReportsOrBuildsAMessageFromAValue() {
        val leaky = Regex("""Logger|println|\bprint\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics|CrashReporter|TelemetrySanitizer|setCustomKey|recordException|printStackTrace|NSLog""")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        assertEquals(emptyList(), shareCode.filterValues { leaky.containsMatchIn(it) }.keys.toList())
        assertEquals(emptyList(), shareCode.filterValues { templated.containsMatchIn(it) }.keys.toList(), "no exception message built from a value")
    }

    @Test
    fun noShareCodeReadsAFileWholeOrWritesAnythingButThePage() {
        val reads = Regex("""readBytes\(|readAllBytes|readText\(|dataWithContentsOfURL|dataWithContentsOfFile|contentsAtPath|readDataToEndOfFile""")
        assertEquals(emptyList(), shareCode.filterValues { reads.containsMatchIn(it) }.keys.toList())
        assertTrue(!shareCode.getValue(android).contains("createTempFile") && !shareCode.getValue(ios).contains("NSUUID"), "no other file is created")
    }

    @Test
    fun theFileNameLivesInOnePlaceAndTheSheetIsReachedOnlyThroughTheTwoPlatformCalls() {
        assertEquals(1, Regex("""\"PayslipMax-order-page\.jpg\"""").findAll(shareCode.getValue(common)).count())
        assertTrue(!shareCode.getValue(android).contains("PayslipMax-order") && !shareCode.getValue(ios).contains("PayslipMax-order"), "the name lives in one place")
        val outward = Regex("""https?://|ktor|HttpClient|okhttp|java\.net|URL\(|openURL|UriHandler|WebView|setPrimaryClip|ClipData|ClipboardManager|UIPasteboard|ACTION_SEND|startActivity""")
        assertEquals(emptyList(), shareCode.filterValues { outward.containsMatchIn(it) }.keys.toList(), "the platform calls are shareImageFile and presentIosShare, in their own files")
        assertTrue(shareCode.getValue(android).contains("::shareImageFile") && shareCode.getValue(ios).contains("presentIosShare"))
    }
}
