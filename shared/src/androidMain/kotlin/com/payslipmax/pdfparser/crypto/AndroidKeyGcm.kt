package com.payslipmax.pdfparser.crypto

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// AES-256-GCM under a raw key, for a v6 backup's one archive key (CryptoHelper.sealWithKey / openWithKey). Layout: IV | ciphertext | tag.
private const val GCM = "AES/GCM/NoPadding"
private const val IV_BYTES = 12
private const val TAG_BITS = 128
private const val TAG_BYTES = TAG_BITS / 8

private val random = SecureRandom()

internal fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { random.nextBytes(it) }

internal fun gcmSeal(
    data: ByteArray,
    key: ByteArray,
    aad: ByteArray,
): Result<ByteArray> =
    runCatching {
        require(key.size == 32) { "An archive key is 32 bytes." }
        val iv = secureRandomBytes(IV_BYTES)
        val cipher = Cipher.getInstance(GCM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad)
        val plain = PcdaMark.BYTES + data
        try {
            iv + cipher.doFinal(plain)
        } finally {
            plain.fill(0)
        }
    }

internal fun gcmOpen(
    sealed: ByteArray,
    key: ByteArray,
    aad: ByteArray,
): Result<ByteArray> {
    if (key.size != 32 || sealed.size < IV_BYTES + TAG_BYTES + PcdaMark.BYTES.size) return Result.failure(DecryptionFailedException())
    return try {
        val cipher = Cipher.getInstance(GCM)
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, sealed, 0, IV_BYTES))
        cipher.updateAAD(aad)
        val plain = cipher.doFinal(sealed, IV_BYTES, sealed.size - IV_BYTES)
        try {
            PcdaMark.open(plain)
        } finally {
            plain.fill(0)
        }
    } catch (e: Exception) {
        Result.failure(decryptFailure(e))
    }
}
