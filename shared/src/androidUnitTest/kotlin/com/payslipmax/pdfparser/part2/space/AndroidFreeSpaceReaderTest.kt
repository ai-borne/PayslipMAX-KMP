package com.payslipmax.pdfparser.part2.space

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the gate is only as honest as its reader. A real directory must read as real free bytes (the figure the platform gives this app),
 * and a missing directory or context must read as unknown rather than zero: zero would refuse every scan on a healthy phone.
 */
class AndroidFreeSpaceReaderTest {
    @Test
    fun aRealDirectoryReadsAsItsUsableBytes() {
        val dir = File(System.getProperty("java.io.tmpdir"))

        val free = AndroidFreeSpaceReader { dir }.freeBytes()

        assertNotNull(free)
        assertTrue(free > 0 && kotlin.math.abs(free - dir.usableSpace) < 64L * 1024 * 1024, "free $free vs ${dir.usableSpace}")
    }

    @Test
    fun aDirectoryThatIsNotThereIsUnknownNotZero() {
        assertNull(AndroidFreeSpaceReader { File(System.getProperty("java.io.tmpdir"), "no-such-dir-for-space-test") }.freeBytes())
    }

    @Test
    fun noContextMeansUnknown() {
        assertNull(AndroidFreeSpaceReader { null }.freeBytes())
    }
}
