package com.payslipmax.pdfparser.ui

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlin.test.assertTrue

class FakeFinancialIntelligenceRepositoryTest {
    // PayslipViewModel runs the startup audit repair on viewModelScope (Main). If the fake hops to Dispatchers.Default,
    // the repair comes back to Main from a worker thread that advanceUntilIdle() does not wait for; when a short test
    // has already called resetMain() it throws "Main dispatcher missing", and kotlinx-coroutines-test blames the next
    // class's runTest (UncaughtExceptionsBeforeTest, seen once on GemmaInstallTelemetryIntegrationTest after
    // AiAutoRunIntegrationTest). So the fake must finish its work without a free Default worker.
    @Test
    fun auditRepairNeedsNoDefaultWorkerSoNoTestEndsWithItInFlight() {
        val workers = Runtime.getRuntime().availableProcessors().coerceAtLeast(2)
        val started = CountDownLatch(workers)
        val release = CountDownLatch(1)
        repeat(workers) {
            Dispatchers.Default.dispatch(
                EmptyCoroutineContext,
                Runnable {
                    started.countDown()
                    release.await()
                },
            )
        }
        val repaired = CountDownLatch(1)
        val finished =
            try {
                assertTrue(started.await(5, TimeUnit.SECONDS), "premise: every Default worker is busy")
                CoroutineScope(Dispatchers.Unconfined).launch {
                    FakeFinancialIntelligenceRepository().repairAuditHistoryIfIncomplete(emptyList())
                    repaired.countDown()
                }
                repaired.await(2, TimeUnit.SECONDS)
            } finally {
                release.countDown()
            }
        assertTrue(finished, "the repair waited for a Default worker")
    }
}
