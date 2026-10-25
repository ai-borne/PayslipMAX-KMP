package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.calendar.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.testing.SyntheticPart2Drafts
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle

/** What the vault list tests share: one repository, orders with a known id sequence, and a model that has settled before the test looks at it. */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class VaultListTestBase : VaultTestBase() {
    protected val repo = CountingRepository()
    private var ids = 0

    protected fun TestScope.model(
        dispatcher: TestDispatcher,
        repository: Part2Repository = repo,
        offset: Long = 0L,
    ) = VaultViewModel(repository, encoder, dispatcher, now = { 5_000L }, newId = { "order-${++ids}" }, localOffset = { offset }).also { advanceUntilIdle() }

    protected suspend fun saveOrder(
        id: String,
        no: String? = "N/$id",
        date: CalendarDate? = CalendarDate(2026, 3, 1),
        updatedAt: Long = 1_700_000_000_000L,
        draftId: String? = null,
        pages: Int = 0,
    ) {
        repo.saveOrder(SyntheticPart2Drafts.order(id, draftId, no, date, updatedAt))
        repeat(pages) { repo.addPage(id, SyntheticPart2Drafts.jpeg(200, it)) }
    }
}
