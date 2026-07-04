package com.ssbmax.pdfparser.insights.gemma

data class GemmaModelInfo(
    val modelName: String = "Gemma 3 1B IT (INT4)",
    val filePath: String,
    val fileSizeBytes: Long = 0L,
    val isReady: Boolean = false,
)

/**
 * The set of on-disk operations the dual-slot state machine needs, grouped behind one collaborator
 * so [GemmaModelStorageManager] can be unit-tested with an in-memory fake instead of touching the
 * filesystem. The default delegates to the platform `expect` functions in [GemmaModelPaths].
 */
interface GemmaModelFileOps {
    fun exists(path: String): Boolean

    fun move(
        fromPath: String,
        toPath: String,
    ): Boolean

    fun delete(path: String): Boolean

    fun readText(path: String): String?

    fun writeText(
        path: String,
        content: String,
    ): Boolean

    fun sha256(path: String): String?
}

object DefaultGemmaModelFileOps : GemmaModelFileOps {
    override fun exists(path: String): Boolean = fileExistsAt(path)

    override fun move(
        fromPath: String,
        toPath: String,
    ): Boolean = moveFileAt(fromPath, toPath)

    override fun delete(path: String): Boolean = deleteFileAt(path)

    override fun readText(path: String): String? = readTextFileAt(path)

    override fun writeText(
        path: String,
        content: String,
    ): Boolean = writeTextFileAt(path, content)

    override fun sha256(path: String): String? = sha256OfFile(path)
}

/**
 * SSOT for "what model file/version lives on disk" and the dual-slot (A/B) state machine that keeps
 * the app usable across a model update: a new version is downloaded into the **staging** slot,
 * checksum-verified, and only then atomically promoted into the **active** slot. If verification
 * fails the active slot is left untouched, so a bad download can never take the app offline — the
 * rollback strategy sketched in docs/ai_insights_adoptgemma.md §10, now actually implemented.
 */
class GemmaModelStorageManager(
    private val storageDir: () -> String = ::gemmaModelStorageDir,
    private val fileOps: GemmaModelFileOps = DefaultGemmaModelFileOps,
    private val isDeviceSupported: () -> Boolean = { DeviceCapabilityManager().checkGemmaSupport().isSupported },
) {
    /** The active-slot filename — the single file the parse path loads the engine from. */
    fun getRecommendedModelFileName(): String = ACTIVE_SLOT_FILE

    /** The staging-slot filename a new download is streamed into before it is verified/promoted. */
    fun stagingSlotFileName(): String = STAGING_SLOT_FILE

    fun verifyModelFile(filePath: String): GemmaModelInfo {
        if (filePath.isEmpty()) {
            return GemmaModelInfo(filePath = "", isReady = false)
        }
        val fullPath = fullPath(filePath)
        // LiteRT-LM replaced the MediaPipe `.task` container; gating on `.litertlm` is what keeps
        // isReady from silently staying false forever (which would loop the download endlessly).
        val isReady = isDeviceSupported() && filePath.endsWith(".litertlm") && fileOps.exists(fullPath)
        return GemmaModelInfo(
            filePath = fullPath,
            isReady = isReady,
        )
    }

    /** The version currently recorded in the active slot, or null if nothing has been promoted yet. */
    fun getActiveVersion(): String? = fileOps.readText(metaPath())?.trim()?.ifEmpty { null }

    /**
     * True iff the downloaded staging file's SHA-256 matches [expectedSha256] from the manifest.
     * This is the gate that must pass before [promoteStagingToActive] is allowed to run.
     */
    fun verifyStagingChecksum(expectedSha256: String): Boolean {
        if (expectedSha256.isEmpty()) return false
        val actual = fileOps.sha256(fullPath(STAGING_SLOT_FILE)) ?: return false
        return actual.equals(expectedSha256, ignoreCase = true)
    }

    /**
     * Atomically promotes the verified staging slot to active and records [version]. Callers must
     * have passed [verifyStagingChecksum] first. Returns false (leaving the active slot untouched)
     * if the staging file is missing or the move fails.
     */
    fun promoteStagingToActive(version: String): Boolean {
        val staging = fullPath(STAGING_SLOT_FILE)
        if (!fileOps.exists(staging)) return false
        if (!fileOps.move(staging, fullPath(ACTIVE_SLOT_FILE))) return false
        return fileOps.writeText(metaPath(), version)
    }

    /** Deletes a leftover staging file (e.g. after a failed checksum) so it can't waste ~500MB. */
    fun discardStaging(): Boolean = fileOps.delete(fullPath(STAGING_SLOT_FILE))

    private fun fullPath(fileName: String): String = "${storageDir()}/$fileName"

    private fun metaPath(): String = fullPath(ACTIVE_META_FILE)

    companion object {
        /** Slot A: the active model the parse path loads. */
        const val ACTIVE_SLOT_FILE = "gemma-active.litertlm"

        /** Slot B: where a new version is downloaded and verified before promotion. */
        const val STAGING_SLOT_FILE = "gemma-staging.litertlm"

        /** Small text record of which version currently occupies the active slot. */
        const val ACTIVE_META_FILE = "gemma-active.version"
    }
}
