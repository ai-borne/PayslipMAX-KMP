package com.payslipmax.pdfparser.part2.data

import platform.Foundation.NSUserDefaults

class IosLastBackupStorage(
    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults,
) : LastBackupStorage {
    override fun load(): Long? = defaults.objectForKey(LAST_BACKUP_KEY)?.let { defaults.integerForKey(LAST_BACKUP_KEY) }

    override fun save(epochMillis: Long) {
        defaults.setInteger(epochMillis, forKey = LAST_BACKUP_KEY)
    }
}

actual fun provideLastBackupStorage(): LastBackupStorage = IosLastBackupStorage()
