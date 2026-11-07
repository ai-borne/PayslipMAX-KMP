@file:OptIn(ExperimentalForeignApi::class)

package com.payslipmax.pdfparser.crypto

import kotlinx.cinterop.CFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.invoke
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.posix.RTLD_LAZY
import platform.posix.dlopen
import platform.posix.dlsym

/** CommonCrypto's signature: algorithm, password, password length, salt, salt length, PRF, rounds, derived key, derived key length. */
private typealias Pbkdf2Function = CFunction<(UInt, COpaquePointer?, ULong, COpaquePointer?, ULong, UInt, UInt, COpaquePointer?, ULong) -> Int>

private const val PBKDF2_ALGORITHM: UInt = 2u // kCCPBKDF2
private const val PRF_HMAC_SHA256: UInt = 3u // kCCPRFHmacAlgSHA256
private const val KEY_BYTES = 32

/**
 * PBKDF2-HMAC-SHA256 over a password held as bytes (docs/Plan/14a P10). Why this calls the C function by address: Kotlin/Native's
 * `platform.CoreCrypto.CCKeyDerivationPBKDF` declares the password as `String?` (the C `const char *`), so using it would make an
 * immutable `String` of the password that can never be wiped. The function is looked up in the process's own image (it is part of
 * libSystem) and called with the byte array pinned in place; the caller zeroes the bytes. `CryptoHelperVectorTest` pins the output against
 * published PBKDF2 vectors, so a wrong symbol, signature or encoding fails there. A non-zero status throws: a derived key of zeros must
 * never seal a backup.
 */
internal fun pbkdf2Sha256(
    password: ByteArray,
    salt: ByteArray,
    iterations: Int,
): ByteArray {
    val handle = dlopen(null, RTLD_LAZY) ?: error("CommonCrypto is not available.")
    val symbol = dlsym(handle, "CCKeyDerivationPBKDF") ?: error("CommonCrypto is not available.")
    val derive = symbol.reinterpret<Pbkdf2Function>()
    val derived = ByteArray(KEY_BYTES)
    // An empty array has no address to pin: read a one-byte array and say its length is 0.
    val readablePassword = if (password.isEmpty()) ByteArray(1) else password
    val readableSalt = if (salt.isEmpty()) ByteArray(1) else salt
    val status =
        readablePassword.usePinned { passwordPinned ->
            readableSalt.usePinned { saltPinned ->
                derived.usePinned { derivedPinned ->
                    derive.invoke(
                        PBKDF2_ALGORITHM,
                        passwordPinned.addressOf(0),
                        password.size.toULong(),
                        saltPinned.addressOf(0),
                        salt.size.toULong(),
                        PRF_HMAC_SHA256,
                        iterations.toUInt(),
                        derivedPinned.addressOf(0),
                        KEY_BYTES.toULong(),
                    )
                }
            }
        }
    check(status == 0) { "Key derivation failed." }
    return derived
}
