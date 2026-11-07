package com.payslipmax.pdfparser.crypto

/**
 * A backup password as a buffer that can be wiped (docs/Plan/14a P10, closes "From P3" item 3). A Kotlin `String` is immutable on both
 * platforms, so a password held as one stays in memory until it is collected; a [Secret] holds the characters in a `CharArray` that [close]
 * overwrites with zeros. It is the one type a password has from the field to the key derivation.
 *
 * Ownership is one rule: **whoever is handed a [Secret] closes it**, on success, on failure and on cancellation, so every function that
 * takes one says "consumes" in its doc. [reveal] and [withUtf8] only borrow it. No function here makes a `String` of the characters, and
 * [toString] says nothing about them, so a log line, a state's `toString()` or an exception message cannot carry the password.
 */
class Secret private constructor(
    private val chars: CharArray,
) : AutoCloseable {
    private var wiped = false

    /** The characters, only while this call runs and only if not wiped; the array is this secret's own, so a caller must not keep or copy it. */
    fun <T> reveal(block: (CharArray) -> T): T {
        check(!wiped) { "The secret was already used." }
        return block(chars)
    }

    /**
     * The password as UTF-8 bytes for a native key derivation (iOS), made without a `String`, handed to [block] and zeroed when it returns
     * or throws. A lone surrogate becomes U+FFFD, as `String.encodeToByteArray` does.
     */
    fun <T> withUtf8(block: (ByteArray) -> T): T {
        check(!wiped) { "The secret was already used." }
        val bytes = utf8Of(chars)
        try {
            return block(bytes)
        } finally {
            bytes.fill(0)
        }
    }

    /** A second, independent secret with the same characters, for a caller that needs the password twice and closes each. */
    fun copy(): Secret {
        check(!wiped) { "The secret was already used." }
        return Secret(chars.copyOf())
    }

    /** Overwrites the characters with zeros. Safe to call again. */
    override fun close() {
        chars.fill('\u0000')
        wiped = true
    }

    override fun toString(): String = "Secret"

    companion object {
        /** A copy of [text] (a text field's contents, say), made one character at a time. */
        fun of(text: CharSequence): Secret = Secret(CharArray(text.length) { text[it] })

        /** [chars] itself, not a copy: the caller gives the array up, and it is zeroed when this is closed. */
        fun adopt(chars: CharArray): Secret = Secret(chars)
    }
}

private fun utf8Of(chars: CharArray): ByteArray {
    var size = 0
    var i = 0
    while (i < chars.size) {
        val c = chars[i]
        size +=
            when {
                c.code < 0x80 -> 1
                c.code < 0x800 -> 2
                c.isHighSurrogate() && i + 1 < chars.size && chars[i + 1].isLowSurrogate() -> 4.also { i++ }
                else -> 3
            }
        i++
    }
    val out = ByteArray(size)
    var o = 0
    i = 0
    while (i < chars.size) {
        val c = chars[i]
        val code =
            when {
                c.isHighSurrogate() && i + 1 < chars.size && chars[i + 1].isLowSurrogate() -> {
                    val pair = 0x10000 + ((c.code - 0xD800) shl 10) + (chars[i + 1].code - 0xDC00)
                    i++
                    pair
                }
                c.isSurrogate() -> 0xFFFD
                else -> c.code
            }
        o = put(out, o, code)
        i++
    }
    return out
}

private fun put(
    out: ByteArray,
    at: Int,
    code: Int,
): Int {
    var o = at
    when {
        code < 0x80 -> out[o++] = code.toByte()
        code < 0x800 -> {
            out[o++] = (0xC0 or (code shr 6)).toByte()
            out[o++] = (0x80 or (code and 0x3F)).toByte()
        }
        code < 0x10000 -> {
            out[o++] = (0xE0 or (code shr 12)).toByte()
            out[o++] = (0x80 or ((code shr 6) and 0x3F)).toByte()
            out[o++] = (0x80 or (code and 0x3F)).toByte()
        }
        else -> {
            out[o++] = (0xF0 or (code shr 18)).toByte()
            out[o++] = (0x80 or ((code shr 12) and 0x3F)).toByte()
            out[o++] = (0x80 or ((code shr 6) and 0x3F)).toByte()
            out[o++] = (0x80 or (code and 0x3F)).toByte()
        }
    }
    return o
}
