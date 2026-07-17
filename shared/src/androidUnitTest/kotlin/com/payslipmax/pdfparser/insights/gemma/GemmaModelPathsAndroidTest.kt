package com.payslipmax.pdfparser.insights.gemma

import com.payslipmax.pdfparser.crypto.ContextHolder
import com.payslipmax.pdfparser.subscription.isDebugBuild
import io.mockk.every
import io.mockk.mockk
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemmaModelPathsAndroidTest {
    @After
    fun tearDown() {
        ContextHolder.context = null
    }

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
        // is that this degrades to null rather than crashing, never claiming a model is installed
        // (and no debug sideload file happens to exist at the resolved-empty storage dir).
        assertNull(resolveInstalledGemmaModelPath())
    }

    @Test
    fun resolveInstalledGemmaModelPathFindsADebugSideloadedFileInFilesDirWhenPlayCoreIsUnusable() {
        // This test source set runs under both testDebugUnitTest and testReleaseUnitTest, but the
        // fallback under test is deliberately debug-only — skip (not silently pass) under release,
        // where isDebugBuild() is false and the fallback branch never fires.
        assumeTrue(isDebugBuild())

        // A plain-JVM test's Context is unusable for real Play Core (mirrors a real debug adb
        // install with no Play-distributed pack) — runCatching around AssetPackManagerFactory must
        // degrade to the debug fallback rather than propagate, and that fallback must find a file
        // manually staged (e.g. via adb push) at the app's files dir under the canonical filename.
        val tempDir =
            File.createTempFile("gemma_sideload_test", "").apply {
                delete()
                mkdir()
            }
        val fileName = GemmaModelStorageManager().getRecommendedModelFileName()
        File(tempDir, fileName).writeText("not a real gemma model")

        ContextHolder.context =
            mockk {
                every { filesDir } returns tempDir
            }

        try {
            assertEquals("${tempDir.absolutePath}/$fileName", resolveInstalledGemmaModelPath())
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
