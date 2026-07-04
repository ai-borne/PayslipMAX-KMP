package com.ssbmax.pdfparser.insights.gemma

/**
 * Platform-writable directory the Gemma model file is downloaded to and read from. Single source
 * of truth for "where the model lives" — [GemmaModelDownloadManager], [GemmaModelStorageManager]
 * and the Android production parse path (`PdfParser.kt`) all resolve through this.
 */
expect fun gemmaModelStorageDir(): String

expect fun fileExistsAt(path: String): Boolean

interface ModelSink {
    fun append(bytes: ByteArray)

    fun close()
}

/** Streams bytes to disk incrementally so a ~1GB model download is never buffered fully in memory. */
expect class ModelFileSink(path: String) : ModelSink {
    override fun append(bytes: ByteArray)

    override fun close()
}
