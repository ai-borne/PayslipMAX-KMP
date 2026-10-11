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
    private val declaration = Regex("""^\s*(?:private\s+)?(?:const\s+)?val\s+([A-Za-z0-9_]+)\s*[=:]""", RegexOption.MULTILINE)

    private fun unreferenced(
        file: File,
        text: String,
    ): List<String> =
        declaration.findAll(text).map { it.groupValues[1] }.filter { name ->
            val use = Regex("""\.$name\b""")
            val usedByItsOwnFile = Regex("""\b$name\b""").findAll(text).count() > 1
            !usedByItsOwnFile && sources.none { it != file && use.containsMatchIn(it.readText()) }
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
