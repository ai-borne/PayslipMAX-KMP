package com.payslipmax.pdfparser.part2.data

import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: a backup of a full vault (120 pages of 500 KB, about 61 MB) used to need about 0.8 GB of heap because the archive was one JSON
 * string copied several times. It is now a page archive written into one buffer and read a page at a time, and this test runs in the
 * 256 MB heap `shared/build.gradle.kts` gives every unit test, with the source database, the target database and the archive all held
 * at once: running out of memory IS the failure. The sampled peaks (heap in use above the starting point, garbage included) are
 * recorded and bounded as a second check. The JVM heap is not an Android heap; `Part2VaultScaleIosTest` reads the resident size of the
 * real process instead.
 */
class Part2VaultScaleJvmTest {
    private val runtime = Runtime.getRuntime()
    private val peaks = mutableMapOf<String, Long>()

    private inner class HeapProbe : ScaleProbe {
        private val peak = AtomicLong()
        private val running = AtomicBoolean()
        private var baseline = 0L
        private var sampler: Thread? = null

        override fun begin(phase: String) {
            repeat(3) { System.gc() }
            baseline = runtime.totalMemory() - runtime.freeMemory()
            peak.set(baseline)
            running.set(true)
            sampler =
                thread(isDaemon = true) {
                    while (running.get()) {
                        peak.accumulateAndGet(runtime.totalMemory() - runtime.freeMemory(), ::maxOf)
                        Thread.sleep(5)
                    }
                }
        }

        override fun end(phase: String) {
            running.set(false)
            sampler?.join()
            peaks[phase] = (peak.get() - baseline) / (1024 * 1024)
        }
    }

    @Test
    fun `a full 120 page vault exports and restores within the time and heap bounds`() {
        val result = runBlocking { runVaultScale(HeapProbe()) }

        println(
            "PART2-VAULT-SCALE jvm pages=${result.pages} archiveMB=${result.archiveBytes / (1024 * 1024)} exportMs=${result.exportMs} " +
                "restoreMs=${result.restoreMs} exportPeakExtraHeapMB=${peaks["export"]} restorePeakExtraHeapMB=${peaks["restore"]}",
        )
        assertEquals(120, result.pages)
        assertTrue(result.exportMs < 120_000 && result.restoreMs < 120_000, "export ${result.exportMs} ms, restore ${result.restoreMs} ms")
        assertTrue(peaks.values.all { it < 230 }, "peak extra heap $peaks")
    }
}
