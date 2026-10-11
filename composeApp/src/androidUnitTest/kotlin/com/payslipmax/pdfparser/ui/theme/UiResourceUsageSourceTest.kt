package com.payslipmax.pdfparser.ui.theme

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every UI string and dimension token earns its place. A constant nobody reads still has to be translated, reviewed and
 * kept in step with the copy, so an unreferenced one is deleted, not kept "for later". A glyph typed straight into a
 * `Text(` bypasses `AppStrings`, which is where all copy lives.
 */
class UiResourceUsageSourceTest {
    private val mainRoots = listOf("src/commonMain", "src/androidMain", "src/iosMain")
    private val themeDir = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/theme")
    private val sources: List<File> =
        mainRoots.flatMap { File(it).walkTopDown().filter { f -> f.isFile && f.extension == "kt" }.toList() }
    private val swiftSources: List<File> =
        File("../iosApp").walkTopDown().filter { it.isFile && it.extension == "swift" }.toList()
    private val declaration = Regex("""^\s*(?:private\s+)?(?:const\s+)?val\s+([A-Za-z0-9_]+)\s*[=:]""", RegexOption.MULTILINE)

    private val swiftUse = Regex("""AppStrings\.shared\.([A-Za-z0-9_]+)""")

    private fun swiftUses(): Set<String> = swiftSources.flatMap { f -> swiftUse.findAll(f.readText()).map { it.groupValues[1] }.toList() }.toSet()

    private fun unreferenced(
        file: File,
        text: String,
    ): List<String> =
        declaration.findAll(text).map { it.groupValues[1] }.filter { name ->
            val use = Regex("""\.$name\b""")
            val usedByItsOwnFile = Regex("""\b$name\b""").findAll(text).count() > 1
            !usedByItsOwnFile && name !in swiftUses() && sources.none { it != file && use.containsMatchIn(it.readText()) }
        }.toList()

    @Test
    fun everyAppStringsConstantIsUsed() {
        val unused =
            themeDir.listFiles { f -> f.name.startsWith("AppStrings") && f.extension == "kt" }.orEmpty().flatMap { file ->
                unreferenced(file, file.readText()).map { "${file.name}: $it" }
            }

        assertTrue(unused.isEmpty(), "unused strings, delete them: $unused")
    }

    @Test
    fun everyStringTheSwiftAppReadsStillExists() {
        // iOS reads AppStrings through the generated framework, so the Kotlin-only scan above cannot see these uses. Deleting
        // one broke the native iOS build and was caught only by the pre-push XCTest run.
        val declared = File(themeDir.path).listFiles { f -> f.name.startsWith("AppStrings") }.orEmpty().joinToString("\n") { it.readText() }
        val missing = swiftUses().filterNot { Regex("""\b$it\b""").containsMatchIn(declared) }

        assertTrue(swiftSources.isNotEmpty() && missing.isEmpty(), "Swift reads AppStrings members that no longer exist: $missing")
    }

    @Test
    fun everyAppDimensionsTokenIsUsed() {
        val themeFile = File(themeDir, "Theme.kt")
        val body = themeFile.readText().substringAfter("object AppDimensions").substringBefore("// Typography Tokens")
        val unused = unreferenced(themeFile, body)

        assertTrue(unused.isEmpty(), "unused dimensions, delete them: $unused")
    }

    @Test
    fun noScreenTypesAGlyphIntoText() {
        val glyph = Regex("""Text\(\s*(?:text\s*=\s*)?"[^"]*[✓✔➔]""")
        val offenders = sources.filter { glyph.containsMatchIn(it.readText()) }.map { it.name }

        assertTrue(offenders.isEmpty(), "use AppStrings.checkMark / rowArrow instead: $offenders")
    }
}
