package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Structural guards on the Part II browse screens (plan 4a, "Debt watch"), the twin of `Part2StorageSourceTest`. The screens show what
 * the bundle says, so they stay blind to what any code means (a content batch must never need a UI edit: even REJOINAL's note comes from
 * the template), never log or report a code, a name or a query, and use no regex (Kotlin/Native's engine). The Guide's three files that
 * carry Part II are held to the same rules.
 */
class Part2LookupSourceTest {
    private val root = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide")
    private val folder = File(root, "part2")

    private fun files() =
        folder.listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name } +
            listOf("GuideTabPart2.kt", "Part2Destination.kt").map { File(root, it) }

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theFolderIsWhereTheTestExpectsItAndHoldsTheBrowseScreens() {
        assertTrue(folder.isDirectory, "run from the composeApp module: ${folder.absolutePath}")
        for (expected in listOf("Part2LookupViewModel.kt", "Part2Availability.kt", "Part2HomeScreen.kt", "Part2RibbonScreen.kt", "Part2CaseScreen.kt", "Part2CodeScreen.kt")) {
            assertTrue(expected in files().map { it.name }, "$expected is missing")
        }
    }

    @Test
    fun noBrowseFileNamesAnyOfTheCodesInTheManual() {
        val text = File("src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val codes = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle.part2!!.serials.flatMap { it.codes }.toSet()
        assertEquals(321, codes.size)
        for (file in files()) {
            val code = withoutComments(file.readText())
            val named = codes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(code) }
            assertEquals(emptyList(), named, "${file.name} names a code; the screens must be blind to what a code means")
        }
    }

    @Test
    fun noBrowseFileLogsReportsOrPrintsAnything() {
        for (file in files()) {
            val code = withoutComments(file.readText())
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer", "printStackTrace").filter { it in code }
            assertEquals(emptyList(), found, "${file.name} must not log or report a code, a name or a query")
        }
    }

    @Test
    fun noBrowseFileUsesARegexOrTheClipboardOrTheNetwork() {
        for (file in files()) {
            val code = withoutComments(file.readText())
            val found = listOf("Regex(", "toRegex", "java.util.regex", "Clipboard", "HttpURLConnection", "NSURLSession").filter { it in code }
            assertEquals(emptyList(), found, "${file.name} uses something the lookup must not")
        }
    }
}
