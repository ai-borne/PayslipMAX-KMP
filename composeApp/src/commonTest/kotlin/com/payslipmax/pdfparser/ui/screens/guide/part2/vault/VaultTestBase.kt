package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.VaultOrder
import com.payslipmax.pdfparser.part2.pages.PageEncoder
import com.payslipmax.pdfparser.part2.pages.PageScan
import com.payslipmax.pdfparser.part2.pages.PageScanner
import com.payslipmax.pdfparser.testing.FakePageScanner
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.FakeScannedPage
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest

/** What the vault view-model tests share: the in-memory repository, a pass-through encoder, scanners that answer on demand, and counting wrappers. */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class VaultTestBase {
    /** A repository that counts how often it was asked to write an order and can be told to refuse one, over the in-memory one. */
    class CountingRepository(
        private val inner: FakePart2Repository = FakePart2Repository(),
    ) : Part2Repository by inner {
        var orderSaves = 0
        var refuseOrders: Part2WriteResult? = null

        override suspend fun saveOrder(order: VaultOrder): Part2WriteResult {
            orderSaves++
            return refuseOrders ?: inner.saveOrder(order)
        }
    }

    /** Hands each page to the repository untouched; the real encoders are proved elsewhere. */
    protected val encoder =
        object : PageEncoder {
            override fun encode(raw: ByteArray): ByteArray = raw.copyOf()
        }

    /** A scanner that stays open until [finish] is called, so a test can act while a scan is in flight. */
    protected class OpenScanner : PageScanner {
        private val answer = CompletableDeferred<PageScan>()
        var scans = 0
            private set
        var leftoversDiscarded = 0
            private set

        override suspend fun scan(maxPages: Int): PageScan {
            scans++
            return answer.await()
        }

        override fun discardLeftovers() {
            leftoversDiscarded++
        }

        fun finish(scan: PageScan) {
            answer.complete(scan)
        }
    }

    protected fun pages(count: Int): PageScan.Pages = PageScan.Pages((1..count).map { FakeScannedPage(SyntheticPart2Drafts.jpeg(300, it), name = "page$it") })

    protected fun scanner(next: PageScan = PageScan.Cancelled) = FakePageScanner(next)

    protected fun check(block: suspend TestScope.(TestDispatcher) -> Unit) =
        runTest {
            block(StandardTestDispatcher(testScheduler))
        }
}
