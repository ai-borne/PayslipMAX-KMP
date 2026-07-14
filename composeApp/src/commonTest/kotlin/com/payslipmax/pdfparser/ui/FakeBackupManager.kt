package com.payslipmax.pdfparser.ui

import com.payslipmax.pdfparser.backup.BackupManager

class FakeBackupManager : BackupManager {
    var backupResult: Result<Unit> = Result.success(Unit)
    var restoreResult: Result<Unit> = Result.success(Unit)
    var backupCalledCount = 0
    var restoreCalledCount = 0

    /**
     * Simulates the whole-DB file swap performed by the real actuals: on a successful restore this
     * runs, letting a test mimic an imported backup that carries a (possibly different) premium flag
     * into the DAO, so the ViewModel's entitlement-sanitization step can be exercised.
     */
    var onRestoreSideEffect: (suspend () -> Unit)? = null

    override suspend fun backup(password: String): Result<Unit> {
        backupCalledCount++
        return backupResult
    }

    override suspend fun restore(password: String): Result<Unit> {
        restoreCalledCount++
        if (restoreResult.isSuccess) {
            onRestoreSideEffect?.invoke()
        }
        return restoreResult
    }
}
