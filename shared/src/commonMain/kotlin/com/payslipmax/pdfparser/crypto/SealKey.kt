package com.payslipmax.pdfparser.crypto

/**
 * How one backup's rows and pages are sealed and opened (docs/Plan/14a P3). Backup formats 1 to 5, and the device database, seal every row
 * with a password ([PasswordKey]: its own salt and PBKDF2 each time). Format 6 derives one key per archive from the header and seals every
 * row and page with it ([ArchiveKey]). The row and page codecs take a [SealKey], so both kinds of file go through the same code.
 */
interface SealKey : AutoCloseable {
    fun seal(plain: ByteArray): Result<ByteArray>

    /** What [seal] sealed, or [DecryptionFailedException] for a wrong key or altered bytes. */
    fun open(sealed: ByteArray): Result<ByteArray>

    /** Wipes key material where there is any. */
    override fun close() {}
}

/** Sealing with a password, exactly as [CryptoHelper.encrypt] and [CryptoHelper.decrypt] do: the legacy backup rows and the device database. */
class PasswordKey(private val password: String) : SealKey {
    override fun seal(plain: ByteArray): Result<ByteArray> = CryptoHelper.encrypt(plain, password)

    override fun open(sealed: ByteArray): Result<ByteArray> = CryptoHelper.decrypt(sealed, password)
}

/**
 * One 32-byte key for a whole v6 archive, derived once from the password and the header's salt and iteration count. Every seal binds
 * [aad] (the archive's plaintext header), so a header changed after writing opens nothing. [close] wipes the key; a closed key refuses work.
 */
class ArchiveKey private constructor(
    private val key: ByteArray,
    private val aad: ByteArray,
) : SealKey {
    private var closed = false

    override fun seal(plain: ByteArray): Result<ByteArray> = live { CryptoHelper.sealWithKey(plain, key, aad) }

    override fun open(sealed: ByteArray): Result<ByteArray> = live { CryptoHelper.openWithKey(sealed, key, aad) }

    override fun close() {
        key.fill(0)
        closed = true
    }

    private fun live(block: () -> Result<ByteArray>): Result<ByteArray> = if (closed) Result.failure(IllegalStateException("The archive key is closed.")) else block()

    companion object {
        /** What one seal adds to its plaintext: IV 12, GCM tag 16 and the 4-byte mark (no salt: the archive has one). */
        const val OVERHEAD_BYTES = 32

        fun derive(
            password: String,
            salt: ByteArray,
            iterations: Int,
            aad: ByteArray,
        ): ArchiveKey = ArchiveKey(CryptoHelper.pbkdf2(password, salt, iterations), aad.copyOf())
    }
}
