@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ssbmax.pdfparser.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*
import platform.posix.size_tVar

actual object CryptoHelper {
    private const val IV_SIZE = 16 // AES block size
    private const val KEY_SIZE = 32 // AES-256
    private const val SALT_SIZE = 16
    private var memoryFallbackKey: String? = null

    actual fun encrypt(
        data: ByteArray,
        password: String,
    ): Result<ByteArray> {
        return try {
            // Generate random 16-byte salt using Apple Secure Enclave random generator
            val salt = ByteArray(SALT_SIZE)
            salt.usePinned { pinned ->
                SecRandomCopyBytes(kSecRandomDefault, SALT_SIZE.toULong(), pinned.addressOf(0).reinterpret<UByteVar>())
            }

            // Derive key using PBKDF2
            val keyBytes = pbkdf2(password, salt)

            // Generate random 16-byte IV using Apple Secure Enclave random generator
            val iv = ByteArray(IV_SIZE)
            iv.usePinned { pinned ->
                SecRandomCopyBytes(kSecRandomDefault, IV_SIZE.toULong(), pinned.addressOf(0).reinterpret<UByteVar>())
            }

            // Prepend MAGIC header "PCDA"
            val magic = "PCDA".encodeToByteArray()
            val prefixedData = ByteArray(magic.size + data.size)
            magic.copyInto(prefixedData, 0)
            data.copyInto(prefixedData, magic.size)

            val dataSize = prefixedData.size
            // Output buffer needs block size + padding overhead
            val bufferSize = dataSize + kCCBlockSizeAES128.toInt()
            val buffer = ByteArray(bufferSize)

            val bytesMoved =
                memScoped {
                    val numBytesEncrypted = alloc<size_tVar>()
                    val cryptStatus =
                        prefixedData.usePinned { dataPinned ->
                            buffer.usePinned { bufferPinned ->
                                keyBytes.usePinned { keyPinned ->
                                    iv.usePinned { ivPinned ->
                                        CCCrypt(
                                            kCCEncrypt,
                                            kCCAlgorithmAES,
                                            kCCOptionPKCS7Padding,
                                            keyPinned.addressOf(0),
                                            KEY_SIZE.toULong(),
                                            ivPinned.addressOf(0),
                                            dataPinned.addressOf(0),
                                            dataSize.toULong(),
                                            bufferPinned.addressOf(0),
                                            bufferSize.toULong(),
                                            numBytesEncrypted.ptr,
                                        )
                                    }
                                }
                            }
                        }

                    if (cryptStatus != kCCSuccess) {
                        return Result.failure(Exception("Encryption failed with status: $cryptStatus"))
                    }
                    numBytesEncrypted.value.toInt()
                }

            // Output is Salt + IV + CipherText
            val result = ByteArray(SALT_SIZE + IV_SIZE + bytesMoved)
            salt.copyInto(result, 0, 0, SALT_SIZE)
            iv.copyInto(result, SALT_SIZE, 0, IV_SIZE)
            buffer.copyInto(result, SALT_SIZE + IV_SIZE, 0, bytesMoved)

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
            val prefixSize = SALT_SIZE + IV_SIZE
            if (encryptedData.size < prefixSize) {
                return Result.failure(IllegalArgumentException("Invalid encrypted data size"))
            }

            // Extract Salt
            val salt = ByteArray(SALT_SIZE)
            encryptedData.copyInto(salt, 0, 0, SALT_SIZE)

            // Derive key using PBKDF2
            val keyBytes = pbkdf2(password, salt)

            // Extract IV
            val iv = ByteArray(IV_SIZE)
            encryptedData.copyInto(iv, 0, SALT_SIZE, prefixSize)

            // Extract CipherText
            val cipherTextSize = encryptedData.size - prefixSize
            val cipherText = ByteArray(cipherTextSize)
            encryptedData.copyInto(cipherText, 0, prefixSize, prefixSize + cipherTextSize)

            val bufferSize = cipherTextSize
            val buffer = ByteArray(bufferSize)

            val bytesMoved =
                memScoped {
                    val numBytesDecrypted = alloc<size_tVar>()
                    val cryptStatus =
                        cipherText.usePinned { cipherPinned ->
                            buffer.usePinned { bufferPinned ->
                                keyBytes.usePinned { keyPinned ->
                                    iv.usePinned { ivPinned ->
                                        CCCrypt(
                                            kCCDecrypt,
                                            kCCAlgorithmAES,
                                            kCCOptionPKCS7Padding,
                                            keyPinned.addressOf(0),
                                            KEY_SIZE.toULong(),
                                            ivPinned.addressOf(0),
                                            cipherPinned.addressOf(0),
                                            cipherTextSize.toULong(),
                                            bufferPinned.addressOf(0),
                                            bufferSize.toULong(),
                                            numBytesDecrypted.ptr,
                                        )
                                    }
                                }
                            }
                        }

                    if (cryptStatus != kCCSuccess) {
                        return Result.failure(Exception("Decryption failed with status: $cryptStatus"))
                    }
                    numBytesDecrypted.value.toInt()
                }

            val result = ByteArray(bytesMoved)
            buffer.copyInto(result, 0, 0, bytesMoved)

            // Verify MAGIC header "PCDA"
            val magic = "PCDA".encodeToByteArray()
            if (result.size < magic.size) {
                return Result.failure(IllegalArgumentException("Decryption failed: incorrect password or tampered data"))
            }
            for (i in magic.indices) {
                if (result[i] != magic[i]) {
                    return Result.failure(IllegalArgumentException("Decryption failed: incorrect password or tampered data"))
                }
            }

            val finalData = ByteArray(result.size - magic.size)
            result.copyInto(finalData, 0, magic.size, result.size)

            Result.success(finalData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun sha256(input: String): String {
        val digestBytes = sha256Bytes(input)
        return digestBytes.toHex()
    }

    private fun sha256Bytes(input: String): ByteArray {
        val bytes = input.encodeToByteArray()
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        bytes.usePinned { bytesPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(bytesPinned.addressOf(0), bytes.size.toUInt(), digestPinned.addressOf(0).reinterpret<UByteVar>())
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
                        kCCPBKDF2,
                        password,
                        password.length.toULong(),
                        saltPinned.addressOf(0).reinterpret(),
                        salt.size.toULong(),
                        // kCCPRFHmacSHA256
                        2u,
                        iterations.toUInt(),
                        derivedPinned.addressOf(0).reinterpret(),
                        32.toULong(),
                    )
                }
            }
        }
        return derivedKey
    }

    private const val KEYCHAIN_SERVICE = "com.ssbmax.pdfparser"
    private const val KEYCHAIN_ACCOUNT = "db_secret_key"

    actual fun getDatabaseSecretKey(): String {
        val query = CFDictionaryCreateMutable(null, 0, null, null)
        CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
        CFDictionarySetValue(query, kSecAttrService, KEYCHAIN_SERVICE.toCFString())
        CFDictionarySetValue(query, kSecAttrAccount, KEYCHAIN_ACCOUNT.toCFString())
        CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
        CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)

        var result: String? = null

        memScoped {
            val resultRef = alloc<COpaquePointerVar>()
            val status = SecItemCopyMatching(query, resultRef.ptr)
            if (status == errSecSuccess) {
                val pointer = resultRef.value
                val data = if (pointer != null) CFBridgingRelease(pointer) as? NSData else null
                if (data != null) {
                    val bytes = data.toByteArray()
                    result = bytes.toHex()
                }
            }
        }

        CFRelease(query)

        result?.let { return it }

        // Headless test runner/simulator fallback
        if (memoryFallbackKey == null) {
            val newKey = ByteArray(32)
            newKey.usePinned { pinned ->
                SecRandomCopyBytes(kSecRandomDefault, 32.toULong(), pinned.addressOf(0).reinterpret<UByteVar>())
            }

            val nsData = newKey.toNSData()
            val addQuery = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(addQuery, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(addQuery, kSecAttrService, KEYCHAIN_SERVICE.toCFString())
            CFDictionarySetValue(addQuery, kSecAttrAccount, KEYCHAIN_ACCOUNT.toCFString())

            val nsDataRef = CFBridgingRetain(nsData)
            CFDictionarySetValue(addQuery, kSecValueData, nsDataRef)

            val addStatus = SecItemAdd(addQuery, null)

            CFRelease(addQuery)
            CFRelease(nsDataRef)

            val hexKey = newKey.toHex()
            if (addStatus != errSecSuccess) {
                memoryFallbackKey = hexKey
            }
            return hexKey
        } else {
            return memoryFallbackKey!!
        }
    }
}
