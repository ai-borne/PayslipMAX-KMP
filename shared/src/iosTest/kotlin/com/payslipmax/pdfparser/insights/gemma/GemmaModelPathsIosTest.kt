@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.payslipmax.pdfparser.insights.gemma

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GemmaModelPathsIosTest {
    private val tempPath = NSTemporaryDirectory() + "gemma_model_paths_test.task"

    @AfterTest
    fun cleanup() {
        NSFileManager.defaultManager.removeItemAtPath(tempPath, error = null)
    }

    @Test
    fun fileExistsAtFindsARealFileAndRejectsAMissingOne() {
        assertFalse(fileExistsAt(tempPath))
        assertFalse(fileExistsAt(""))

        val bytes = "not a real gemma model".encodeToByteArray()
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(tempPath, atomically = true)
        }

        assertTrue(fileExistsAt(tempPath))
    }

    @Test
    fun gemmaModelStorageDirResolvesToAWritableAbsolutePath() {
        // The XCTest bundle sandbox doesn't necessarily pre-create the app's Documents directory
        // the way a real app launch does, so this proves the resolved path is usable (absolute,
        // and a file can be written under it) rather than asserting the directory pre-exists.
        val dir = gemmaModelStorageDir()
        assertTrue(dir.isNotEmpty())
        assertTrue(dir.startsWith("/"))

        val sink = ModelFileSink("$dir/gemma_model_paths_storage_dir_test.tmp")
        sink.append(byteArrayOf(1))
        sink.close()
        assertTrue(fileExistsAt("$dir/gemma_model_paths_storage_dir_test.tmp"))
        NSFileManager.defaultManager.removeItemAtPath("$dir/gemma_model_paths_storage_dir_test.tmp", error = null)
    }

    @Test
    fun modelFileSinkPersistsAppendedBytesAndTruncatesOnReopen() {
        val sink = ModelFileSink(tempPath)
        sink.append(byteArrayOf(1, 2, 3))
        sink.append(byteArrayOf(4, 5))
        sink.close()
        assertTrue(fileExistsAt(tempPath))

        // Reopening for a fresh download must truncate the old content, not append onto it.
        val secondSink = ModelFileSink(tempPath)
        secondSink.append(byteArrayOf(9))
        secondSink.close()

        val data = NSData.dataWithContentsOfFile(tempPath)
        assertTrue(data != null && data.length.toInt() == 1)
    }
}
