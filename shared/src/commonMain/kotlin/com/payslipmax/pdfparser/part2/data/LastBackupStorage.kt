package com.payslipmax.pdfparser.part2.data

/**
 * Where the time of the officer's last `.pcda` backup is kept on this device (the `GuidePinsStorage` pattern): one number, in
 * preferences. Not in the encrypted database and not in the backup file, so a phone never claims a backup it did not make.
 */
interface LastBackupStorage {
    /** Epoch milliseconds of the last backup made on this device, or null if none. */
    fun load(): Long?

    fun save(epochMillis: Long)
}

/** The preference key; a future format gets a new key. */
internal const val LAST_BACKUP_KEY = "last_backup_v1"

expect fun provideLastBackupStorage(): LastBackupStorage

/** Keeps the time in memory only; the default where nothing needs it to outlive the process (a screen test). */
class InMemoryLastBackupStorage : LastBackupStorage {
    private var millis: Long? = null

    override fun load(): Long? = millis

    override fun save(epochMillis: Long) {
        millis = epochMillis
    }
}
