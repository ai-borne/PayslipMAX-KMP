package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.toByteArray
import com.payslipmax.pdfparser.crypto.toNSData
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.autoreleasepool
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSError
import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.fileHandleForReadingAtPath
import platform.Foundation.fileHandleForWritingAtPath
import platform.Foundation.writeData

/** Runs one NSFileHandle call that reports failure through an NSError; the error is dropped because its text can carry a path. */
@OptIn(ExperimentalForeignApi::class)
private inline fun <T> handleCall(call: (CPointer<ObjCObjectVar<NSError?>>) -> T?): T =
    memScoped {
        val error = alloc<ObjCObjectVar<NSError?>>()
        val out = call(error.ptr)
        if (out == null || error.value != null) throw BackupFileException()
        out
    }

/** Writes a backup to the file at [path], starting it afresh. Close it to flush and release the file. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class FileBackupSink(path: String) : ClosableBackupSink {
    private val handle: NSFileHandle

    init {
        if (!NSFileManager.defaultManager.createFileAtPath(path, null, null)) throw BackupFileException()
        handle = NSFileHandle.fileHandleForWritingAtPath(path) ?: throw BackupFileException()
    }

    override fun write(bytes: ByteArray) {
        autoreleasepool { handleCall { handle.writeData(bytes.toNSData(), it).takeIf { ok -> ok } } }
    }

    override fun close() {
        handleCall { handle.closeAndReturnError(it).takeIf { ok -> ok } }
    }
}

/** Reads a backup from the file at [path] by range. Close it to release the file. */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class FileBackupSource(path: String) : ClosableBackupSource {
    private val handle: NSFileHandle = NSFileHandle.fileHandleForReadingAtPath(path) ?: throw BackupFileException()

    override val size: Long =
        (NSFileManager.defaultManager.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? NSNumber)?.longLongValue
            ?: throw BackupFileException()

    override fun read(
        offset: Long,
        length: Int,
    ): ByteArray {
        require(offset >= 0 && length >= 0 && offset + length <= size) { "A range outside the backup." }
        if (length == 0) return ByteArray(0)
        // The NSData is autoreleased: with no pool draining (a background coroutine) a restore would pile up one page's worth per read.
        return autoreleasepool {
            handleCall { handle.seekToOffset(offset.toULong(), it).takeIf { ok -> ok } }
            val data = handleCall { handle.readDataUpToLength(length.toULong(), it) }
            if (data.length.toInt() != length) throw BackupFileException()
            data.toByteArray()
        }
    }

    override fun close() {
        handleCall { handle.closeAndReturnError(it).takeIf { ok -> ok } }
    }
}
