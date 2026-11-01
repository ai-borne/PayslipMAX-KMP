package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.runtime.compositionLocalOf
import com.payslipmax.pdfparser.guide.preclaim.CalendarDate
import com.payslipmax.pdfparser.part2.data.Part2Repository
import com.payslipmax.pdfparser.part2.data.StoredPart2
import com.payslipmax.pdfparser.part2.print.IndexPrinter
import com.payslipmax.pdfparser.ui.theme.IndexStrings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Prints the whole Bank, whatever chip is chosen, through [IndexPrinter]. It reads the Bank only when asked, hands the worded page to the
 * printer under the fixed job name and keeps nothing: no record is written whether the job is printed, cancelled or fails, and the outcome
 * is not used. One print at a time. Nothing here logs or reports (an index holds the officer's casualties).
 */
class IndexPrinting(
    private val repository: Part2Repository,
    private val nameOf: (String) -> String?,
    private val today: () -> CalendarDate,
    private val scope: CoroutineScope,
) {
    private val busy = MutableStateFlow(false)

    /** True while a print is being prepared or its dialog is open: the button is off. */
    val printing: StateFlow<Boolean> get() = busy

    fun print(printer: IndexPrinter) {
        if (!busy.compareAndSet(expect = false, update = true)) return
        scope.launch {
            try {
                val entries = repository.observeEntries().catch { emit(StoredPart2(emptyList())) }.first().items
                val orders = repository.observeOrders().catch { emit(StoredPart2(emptyList())) }.first().items
                val document = IndexDocumentBuilder.build(entries, orders, nameOf, today())
                if (document.years.isNotEmpty()) printer.print(IndexStrings.jobName, document)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Nothing printed; the button is usable again.
            } finally {
                busy.value = false
            }
        }
    }
}

/** A printer a test (or a host with its own) provides in place of the platform's; null means the platform's own, built from the screen. */
internal val LocalIndexPrinter = compositionLocalOf<IndexPrinter?> { null }
