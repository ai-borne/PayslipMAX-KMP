package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.repository.PageShare
import com.payslipmax.pdfparser.repository.PageShareException

/** A page share that keeps what it was handed in memory, so a test can say "the officer's page went to the sheet, and only that page". */
class FakePageShare : PageShare {
    /** Copies of every page handed to the share sheet, in order; the real thing is told to forget the bytes at once, a test copy is not. */
    val shared = mutableListOf<ByteArray>()
    var fails = false
    var sweepCount = 0
        private set

    override fun share(jpeg: ByteArray): Result<Unit> {
        if (fails) return Result.failure(PageShareException())
        shared += jpeg.copyOf()
        return Result.success(Unit)
    }

    override fun sweep() {
        sweepCount++
    }
}
