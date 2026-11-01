package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.PageStore
import com.payslipmax.pdfparser.testing.FakePageEncoder
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakeScannedPage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** The attach view model over the in-memory repository, a recording scanner and an encoder that can refuse a page. Synthetic bytes only. */
@OptIn(ExperimentalCoroutinesApi::class)
open class AttachTestBase {
    val fake = FakePart2Repository()
    var repository: Part2Repository = fake
    val scanner = FakePageScanner()
    val dispatcher = UnconfinedTestDispatcher()
    var refuseMarker: Byte? = null
    private var ids = 0

    fun pages(vararg names: String): List<FakeScannedPage> = names.map { FakeScannedPage(byteArrayOf(1, 2, it.length.toByte()), name = it) }

    fun scans(pages: List<FakeScannedPage>) {
        scanner.next = PageScan.Pages(pages)
    }

    fun attach(): AttachOrderViewModel =
        AttachOrderViewModel(
            repository = repository,
            pageStore = PageStore(FakePageEncoder { raw -> refuseMarker != null && raw.last() == refuseMarker }, repository, dispatcher),
            clock = { 5_000L },
            newId = { "order-${++ids}" },
            dispatcher = dispatcher,
        )
}
