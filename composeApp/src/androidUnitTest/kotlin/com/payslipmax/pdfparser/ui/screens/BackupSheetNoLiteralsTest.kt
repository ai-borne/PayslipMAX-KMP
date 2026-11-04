package com.payslipmax.pdfparser.ui.screens

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a section 1, rule 4): all copy lives in `BackupRestoreStrings`/`AppStrings` and all colours in `Theme.kt`, so the owner's
 * D13 copy review reads one file and a theme change reaches the sheet. A source scan is the only check that locks this: a literal still
 * renders, so nothing else would fail. Test tags and `@file` annotations are not copy.
 */
class BackupSheetNoLiteralsTest {
    private val sheetFiles =
        listOf(
            "BackupProgressPanel.kt",
            "RestorePreviewCard.kt",
            "BackupResultCard.kt",
            "RestoreChoice.kt",
            "BackupRestoreSheet.kt",
            "BackupRestoreSettingsCard.kt",
            "BackupRestoreComponents.kt",
            "BackupSheetRules.kt",
            "BackupStatusMessage.kt",
            "RestoreFailureStatus.kt",
        )

    private fun source(name: String): String {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile ?: error("repo root not found")
        val file = File(dir, "composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/$name")
        assertTrue(file.exists(), "expected $file")
        return file.readText()
    }

    private fun codeLines(name: String) =
        source(name).lines().map { it.trim() }.filterNot { it.startsWith("*") || it.startsWith("/*") || it.startsWith("//") }

    @Test
    fun noSheetFileTypesAColourDirectly() {
        val colour = Regex("""Color\(\s*0x|Color\.(Red|Green|Blue|Yellow|Black|White|Gray|Magenta|Cyan)\b|Color\(\d""")
        val offenders = sheetFiles.filter { name -> codeLines(name).any { colour.containsMatchIn(it) } }
        assertTrue(offenders.isEmpty(), "colours belong in Theme.kt tokens: $offenders")
    }

    @Test
    fun noSheetFileTypesCopyDirectly() {
        val copy = Regex("""(Text\(|text\s*=|label\s*=|title\s*=|placeholder\s*=|contentDescription\s*=)\s*(if \([^)]*\)\s*)?"[^"$]*[A-Za-z]""")
        val offenders = sheetFiles.flatMap { name -> codeLines(name).filter { copy.containsMatchIn(it) }.map { "$name: $it" } }
        assertTrue(offenders.isEmpty(), "copy belongs in BackupRestoreStrings/AppStrings: $offenders")
    }
}
