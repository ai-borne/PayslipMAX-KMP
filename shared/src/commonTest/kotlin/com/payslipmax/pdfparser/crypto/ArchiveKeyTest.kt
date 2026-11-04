package com.payslipmax.pdfparser.crypto

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P3, F9): a v6 backup derives ONE key from the password and seals every row and page with it, so the key and how it is
 * used are the whole of the file's secrecy. Each seal must take a fresh IV (one key, thousands of seals: a repeated IV under GCM leaks the
 * key stream), must bind the archive's plaintext header (a header edited after writing opens nothing), and must fail by type with no
 * library text. A closed key is wiped and does no more work. Runs on the JVM (Android's crypto) and on the iOS simulator.
 */
class ArchiveKeyTest {
    private val salt = ByteArray(16) { (it * 7).toByte() }
    private val header = "PCDX-header".encodeToByteArray()

    // A low count keeps the test quick; the iteration count itself is the envelope's business (BackupEnvelopeTest).
    private fun key(
        password: String = "BackupPass#1",
        aad: ByteArray = header,
    ) = ArchiveKey.derive(Secret.of(password), salt, 10_000, aad)

    @Test
    fun whatOneKeySealsTheSameDerivationOpens() {
        val plain = "a payslip row".encodeToByteArray()

        val sealed = key().seal(plain).getOrThrow()

        assertContentEquals(plain, key().open(sealed).getOrThrow())
    }

    @Test
    fun aSealAddsOnlyIvTagAndMarkWhichFitsUnderThePageLimitsOverhead() {
        val sealed = key().seal(ByteArray(1000)).getOrThrow()

        assertEquals(1000 + ArchiveKey.OVERHEAD_BYTES, sealed.size)
        assertEquals(32, ArchiveKey.OVERHEAD_BYTES, "IV 12 + tag 16 + mark 4, and no per-row salt")
        assertTrue(ArchiveKey.OVERHEAD_BYTES <= Part2Limits.SEAL_OVERHEAD_BYTES, "a page's size limit must hold for both kinds of seal")
    }

    @Test
    fun everySealTakesAFreshIvSoOneKeyNeverRepeatsOne() {
        val k = key()
        val ivs = List(64) { k.seal(ByteArray(8)).getOrThrow().copyOfRange(0, 12).toList() }

        assertEquals(64, ivs.toSet().size, "64 seals, 64 different IVs")
    }

    @Test
    fun anotherHeaderAnotherPasswordOrAChangedByteOpensNothingAndSaysSoByType() {
        val sealed = key().seal("row".encodeToByteArray()).getOrThrow()
        val flipped = sealed.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }

        for (attempt in listOf(
            key(aad = "PCDX-heaDer".encodeToByteArray()).open(sealed),
            key(password = "Not-The-Password").open(sealed),
            key().open(flipped),
            key().open(sealed.copyOf(20)),
        )) {
            val error = attempt.exceptionOrNull()
            assertTrue(error is DecryptionFailedException, "expected the typed refusal but was $error")
            assertNull(error.cause)
        }
    }

    @Test
    fun aRowSealedPerRowWithThePasswordIsNotOpenedByTheArchiveKeyNorTheOtherWayRound() {
        val perRow = CryptoHelper.encrypt("row".encodeToByteArray(), Secret.of("BackupPass#1")).getOrThrow()
        val archived = key().seal("row".encodeToByteArray()).getOrThrow()

        assertTrue(key().open(perRow).isFailure)
        assertTrue(CryptoHelper.decrypt(archived, Secret.of("BackupPass#1")).isFailure)
    }

    @Test
    fun aClosedKeyIsWipedAndRefusesWork() {
        val k = key()
        val sealed = k.seal(ByteArray(4)).getOrThrow()

        k.close()

        assertTrue(k.seal(ByteArray(4)).isFailure)
        assertTrue(k.open(sealed).isFailure)
    }

    @Test
    fun randomBytesAreFreshEachTime() {
        val a = CryptoHelper.randomBytes(16)
        val b = CryptoHelper.randomBytes(16)

        assertEquals(16, a.size)
        assertFalse(a.contentEquals(b))
    }
}
