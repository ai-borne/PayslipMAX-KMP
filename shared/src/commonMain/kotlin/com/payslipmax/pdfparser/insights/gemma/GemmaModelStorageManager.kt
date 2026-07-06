package com.payslipmax.pdfparser.insights.gemma

data class GemmaModelInfo(
    val modelName: String = "Gemma 3 1B IT (INT4)",
    val filePath: String,
    val fileSizeBytes: Long = 0L,
    val isReady: Boolean = false,
)

/**
 * The on-disk existence check [GemmaModelStorageManager] needs, grouped behind one collaborator so
 * it can be unit-tested with an in-memory fake instead of touching the filesystem. The default
 * delegates to the platform `expect` function in [GemmaModelPaths].
 */
interface GemmaModelFileOps {
    fun exists(path: String): Boolean
}

object DefaultGemmaModelFileOps : GemmaModelFileOps {
    override fun exists(path: String): Boolean = fileExistsAt(path)
}

/**
 * SSOT for "what model file lives on disk". Play Asset Delivery (Android) and Background Assets
 * (iOS) deliver the model pre-verified by the store, so there is no self-managed checksum/staging/
 * promote dance here — [verifyModelFile] is just an app-level sanity check independent of who
 * delivered the bytes.
 */
class GemmaModelStorageManager(
    private val storageDir: () -> String = ::gemmaModelStorageDir,
    private val fileOps: GemmaModelFileOps = DefaultGemmaModelFileOps,
    private val isDeviceSupported: () -> Boolean = { DeviceCapabilityManager().checkGemmaSupport().isSupported },
) {
    /** The filename the parse path loads the engine from. */
    fun getRecommendedModelFileName(): String = ACTIVE_SLOT_FILE

    fun verifyModelFile(filePath: String): GemmaModelInfo {
        if (filePath.isEmpty()) {
            return GemmaModelInfo(filePath = "", isReady = false)
        }
        val fullPath = fullPath(filePath)
        // LiteRT-LM replaced the MediaPipe `.task` container; gating on `.litertlm` is what keeps
        // isReady from silently staying false forever (which would loop the install trigger endlessly).
        val isReady = isDeviceSupported() && filePath.endsWith(".litertlm") && fileOps.exists(fullPath)
        return GemmaModelInfo(
            filePath = fullPath,
            isReady = isReady,
        )
    }

    private fun fullPath(fileName: String): String = "${storageDir()}/$fileName"

    companion object {
        const val ACTIVE_SLOT_FILE = "gemma-active.litertlm"
    }
}
