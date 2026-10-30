package com.payslipmax.pdfparser.crypto

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the restore message is chosen by the failure's type. A later edit that goes back to reading `.message` in the decrypt path or in the
 * status mapping would work on one platform and print library text on the other, and no behaviour test would notice until a phone did.
 */
class DecryptFailureSourceTest {
    private val actuals = listOf("androidMain", "iosMain").map { File("src/$it/kotlin/com/payslipmax/pdfparser/crypto/CryptoHelper.kt") }
    private val status = File("../composeApp/src/commonMain/kotlin/com/payslipmax/pdfparser/ui/screens/RestoreFailureStatus.kt")
    private val service = File("src/commonMain/kotlin/com/payslipmax/pdfparser/repository/PayslipBackupService.kt")
    private val exception = File("src/commonMain/kotlin/com/payslipmax/pdfparser/crypto/DecryptionFailedException.kt")

    private fun code(file: File): String {
        assertTrue(file.isFile, "run from the shared module: ${file.absolutePath}")
        return file.readLines().filterNot { it.trim().startsWith("//") || it.trim().startsWith("*") || it.trim().startsWith("/*") }.joinToString("\n")
    }

    private fun decryptBody(file: File): String {
        val text = code(file)
        val start = text.indexOf("actual fun decrypt")
        return text.substring(start, text.indexOf("actual fun sha256"))
    }

    @Test
    fun neitherDecryptReadsAnExceptionsTextOrKeepsItAsACause() {
        for (file in actuals) {
            val body = decryptBody(file)
            assertTrue(".message" !in body && ".cause" !in body && "DecryptionFailedException(e" !in body, "${file.path}: decide by type, keep no cause")
        }
    }

    @Test
    fun theStatusMappingAndTheServiceNeverMatchOnMessageText() {
        val mapping = code(status).substringAfter("fun restoreFailedStatus")
        assertTrue("is DecryptionFailedException" in mapping, "the status is chosen by type")
        // The only `.message` allowed is the one that shows our own fixed text of two types we define.
        assertEquals(emptyList(), listOf("contains(", "startsWith(", "endsWith(", "Regex", "equals(", "== \"").filter { it in mapping })
        assertTrue("contains(" !in code(service) && ".message" !in code(service))
    }

    @Test
    fun theExceptionIsBuiltWithAFixedMessageAndNoCause() {
        val text = code(exception)
        assertTrue("class DecryptionFailedException : Exception(\"" in text, "a fixed message, and no cause parameter")
    }
}
