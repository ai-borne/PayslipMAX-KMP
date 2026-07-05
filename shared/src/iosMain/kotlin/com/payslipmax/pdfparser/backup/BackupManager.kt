@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.payslipmax.pdfparser.backup

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.PayslipDatabase
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.*
import platform.posix.memcpy

actual class PlatformBackupManager actual constructor(private val database: PayslipDatabase) : BackupManager {
    private val fileManager = NSFileManager.defaultManager

    actual override suspend fun backup(password: String): Result<Unit> {
        return try {
            val dbPath = dbFilePath()
            if (!fileManager.fileExistsAtPath(dbPath)) {
                return Result.failure(Exception("Database file does not exist at: $dbPath"))
            }

            // Read raw DB bytes
            val dbData = NSData.dataWithContentsOfFile(dbPath) ?: return Result.failure(Exception("Failed to read database file"))
            val dbBytes = dbData.toByteArray()

            // Encrypt using our shared AES-256 CryptoHelper
            val encryptResult = CryptoHelper.encrypt(dbBytes, password)
            if (encryptResult.isFailure) {
                return Result.failure(encryptResult.exceptionOrNull() ?: Exception("Encryption failed"))
            }
            val encryptedBytes = encryptResult.getOrThrow()

            // Save to iCloud container (or fallback to local backup)
            val backupPath = backupFilePath()
            val backupData = encryptedBytes.toNSData()

            val success = backupData.writeToFile(path = backupPath, atomically = true)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to write encrypted backup to: $backupPath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun restore(password: String): Result<Unit> {
        return try {
            val backupPath = backupFilePath()
            if (!fileManager.fileExistsAtPath(backupPath)) {
                return Result.failure(Exception("Backup file does not exist at: $backupPath"))
            }

            // Read encrypted bytes
            val backupData = NSData.dataWithContentsOfFile(backupPath) ?: return Result.failure(Exception("Failed to read backup file"))
            val encryptedBytes = backupData.toByteArray()

            // Decrypt using our shared AES-256 CryptoHelper
            val decryptResult = CryptoHelper.decrypt(encryptedBytes, password)
            if (decryptResult.isFailure) {
                return Result.failure(decryptResult.exceptionOrNull() ?: Exception("Decryption failed: incorrect password"))
            }
            val decryptedBytes = decryptResult.getOrThrow()

            try {
                database.close()
            } catch (e: Exception) {
                // Database might not be initialized yet, which is safe to ignore
            }

            // Overwrite database file
            val dbPath = dbFilePath()
            val decryptedData = decryptedBytes.toNSData()
            val success = decryptedData.writeToFile(path = dbPath, atomically = true)

            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to write restored database to path: $dbPath"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun dbFilePath(): String {
        return documentDirectory() + "/payslips.db"
    }

    private fun backupFilePath(): String {
        // Attempt to find the user's iCloud Ubiquity container
        val iCloudUrl = fileManager.URLForUbiquityContainerIdentifier(null)
        val backupDir =
            if (iCloudUrl != null) {
                val path = iCloudUrl.path + "/Documents"
                // Ensure Documents directory exists inside iCloud container
                fileManager.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
                path
            } else {
                // Fallback to local Document Directory
                documentDirectory()
            }
        return "$backupDir/backup.pcda"
    }

    private fun documentDirectory(): String {
        val documentDirectory =
            fileManager.URLForDirectory(
                directory = NSDocumentDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = false,
                error = null,
            )
        return documentDirectory?.path ?: ""
    }

    private fun NSData.toByteArray(): ByteArray {
        val size = this.length.toInt()
        val byteArray = ByteArray(size)
        if (size > 0) {
            byteArray.usePinned { pinned ->
                memcpy(pinned.addressOf(0), this.bytes, this.length)
            }
        }
        return byteArray
    }

    private fun ByteArray.toNSData(): NSData {
        if (isEmpty()) return NSData()
        return this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }
}
