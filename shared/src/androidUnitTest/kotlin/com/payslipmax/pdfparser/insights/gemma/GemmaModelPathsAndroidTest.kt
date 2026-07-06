package com.payslipmax.pdfparser.insights.gemma

import org.junit.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemmaModelPathsAndroidTest {
    @Test
    fun fileExistsAtFindsARealFileAndRejectsAMissingOne() {
        val tempFile = File.createTempFile("gemma_model_paths_test", ".litertlm")
        tempFile.deleteOnExit()
        try {
            assertTrue(fileExistsAt(tempFile.absolutePath))
        } finally {
            tempFile.delete()
        }

        assertFalse(fileExistsAt(tempFile.absolutePath))
        assertFalse(fileExistsAt(""))
    }

    @Test
    fun gemmaModelStorageDirNeverThrowsAndOnlyReturnsExistingDirectories() {
        // No real android.content.Context is available in a plain-JVM unit test, so this can
        // legitimately resolve to "" here — the invariant this proves is that it degrades safely
        // rather than crashing, and never claims a directory exists when it doesn't.
        val dir = gemmaModelStorageDir()
        assertTrue(dir.isEmpty() || File(dir).exists())
    }

    @Test
    fun resolveInstalledGemmaModelPathDegradesSafelyWithoutAContext() {
        // No real android.content.Context is available in a plain-JVM unit test (ContextHolder.context
        // is never set here), so AssetPackManagerFactory can't be reached — the invariant this proves
        // is that this degrades to null rather than crashing, never claiming a model is installed.
        assertNull(resolveInstalledGemmaModelPath())
    }
}
