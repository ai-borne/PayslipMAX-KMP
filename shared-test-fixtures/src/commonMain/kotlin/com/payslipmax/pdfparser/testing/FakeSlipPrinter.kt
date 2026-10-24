package com.payslipmax.pdfparser.testing

import com.payslipmax.pdfparser.part2.print.PrintOutcome
import com.payslipmax.pdfparser.part2.print.SlipDocument
import com.payslipmax.pdfparser.part2.print.SlipPrinter
import kotlinx.coroutines.CompletableDeferred

/**
 * A [SlipPrinter] that records what it was asked to print and answers with [outcome]: no dialog, no platform. [hold] keeps the next print
 * open (as a system dialog is) until [release], so a test can look at the model while the officer is still choosing.
 */
class FakeSlipPrinter(
    var outcome: PrintOutcome = PrintOutcome.COMPLETED,
) : SlipPrinter {
    val jobNames = mutableListOf<String>()
    val documents = mutableListOf<SlipDocument>()
    private var gate: CompletableDeferred<Unit>? = null

    fun hold() {
        gate = CompletableDeferred()
    }

    fun release() {
        gate?.complete(Unit)
        gate = null
    }

    override suspend fun print(
        jobName: String,
        document: SlipDocument,
    ): PrintOutcome {
        jobNames += jobName
        documents += document
        gate?.await()
        return outcome
    }
}
