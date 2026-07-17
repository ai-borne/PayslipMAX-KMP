@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.payslipmax.pdfparser.insights.gemma

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.create
import platform.Foundation.writeToFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GemmaModelPathsIosTest {
    private val tempPath = NSTemporaryDirectory() + "gemma_model_paths_test.litertlm"

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

        val probePath = "$dir/gemma_model_paths_storage_dir_test.tmp"
        val bytes = byteArrayOf(1)
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(probePath, atomically = true)
        }
        assertTrue(fileExistsAt(probePath))
        NSFileManager.defaultManager.removeItemAtPath(probePath, error = null)
    }

    @Test
    fun resolveInstalledGemmaModelPathDegradesSafelyWithoutTheAppGroupEntitlement() {
        // The App Group entitlement (Phase 4's remaining Xcode-side work, blocked on Apple
        // Developer Program enrollment) isn't configured in a plain XCTest host, so
        // containerURLForSecurityApplicationGroupIdentifier returns nil — this proves the
        // invariant that resolution degrades to null rather than crashing, never claiming a model
        // is installed when the container isn't even reachable (and no debug sideload file is
        // present in Documents either).
        assertNull(resolveInstalledGemmaModelPath())
    }

    @Test
    fun resolveInstalledGemmaModelPathFindsADebugSideloadedFileInDocumentsWhenTheAppGroupIsUnreachable() {
        // Mirrors the real-device workflow: with no App Group entitlement configured, a file
        // manually dropped into Documents (e.g. via Xcode's device file browser) under the
        // canonical active-slot filename must still resolve, since XCTest binaries are debug
        // binaries (Platform.isDebugBinary == true).
        val sideloadPath = "${gemmaModelStorageDir()}/${GemmaModelStorageManager().getRecommendedModelFileName()}"
        val bytes = "not a real gemma model".encodeToByteArray()
        bytes.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
                .writeToFile(sideloadPath, atomically = true)
        }

        try {
            assertEquals(sideloadPath, resolveInstalledGemmaModelPath())
        } finally {
            NSFileManager.defaultManager.removeItemAtPath(sideloadPath, error = null)
        }
    }
}
