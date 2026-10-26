package com.payslipmax.pdfparser.part2.pages

import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Structural guards on the page pipeline (Part II plan, Phase 6a), beside `Part2PrintSourceTest`. A scanned page is a photo of a
 * RESTRICTED order: it goes from the system scanner to the encrypted vault and nowhere else. It is never written to the gallery,
 * MediaStore, external storage or the FileProvider, never put on the clipboard or the network, never logged or reported, and the app asks
 * for no camera permission of its own on Android (the scanner runs in Google Play services). These scan every source set of the folder
 * and the screen glue in composeApp.
 */
class Part2PagesSourceTest {
    private val folders = listOf("commonMain", "androidMain", "iosMain").map { File("src/$it/kotlin/com/payslipmax/pdfparser/part2/pages") }
    private val glue = listOf("androidMain", "iosMain", "commonMain").map { File("../composeApp/src/$it/kotlin/com/payslipmax/pdfparser/ui/platform/PageScannerProvider.kt") }

    private fun files() = folders.flatMap { it.listFiles { f -> f.extension == "kt" }.orEmpty().toList() }.sortedBy { it.name } + glue

    private fun withoutComments(source: String): String =
        source.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), " ").lines().joinToString("\n") { it.substringBefore("//") }

    private fun code(file: File) = withoutComments(file.readText())

    @Test
    fun theFoldersAreWhereTheTestExpectsThemAndHoldBothPlatformsPipelines() {
        assertTrue(folders.all { it.isDirectory } && glue.all { it.isFile }, "run from the shared module: ${folders.map { it.absolutePath }}")
        for (expected in listOf("PageScanner.kt", "PageEncoder.kt", "PageCapture.kt", "JpegMetadata.kt", "AndroidPageScanner.kt", "AndroidPageEncoder.kt", "MlKitScanSource.kt", "IosPageScanner.kt", "IosPageEncoder.kt")) {
            assertTrue(expected in files().map { it.name }, "$expected is missing")
        }
    }

    @Test
    fun noPageFileTouchesTheGalleryExternalStorageTheFileProviderTheClipboardOrTheNetwork() {
        val forbidden =
            listOf(
                "MediaStore", "getExternal", "externalCacheDir", "externalFilesDir", "Environment.", "FileProvider", "ACTION_SEND", "ACTION_VIEW", "startActivity(",
                "Clipboard", "ClipData", "UIPasteboard", "UIActivityViewController", "PHPhotoLibrary", "PHAsset", "UIImageWriteToSavedPhotosAlbum", "UIImagePickerController",
                "HttpURLConnection", "NSURLSession", "NSURLConnection", "WebView", "okhttp", "java.net", "sharePdf", "filesDir", "noBackupFilesDir", "NSDocumentDirectory",
                "NSTemporaryDirectory", "NSFileManager", "writeToFile", "writeBytes", "FileOutputStream", "createTempFile", "outputStream(", "bufferedWriter",
            )
        for (file in files()) {
            val found = forbidden.filter { it in code(file) } + listOfNotNull("Intent(".takeIf { Regex("(?<![A-Za-z])Intent\\(").containsMatchIn(code(file)) })
            assertEquals(emptyList(), found, "${file.name} uses something a page must never touch: it goes from the scanner to the vault and nowhere else")
        }
    }

    @Test
    fun noPageFileLogsReportsOrPrintsAndNoneIncludesAnErrorsTextInAnything() {
        val forbidden =
            listOf("Logger", "CrashReporter", "TelemetrySanitizer", "println(", "print(", "printStackTrace", "Log.", "NSLog", "Regex(", "toRegex", ".message", ".cause", ".stackTrace", "error(", "check(", "require(")
        for (file in files()) {
            // `throw e` rethrows a CancellationException unchanged; any other throw would build a message.
            val found = forbidden.filter { it in code(file) } + listOfNotNull("throw".takeIf { Regex("throw (?!e\\s*$)", RegexOption.MULTILINE).containsMatchIn(code(file)) })
            assertEquals(emptyList(), found, "${file.name} must not log, report or raise anything that could carry what a page shows")
        }
    }

    @Test
    fun noPageFileTouchesAPayslipAnIdentityTheDatabaseOrTheCrypto() {
        val forbidden = listOf("Payslip", "Officer", "panNumber", "accountNumber", "Dao", "CryptoHelper", "RoomDatabase", "SharedPreferences", "NSUserDefaults")
        for (file in files()) {
            val found = forbidden.filter { it in code(file) }
            assertEquals(emptyList(), found, "${file.name}: pages reach storage only through Part2Repository")
        }
    }

    @Test
    fun theAppAsksForNoCameraPermissionOfItsOwnOnAndroid() {
        for (file in files()) {
            val found = listOf("CAMERA", "Manifest.permission", "requestPermission", "checkSelfPermission", "ActivityCompat").filter { it in code(file) }
            assertEquals(emptyList(), found, "${file.name}: the scanner runs in Google Play services, so the app holds no camera permission (the merged-manifest guard enforces it)")
        }
        val manifest = File("../composeApp/src/androidMain/AndroidManifest.xml").readText()
        // Phase 7b added the notification permission for Part II reminders; nothing else, and never the camera.
        val declared = Regex("<uses-permission android:name=\"([^\"]+)\"").findAll(manifest).map { it.groupValues[1] }.toList()
        assertEquals(listOf("android.permission.POST_NOTIFICATIONS"), declared)
        assertTrue("CAMERA" !in manifest)
    }

    @Test
    fun theOnlyPlaceAPageIsStoredIsTheRepositoryAndOnlyPageCaptureCallsIt() {
        val callers = files().filter { "addPage(" in code(it) }.map { it.name }

        assertEquals(listOf("PageCapture.kt"), callers, "one place moves a page, so one place guarantees its cleanup")
    }
}
