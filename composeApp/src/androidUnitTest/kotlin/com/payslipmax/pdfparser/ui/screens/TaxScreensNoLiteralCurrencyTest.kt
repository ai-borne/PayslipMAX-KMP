package com.payslipmax.pdfparser.ui.screens

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Phase 6 (D18): every rupee glyph on the Tax Planner screens must come from
 * [com.payslipmax.pdfparser.ui.theme.AppStringsTaxPlanner.rupeeSymbol] or another `AppStrings*`
 * constant, never typed directly into a composable -- the bug the deleted Peer Benchmark card
 * ("★ Best", inline "₹" concatenation) shipped with. A source-level scan is the only check that
 * actually locks this, since a missing symbol renders as merely "1,234" rather than crashing.
 */
class TaxScreensNoLiteralCurrencyTest {
    private fun findRepoRoot(): File {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile ?: error("Could not locate repo root (settings.gradle.kts) from ${System.getProperty("user.dir")}")
        }
        return dir
    }

    private fun taxScreenFiles(): List<File> {
        val screensDir = File(findRepoRoot(), "composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens")
        val files = screensDir.listFiles { f -> f.name.startsWith("Tax") && f.extension == "kt" }?.toList() ?: emptyList()
        assertTrue(files.isNotEmpty(), "expected to find Tax*.kt screen files under $screensDir")
        return files
    }

    @Test
    fun noTaxScreenFileContainsALiteralRupeeGlyph() {
        val offenders =
            taxScreenFiles().filter { file ->
                file.readText().contains("₹")
            }
        assertTrue(
            offenders.isEmpty(),
            "Tax screen composables must never type '₹' directly -- use AppStringsTaxPlanner.rupeeSymbol " +
                "or another AppStrings* constant instead. Offending files: ${offenders.map { it.name }}",
        )
    }
}
