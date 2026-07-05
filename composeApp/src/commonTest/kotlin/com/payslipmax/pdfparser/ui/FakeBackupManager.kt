package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.backup.BackupManager

class FakeBackupManager : BackupManager {
    var backupResult: Result<Unit> = Result.success(Unit)
    var restoreResult: Result<Unit> = Result.success(Unit)
    var backupCalledCount = 0
    var restoreCalledCount = 0

    override suspend fun backup(password: String): Result<Unit> {
        backupCalledCount++
        return backupResult
    }

    override suspend fun restore(password: String): Result<Unit> {
        restoreCalledCount++
        return restoreResult
    }
}
