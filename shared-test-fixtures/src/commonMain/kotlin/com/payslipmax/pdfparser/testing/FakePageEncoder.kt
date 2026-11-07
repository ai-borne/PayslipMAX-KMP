package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.pages.PageEncoder

/**
 * A [PageEncoder] that passes the bytes through behind a marker byte, so a test can tell stored bytes from scanner bytes, and refuses a page
 * (answers null) when [refuse] says so, as the real encoder does for a page that is not an image or does not fit.
 */
class FakePageEncoder(
    private val refuse: (ByteArray) -> Boolean = { false },
) : PageEncoder {
    override fun encode(raw: ByteArray): ByteArray? = if (refuse(raw)) null else byteArrayOf(0x7E) + raw
}
