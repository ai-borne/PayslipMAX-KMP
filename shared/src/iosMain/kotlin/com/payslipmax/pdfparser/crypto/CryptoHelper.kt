@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.payslipmax.pdfparser.crypto

import kotlinx.cinterop.*
import platform.CoreCrypto.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

actual object CryptoHelper {
    private const val IV_SIZE = 12
    private const val SALT_SIZE = 16
    private const val TAG_SIZE = 16
    private var memoryFallbackKey: String? = null

    @kotlin.concurrent.Volatile
    private var cachedDatabaseKey: String? = null

    actual fun encrypt(
        data: ByteArray,
        password: String,
    ): Result<ByteArray> = encryptWith(data, password, ::secureRandomBytes)

    /** [encrypt] with its salt and IV taken from [random]; a seam so a test can show a failed generator fails the seal. */
    internal fun encryptWith(
        data: ByteArray,
        password: String,
        random: (Int) -> ByteArray,
    ): Result<ByteArray> {
        return try {
            // From the checked generator: a failure throws (caught below) instead of sealing under a zero salt and IV.
            val salt = random(SALT_SIZE)
            val keyBytes = pbkdf2(password, salt)
            val iv = random(IV_SIZE)
            val magic = PcdaMark.BYTES
            val prefixedData =
                ByteArray(magic.size + data.size).apply {
                    magic.copyInto(this, 0)
                    data.copyInto(this, magic.size)
                }
            val dataSize = prefixedData.size
            val buffer = ByteArray(dataSize)
            val tag = ByteArray(TAG_SIZE)

            val cryptStatus = gcmEncrypt(prefixedData, keyBytes, iv, null, buffer, tag)
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
            val prefixSize = SALT_SIZE + IV_SIZE
            // Salt, IV and tag around at least the "PCDA" mark: a shorter file cannot be ours (and has no ciphertext to pin).
            if (encryptedData.size < prefixSize + TAG_SIZE + PcdaMark.BYTES.size) return Result.failure(DecryptionFailedException())

            val salt = ByteArray(SALT_SIZE).apply { encryptedData.copyInto(this, 0, 0, SALT_SIZE) }
            val keyBytes = pbkdf2(password, salt)
            val iv = ByteArray(IV_SIZE).apply { encryptedData.copyInto(this, 0, SALT_SIZE, prefixSize) }
            val cipherTextSize = encryptedData.size - prefixSize - TAG_SIZE
            val cipherText = ByteArray(cipherTextSize).apply { encryptedData.copyInto(this, 0, prefixSize, prefixSize + cipherTextSize) }
            val tag = ByteArray(TAG_SIZE).apply { encryptedData.copyInto(this, 0, prefixSize + cipherTextSize, encryptedData.size) }
            val buffer = ByteArray(cipherTextSize)

            val cryptStatus = gcmDecrypt(cipherText, keyBytes, iv, null, buffer, tag)
            if (cryptStatus != kCCSuccess) return Result.failure(decryptStatusFailure(cryptStatus) ?: Exception("Decrypt status: $cryptStatus"))
            // The plaintext must start with the MAGIC header "PCDA"; anything else was not sealed by this app.
            PcdaMark.open(buffer)
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

    private const val CC_PRF_HMAC_ALG_SHA256: UInt = 3u

    actual fun pbkdf2(
        password: String,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val derivedKey = ByteArray(32)
        val passBytes = password.encodeToByteArray()
        memScoped {
            derivedKey.usePinned { derivedPinned ->
                salt.usePinned { saltPinned ->
                    CCKeyDerivationPBKDF(
                        kCCPBKDF2, password, passBytes.size.toULong(),
                        saltPinned.addressOf(0).reinterpret(), salt.size.toULong(),
                        CC_PRF_HMAC_ALG_SHA256, iterations.toUInt(), derivedPinned.addressOf(0).reinterpret(), 32.toULong(),
                    )
                }
            }
        }
        return derivedKey
    }

    actual fun randomBytes(size: Int): ByteArray = secureRandomBytes(size)

    actual fun sealWithKey(
        data: ByteArray,
        key: ByteArray,
        aad: ByteArray,
    ): Result<ByteArray> = gcmSeal(data, key, aad)

    actual fun openWithKey(
        sealed: ByteArray,
        key: ByteArray,
        aad: ByteArray,
    ): Result<ByteArray> = gcmOpen(sealed, key, aad)

    private const val KEYCHAIN_SERVICE = "com.payslipmax.pdfparser"
    private const val KEYCHAIN_ACCOUNT = "db_secret_key"

    actual fun getDatabaseSecretKey(): String {
        cachedDatabaseKey?.let { return it }
        val resolved = resolveDatabaseSecretKey()
        cachedDatabaseKey = resolved
        return resolved
    }

    private fun resolveDatabaseSecretKey(): String {
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
