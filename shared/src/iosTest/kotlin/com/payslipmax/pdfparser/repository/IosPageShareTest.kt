package com.payslipmax.pdfparser.repository

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the page the officer shares is a plain picture of an order, so on Kotlin/Native's real file calls it must sit in exactly one file
 * under the fixed name, reach the share sheet from that file, be deleted when the sheet reports it is done, be replaced (never
 * accumulated) by the next page, and leave nothing behind when the sheet cannot open or the launch sweep runs.
 */
@OptIn(ExperimentalForeignApi::class)
class IosPageShareTest {
    private val dir = NSTemporaryDirectory().trimEnd('/') + "/page-" + NSUUID().UUIDString
    private val manager = NSFileManager.defaultManager

    private fun names() = (manager.contentsOfDirectoryAtPath(dir, null) as List<*>?).orEmpty().map { it as String }.sorted()

    private fun withDir(body: () -> Unit) {
        manager.createDirectoryAtPath(dir, true, null, null)
        try {
            body()
        } finally {
            manager.removeItemAtPath(dir, null)
        }
    }

    @Test
    fun thePageIsWrittenToTheFixedNameAndHandedToTheSheetFromThatFile() =
        withDir {
            val offered = mutableListOf<String>()
            val share = IosPageShare(dir, share = { path, _ -> offered += path })

            assertTrue(share.share(byteArrayOf(1, 2, 3)).isSuccess)

            assertEquals(listOf(PAGE_SHARE_FILE_NAME), names())
            assertEquals(listOf("$dir/$PAGE_SHARE_FILE_NAME"), offered)
        }

    @Test
    fun theFileIsDeletedWhenTheSheetSaysItIsDone() =
        withDir {
            val share = IosPageShare(dir, share = { _, done -> done() })

            share.share(byteArrayOf(1, 2, 3))

            assertEquals(emptyList(), names())
        }

    @Test
    fun aSecondPageReplacesTheFirst() =
        withDir {
            val share = IosPageShare(dir, share = { _, _ -> })
            share.share(byteArrayOf(1))
            share.share(byteArrayOf(2, 2))

            assertEquals(listOf(PAGE_SHARE_FILE_NAME), names())
        }

    @Test
    fun aSheetThatCannotOpenLeavesNothingAndTheSweepClearsALeftoverFile() =
        withDir {
            val broken = IosPageShare(dir, share = { _, _ -> error("no sheet") })
            assertTrue(broken.share(byteArrayOf(1)).isFailure)
            assertEquals(emptyList(), names())

            IosPageShare(dir, share = { _, _ -> }).apply {
                share(byteArrayOf(1))
                sweep()
            }
            assertEquals(emptyList(), names())
        }
}
