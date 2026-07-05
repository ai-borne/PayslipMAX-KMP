package com.payslipmax.pdfparser.insights.gemma

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The SHA-256 digest is the security gate that decides whether a freshly downloaded model may be
 * promoted into the active slot. A wrong hash would either reject every good download (permanent
 * failure) or, worse, accept a corrupted/tampered one — so it is pinned against the published NIST
 * test vectors, not just checked for internal self-consistency.
 */
class Sha256Test {
    private fun hash(input: String): String {
        val digest = Sha256Digest()
        val bytes = input.encodeToByteArray()
        digest.update(bytes)
        return digest.hexDigest()
    }

    @Test
    fun emptyStringMatchesNistVector() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash(""))
    }

    @Test
    fun abcMatchesNistVector() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", hash("abc"))
    }

    @Test
    fun multiBlockInputMatchesNistVector() {
        // 56 chars — forces a two-block padding path (the length no longer fits in the first block).
        assertEquals(
            "248d6a61d20638b8e5c026930c3e6039a33ce45964ff2167f6ecedd419db06c1",
            hash("abcdbcdecdefdefgefghfghighijhijkijkljklmklmnlmnomnopnopq"),
        )
    }

    @Test
    fun chunkedUpdatesMatchSingleUpdate() {
        // Feeding the same bytes in arbitrary chunk boundaries (as readFileInChunks does) must
        // produce the identical digest — this is what makes streaming a 500MB file safe.
        val message = "The quick brown fox jumps over the lazy dog".encodeToByteArray()
        val oneShot = Sha256Digest().apply { update(message) }.hexDigest()

        val chunked = Sha256Digest()
        var i = 0
        var size = 1
        while (i < message.size) {
            val end = minOf(i + size, message.size)
            chunked.update(message.copyOfRange(i, end), end - i)
            i = end
            size++
        }
        assertEquals(oneShot, chunked.hexDigest())
        assertEquals("d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592", oneShot)
    }
}
