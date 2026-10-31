package com.payslipmax.pdfparser.part2.data

import android.content.Context
import com.payslipmax.pdfparser.crypto.ContextHolder

private const val PREFS_NAME = "payslipmax_part2_prefs"

class AndroidLastBackupStorage : LastBackupStorage {
    private fun prefs() = ContextHolder.context?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): Long? = prefs()?.takeIf { it.contains(LAST_BACKUP_KEY) }?.getLong(LAST_BACKUP_KEY, 0L)

    override fun save(epochMillis: Long) {
        prefs()?.edit()?.putLong(LAST_BACKUP_KEY, epochMillis)?.apply()
    }
}

actual fun provideLastBackupStorage(): LastBackupStorage = AndroidLastBackupStorage()
