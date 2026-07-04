package com.ssbmax.pdfparser.insights.gemma

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * In-memory [GemmaModelFileOps] so the dual-slot state machine can be exercised without touching
 * disk. Keyed by full path exactly as the real filesystem would be.
 */
private class FakeFileOps(
    private val files: MutableMap<String, ByteArray> = mutableMapOf(),
    private val hashes: MutableMap<String, String> = mutableMapOf(),
) : GemmaModelFileOps {
    fun putModel(
        path: String,
        sha256: String,
    ) {
        files[path] = ByteArray(0)
        hashes[path] = sha256
    }

    fun putText(
        path: String,
        content: String,
    ) {
        files[path] = content.encodeToByteArray()
    }

    fun has(path: String): Boolean = files.containsKey(path)

    override fun exists(path: String): Boolean = files.containsKey(path)

    override fun move(
        fromPath: String,
        toPath: String,
    ): Boolean {
        val bytes = files.remove(fromPath) ?: return false
        files[toPath] = bytes
        hashes.remove(fromPath)?.let { hashes[toPath] = it }
        return true
    }

    override fun delete(path: String): Boolean {
        files.remove(path)
        hashes.remove(path)
        return true
    }

    override fun readText(path: String): String? = files[path]?.decodeToString()

    override fun writeText(
        path: String,
        content: String,
    ): Boolean {
        files[path] = content.encodeToByteArray()
        return true
    }

    override fun sha256(path: String): String? = hashes[path]
}

class GemmaModelStorageManagerTest {
    private val dir = "/models"
    private val activePath = "$dir/${GemmaModelStorageManager.ACTIVE_SLOT_FILE}"
    private val stagingPath = "$dir/${GemmaModelStorageManager.STAGING_SLOT_FILE}"
    private val metaPath = "$dir/${GemmaModelStorageManager.ACTIVE_META_FILE}"

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
    fun verifyModelFileReadyForPresentLitertlmFile() {
        val ops = FakeFileOps().apply { putModel(activePath, "hash") }
        // The positive ready-path the old code never exercised: a present `.litertlm` file on a
        // supported device is ready. Guards the `.endsWith(".litertlm")` gate (was `.task`).
        assertTrue(manager(ops).verifyModelFile(GemmaModelStorageManager.ACTIVE_SLOT_FILE).isReady)
    }

    @Test
    fun verifyModelFileNotReadyForWrongExtension() {
        val ops = FakeFileOps().apply { putModel("$dir/gemma-active.task", "hash") }
        // A leftover MediaPipe `.task` file must not count as ready under the LiteRT-LM runtime.
        assertFalse(manager(ops).verifyModelFile("gemma-active.task").isReady)
    }

    @Test
    fun verifyModelFileNotReadyWhenDeviceUnsupported() {
        val ops = FakeFileOps().apply { putModel(activePath, "hash") }
        assertFalse(manager(ops, supported = false).verifyModelFile(GemmaModelStorageManager.ACTIVE_SLOT_FILE).isReady)
    }

    @Test
    fun getActiveVersionNullBeforeAnyPromotion() {
        assertNull(manager(FakeFileOps()).getActiveVersion())
    }

    @Test
    fun promoteMovesStagingToActiveAndRecordsVersion() {
        val ops = FakeFileOps().apply { putModel(stagingPath, "goodhash") }
        val manager = manager(ops)

        assertTrue(manager.verifyStagingChecksum("goodhash"))
        assertTrue(manager.promoteStagingToActive("v2"))

        // Staging consumed, active populated, version recorded.
        assertFalse(ops.has(stagingPath))
        assertTrue(ops.has(activePath))
        assertEquals("v2", ops.readText(metaPath))
        assertEquals("v2", manager.getActiveVersion())
    }

    @Test
    fun checksumMismatchRejectsAndLeavesActiveUntouched() {
        val ops =
            FakeFileOps().apply {
                putModel(activePath, "existinghash")
                putText(metaPath, "v1")
                putModel(stagingPath, "actualhash")
            }
        val manager = manager(ops)

        // Manifest promised a different hash than what we downloaded → verification fails.
        assertFalse(manager.verifyStagingChecksum("expectedhash"))

        // The rollback contract: the previously-active slot and its version record survive intact.
        manager.discardStaging()
        assertFalse(ops.has(stagingPath))
        assertTrue(ops.has(activePath))
        assertEquals("v1", manager.getActiveVersion())
    }

    @Test
    fun promoteFailsWhenStagingMissing() {
        assertFalse(manager(FakeFileOps()).promoteStagingToActive("v1"))
    }

    @Test
    fun verifyStagingChecksumIsCaseInsensitive() {
        val ops = FakeFileOps().apply { putModel(stagingPath, "ABCDEF") }
        assertTrue(manager(ops).verifyStagingChecksum("abcdef"))
    }

    @Test
    fun getRecommendedModelFileNameIsActiveLitertlmSlot() {
        assertEquals("gemma-active.litertlm", manager(FakeFileOps()).getRecommendedModelFileName())
    }
}
