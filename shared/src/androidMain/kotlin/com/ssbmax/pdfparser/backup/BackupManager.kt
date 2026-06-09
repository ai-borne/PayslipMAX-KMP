package com.ssbmax.pdfparser.backup

import android.content.Context
import com.ssbmax.pdfparser.crypto.CryptoHelper
import com.ssbmax.pdfparser.database.PayslipDatabase
import org.koin.mp.KoinPlatformTools
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

actual class PlatformBackupManager actual constructor() : BackupManager {

    actual override suspend fun backup(password: String): Result<Unit> {
        return try {
            val context = getContext() ?: return Result.failure(Exception("Android Context not available"))
            
            val dbFile = context.getDatabasePath("payslips.db")
            if (!dbFile.exists()) {
                return Result.failure(Exception("Database file does not exist"))
            }

            // Read database file
            val dbBytes = dbFile.readBytes()

            // Encrypt using shared AES-256 CryptoHelper
            val encryptResult = CryptoHelper.encrypt(dbBytes, password)
            if (encryptResult.isFailure) {
                return Result.failure(encryptResult.exceptionOrNull() ?: Exception("Encryption failed"))
            }
            val encryptedBytes = encryptResult.getOrThrow()

            // Save to external files dir (which is secure and synced via Google Auto Backup)
            val backupFile = getBackupFile(context)
            backupFile.parentFile?.mkdirs()
            
            FileOutputStream(backupFile).use { it.write(encryptedBytes) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun restore(password: String): Result<Unit> {
        return try {
            val context = getContext() ?: return Result.failure(Exception("Android Context not available"))
            
            val backupFile = getBackupFile(context)
            if (!backupFile.exists()) {
                return Result.failure(Exception("Backup file does not exist at: ${backupFile.absolutePath}"))
            }

            // Read encrypted bytes
            val encryptedBytes = backupFile.readBytes()

            // Decrypt using shared AES-256 CryptoHelper
            val decryptResult = CryptoHelper.decrypt(encryptedBytes, password)
            if (decryptResult.isFailure) {
                return Result.failure(decryptResult.exceptionOrNull() ?: Exception("Decryption failed: incorrect password"))
            }
            val decryptedBytes = decryptResult.getOrThrow()

            // Close Room database instance before overwriting the file on disk
            try {
                val db = KoinPlatformTools.defaultContext().get().get<PayslipDatabase>()
                db.close()
            } catch (e: Exception) {
                // Database might not be initialized, safe to ignore
            }

            // Overwrite database file
            val dbFile = context.getDatabasePath("payslips.db")
            dbFile.parentFile?.mkdirs()
            
            FileOutputStream(dbFile).use { it.write(decryptedBytes) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getContext(): Context? {
        return try {
            Class.forName("android.app.ActivityThread")
                .getMethod("currentApplication")
                .invoke(null) as? Context
        } catch (e: Exception) {
            null
        }
    }

    private fun getBackupFile(context: Context): File {
        val backupDir = context.getExternalFilesDir(null) ?: context.filesDir
        return File(backupDir, "backup.pcda")
    }
}
