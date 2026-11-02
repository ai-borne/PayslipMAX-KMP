package com.payslipmax.pdfparser.part2.space

import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * WHY: the same promise as the Android reader on the file manager's attributes: the documents volume reads as real free bytes, a path
 * that is not there reads as unknown (never zero), and the read is cheap enough to run before every scan (best of three; Native timing is noisy).
 */
class IosFreeSpaceReaderTest {
    @Test
    fun theDocumentsVolumeReadsAsRealBytes() {
        val free = IosFreeSpaceReader().freeBytes()

        assertNotNull(free)
        assertTrue(free > 0, "free $free")
    }

    @Test
    fun aMissingPathIsUnknownNotZero() {
        assertNull(IosFreeSpaceReader { "/no/such/place/for/space/test" }.freeBytes())
        assertNull(IosFreeSpaceReader { null }.freeBytes())
    }

    @Test
    fun theReadIsCheap() {
        val reader = IosFreeSpaceReader()
        val best =
            (1..3).minOf {
                TimeSource.Monotonic.markNow().let { start ->
                    repeat(50) { reader.freeBytes() }
                    start.elapsedNow().inWholeMilliseconds
                }
            }

        assertTrue(best < 1_000, "50 reads took $best ms")
    }
}
