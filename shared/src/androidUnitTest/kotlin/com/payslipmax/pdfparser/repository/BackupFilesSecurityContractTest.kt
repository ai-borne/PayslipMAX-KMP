package com.payslipmax.pdfparser.repository

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Security and memory evidence for the backup files (plan 13, Phase 7b), read from the sources because the property is "this code does
 * not exist": no backup code logs, reports or builds a message from a path or a name; no backup code reads a whole file into one array;
 * the file names are fixed and written once; the picker paths copy by stream; the only share sheet is the one for the export; and
 * nothing else is written to disk. File lists are explicit so a moved source fails loudly instead of making a scan vacuous.
 */
class BackupFilesSecurityContractTest {
    // Comment lines go, by line, because a regex across lines would eat code after the picker's "*/*" MIME literal.
    private fun clean(file: File): String =
        file.readLines().map { it.trim() }.filterNot { it.startsWith("/*") || it.startsWith("*") || it.startsWith("//") }.joinToString("\n") { it.substringBefore(" // ") }

    private val backupCode =
        listOf(
            "src/commonMain/kotlin/com/payslipmax/pdfparser/repository/BackupFiles.kt",
            "src/commonMain/kotlin/com/payslipmax/pdfparser/repository/FileBackupFiles.kt",
            "src/androidMain/kotlin/com/payslipmax/pdfparser/repository/AndroidBackupFiles.kt",
            "src/androidMain/kotlin/com/payslipmax/pdfparser/repository/FileBackupIo.kt",
            "src/iosMain/kotlin/com/payslipmax/pdfparser/repository/IosBackupFiles.kt",
            "src/iosMain/kotlin/com/payslipmax/pdfparser/repository/FileBackupIo.kt",
            // Since docs/Plan/14a P7 the backup view model owns backup and restore (they left PayslipViewModelExtensions.kt).
            "../composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/backup/BackupRestoreViewModel.kt",
            "../composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/backup/BackupUiState.kt",
            "../composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/backup/ProgressThrottle.kt",
            "../composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/BackupRestoreSettingsCard.kt",
        ).associateWith { clean(File(it)) }

    @Test
    fun theScansReadTheRealFilesSoTheyAreNotVacuous() {
        assertTrue(backupCode.values.all { it.length > 300 })
    }

    @Test
    fun noBackupCodeLogsPrintsReportsOrBuildsAMessageFromAValue() {
        val leaky = Regex("""Logger|println|\bprint\(|Log\.[dievw]\(|Napier|Timber|Firebase|Analytics|CrashReporter|TelemetrySanitizer|setCustomKey|recordException|printStackTrace|NSLog""")
        val templated = Regex("""(Exception|error|require|check|checkNotNull)\([^)]*\$""")
        assertEquals(emptyList(), backupCode.filterValues { leaky.containsMatchIn(it) }.keys.toList())
        assertEquals(emptyList(), backupCode.filterValues { templated.containsMatchIn(it) }.keys.toList(), "no exception message built from a value")
    }

    @Test
    fun noBackupFileCodeReadsAWholeFileIntoOneArray() {
        val whole = Regex("""readBytes\(|readAllBytes|readText\(|dataWithContentsOfURL|dataWithContentsOfFile|contentsAtPath|readDataToEndOfFile|NSData\.dataWith""")
        val files = backupCode.filterKeys { "Backup" in it && "Settings" !in it && "Extensions" !in it }
        assertEquals(emptyList(), files.filterValues { whole.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun theFileNamesAreFixedAndEachIsWrittenOnce() {
        val common = backupCode.getValue("src/commonMain/kotlin/com/payslipmax/pdfparser/repository/BackupFiles.kt")
        val android = backupCode.getValue("src/androidMain/kotlin/com/payslipmax/pdfparser/repository/AndroidBackupFiles.kt")
        val ios = backupCode.getValue("src/iosMain/kotlin/com/payslipmax/pdfparser/repository/IosBackupFiles.kt")
        assertEquals(1, Regex("""\"PayslipMax-backup\.pcda\"""").findAll(common).count())
        assertEquals(1, Regex("""\"PayslipMax-restore\.pcda\"""").findAll(android).count())
        assertTrue(!android.contains("PayslipMax-backup") && !ios.contains("PayslipMax-backup"), "the export name lives in one place")
        assertTrue(!android.contains("createTempFile") && !ios.contains("NSUUID"), "no other file is created by the backup code")
    }

    @Test
    fun theViewModelAndCardNeverHoldBackupBytes() {
        val vm = backupCode.getValue("../composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/backup/BackupRestoreViewModel.kt")
        val card = backupCode.getValue("../composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/BackupRestoreSettingsCard.kt")
        assertTrue(!card.contains("ByteArray") && !card.contains("shareBytes"), "the card passes a picked file's name, never its bytes")
        assertTrue(vm.contains("fun backUp(") && vm.contains("fun restore("), "the scan reads the view model that backs up and restores")
        assertTrue(!vm.contains("ByteArray") && !vm.contains(".export("), "the view model uses the file forms only")
    }

    @Test
    fun theAndroidPickerCopiesByStreamAndTheIosPickerNeverReadsTheFile() {
        val main = clean(File("../composeApp/src/androidMain/kotlin/com/payslipmax/pdfparser/MainActivity.kt"))
        val pickBackup = main.substringAfter("pickBackupLauncher =").substringBefore("Play Asset Delivery").substringBefore("private val gemma")
        val copy = main.substringAfter("fun copyPickedBackupOrNull").substringBefore("private fun readBytes")
        assertTrue(pickBackup.contains("copyPickedBackupOrNull") && !pickBackup.contains("readBytes"), "the backup picker never reads the file into an array")
        assertTrue(copy.contains("copyPickedBackup(cacheDir") && !copy.contains("readBytes") && !copy.contains("readAllBytes"), "the copy streams the chosen file")
        val swift = File("../iosApp/iosApp/ContentView.swift").readText()
        val delegate = swift.substringAfter("class BackupPickerDelegate").substringBefore("\n}\n")
        assertTrue(!delegate.contains("Data(contentsOf") && !delegate.contains("toKotlinByteArray"), "the Swift picker hands over a path, not bytes")
        assertTrue(swift.contains("asCopy: true"), "iOS makes the private copy itself")
    }

    @Test
    fun theOnlyShareSheetForBackupIsTheExportsAndTheOldByteShareIsGone() {
        val androidShare = clean(File("src/androidMain/kotlin/com/payslipmax/pdfparser/utils/ShareUtils.kt"))
        assertTrue(!androidShare.contains("writeBytes") && !androidShare.contains("shareBytes"))
        assertEquals(1, Regex("""fun shareBackupFile""").findAll(androidShare).count())
        val iosShare = clean(File("src/iosMain/kotlin/com/payslipmax/pdfparser/utils/ShareUtils.kt"))
        assertTrue(!iosShare.contains("shareBytes") && !iosShare.contains("writeToFile"))
        assertTrue(!clean(File("src/commonMain/kotlin/com/payslipmax/pdfparser/utils/ShareUtils.kt")).contains("shareBytes"))
    }

    @Test
    fun theSweepRunsAtLaunchOnBothPlatforms() {
        assertTrue(clean(File("../composeApp/src/androidMain/kotlin/com/payslipmax/pdfparser/PayslipApplication.kt")).contains("BackupFiles>().sweep()"))
        assertTrue(clean(File("../composeApp/src/iosMain/kotlin/com/payslipmax/pdfparser/MainViewController.kt")).contains("BackupFiles>().sweep()"))
    }
}
