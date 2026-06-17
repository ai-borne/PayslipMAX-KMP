package com.ssbmax.pdfparser.database

import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<PayslipDatabase> {
    val context =
        com.ssbmax.pdfparser.crypto.ContextHolder.context
            ?: throw IllegalStateException("Android Context is not initialized in ContextHolder")

    val dbFile = context.getDatabasePath("payslips.db")
    return Room.databaseBuilder<PayslipDatabase>(
        context = context,
        name = dbFile.absolutePath,
    ).fallbackToDestructiveMigration(true)
}
