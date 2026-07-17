package com.payslipmax.pdfparser.insights.gemma

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** In-memory [GemmaModelFileOps] so [GemmaModelStorageManager] can be tested without touching disk. */
private class FakeFileOps(
    private val files: MutableSet<String> = mutableSetOf(),
) : GemmaModelFileOps {
    fun putModel(path: String) {
        files.add(path)
    }

    override fun exists(path: String): Boolean = files.contains(path)
}

class GemmaModelStorageManagerTest {
    private val dir = "/models"
    private val activePath = "$dir/${GemmaModelStorageManager.ACTIVE_SLOT_FILE}"

    private fun manager(
        fileOps: GemmaModelFileOps,
        supported: Boolean = true,
    ) = GemmaModelStorageManager(storageDir = { dir }, fileOps = fileOps, isDeviceSupported = { supported })

    @Test
    fun verifyModelFileReturnsNotReadyWhenFileMissing() {
        val manager = manager(FakeFileOps())
        // A resolvable filename that does not exist on disk must never be reported as ready.
        assertFalse(manager.verifyModelFile(GemmaModelStorageManager.ACTIVE_SLOT_FILE).isReady)
    }

    @Test
    fun verifyModelFileReturnsNotReadyForEmptyPath() {
        assertFalse(manager(FakeFileOps()).verifyModelFile("").isReady)
    }

    @Test
    fun verifyModelFileReadyForPresentLitertlmFile() {
        val ops = FakeFileOps().apply { putModel(activePath) }
        // The positive ready-path: a present `.litertlm` file on a supported device is ready.
        assertTrue(manager(ops).verifyModelFile(GemmaModelStorageManager.ACTIVE_SLOT_FILE).isReady)
    }

    @Test
    fun verifyModelFileNotReadyForWrongExtension() {
        val ops = FakeFileOps().apply { putModel("$dir/gemma-active.task") }
        // A leftover MediaPipe `.task` file must not count as ready under the LiteRT-LM runtime.
        assertFalse(manager(ops).verifyModelFile("gemma-active.task").isReady)
    }

    @Test
    fun verifyModelFileNotReadyWhenDeviceUnsupported() {
        val ops = FakeFileOps().apply { putModel(activePath) }
        assertFalse(manager(ops, supported = false).verifyModelFile(GemmaModelStorageManager.ACTIVE_SLOT_FILE).isReady)
    }

    @Test
    fun getRecommendedModelFileNameIsActiveLitertlmSlot() {
        assertEquals("gemma-active.litertlm", manager(FakeFileOps()).getRecommendedModelFileName())
    }
}
