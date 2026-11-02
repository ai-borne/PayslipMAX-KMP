package com.payslipmax.pdfparser.repository

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: a shared order page is the one place an order's plain picture leaves the vault, and only because the officer tapped Share. It must
 * sit in exactly one private file under a fixed name, so pages never pile up in the cache; a failed write or a sheet that cannot open
 * must leave nothing behind; and the launch sweep must clear what a killed process left.
 */
class PageShareTest {
    private class MemoryPageShare(private val sheetOpens: Boolean = true, private val diskWorks: Boolean = true) : FilePageShare() {
        val files = mutableMapOf<String, ByteArray>()
        var presented = 0
            private set

        override val path = "cache/$PAGE_SHARE_FILE_NAME"

        override fun write(jpeg: ByteArray) {
            check(diskWorks)
            files[path] = jpeg.copyOf()
        }

        override fun present(path: String) {
            check(sheetOpens)
            presented++
        }

        override fun delete(path: String) {
            files.remove(path)
        }
    }

    @Test
    fun aSharedPageIsWrittenToTheOneFixedFileAndTheSheetOpensOnIt() {
        val share = MemoryPageShare()

        assertTrue(share.share(byteArrayOf(1, 2, 3)).isSuccess)

        assertEquals(setOf("cache/$PAGE_SHARE_FILE_NAME"), share.files.keys)
        assertContentEquals(byteArrayOf(1, 2, 3), share.files.getValue("cache/$PAGE_SHARE_FILE_NAME"))
        assertEquals(1, share.presented)
    }

    @Test
    fun aSecondPageReplacesTheFirstSoPagesNeverPileUpInTheCache() {
        val share = MemoryPageShare()
        share.share(byteArrayOf(1))
        share.share(byteArrayOf(2))

        assertEquals(1, share.files.size)
        assertContentEquals(byteArrayOf(2), share.files.getValue("cache/$PAGE_SHARE_FILE_NAME"))
    }

    @Test
    fun aSheetThatCannotOpenLeavesNoPictureOfTheOrderBehind() {
        val share = MemoryPageShare(sheetOpens = false)

        assertTrue(share.share(byteArrayOf(1, 2, 3)).isFailure)

        assertTrue(share.files.isEmpty(), "a page nobody was offered must not stay on disk")
    }

    @Test
    fun aWriteThatFailsSaysSoWithoutNamingAPath() {
        val failure = MemoryPageShare(diskWorks = false).share(byteArrayOf(1)).exceptionOrNull()

        assertEquals("The page could not be shared.", failure?.message)
        assertTrue(failure is PageShareException)
    }

    @Test
    fun theLaunchSweepClearsAPageAKilledProcessLeft() {
        val share = MemoryPageShare()
        share.share(byteArrayOf(1))

        share.sweep()

        assertNull(share.files["cache/$PAGE_SHARE_FILE_NAME"])
    }
}
