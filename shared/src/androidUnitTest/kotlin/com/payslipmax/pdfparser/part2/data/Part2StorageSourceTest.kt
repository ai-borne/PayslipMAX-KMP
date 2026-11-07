package com.payslipmax.pdfparser.part2.data

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Structural guards on the storage source (plan 3, "Debt watch"), the twin of `Part2EngineSourceTest`. Storage holds what the officer
 * typed, so it must stay blind to what a code means (a content batch must never need a storage edit), must never log or report
 * anything about an item (a entry's dates, codes and order numbers are private), and must not use regex (Kotlin/Native's engine).
 */
class Part2StorageSourceTest {
    private val folder = File("src/commonMain/kotlin/com/payslipmax/pdfparser/part2/data")

    private fun files() = folder.listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name }

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theFolderIsWhereTheTestExpectsItAndHoldsTheStorageFiles() {
        assertTrue(folder.isDirectory, "run from the shared module: ${folder.absolutePath}")
        for (expected in listOf("Part2Entities.kt", "Part2Payloads.kt", "Part2Dao.kt", "Part2Repository.kt", "RoomPart2Repository.kt", "Part2Backup.kt", "Part2Codec.kt")) {
            assertTrue(expected in files().map { it.name }, "$expected is missing")
        }
    }

    @Test
    fun noStorageFileNamesAnyOfTheCodesInTheManual() {
        val codes = realCodes()
        assertEquals(321, codes.size, "the real code list is the one the shipped bundle carries")
        for (file in files()) {
            val code = withoutComments(file.readText())
            val named = codes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(code) }
            assertEquals(emptyList(), named, "${file.name} names a code; storage must be blind to what a code means")
        }
    }

    @Test
    fun noStorageFileLogsReportsOrPrintsAnything() {
        for (file in files()) {
            val code = withoutComments(file.readText())
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer", "printStackTrace").filter { it in code }
            assertEquals(emptyList(), found, "${file.name} must not log or report anything about a entry, an order or a page")
        }
    }

    @Test
    fun noStorageFileUsesARegexOrTheClipboardOrTheNetwork() {
        for (file in files()) {
            val code = withoutComments(file.readText())
            val found = listOf("Regex(", "toRegex", "java.util.regex", "Clipboard", "HttpURLConnection", "NSURLSession").filter { it in code }
            assertEquals(emptyList(), found, "${file.name} uses something storage must not")
        }
    }

    @Test
    fun noStorageExceptionIsBuiltWithACauseOrTheContentOfAnItem() {
        val source = withoutComments(File(folder, "Part2Exceptions.kt").readText())
        assertTrue("Exception(\"A Part II item could not be read.\")" in source && "Exception(\"A Part II item is too large to store.\")" in source)
        assertTrue("cause" !in source && "Throwable" !in source, "an exception cause could carry the input")
    }

    private fun realCodes(): Set<String> {
        val bundle = File("../composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(bundle))
        return loaded.bundle.part2!!.serials.flatMap { it.codes }.toSet()
    }
}
