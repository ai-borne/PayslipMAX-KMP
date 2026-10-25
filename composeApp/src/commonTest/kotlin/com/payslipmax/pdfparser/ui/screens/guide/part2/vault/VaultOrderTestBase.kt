package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/** What one order's tests share: a repository holding the order, the notices and list refreshes the model reports, and a model that has settled. */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class VaultOrderTestBase : VaultTestBase() {
    protected val repo = CountingRepository()
    protected val notices = mutableListOf<String?>()
    protected var changes = 0

    protected suspend fun stored(
        id: String = "order-1",
        no: String? = null,
        date: CalendarDate? = null,
        draftId: String? = "draft-1",
        pages: Int = 0,
    ) {
        repo.saveOrder(SyntheticPart2Drafts.order(id, draftId, no, date))
        repeat(pages) { repo.addPage(id, SyntheticPart2Drafts.jpeg(200, it)) }
        repo.orderSaves = 0
    }

    protected fun TestScope.model(
        dispatcher: TestDispatcher,
        id: String = "order-1",
    ) = VaultOrderViewModel(id, repo, encoder, { notices += it }, { changes++ }, now = { 9_000L }, dispatcher = dispatcher).also { advanceUntilIdle() }
}
