package com.ssbmax.pdfparser.insights.gemma

data class GemmaModelInfo(
    val modelName: String = "Gemma 3 1B IT (INT4)",
    val filePath: String,
    val fileSizeBytes: Long = 0L,
    val isReady: Boolean = false,
)

class GemmaModelStorageManager(
    private val defaultFileName: String = "gemma-3-1b-it-int4.task",
) {
    fun verifyModelFile(filePath: String): GemmaModelInfo {
        if (filePath.isEmpty()) {
            return GemmaModelInfo(filePath = "", isReady = false)
        }
        val fullPath = "${gemmaModelStorageDir()}/$filePath"
        val capabilityManager = DeviceCapabilityManager()
        val status = capabilityManager.checkGemmaSupport()
        val isReady = status.isSupported && filePath.endsWith(".task") && fileExistsAt(fullPath)
        return GemmaModelInfo(
            filePath = fullPath,
            isReady = isReady,
        )
    }

    fun getRecommendedModelFileName(): String = defaultFileName
}
