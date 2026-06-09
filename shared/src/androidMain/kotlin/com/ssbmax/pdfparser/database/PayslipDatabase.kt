package com.ssbmax.pdfparser.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

actual fun getDatabaseBuilder(): RoomDatabase.Builder<PayslipDatabase> {
    // Obtain application context dynamically using reflection
    val context =
        Class.forName("android.app.ActivityThread")
            .getMethod("currentApplication")
            .invoke(null) as Context

    val dbFile = context.getDatabasePath("payslips.db")
    return Room.databaseBuilder<PayslipDatabase>(
        context = context,
        name = dbFile.absolutePath,
    )
}
