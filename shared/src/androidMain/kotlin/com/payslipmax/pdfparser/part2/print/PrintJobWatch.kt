package com.payslipmax.pdfparser.part2.print

import android.print.PrintJobInfo
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlinx.coroutines.delay

/**
 * Tells whether a system print job was printed. `PrintDocumentAdapter.onFinish` does not say (it is called for a cancelled dialog and a
 * finished job alike), so the job the print manager returns is watched instead: only `COMPLETED` is a print (Round 18). A job that is
 * cancelled or fails is not. A job that never ends (a printer that never answers) is given up on after `Part2Limits.PRINT_WATCH_MS`,
 * and the officer can print again; the cost of a missed record is a missing reminder, never a wrong one.
 */
internal object PrintJobWatch {
    const val POLL_MS = 500L

    suspend fun outcome(
        state: () -> Int,
        pause: suspend (Long) -> Unit = { delay(it) },
    ): PrintOutcome {
        var waited = 0L
        while (true) {
            val now =
                try {
                    state()
                } catch (e: RuntimeException) {
                    return PrintOutcome.NOT_PRINTED
                }
            when (now) {
                PrintJobInfo.STATE_COMPLETED -> return PrintOutcome.COMPLETED
                PrintJobInfo.STATE_FAILED, PrintJobInfo.STATE_CANCELED -> return PrintOutcome.NOT_PRINTED
            }
            if (waited >= Part2Limits.PRINT_WATCH_MS) return PrintOutcome.NOT_PRINTED
            pause(POLL_MS)
            waited += POLL_MS
        }
    }
}
