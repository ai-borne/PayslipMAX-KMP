package com.payslipmax.pdfparser.crypto

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, section 4): the property "a backup password is never a `String`" is the absence of code, so it is read from the
 * sources. On the backup path (the field, the view model, the service, the engine, the archive, the KDF) no function takes a password as
 * a `String`, no code makes a `String` of a [Secret]'s characters, and the one place a `String` still becomes a key (the device
 * database's own hex key, wrapped by `PasswordKey(String)`) is not reachable from a typed password. File lists are explicit, so a moved
 * source fails loudly instead of making the scan vacuous.
 */
class BackupPasswordContractTest {
    private fun root(): File {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        while (!File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile ?: error("repo root not found")
        return dir
    }

    private fun code(path: String): String =
        File(root(), path).readLines().map { it.trim() }.filterNot { it.startsWith("/*") || it.startsWith("*") || it.startsWith("//") }.joinToString("\n") { it.substringBefore(" // ") }

    private val shared = "shared/src/commonMain/kotlin/com/payslipmax/pdfparser"
    private val app = "composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui"

    private val backupPath =
        listOf(
            "$shared/repository/PayslipBackupService.kt",
            "$shared/repository/BackupRestoreEngine.kt",
            "$shared/repository/backup/BackupArchive.kt",
            "$shared/crypto/CryptoHelper.kt",
            "shared/src/androidMain/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt",
            "shared/src/iosMain/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt",
            "shared/src/iosMain/kotlin/com/payslipmax/pdfparser/crypto/IosPbkdf2.kt",
            "$app/backup/BackupRestoreViewModel.kt",
            "$app/backup/BackupUiState.kt",
            "$app/screens/BackupPasswordFields.kt",
            "$app/screens/BackupRestoreSheet.kt",
            "$app/screens/BackupRestoreSettingsCard.kt",
        ).associateWith { code(it) }

    @Test
    fun theScansReadTheRealFilesSoTheyAreNotVacuous() {
        assertTrue(backupPath.values.all { it.length > 300 }, backupPath.filterValues { it.length <= 300 }.keys.toString())
    }

    @Test
    fun noFunctionOnTheBackupPathTakesAPasswordAsAString() {
        val stringPassword = Regex("""\b(password|backupPassword|passphrase)\s*:\s*String""")
        assertEquals(emptyList(), backupPath.filterValues { stringPassword.containsMatchIn(it) }.keys.toList())
    }

    @Test
    fun nothingMakesAStringOfASecretsCharacters() {
        val asString = Regex("""concatToString|\bString\(|\.toString\(\)|\bdecodeToString|"\$\{?password""")
        val offenders = backupPath.filterKeys { "BackupPasswordFields" in it || "CryptoHelper" in it || "BackupArchive" in it || "IosPbkdf2" in it || "BackupRestoreSheet" in it }.filterValues { asString.containsMatchIn(it) }
        // The cipher code makes strings of plaintext (decodeToString on a document), never of the password: only password-named lines count.
        val onPassword = offenders.mapValues { (_, text) -> text.lines().filter { asString.containsMatchIn(it) && Regex("""(?i)password|secret|chars""").containsMatchIn(it) } }.filterValues { it.isNotEmpty() }
        assertEquals(emptyMap(), onPassword)
    }

    @Test
    fun noStateOrViewModelFieldHoldsASecret() {
        val state = backupPath.getValue("$app/backup/BackupUiState.kt")
        assertTrue("Secret" !in state && "CharArray" !in state, "a UiState holds counts, codes and enums only")
        val viewModel = backupPath.getValue("$app/backup/BackupRestoreViewModel.kt")
        val holders = Regex("""\b(val|var)\s+\w+\s*:\s*(Secret|CharArray)""").findAll(viewModel).map { it.value }.toList()
        assertEquals(emptyList(), holders, "the view model keeps no password in a field")
    }

    @Test
    fun theOnlyStringConstructorOfAPasswordKeyIsTheDeviceKeysAndSaysSo() {
        val seal = File(root(), "$shared/crypto/SealKey.kt").readText()
        assertTrue(Regex("""constructor\(deviceKey: String\)""").containsMatchIn(seal), "the String entry point is named for the device key")
        assertEquals(1, Regex("""(?<!private )constructor\(""").findAll(seal).count(), "and there is no other public one")
    }

    @Test
    fun theAndroidKeySpecIsAlwaysClearedInAFinally() {
        val android = backupPath.getValue("shared/src/androidMain/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt")
        assertTrue(Regex("""finally\s*\{\s*spec\.clearPassword\(\)""").containsMatchIn(android))
    }
}
