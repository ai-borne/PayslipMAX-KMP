package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.data.InMemoryLastBackupStorage
import com.payslipmax.pdfparser.part2.domain.EntryForm
import com.payslipmax.pdfparser.part2.domain.Part2LookupIndex
import com.payslipmax.pdfparser.testing.FakePart2Repository
import com.payslipmax.pdfparser.testing.SyntheticGuidePart2
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** The Bank's view models over the in-memory repository and the synthetic Part II lookup, with a fixed day and clock so ages are exact. */
@OptIn(ExperimentalCoroutinesApi::class)
open class BankTestBase {
    val repository = FakePart2Repository()
    val lastBackup = InMemoryLastBackupStorage()
    val dispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    val today = CalendarDate(2026, 10, 30)
    var now = 1_000_000L
    private var ids = 0
    private val index = Part2LookupIndex(SyntheticGuidePart2.lookup)

    fun bank(): BankViewModel =
        BankViewModel(
            repository = repository,
            formFor = { EntryForm.of(index, it) },
            nameOf = { index.code(it)?.name },
            lastBackup = lastBackup,
            today = { today },
            clock = { now },
            newId = { "new-${++ids}" },
            dispatcher = dispatcher,
        )
}
