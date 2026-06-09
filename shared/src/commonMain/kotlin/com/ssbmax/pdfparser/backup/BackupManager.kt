package com.ssbmax.pdfparser.backup

interface BackupManager {
    /**
     * Reads the local database file, encrypts it using AES-256 with the user password,
     * and saves it to the platform's secure cloud/local folder.
     */
    suspend fun backup(password: String): Result<Unit>

    /**
     * Restores the database by downloading/reading the encrypted backup file,
     * decrypting it with the user password, and replacing the current database file.
     */
    suspend fun restore(password: String): Result<Unit>
}

expect class PlatformBackupManager() : BackupManager {
    override suspend fun backup(password: String): Result<Unit>
    override suspend fun restore(password: String): Result<Unit>
}
