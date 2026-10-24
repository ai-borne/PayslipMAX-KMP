package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Structural guards on the draft flow (Part II plan, Phase 4b), the twin of `Part2LookupSourceTest`. A draft holds what the officer
 * typed, so these files must stay blind to what a code means (a content batch never needs a UI edit), never log or report anything,
 * never reach for a payslip or an identity (the slip carries none, Round 5 and 7), and never send anything anywhere (printing goes through the system
 * dialog only, and the print code has its own guard, `Part2PrintSourceTest` in shared).
 */
class Part2DraftSourceTest {
    private val root = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide")
    private val folder = File(root, "part2/draft")

    private fun files() = folder.listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name } + File(root, "GuideTabPart2Draft.kt")

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theFolderIsWhereTheTestExpectsItAndHoldsTheDraftFlow() {
        assertTrue(folder.isDirectory, "run from the composeApp module: ${folder.absolutePath}")
        for (expected in listOf("LeaveDraftViewModel.kt", "LeaveDraftStateBuilder.kt", "LeaveDraftScreen.kt", "SlipPreviewScreen.kt", "Part2DraftsViewModel.kt", "Part2LockedScreen.kt")) {
            assertTrue(expected in files().map { it.name }, "$expected is missing")
        }
    }

    @Test
    fun noDraftFileNamesAnyOfTheCodesInTheManual() {
        val text = File("src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val codes = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle.part2!!.serials.flatMap { it.codes }.toSet()
        assertEquals(321, codes.size)
        for (file in files()) {
            val code = withoutComments(file.readText())
            val named = codes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(code) }
            assertEquals(emptyList(), named, "${file.name} names a code; the draft flow must be blind to what a code means")
        }
    }

    @Test
    fun noDraftFileLogsReportsOrPrintsAnything() {
        for (file in files()) {
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer", "printStackTrace").filter { it in withoutComments(file.readText()) }
            assertEquals(emptyList(), found, "${file.name} must not log or report what the officer typed")
        }
    }

    @Test
    fun noDraftFileTouchesAPayslipAnIdentityOrTheOutsideWorld() {
        val forbidden = listOf("Payslip", "Officer", "panNumber", "accountNumber", "Clipboard", "HttpURLConnection", "NSURLSession", "sharePdf", "ACTION_SEND", "FileProvider", "Intent", "startActivity", "UIActivityViewController", "Regex(", "toRegex")
        for (file in files()) {
            val found = forbidden.filter { it in withoutComments(file.readText()) }
            assertEquals(emptyList(), found, "${file.name} uses something the draft flow must not: the slip has no identity and goes nowhere yet")
        }
    }

    @Test
    fun theDraftIsOnlyEverSavedThroughTheRepositoryInterface() {
        val model = withoutComments(File(folder, "LeaveDraftViewModel.kt").readText())
        assertTrue("Part2Repository" in model && "Room" !in model && "Dao" !in model && "CryptoHelper" !in model, "autosave writes through Part2Repository, which encrypts; the model never touches storage itself")
        for (file in files().filter { it.name != "LeaveDraftViewModel.kt" && it.name != "Part2Drafting.kt" && it.name != "Part2DraftsViewModel.kt" }) {
            assertTrue("saveDraft" !in withoutComments(file.readText()), "${file.name}: only the model saves")
        }
    }
}
