package com.payslipmax.pdfparser.part2.domain

import com.payslipmax.pdfparser.guide.GuideLoadResult
import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Structural guards on the engine's own source (plan 2a, "Debt watch"). Code-specific behaviour belongs in templates and rule
 * families only; if an engine file names a code, the next content batch would have to edit the engine, which is the failure this
 * phase exists to prevent. And nothing about a payslip may reach the slip engine (Round 5): it has no payslip input and imports none.
 */
class Part2EngineSourceTest {
    private val folder = File("src/commonMain/kotlin/com/payslipmax/pdfparser/part2/domain")

    /** Files that are rule families (code-specific by design) are the only ones allowed to name a code. */
    private fun engineFiles() = folder.listFiles { f -> f.extension == "kt" && !f.name.endsWith("Rules.kt") || f.name == "Part2Rules.kt" }.orEmpty().sortedBy { it.name }

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theEngineFolderIsWhereTheTestExpectsItAndHoldsTheEngineFiles() {
        assertTrue(folder.isDirectory, "run from the shared module: ${folder.absolutePath}")
        val names = engineFiles().map { it.name }
        for (expected in listOf("Part2DraftEngine.kt", "OccurrenceRenderer.kt", "SlipOrdering.kt", "Part2Rules.kt", "Part2Slip.kt", "LeaveEventInput.kt", "Part2Limits.kt")) {
            assertTrue(expected in names, "$expected is missing from $names")
        }
    }

    @Test
    fun noEngineFileNamesAnyOfTheCodesInTheManual() {
        val realCodes = realCodes()
        assertEquals(321, realCodes.size, "the real code list is the one the shipped bundle carries")
        for (file in engineFiles()) {
            val code = withoutComments(file.readText())
            val named = realCodes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(code) }
            assertEquals(emptyList(), named, "${file.name} names a code; code-specific behaviour belongs in a template or a rule family")
        }
    }

    @Test
    fun theCodeScanCatchesACodeInASampleSoItCannotPassByScanningNothing() {
        val sample = "val x = \"GTDAL\" // GTDRSHNA in a comment"
        val hits = realCodes().filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(withoutComments(sample)) }
        assertEquals(listOf("GTDAL"), hits)
    }

    @Test
    fun noEngineFileTouchesAPayslipOrWhoTheOfficerIs() {
        val forbidden = listOf("ParsedPayslip", "Officer", "panNumber", "PayslipRepository", "LeaveBankCalculator", "domain.Earnings", "accountNumber")
        for (file in engineFiles()) {
            val code = withoutComments(file.readText())
            val found = forbidden.filter { it in code }
            assertEquals(emptyList(), found, "${file.name} reaches for payslip or identity data")
        }
    }

    @Test
    fun noEngineFileCarriesALogOrATelemetryCall() {
        for (file in engineFiles()) {
            val code = withoutComments(file.readText())
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer").filter { it in code }
            assertEquals(emptyList(), found, "${file.name} must not log or report anything about a draft")
        }
    }

    private fun realCodes(): Set<String> {
        val bundle = File("../composeApp/src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val loaded = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(bundle))
        return loaded.bundle.part2!!.serials.flatMap { it.codes }.toSet()
    }
}
