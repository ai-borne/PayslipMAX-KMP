package com.ssbmax.pdfparser.crypto

import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

actual object CryptoHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val AES_KEY_SIZE = 32 // 256 bits
    private const val IV_SIZE = 16 // 128 bits block size

    actual fun encrypt(data: ByteArray, password: String): Result<ByteArray> {
        return try {
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            
            // Generate a random 16-byte IV
            val iv = ByteArray(IV_SIZE)
            java.security.SecureRandom().nextBytes(iv)
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
            val cipherText = cipher.doFinal(data)
            
            // Output: IV + CipherText
            val result = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, result, 0, iv.size)
            System.arraycopy(cipherText, 0, result, iv.size, cipherText.size)
            
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
            
            val keyBytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray(Charsets.UTF_8))
            val secretKey = SecretKeySpec(keyBytes, "AES")
            
            // Extract IV
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(encryptedData, 0, iv, 0, iv.size)
            val ivSpec = IvParameterSpec(iv)
            
            // Extract CipherText
            val cipherTextSize = encryptedData.size - IV_SIZE
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(encryptedData, IV_SIZE, cipherText, 0, cipherTextSize)
            
            val cipher = Cipher.getInstance(ALGORITHM)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)
            val plainText = cipher.doFinal(cipherText)
            
            Result.success(plainText)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
