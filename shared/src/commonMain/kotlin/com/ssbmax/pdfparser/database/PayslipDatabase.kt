package com.ssbmax.pdfparser.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

@Database(
    entities = [
        EncryptedPayslipEntity::class,
        PayslipPdfEntity::class,
        AppSettingsEntity::class,
        LedgerRecordEntity::class,
        FinancialInsightEntity::class,
        RepresentationDraftEntity::class,
    ],
    version = 5,
    exportSchema = false,
)
@ConstructedBy(PayslipDatabaseConstructor::class)
abstract class PayslipDatabase : RoomDatabase() {
    abstract fun payslipDao(): PayslipDao
}

// expect object constructor required for Room KMP database instantiation
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object PayslipDatabaseConstructor : RoomDatabaseConstructor<PayslipDatabase> {
    override fun initialize(): PayslipDatabase
}

/**
 * Returns a Room database builder configured for the specific platform.
 */
expect fun getDatabaseBuilder(): RoomDatabase.Builder<PayslipDatabase>
