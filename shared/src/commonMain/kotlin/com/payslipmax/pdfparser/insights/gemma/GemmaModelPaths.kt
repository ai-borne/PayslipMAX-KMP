package com.payslipmax.pdfparser.insights.gemma

/**
 * Platform-writable directory the Gemma model file is downloaded to and read from. Single source
 * of truth for "where the model lives" — [GemmaModelDownloadManager], [GemmaModelStorageManager]
 * and the Android production parse path (`PdfParser.kt`) all resolve through this.
 */
expect fun gemmaModelStorageDir(): String

expect fun fileExistsAt(path: String): Boolean

/**
 * Atomically moves [fromPath] onto [toPath] (overwriting any existing target), used to promote a
 * verified staging slot into the active slot. Returns false if the move fails. The atomicity is what
 * keeps the active slot never-half-written: the reader either sees the old file or the new one.
 */
expect fun moveFileAt(
    fromPath: String,
    toPath: String,
): Boolean

/** Deletes the file at [path]. Returns true if it no longer exists afterwards. */
expect fun deleteFileAt(path: String): Boolean

/** Reads a small text file (the active-slot version record). Returns null if absent/unreadable. */
expect fun readTextFileAt(path: String): String?

/** Writes [content] to a small text file (the active-slot version record). Returns false on failure. */
expect fun writeTextFileAt(
    path: String,
    content: String,
): Boolean

/**
 * Streams the file at [path] to [onChunk] in fixed-size buffers so a ~500MB model can be hashed
 * without ever being fully in memory. The buffer is reused across calls, so [onChunk] must consume
 * only the first `length` bytes and must not retain the array. Returns false if the file can't be read.
 */
expect fun readFileInChunks(
    path: String,
    onChunk: (chunk: ByteArray, length: Int) -> Unit,
): Boolean

interface ModelSink {
    fun append(bytes: ByteArray)

    fun close()
}

/** Streams bytes to disk incrementally so a ~1GB model download is never buffered fully in memory. */
expect class ModelFileSink(path: String) : ModelSink {
    override fun append(bytes: ByteArray)

    override fun close()
}
