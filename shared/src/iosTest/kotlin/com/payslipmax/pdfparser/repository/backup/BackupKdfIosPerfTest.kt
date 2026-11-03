package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.crypto.ArchiveKey
import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.repository.MemoryBackupSource
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue
import kotlin.time.TimeSource

/**
 * WHY (docs/Plan/14a P3): a v6 backup derives its key once with PBKDF2 at [BackupEnvelope.ITERATIONS] (D6). That is the officer's wait
 * before any backup or restore starts, and Kotlin/Native timing is not the JVM's (`ParserUtilsIosPerfTest` for why that matters). Measured
 * before the decision: ~0.34-0.45 s on the simulator, 0.42 s on a Pixel 9. Every page of a 250 MB vault is then sealed and opened with that
 * one key, so one page must cost milliseconds, not another derivation. The whole 250 MB v6 restore is timed by `Part2VaultHugeIosTest`.
 */
class BackupKdfIosPerfTest {
    private fun elapsedMillis(block: () -> Unit): Long {
        val mark = TimeSource.Monotonic.markNow()
        block()
        return mark.elapsedNow().inWholeMilliseconds
    }

    @Test
    fun oneDerivationAtTheArchiveIterationCountStaysUnderTwoSeconds() {
        val bytes = BackupEnvelope.header(6, BackupEnvelope.ITERATIONS, CryptoHelper.randomBytes(BackupEnvelope.SALT_BYTES))
        val header = BackupEnvelope.read(MemoryBackupSource(bytes))!!
        lateinit var key: ArchiveKey

        val ms = elapsedMillis { key = ArchiveKey.derive("BackupPass#1", header.salt, header.iterations, header.bytes) }

        println("BACKUP-KDF ios iterations=${BackupEnvelope.ITERATIONS} deriveMs=$ms")
        assertTrue(ms < KDF_BOUND_MS, "one derivation took $ms ms, expected < $KDF_BOUND_MS ms")
        key.close()
    }

    @Test
    fun aLargestPageSealsAndOpensWithTheArchiveKeyInMilliseconds() {
        val key = ArchiveKey.derive("BackupPass#1", ByteArray(16), BackupEnvelope.ITERATIONS_MIN, "header".encodeToByteArray())
        val page = ByteArray(Part2Limits.PAGE_MAX_BYTES) { (it * 13).toByte() }
        lateinit var sealed: ByteArray
        lateinit var opened: ByteArray

        val sealMs = elapsedMillis { repeat(PAGES) { sealed = key.seal(page).getOrThrow() } }
        val openMs = elapsedMillis { repeat(PAGES) { opened = key.open(sealed).getOrThrow() } }

        println("BACKUP-PAGE-SEAL ios pages=$PAGES sealMs=$sealMs openMs=$openMs")
        assertContentEquals(page, opened)
        assertTrue(sealMs < PAGES_BOUND_MS && openMs < PAGES_BOUND_MS, "$PAGES pages of 500 KB: seal $sealMs ms, open $openMs ms")
        key.close()
    }

    companion object {
        // Four times the measured simulator time: a real regression (a second derivation, a far higher count) still blows it.
        private const val KDF_BOUND_MS = 2_000L
        private const val PAGES = 50

        // 25 MB each way; CommonCrypto's GCM does that in well under a second, while a derivation per page would take ~17 s.
        private const val PAGES_BOUND_MS = 3_000L
    }
}
