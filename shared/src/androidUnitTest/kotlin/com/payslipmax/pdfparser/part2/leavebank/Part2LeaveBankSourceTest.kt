package com.payslipmax.pdfparser.part2.leavebank

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural guards on the one place Part II reads Leave Bank rows (plan, phase 8b). The rows are the officer's real financial data: this
 * folder takes `LeaveEntry` rows and a summary somebody else derived, never a payslip, never derives the summary again, and never logs or
 * reports a figure. (The slip engine's own folder is guarded by `Part2EngineSourceTest`.)
 */
class Part2LeaveBankSourceTest {
    private val folder = File("src/commonMain/kotlin/com/payslipmax/pdfparser/part2/leavebank")

    private fun files() = folder.listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name }

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    @Test
    fun theFolderIsWhereTheTestExpectsItAndHoldsTheFiguresAndTheSource() {
        assertTrue(folder.isDirectory, "run from the shared module: ${folder.absolutePath}")
        assertEquals(listOf("LeaveBankFigures.kt", "LeaveBankSource.kt"), files().map { it.name })
    }

    @Test
    fun noFileReachesForAPayslipAnIdentityOrTheCalculatorThatDerivesTheSummary() {
        val forbidden = listOf("ParsedPayslip", "Officer", "panNumber", "accountNumber", "PayslipRepository", "LeaveBankCalculator", "summarize(", "domain.Earnings")
        for (file in files()) {
            val found = forbidden.filter { it in withoutComments(file.readText()) }
            assertEquals(emptyList(), found, "${file.name} must read the rows of the one summary, never a payslip or a second derivation")
        }
    }

    @Test
    fun noFileLogsReportsOrPrintsAFigure() {
        for (file in files()) {
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer", "printStackTrace").filter { it in withoutComments(file.readText()) }
            assertEquals(emptyList(), found, "${file.name} must not log or report a Leave Bank figure")
        }
    }
}
