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
    private val mapper = File("src/commonMain/kotlin/com/payslipmax/pdfparser/repository/BackupFailure.kt")
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

    /**
     * Rewritten in docs/Plan/14a P6: `restoreFailedStatus` is replaced by the one mapper, `BackupFailure.of` (shared), and the screen's
     * status only looks its fixed line up. Neither may read or match an exception's text, and the screen may not show it.
     */
    @Test
    fun theStatusMappingAndTheServiceNeverMatchOnMessageText() {
        val mapping = code(mapper).substringAfter("fun of(")
        assertTrue("is DecryptionFailedException" in mapping, "the failure is chosen by type")
        val matchers = listOf("contains(", "startsWith(", "endsWith(", "Regex", "equals(", "== \"", ".message")
        assertEquals(emptyList(), matchers.filter { it in mapping })
        assertEquals(emptyList(), matchers.filter { it in code(status) }, "the screen shows fixed lines only")
        assertTrue("contains(" !in code(service) && ".message" !in code(service))
    }

    @Test
    fun theExceptionIsBuiltWithAFixedMessageAndNoCause() {
        val text = code(exception)
        assertTrue("class DecryptionFailedException : Exception(\"" in text, "a fixed message, and no cause parameter")
    }
}
