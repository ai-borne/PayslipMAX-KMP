package com.payslipmax.pdfparser.ui.screens.guide.part2.orders

import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.Part2WriteResult
import com.payslipmax.pdfparser.part2.domain.BankEntry

/** A repository that refuses to save one entry, as a full or broken store would, and behaves like [delegate] for everything else. */
class FailingEntryRepository(
    private val delegate: Part2Repository,
    private val failFor: String,
) : Part2Repository by delegate {
    override suspend fun saveEntry(entry: BankEntry): Part2WriteResult = if (entry.id == failFor) Part2WriteResult.REJECTED else delegate.saveEntry(entry)
}
