@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.ssbmax.pdfparser.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.Security.SecRandomCopyBytes
import platform.Security.kSecRandomDefault
import platform.posix.size_tVar

actual object CryptoHelper {
    private const val IV_SIZE = 16 // AES block size
    private const val KEY_SIZE = 32 // AES-256

    actual fun encrypt(data: ByteArray, password: String): Result<ByteArray> {
        return try {
            val keyBytes = sha256(password)
            
            // Generate random 16-byte IV using Apple Secure Enclave random generator
            val iv = ByteArray(IV_SIZE)
            iv.usePinned { pinned ->
                SecRandomCopyBytes(kSecRandomDefault, IV_SIZE.toULong(), pinned.addressOf(0).reinterpret<UByteVar>())
            }

            val dataSize = data.size
            // Output buffer needs block size + padding overhead
            val bufferSize = dataSize + kCCBlockSizeAES128.toInt()
            val buffer = ByteArray(bufferSize)

            val bytesMoved = memScoped {
                val numBytesEncrypted = alloc<size_tVar>()
                val cryptStatus = data.usePinned { dataPinned ->
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
                                    numBytesEncrypted.ptr
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

            // Output is IV + CipherText
            val result = ByteArray(IV_SIZE + bytesMoved)
            iv.copyInto(result, 0, 0, IV_SIZE)
            buffer.copyInto(result, IV_SIZE, 0, bytesMoved)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual fun decrypt(encryptedData: ByteArray, password: String): Result<ByteArray> {
        return try {
            if (encryptedData.size < IV_SIZE) {
                return Result.failure(IllegalArgumentException("Invalid encrypted data size"))
            }

            val keyBytes = sha256(password)
            
            // Extract IV
            val iv = ByteArray(IV_SIZE)
            encryptedData.copyInto(iv, 0, 0, IV_SIZE)

            // Extract CipherText
            val cipherTextSize = encryptedData.size - IV_SIZE
            val cipherText = ByteArray(cipherTextSize)
            encryptedData.copyInto(cipherText, 0, IV_SIZE, IV_SIZE + cipherTextSize)

            val bufferSize = cipherTextSize
            val buffer = ByteArray(bufferSize)

            val bytesMoved = memScoped {
                val numBytesDecrypted = alloc<size_tVar>()
                val cryptStatus = cipherText.usePinned { cipherPinned ->
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
                                    numBytesDecrypted.ptr
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

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun sha256(input: String): ByteArray {
        val bytes = input.encodeToByteArray()
        val digest = ByteArray(CC_SHA256_DIGEST_LENGTH)
        bytes.usePinned { bytesPinned ->
            digest.usePinned { digestPinned ->
                CC_SHA256(bytesPinned.addressOf(0), bytes.size.toUInt(), digestPinned.addressOf(0).reinterpret<UByteVar>())
            }
        }
        return digest
    }
}
