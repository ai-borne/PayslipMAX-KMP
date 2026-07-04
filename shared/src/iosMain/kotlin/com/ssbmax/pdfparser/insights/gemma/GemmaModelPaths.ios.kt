@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ssbmax.pdfparser.insights.gemma

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.posix.O_CREAT
import platform.posix.O_TRUNC
import platform.posix.O_WRONLY
import platform.posix.close
import platform.posix.open
import platform.posix.write

actual fun gemmaModelStorageDir(): String = documentDirectory()

actual fun fileExistsAt(path: String): Boolean = path.isNotEmpty() && NSFileManager.defaultManager.fileExistsAtPath(path)

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
