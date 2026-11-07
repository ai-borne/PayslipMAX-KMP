@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.payslipmax.pdfparser.part2.data

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.coroutines.test.runTest
import platform.posix.RUSAGE_SELF
import platform.posix.getrusage
import platform.posix.rusage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY: Kotlin/Native crypto and string handling are not the JVM's. A full vault (120 pages of 500 KB) must export and restore within a
 * bound on the real runtime, and the process's peak resident size over the run is recorded (the JVM twin is `Part2VaultScaleJvmTest`).
 * `ru_maxrss` only ever rises, so the figure is the growth of the process peak across the run: a floor for the cost, not a heap reading.
 */
class Part2VaultScaleIosTest {
    private fun peakResidentMb(): Long =
        memScoped {
            val usage = alloc<rusage>()
            getrusage(RUSAGE_SELF, usage.ptr)
            usage.ru_maxrss / (1024 * 1024) // Darwin reports bytes
        }

    @Test
    fun aFullVaultExportsAndRestoresWithinTheBound() =
        runTest {
            val before = peakResidentMb()

            val result = runVaultScale()

            val after = peakResidentMb()
            println("PART2-VAULT-SCALE ios pages=${result.pages} archiveMB=${result.archiveBytes / (1024 * 1024)} exportMs=${result.exportMs} restoreMs=${result.restoreMs} peakResidentMB=$after (was $before)")
            assertEquals(120, result.pages)
            assertTrue(result.exportMs < 300_000 && result.restoreMs < 300_000, "export ${result.exportMs} ms, restore ${result.restoreMs} ms")
        }
}
