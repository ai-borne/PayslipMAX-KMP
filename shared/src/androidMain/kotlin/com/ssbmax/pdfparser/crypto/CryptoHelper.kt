package com.ssbmax.pdfparser.crypto

import android.content.Context
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object CryptoHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val AES_KEY_SIZE = 32 // 256 bits
    private const val IV_SIZE = 16 // 128 bits block size

    private const val SALT_SIZE = 16

    actual fun encrypt(
        data: ByteArray,
        password: String,
    ): Result<ByteArray> {
        return try {
            // Generate a random 16-byte Salt
            val salt = ByteArray(SALT_SIZE)
            java.security.SecureRandom().nextBytes(salt)

            // Derive key using PBKDF2
            val keyBytes = pbkdf2(password, salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // Generate a random 16-byte IV
            val iv = ByteArray(IV_SIZE)
            java.security.SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)

            // Prepend MAGIC header "PCDA" for integrity check
            val magic = "PCDA".toByteArray(Charsets.UTF_8)
            val prefixedData = ByteArray(magic.size + data.size)
            System.arraycopy(magic, 0, prefixedData, 0, magic.size)
            System.arraycopy(data, 0, prefixedData, magic.size, data.size)

            val cipherText = cipher.doFinal(prefixedData)

            // Output: Salt + IV + CipherText
            val result = ByteArray(salt.size + iv.size + cipherText.size)
            System.arraycopy(salt, 0, result, 0, salt.size)
            System.arraycopy(iv, 0, result, salt.size, iv.size)
            System.arraycopy(cipherText, 0, result, salt.size + iv.size, cipherText.size)

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
            System.arraycopy(encryptedData, 0, salt, 0, salt.size)

            // Derive key using PBKDF2
            val keyBytes = pbkdf2(password, salt)
            val secretKey = SecretKeySpec(keyBytes, "AES")

            // Extract IV
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(encryptedData, SALT_SIZE, iv, 0, iv.size)
            val ivSpec = IvParameterSpec(iv)

            // Extract CipherText
            val cipherTextSize = encryptedData.size - prefixSize
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(encryptedData, prefixSize, cipherText, 0, cipherTextSize)

            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val plainText = cipher.doFinal(cipherText)

            // Verify MAGIC header "PCDA"
            val magic = "PCDA".toByteArray(Charsets.UTF_8)
            if (plainText.size < magic.size) {
                return Result.failure(IllegalArgumentException("Decryption failed: incorrect password or tampered data"))
            }
            for (i in magic.indices) {
                if (plainText[i] != magic[i]) {
                    return Result.failure(IllegalArgumentException("Decryption failed: incorrect password or tampered data"))
                }
            }

            val result = ByteArray(plainText.size - magic.size)
            System.arraycopy(plainText, magic.size, result, 0, result.size)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    actual fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    actual fun pbkdf2(
        password: String,
        salt: ByteArray,
        iterations: Int,
    ): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private const val KEYSTORE_ALIAS = "DatabaseSecretKeyAlias"
    private const val PREFS_NAME = "secure_prefs"
    private const val ENCRYPTED_KEY_PREF = "encrypted_db_key"
    private const val IV_PREF = "encrypted_db_key_iv"
    private var memoryFallbackKey: String? = null

    private fun getOrCreateAndroidKeystoreKey(): javax.crypto.SecretKey {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
            val entry = keyStore.getEntry(KEYSTORE_ALIAS, null) as? java.security.KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator =
            javax.crypto.KeyGenerator.getInstance(
                android.security.keystore.KeyProperties.KEY_ALGORITHM_AES,
                "AndroidKeyStore",
            )
        val spec =
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    actual fun getDatabaseSecretKey(): String {
        val ctx = ContextHolder.context
        if (ctx == null) {
            if (memoryFallbackKey == null) {
                val bytes = ByteArray(32)
                java.security.SecureRandom().nextBytes(bytes)
                memoryFallbackKey = bytes.joinToString("") { "%02x".format(it) }
            }
            return memoryFallbackKey!!
        }

        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encryptedKeyHex = prefs.getString(ENCRYPTED_KEY_PREF, null)
        val ivHex = prefs.getString(IV_PREF, null)

        if (encryptedKeyHex != null && ivHex != null) {
            try {
                val encryptedKey = parseHex(encryptedKeyHex)
                val iv = parseHex(ivHex)
                val secretKey = getOrCreateAndroidKeystoreKey()

                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))
                val decryptedBytes = cipher.doFinal(encryptedKey)
                return decryptedBytes.joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                // Generate a new key if decryption fails
            }
        }

        val rawKey = ByteArray(32)
        java.security.SecureRandom().nextBytes(rawKey)

        try {
            val secretKey = getOrCreateAndroidKeystoreKey()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val encryptedKey = cipher.doFinal(rawKey)
            val iv = cipher.iv

            val editor = prefs.edit()
            editor.putString(ENCRYPTED_KEY_PREF, encryptedKey.joinToString("") { "%02x".format(it) })
            editor.putString(IV_PREF, iv.joinToString("") { "%02x".format(it) })
            editor.apply()
        } catch (e: Exception) {
            // If keystore fails, return memory fallback
            return rawKey.joinToString("") { "%02x".format(it) }
        }

        return rawKey.joinToString("") { "%02x".format(it) }
    }

    private fun parseHex(hex: String): ByteArray {
        val result = ByteArray(hex.length / 2)
        for (i in 0 until hex.length step 2) {
            val firstDigit = Character.digit(hex[i], 16)
            val secondDigit = Character.digit(hex[i + 1], 16)
            result[i / 2] = ((firstDigit shl 4) + secondDigit).toByte()
        }
        return result
    }
}

object ContextHolder {
    @Volatile
    var context: Context? = null
}
