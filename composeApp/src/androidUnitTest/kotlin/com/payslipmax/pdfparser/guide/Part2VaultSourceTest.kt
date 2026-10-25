package com.payslipmax.pdfparser.guide

import com.payslipmax.pdfparser.guide.data.GuideBundleParser
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Structural guards on the vault screens (Part II plan, Phase 6b), the twin of `Part2DraftSourceTest` and the screen-side partner of
 * `Part2PagesSourceTest`. A published order is the officer's own document: these files must stay blind to what a code means, never log or
 * report anything about an order, never send a page anywhere or write one to a file, the gallery or the clipboard, and only the order's
 * model may decrypt a page or write an order.
 */
class Part2VaultSourceTest {
    private val root = File("src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/guide")
    private val folder = File(root, "part2/vault")

    private fun files() = folder.listFiles { f -> f.extension == "kt" }.orEmpty().sortedBy { it.name } + File(root, "GuideTabPart2Vault.kt")

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    private fun code(file: File) = withoutComments(file.readText())

    @Test
    fun theFolderIsWhereTheTestExpectsItAndHoldsTheVaultScreens() {
        assertTrue(folder.isDirectory, "run from the composeApp module: ${folder.absolutePath}")
        for (expected in listOf("VaultViewModel.kt", "VaultOrderViewModel.kt", "VaultOrderScreen.kt", "VaultPageRow.kt", "VaultSection.kt", "VaultNotices.kt", "GuideTabPart2Vault.kt")) {
            assertTrue(expected in files().map { it.name }, "$expected is missing")
        }
    }

    @Test
    fun noVaultFileNamesAnyOfTheCodesInTheManual() {
        val text = File("src/commonMain/composeResources/files/guide/guide_bundle.json").readText()
        val codes = assertIs<GuideLoadResult.Loaded>(GuideBundleParser.parse(text)).bundle.part2!!.serials.flatMap { it.codes }.toSet()
        for (file in files()) {
            val named = codes.filter { Regex("\\b${Regex.escape(it)}\\b").containsMatchIn(code(file)) }
            assertEquals(emptyList(), named, "${file.name} names a code; the vault must be blind to what a code means")
        }
    }

    @Test
    fun noVaultFileLogsOrReportsAnything() {
        for (file in files()) {
            val found = listOf("Logger", "CrashReporter", "println(", "TelemetrySanitizer", "printStackTrace", "Log.").filter { it in code(file) }
            assertEquals(emptyList(), found, "${file.name} must not log or report what is in an order")
        }
    }

    @Test
    fun noVaultFileSendsAPageAnywhereOrWritesOneOut() {
        val forbidden =
            listOf(
                "Payslip", "Officer", "panNumber", "accountNumber", "Clipboard", "HttpURLConnection", "NSURLSession", "sharePdf", "ACTION_SEND", "FileProvider",
                "Intent", "startActivity", "UIActivityViewController", "Regex(", "toRegex", "java.io", "File(", "writeBytes", "MediaStore", "ContentResolver",
                "externalCacheDir", "getExternal", "NSFileManager", "writeToFile", "UIImageWriteToSavedPhotosAlbum", "PHPhotoLibrary", "FLAG_SECURE",
            )
        for (file in files()) {
            val found = forbidden.filter { it in code(file) }
            assertEquals(emptyList(), found, "${file.name} uses something a page must never reach: it stays in memory, in the vault, on this device")
        }
    }

    @Test
    fun onlyTheOrdersModelDecryptsAPageAndOnlyTheTwoModelsWriteAnOrder() {
        for (file in files().filter { it.name != "VaultOrderViewModel.kt" }) {
            assertTrue("readPage" !in code(file), "${file.name}: only VaultOrderViewModel reads a stored page, and zeroes it after")
        }
        for (file in files().filter { it.name !in listOf("VaultViewModel.kt", "VaultOrderViewModel.kt") }) {
            val source = code(file)
            if (file.name == "VaultSizeWarning.kt") {
                // The Settings line is the one other reader: it asks for the vault's size and nothing else.
                assertEquals(listOf("repository.vaultBytes()"), Regex("repository\\.\\w+\\(\\)").findAll(source).map { it.value }.toList(), "the Settings line only asks for the size")
            } else {
                assertTrue("repository." !in source && "Part2Repository" !in source, "${file.name}: only the two models hold the repository (a screen goes through its model)")
            }
        }
        for (name in listOf("VaultViewModel.kt", "VaultOrderViewModel.kt")) {
            val source = code(File(folder, name))
            assertTrue("Room" !in source && "Dao" !in source && "CryptoHelper" !in source, "$name writes through Part2Repository, which encrypts; it never touches storage itself")
        }
    }

    @Test
    fun aPictureIsBuiltFromTheBytesWithTheLibrarysInMemoryDecoderAndNothingElse() {
        val source = code(File(folder, "LocalPageScanner.kt"))
        assertTrue("decodeToImageBitmap()" in source && "Bitmap.createBitmap" !in source && "BitmapFactory" !in source, "one in-memory decode call, no hand-made image code")
    }
}
