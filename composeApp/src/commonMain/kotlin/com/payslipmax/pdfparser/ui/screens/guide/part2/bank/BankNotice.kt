package com.payslipmax.pdfparser.ui.screens.guide.part2.bank

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.payslipmax.pdfparser.ui.theme.AppDimensions
import com.payslipmax.pdfparser.ui.theme.OrdersBankStrings
import kotlinx.coroutines.flow.filterNotNull

/** What the officer is told after a save or a delete: what happened to which casualty. The code and the entry id only, never a date, a note or a name. */
data class BankNotice(
    val kind: Kind,
    val code: String,
    val entryId: String,
) {
    enum class Kind { ADDED, SAVED, DELETED }

    /** The message: fixed words around the casualty's code. */
    val message: String
        get() =
            when (kind) {
                Kind.ADDED -> OrdersBankStrings.noticeAdded(code)
                Kind.SAVED -> OrdersBankStrings.noticeSaved(code)
                Kind.DELETED -> OrdersBankStrings.noticeDeleted(code)
            }

    /** A new entry is waiting for its order copy, so it alone offers the next step. */
    val actionLabel: String? get() = if (kind == Kind.ADDED) OrdersBankStrings.noticeAttach else null
}

/** Shows each [BankNotice] the Bank model announces, one at a time, above the bottom bar. [onAttach] is the action on a new entry's message: it gets that entry's id. */
@Composable
internal fun BankNoticeHost(
    bank: BankViewModel,
    onAttach: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val host = remember { SnackbarHostState() }
    // Collected, not keyed on the value: clearing a notice once it is taken must not cancel the message that is on screen.
    LaunchedEffect(bank) {
        bank.notice.filterNotNull().collect { notice ->
            bank.noticeTaken()
            val result = host.showSnackbar(notice.message, notice.actionLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onAttach(notice.entryId)
        }
    }
    SnackbarHost(host, modifier = modifier.fillMaxWidth().padding(AppDimensions.PaddingSmall))
}
