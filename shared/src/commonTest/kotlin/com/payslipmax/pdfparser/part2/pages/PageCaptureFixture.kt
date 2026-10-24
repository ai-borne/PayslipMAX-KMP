package com.payslipmax.pdfparser.part2.pages

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.Dispatchers

/** What the [PageCapture] tests share: a fake repository and scanner, an encoder that records what it was given, and page and order helpers. */
internal abstract class PageCaptureFixture {
    /** Passes bytes through; records the arrays it was given and returns a copy so the test can tell the raw from the stored bytes. */
    protected class RecordingEncoder(private val refuse: (ByteArray) -> Boolean = { false }, private val explode: (ByteArray) -> Boolean = { false }) : PageEncoder {
        val received = mutableListOf<ByteArray>()

        override fun encode(raw: ByteArray): ByteArray? {
            received += raw
            if (explode(raw)) error("MARKER_BOOM")
            return if (refuse(raw)) null else byteArrayOf(0x7E) + raw
        }
    }

    protected val repository = FakePart2Repository()
    protected val scanner = FakePageScanner()
    protected val encoder = RecordingEncoder()

    protected fun capture(
        encoder: PageEncoder = this.encoder,
        repository: Part2Repository = this.repository,
    ) = PageCapture(scanner, encoder, repository, Dispatchers.Unconfined)

    protected fun page(
        vararg bytes: Byte,
        log: MutableList<String>? = null,
        name: String = "p",
    ) = FakeScannedPage(bytes, log = log, name = name)

    protected suspend fun order(id: String = "o1") = repository.saveOrder(SyntheticPart2Drafts.order(id))
}
