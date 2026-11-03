package com.payslipmax.pdfparser.repository

import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: the page the officer shares is a plain picture of an order in the app's private cache, so on the real files it must be written
 * to the one fixed name, be what the share sheet is given, be replaced (never accumulated) by the next page, leave nothing behind when
 * the sheet cannot open, and be cleared by the launch sweep.
 */
class AndroidPageShareTest {
    private lateinit var dir: File
    private val offered = mutableListOf<File>()

    @Before
    fun make() {
        dir =
            File.createTempFile("cache", "").apply {
                delete()
                mkdirs()
            }
    }

    @After
    fun remove() {
        dir.deleteRecursively()
    }

    private fun names() = dir.list().orEmpty().sorted()

    private fun share(sheet: (File) -> Unit = { offered += it }) = AndroidPageShare(cacheDir = { dir }, share = sheet)

    @Test
    fun thePageIsWrittenToTheFixedNameAndThatFileIsWhatTheSheetIsGiven() {
        assertTrue(share().share(byteArrayOf(1, 2, 3)).isSuccess)

        assertEquals(listOf(PAGE_SHARE_FILE_NAME), names())
        assertEquals(listOf(PAGE_SHARE_FILE_NAME), offered.map { it.name })
        assertContentEquals(byteArrayOf(1, 2, 3), offered.single().readBytes())
    }

    @Test
    fun aSecondPageReplacesTheFirstSoNothingAccumulatesInTheCache() {
        val page = share()
        page.share(byteArrayOf(1))
        page.share(byteArrayOf(2, 2))

        assertEquals(listOf(PAGE_SHARE_FILE_NAME), names())
        assertContentEquals(byteArrayOf(2, 2), File(dir, PAGE_SHARE_FILE_NAME).readBytes())
    }

    @Test
    fun aSheetThatCannotOpenLeavesNoPictureInTheCache() {
        val failure = share { error("no sheet") }.share(byteArrayOf(1)).exceptionOrNull()

        assertTrue(failure is PageShareException)
        assertEquals(emptyList(), names())
    }

    @Test
    fun theLaunchSweepClearsAPageAKilledProcessLeft() {
        val page = share()
        page.share(byteArrayOf(1))

        page.sweep()

        assertEquals(emptyList(), names())
    }
}
