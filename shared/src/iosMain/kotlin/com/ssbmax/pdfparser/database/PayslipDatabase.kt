@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ssbmax.pdfparser.database

import androidx.room.Room
import androidx.room.RoomDatabase
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

actual fun getDatabaseBuilder(): RoomDatabase.Builder<PayslipDatabase> {
    val dbFilePath = documentDirectory() + "/payslips.db"
    return Room.databaseBuilder<PayslipDatabase>(
        name = dbFilePath,
        factory = { PayslipDatabaseConstructor.initialize() },
    ).fallbackToDestructiveMigration(true)
}

private fun documentDirectory(): String {
    val documentDirectory =
        NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null,
        )
    return documentDirectory?.path ?: ""
}
