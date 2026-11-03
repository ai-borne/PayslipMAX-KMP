@file:OptIn(ExperimentalForeignApi::class)

package com.payslipmax.pdfparser.crypto

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCAlgorithm
import platform.CoreCrypto.CCCryptorStatus
import platform.CoreCrypto.kCCAlgorithmAES
import platform.CoreCrypto.kCCSuccess
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault
import platform.posix.dlsym

// CommonCrypto's one-shot AES-GCM, found at run time (it is not in the public headers), and the two ways the app uses it: under a key
// derived per row from a password (CryptoHelper.encrypt / decrypt, no AAD) and under a v6 backup's one archive key with its header as AAD
// (sealWithKey / openWithKey). One copy of the pinned call for both.
private val RTLD_DEFAULT = 0xfffffffffffffffeUL.toLong().toCPointer<CPointed>()

private val oneshotEncrypt =
    dlsym(RTLD_DEFAULT, "CCCryptorGCMOneshotEncrypt")?.reinterpret<
        CFunction<(alg: CCAlgorithm, key: COpaquePointer?, keyLength: ULong, iv: COpaquePointer?, ivLength: ULong, aData: COpaquePointer?, aDataLength: ULong, dataIn: COpaquePointer?, dataInLength: ULong, dataOut: COpaquePointer?, tagOut: COpaquePointer?, tagLength: ULong) -> CCCryptorStatus>,
        >()

private val oneshotDecrypt =
    dlsym(RTLD_DEFAULT, "CCCryptorGCMOneshotDecrypt")?.reinterpret<
        CFunction<(alg: CCAlgorithm, key: COpaquePointer?, keyLength: ULong, iv: COpaquePointer?, ivLength: ULong, aData: COpaquePointer?, aDataLength: ULong, dataIn: COpaquePointer?, dataInLength: ULong, dataOut: COpaquePointer?, tagIn: COpaquePointer?, tagLength: ULong) -> CCCryptorStatus>,
        >()

internal const val GCM_IV_BYTES = 12
internal const val GCM_TAG_BYTES = 16
private const val KEY_BYTES = 32

/** [size] bytes from the system's secure generator; a failure throws rather than return predictable bytes. */
internal fun secureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    if (size == 0) return bytes
    val status = bytes.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.toULong(), it.addressOf(0).reinterpret<UByteVar>()) }
    check(status == errSecSuccess) { "No secure random bytes." }
    return bytes
}

/** [aad] pinned for the call, or a null pointer when there is none (an empty array has no address to pin). */
private fun <R> withAad(
    aad: ByteArray?,
    block: (COpaquePointer?, ULong) -> R,
): R = if (aad == null || aad.isEmpty()) block(null, 0UL) else aad.usePinned { block(it.addressOf(0), aad.size.toULong()) }

/** Encrypts [input] into [output] and [tag]; the status, or -1 when CommonCrypto has no one-shot GCM. */
internal fun gcmEncrypt(
    input: ByteArray,
    key: ByteArray,
    iv: ByteArray,
    aad: ByteArray?,
    output: ByteArray,
    tag: ByteArray,
): Int {
    val encrypt = oneshotEncrypt ?: return -1
    return withAad(aad) { aadAt, aadLength ->
        input.usePinned { inPin ->
            output.usePinned { outPin ->
                key.usePinned { keyPin ->
                    iv.usePinned { ivPin ->
                        tag.usePinned { tagPin ->
                            encrypt(
                                kCCAlgorithmAES, keyPin.addressOf(0), KEY_BYTES.toULong(), ivPin.addressOf(0), GCM_IV_BYTES.toULong(), aadAt, aadLength,
                                inPin.addressOf(0), input.size.toULong(), outPin.addressOf(0), tagPin.addressOf(0), GCM_TAG_BYTES.toULong(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Decrypts [input] into [output] when [tag] matches; the status, or -1 when CommonCrypto has no one-shot GCM. */
internal fun gcmDecrypt(
    input: ByteArray,
    key: ByteArray,
    iv: ByteArray,
    aad: ByteArray?,
    output: ByteArray,
    tag: ByteArray,
): Int {
    val decrypt = oneshotDecrypt ?: return -1
    return withAad(aad) { aadAt, aadLength ->
        input.usePinned { inPin ->
            output.usePinned { outPin ->
                key.usePinned { keyPin ->
                    iv.usePinned { ivPin ->
                        tag.usePinned { tagPin ->
                            decrypt(
                                kCCAlgorithmAES, keyPin.addressOf(0), KEY_BYTES.toULong(), ivPin.addressOf(0), GCM_IV_BYTES.toULong(), aadAt, aadLength,
                                inPin.addressOf(0), input.size.toULong(), outPin.addressOf(0), tagPin.addressOf(0), GCM_TAG_BYTES.toULong(),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** IV | ciphertext | tag under a raw 32-byte [key], the plaintext starting with the "PCDA" mark, [aad] authenticated. */
internal fun gcmSeal(
    data: ByteArray,
    key: ByteArray,
    aad: ByteArray,
): Result<ByteArray> {
    if (key.size != KEY_BYTES) return Result.failure(IllegalArgumentException("An archive key is 32 bytes."))
    val plain = PcdaMark.BYTES + data
    try {
        val iv = secureRandomBytes(GCM_IV_BYTES)
        val out = ByteArray(plain.size)
        val tag = ByteArray(GCM_TAG_BYTES)
        val status = gcmEncrypt(plain, key, iv, aad, out, tag)
        if (status != kCCSuccess) return Result.failure(Exception("Encrypt status: $status"))
        return Result.success(iv + out + tag)
    } finally {
        plain.fill(0)
    }
}

/** What [gcmSeal] sealed, or [DecryptionFailedException] for another key, another [aad] or a changed byte. */
internal fun gcmOpen(
    sealed: ByteArray,
    key: ByteArray,
    aad: ByteArray,
): Result<ByteArray> {
    if (key.size != KEY_BYTES || sealed.size < GCM_IV_BYTES + GCM_TAG_BYTES + PcdaMark.BYTES.size) return Result.failure(DecryptionFailedException())
    val iv = sealed.copyOfRange(0, GCM_IV_BYTES)
    val body = sealed.copyOfRange(GCM_IV_BYTES, sealed.size - GCM_TAG_BYTES)
    val tag = sealed.copyOfRange(sealed.size - GCM_TAG_BYTES, sealed.size)
    val plain = ByteArray(body.size)
    try {
        val status = gcmDecrypt(body, key, iv, aad, plain, tag)
        if (status != kCCSuccess) return Result.failure(decryptStatusFailure(status) ?: Exception("Decrypt status: $status"))
        return PcdaMark.open(plain)
    } finally {
        plain.fill(0)
    }
}
