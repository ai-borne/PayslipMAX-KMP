package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.space.FreeSpaceReader

private const val PLENTY_BYTES = 10L * 1024 * 1024 * 1024

/** A [FreeSpaceReader] that answers [free] (plenty by default), or throws when [fails] is set. */
class FakeFreeSpaceReader(
    var free: Long? = PLENTY_BYTES,
    var fails: Boolean = false,
) : FreeSpaceReader {
    var reads = 0
        private set

    override fun freeBytes(): Long? {
        reads++
        check(!fails) { "the platform could not read free space" }
        return free
    }
}
