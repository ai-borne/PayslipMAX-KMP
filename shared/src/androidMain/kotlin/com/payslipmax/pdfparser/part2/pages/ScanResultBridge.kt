package com.payslipmax.pdfparser.part2.pages

import android.content.IntentSender
import kotlinx.coroutines.CompletableDeferred

/**
 * Joins the screen's activity-result callback to the scanner's suspending [launch]. The screen (`rememberPageScanner`) sets [start] to
 * its result launcher and forwards each answer to [onResult]. One scan waits at a time: each [launch] replaces the one before, so an
 * answer for a finished or abandoned scan can only reach a deferred nobody awaits, and an answer nobody asked for is dropped.
 */
class ScanResultBridge : ScanLauncher {
    var start: (IntentSender) -> Unit = {}

    private var waiting: CompletableDeferred<ScanActivityResult>? = null

    fun onResult(result: ScanActivityResult) {
        waiting?.complete(result)
    }

    override suspend fun launch(sender: IntentSender): ScanActivityResult {
        val answer = CompletableDeferred<ScanActivityResult>()
        waiting = answer
        start(sender)
        return answer.await()
    }
}
