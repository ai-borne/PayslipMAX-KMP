package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, "From P6" item 6, chosen by the owner): the device database's key is made once and kept in the Keychain for the
 * life of the install, so a key made from a failed generator would be a permanent hole. It used to ignore `SecRandomCopyBytes`'s status,
 * which would have stored 32 zero bytes. It now comes from the checked generator, which throws, and the key is made before anything is
 * written to the Keychain or kept in memory: a failure means the app cannot open its database on that launch, never a weak key for good.
 */
class IosDatabaseKeyRandomTest {
    @Test
    fun aGeneratorThatFailsMakesNoKeyInsteadOfAnAllZeroOne() {
        assertFailsWith<IllegalStateException> { CryptoHelper.generateDatabaseKey { error("No secure random bytes.") } }
    }

    @Test
    fun theKeyIs32BytesFromTheGeneratorAndTheRealGeneratorGivesADifferentOneEachTime() {
        val asked = mutableListOf<Int>()
        val key =
            CryptoHelper.generateDatabaseKey { size ->
                asked += size
                ByteArray(size) { (it + 1).toByte() }
            }

        assertEquals(listOf(32), asked)
        assertContentEquals(ByteArray(32) { (it + 1).toByte() }, key)
        val first = CryptoHelper.generateDatabaseKey()
        val second = CryptoHelper.generateDatabaseKey()
        assertTrue(!first.contentEquals(second) && first.any { it != 0.toByte() }, "a fresh, non-zero key each time")
    }
}
