@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.payslipmax.pdfparser.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.posix.dlsym

private val RTLD_DEFAULT = 0xfffffffffffffffeUL.toLong().toCPointer<CPointed>()

@OptIn(ExperimentalForeignApi::class)
private val cccryptorGCMOneshotEncrypt =
    dlsym(RTLD_DEFAULT, "CCCryptorGCMOneshotEncrypt")?.reinterpret<
        CFunction<(alg: CCAlgorithm, key: COpaquePointer?, keyLength: ULong, iv: COpaquePointer?, ivLength: ULong, aData: COpaquePointer?, aDataLength: ULong, dataIn: COpaquePointer?, dataInLength: ULong, dataOut: COpaquePointer?, tagOut: COpaquePointer?, tagLength: ULong) -> CCCryptorStatus>,
        >()

@OptIn(ExperimentalForeignApi::class)
private val cccryptorGCMOneshotDecrypt =
    dlsym(RTLD_DEFAULT, "CCCryptorGCMOneshotDecrypt")?.reinterpret<
        CFunction<(alg: CCAlgorithm, key: COpaquePointer?, keyLength: ULong, iv: COpaquePointer?, ivLength: ULong, aData: COpaquePointer?, aDataLength: ULong, dataIn: COpaquePointer?, dataInLength: ULong, dataOut: COpaquePointer?, tagIn: COpaquePointer?, tagLength: ULong) -> CCCryptorStatus>,
        >()

actual object CryptoHelper {
    private const val IV_SIZE = 12
    private const val KEY_SIZE = 32
    private const val SALT_SIZE = 16
    private const val TAG_SIZE = 16
    private var memoryFallbackKey: String? = null

    actual fun encrypt(
        data: ByteArray,
        password: String,
    ): Result<ByteArray> {
        return try {
            val encryptFunc = cccryptorGCMOneshotEncrypt ?: return Result.failure(Exception("GCM Encrypt not found"))
            val salt =
                ByteArray(SALT_SIZE).apply {
                    usePinned { SecRandomCopyBytes(kSecRandomDefault, SALT_SIZE.toULong(), it.addressOf(0).reinterpret<UByteVar>()) }
                }
            val keyBytes = pbkdf2(password, salt)
            val iv =
                ByteArray(IV_SIZE).apply {
                    usePinned { SecRandomCopyBytes(kSecRandomDefault, IV_SIZE.toULong(), it.addressOf(0).reinterpret<UByteVar>()) }
                }
            val magic = "PCDA".encodeToByteArray()
            val prefixedData =
                ByteArray(magic.size + data.size).apply {
                    magic.copyInto(this, 0)
                    data.copyInto(this, magic.size)
                }
            val dataSize = prefixedData.size
            val buffer = ByteArray(dataSize)
            val tag = ByteArray(TAG_SIZE)

            val cryptStatus =
                prefixedData.usePinned { dataPinned ->
                    buffer.usePinned { bufferPinned ->
                        keyBytes.usePinned { keyPinned ->
                            iv.usePinned { ivPinned ->
                                tag.usePinned { tagPinned ->
                                    encryptFunc(
                                        kCCAlgorithmAES, keyPinned.addressOf(0), KEY_SIZE.toULong(),
                                        ivPinned.addressOf(0), IV_SIZE.toULong(), null, 0UL,
                                        dataPinned.addressOf(0), dataSize.toULong(),
                                        bufferPinned.addressOf(0), tagPinned.addressOf(0), TAG_SIZE.toULong(),
                                    )
                                }
                            }
                        }
                    }
                }
            if (cryptStatus != kCCSuccess) return Result.failure(Exception("Encrypt status: $cryptStatus"))
            val result =
                ByteArray(SALT_SIZE + IV_SIZE + dataSize + TAG_SIZE).apply {
                    salt.copyInto(this, 0, 0, SALT_SIZE)
                    iv.copyInto(this, SALT_SIZE, 0, IV_SIZE)
                    buffer.copyInto(this, SALT_SIZE + IV_SIZE, 0, dataSize)
                    tag.copyInto(this, SALT_SIZE + IV_SIZE + dataSize, 0, TAG_SIZE)
                }
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun decrypt(
        encryptedData: ByteArray,
        password: String,
    ): Result<ByteArray> {
        return try {
            val decryptFunc = cccryptorGCMOneshotDecrypt ?: return Result.failure(Exception("GCM Decrypt not found"))
            val prefixSize = SALT_SIZE + IV_SIZE
            if (encryptedData.size < prefixSize + TAG_SIZE) return Result.failure(IllegalArgumentException("Invalid size"))

            val salt = ByteArray(SALT_SIZE).apply { encryptedData.copyInto(this, 0, 0, SALT_SIZE) }
            val keyBytes = pbkdf2(password, salt)
            val iv = ByteArray(IV_SIZE).apply { encryptedData.copyInto(this, 0, SALT_SIZE, prefixSize) }
            val cipherTextSize = encryptedData.size - prefixSize - TAG_SIZE
            val cipherText = ByteArray(cipherTextSize).apply { encryptedData.copyInto(this, 0, prefixSize, prefixSize + cipherTextSize) }
            val tag = ByteArray(TAG_SIZE).apply { encryptedData.copyInto(this, 0, prefixSize + cipherTextSize, encryptedData.size) }
            val buffer = ByteArray(cipherTextSize)

            val cryptStatus =
                cipherText.usePinned { cipherPinned ->
                    buffer.usePinned { bufferPinned ->
                        keyBytes.usePinned { keyPinned ->
                            iv.usePinned { ivPinned ->
                                tag.usePinned { tagPinned ->
                                    decryptFunc(
                                        kCCAlgorithmAES, keyPinned.addressOf(0), KEY_SIZE.toULong(),
                                        ivPinned.addressOf(0), IV_SIZE.toULong(), null, 0UL,
                                        cipherPinned.addressOf(0), cipherTextSize.toULong(),
                                        bufferPinned.addressOf(0), tagPinned.addressOf(0), TAG_SIZE.toULong(),
                                    )
                                }
                            }
                        }
                    }
                }
            if (cryptStatus != kCCSuccess) return Result.failure(Exception("Decrypt status: $cryptStatus"))
            val magic = "PCDA".encodeToByteArray()
            if (buffer.size < magic.size || !buffer.take(magic.size).toByteArray().contentEquals(magic)) {
                return Result.failure(IllegalArgumentException("Decrypt failed: bad magic/password"))
            }
            val finalData = ByteArray(buffer.size - magic.size).apply { buffer.copyInto(this, 0, magic.size, buffer.size) }
            Result.success(finalData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun sha256(input: String): String = sha256Bytes(input).toHex()

    private fun sha256Bytes(input: String): ByteArray {
        val bytes = input.encodeToByteArray()
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        bytes.usePinned { bytesPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(bytesPinned.addressOf(0), bytes.size.toUInt(), digestPinned.addressOf(0).reinterpret())
            }
        }
        return digest
    }

    actual fun pbkdf2(
        password: String,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val derivedKey = ByteArray(32)
        memScoped {
            derivedKey.usePinned { derivedPinned ->
                salt.usePinned { saltPinned ->
                    CCKeyDerivationPBKDF(
                        kCCPBKDF2, password, password.length.toULong(),
                        saltPinned.addressOf(0).reinterpret(), salt.size.toULong(),
                        2u, iterations.toUInt(), derivedPinned.addressOf(0).reinterpret(), 32.toULong(),
                    )
                }
            }
        }
        return derivedKey
    }

    private const val KEYCHAIN_SERVICE = "com.payslipmax.pdfparser"
    private const val KEYCHAIN_ACCOUNT = "db_secret_key"

    actual fun getDatabaseSecretKey(): String {
        val query =
            CFDictionaryCreateMutable(null, 0, null, null).apply {
                CFDictionarySetValue(this, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(this, kSecAttrService, KEYCHAIN_SERVICE.toCFString())
                CFDictionarySetValue(this, kSecAttrAccount, KEYCHAIN_ACCOUNT.toCFString())
                CFDictionarySetValue(this, kSecReturnData, kCFBooleanTrue)
                CFDictionarySetValue(this, kSecMatchLimit, kSecMatchLimitOne)
            }
        var result: String? = null
        memScoped {
            val resultRef = alloc<COpaquePointerVar>()
            if (SecItemCopyMatching(query, resultRef.ptr) == errSecSuccess) {
                val pointer = resultRef.value
                val data = if (pointer != null) CFBridgingRelease(pointer) as? NSData else null
                data?.let { result = it.toByteArray().toHex() }
            }
        }
        CFRelease(query)
        result?.let { return it }

        val existingFallback = memoryFallbackKey
        if (existingFallback != null) {
            return existingFallback
        }

        val newKey =
            ByteArray(32).apply {
                usePinned { SecRandomCopyBytes(kSecRandomDefault, 32.toULong(), it.addressOf(0).reinterpret<UByteVar>()) }
            }
        val nsData = newKey.toNSData()
        val addQuery =
            CFDictionaryCreateMutable(null, 0, null, null).apply {
                CFDictionarySetValue(this, kSecClass, kSecClassGenericPassword)
                CFDictionarySetValue(this, kSecAttrService, KEYCHAIN_SERVICE.toCFString())
                CFDictionarySetValue(this, kSecAttrAccount, KEYCHAIN_ACCOUNT.toCFString())
                val nsDataRef = CFBridgingRetain(nsData)
                CFDictionarySetValue(this, kSecValueData, nsDataRef)
                CFRelease(nsDataRef)
            }
        val addStatus = SecItemAdd(addQuery, null)
        CFRelease(addQuery)
        val hexKey = newKey.toHex()
        if (addStatus != errSecSuccess) memoryFallbackKey = hexKey
        return hexKey
    }

    actual fun getCurrentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000.0).toLong()
}
