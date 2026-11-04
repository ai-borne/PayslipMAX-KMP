package com.payslipmax.pdfparser.crypto

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * WHY (docs/Plan/14a P10, closes "From P3" item 3): a backup password held as a Kotlin `String` stays in memory until it is collected,
 * because a `String` cannot be wiped. A [Secret] holds it in a `CharArray` that is zeroed on [Secret.close], and the UTF-8 bytes a native
 * key derivation needs are zeroed as soon as it returns. These tests watch the very arrays the secret was made from, so "wiped" means the
 * bytes in memory are zero, not that a flag was set. Runs on the JVM and on the iOS simulator.
 */
class SecretTest {
    private fun zero(chars: CharArray) = chars.all { it == '\u0000' }

    @Test
    fun closingOverwritesTheCharactersWithZeros() {
        val chars = "Zq7!secretWord".toCharArray()
        val secret = Secret.adopt(chars)

        secret.close()

        assertTrue(zero(chars), "the password's characters must be gone from memory, not just marked used")
    }

    @Test
    fun aWipedSecretRefusesToBeUsedAgainInsteadOfSilentlyUsingAnEmptyPassword() {
        val secret = Secret.of("pw").also { it.close() }

        assertFailsWith<IllegalStateException> { secret.reveal { it.size } }
        assertFailsWith<IllegalStateException> { secret.withUtf8 { it.size } }
        assertFailsWith<IllegalStateException> { secret.copy() }
        secret.close() // closing again is harmless
    }

    @Test
    fun ofCopiesSoTheTextItWasMadeFromIsNotTheSecretsToWipe() {
        val typed = StringBuilder("typed-here")
        val secret = Secret.of(typed)

        secret.close()

        assertEquals("typed-here", typed.toString(), "a field's own text is not changed by taking a secret from it")
    }

    @Test
    fun aCopyIsIndependentSoClosingOneLeavesTheOtherUsable() {
        val chars = "same".toCharArray()
        val original = Secret.adopt(chars)
        val copy = original.copy()

        original.close()

        assertTrue(zero(chars))
        assertEquals("same", copy.reveal { it.concatToString() })
        copy.close()
    }

    @Test
    fun theUtf8BytesAreZeroedWhenTheBlockReturnsAndWhenItThrows() {
        val secret = Secret.of("p@sswörd🔑")
        var seen = ByteArray(0)

        secret.withUtf8 { seen = it }
        assertTrue(seen.isNotEmpty() && seen.all { it == 0.toByte() }, "the bytes handed to the key derivation are wiped after it")

        var seenOnThrow = ByteArray(0)
        assertFailsWith<IllegalArgumentException> {
            secret.withUtf8 {
                seenOnThrow = it
                throw IllegalArgumentException()
            }
        }
        assertTrue(seenOnThrow.all { it == 0.toByte() }, "and when the derivation fails")
        assertEquals("p@sswörd🔑".length, secret.reveal { it.size }, "borrowing the bytes does not use the secret up")
    }

    @Test
    fun theUtf8BytesAreWhatAStringWouldHaveGivenForRealText() {
        for (text in listOf("", "plain", "pässwörd", "पासवर्ड", "p@sswörd🔑🇮🇳")) {
            val bytes = Secret.of(text).withUtf8 { it.copyOf() }
            assertContentEquals(text.encodeToByteArray(), bytes, text)
        }
    }

    @Test
    fun aLoneSurrogateBecomesTheReplacementCharacterNotAnException() {
        val bytes = Secret.of("a\uD800b").withUtf8 { it.copyOf() }

        assertContentEquals(byteArrayOf(0x61, 0xEF.toByte(), 0xBF.toByte(), 0xBD.toByte(), 0x62), bytes)
    }

    @Test
    fun noWayOfPrintingOneCarriesThePassword() {
        val secret = Secret.of("Zq7!secretWord")

        assertFalse("secretWord" in secret.toString())
        assertFalse("secretWord" in "$secret")
        assertFalse("secretWord" in listOf(secret).toString())
    }
}
