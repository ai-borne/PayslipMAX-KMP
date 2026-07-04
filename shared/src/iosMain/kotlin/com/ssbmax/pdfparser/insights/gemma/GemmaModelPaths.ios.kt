@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ssbmax.pdfparser.insights.gemma

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.O_CREAT
import platform.posix.O_RDONLY
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.close
import platform.posix.memcpy
import platform.posix.open
import platform.posix.read
import platform.posix.rename
import platform.posix.write

actual fun gemmaModelStorageDir(): String = documentDirectory()

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && NSFileManager.defaultManager.fileExistsAtPath(path)

actual fun moveFileAt(
    fromPath: String,
    toPath: String,
): Boolean {
    // POSIX rename atomically replaces the destination when both live on the same volume (active and
    // staging slots share the Documents directory here), so the active slot is never seen half-written.
    return rename(fromPath, toPath) == 0
}

actual fun deleteFileAt(path: String): Boolean {
    val fileManager = NSFileManager.defaultManager
    if (!fileManager.fileExistsAtPath(path)) return true
    return fileManager.removeItemAtPath(path, error = null)
}

actual fun readTextFileAt(path: String): String? {
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    return data.toByteArray().decodeToString()
}

actual fun writeTextFileAt(
    path: String,
    content: String,
): Boolean {
    return content.encodeToByteArray().toNSData().writeToFile(path, atomically = true)
}

actual fun readFileInChunks(
    path: String,
    onChunk: (chunk: ByteArray, length: Int) -> Unit,
): Boolean {
    val fd = open(path, O_RDONLY)
    if (fd < 0) return false
    try {
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val readCount =
                buffer.usePinned { pinned ->
                    read(fd, pinned.addressOf(0), buffer.size.convert()).toInt()
                }
            if (readCount <= 0) break
            onChunk(buffer, readCount)
        }
        return true
    } finally {
        close(fd)
    }
}

private fun NSData.toByteArray(): ByteArray {
    val size = this.length.toInt()
    if (size == 0) return ByteArray(0)
    val byteArray = ByteArray(size)
    byteArray.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return byteArray
}

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return this.usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = this.size.convert())
    }
}

private fun documentDirectory(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return documentDirectory?.path ?: ""
}

// Plain POSIX file I/O rather than NSFileHandle: it streams incoming chunks straight to disk
// without buffering a ~1GB model file in memory, and POSIX open/write/close is stable across
// Kotlin/Native SDK versions where Foundation's NSFileHandle factory bindings have proven brittle.
actual class ModelFileSink actual constructor(path: String) : ModelSink {
    private val fd: Int

    init {
        // The sandbox's Documents directory isn't always guaranteed to already exist (e.g. in a
        // bare XCTest host process) — create it first, mirroring the androidMain sink's
        // File.parentFile?.mkdirs().
        val dir = path.substringBeforeLast('/', missingDelimiterValue = "")
        if (dir.isNotEmpty()) {
            NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
        }

        // O_TRUNC: every downloadModel() call is a full, non-resumable download from byte zero, so
        // a stale partial file from a previous failed attempt must not be appended onto.
        // Mode 420 == 0644 (rw-r--r--).
        fd = open(path, O_WRONLY or O_CREAT or O_TRUNC, 420)
        check(fd >= 0) { "Unable to open file for writing at $path" }
    }

    actual override fun append(bytes: ByteArray) {
        if (bytes.isEmpty()) return
        bytes.usePinned { pinned ->
            write(fd, pinned.addressOf(0), bytes.size.convert())
        }
    }

    actual override fun close() {
        close(fd)
    }
}
