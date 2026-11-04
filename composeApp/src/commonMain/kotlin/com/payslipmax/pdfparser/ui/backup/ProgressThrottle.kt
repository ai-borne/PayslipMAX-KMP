package com.payslipmax.pdfparser.ui.backup

import com.payslipmax.pdfparser.repository.BackupOp
import com.payslipmax.pdfparser.repository.BackupProgress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Passes one backup's or restore's progress on to the screen about ten times a second (docs/Plan/14a P7). The service reports every row and
 * page from its worker thread, far more often than a sheet can usefully redraw. Only the latest update is kept, and it is always delivered
 * once the interval has passed, so a bar never stops short of the work. After [stop] nothing is delivered, so a late update cannot overwrite
 * the state the operation ended with.
 */
internal class ProgressThrottle(
    private val scope: CoroutineScope,
    private val intervalMs: Long = INTERVAL_MS,
    private val deliver: (BackupUiState.Working) -> Unit,
) {
    private val latest = MutableStateFlow<BackupUiState.Working?>(null)
    private var relay: Job? = null

    /** Shows [first] at once, then relays what [progress] reports, at most once per interval. */
    fun start(first: BackupUiState.Working) {
        stop()
        latest.value = null
        deliver(first)
        relay =
            scope.launch {
                latest.filterNotNull().collect {
                    deliver(it)
                    delay(intervalMs)
                }
            }
    }

    /** The callback the service reports [op]'s progress to; safe to call from any thread. */
    fun progress(op: BackupOp): BackupProgress = BackupProgress { stage, done, total -> latest.value = BackupUiState.Working(op, stage, done, total) }

    fun stop() {
        relay?.cancel()
        relay = null
    }

    companion object {
        /** About ten updates a second. */
        const val INTERVAL_MS = 100L
    }
}
