package com.ssbmax.pdfparser.database

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.migration.AutoMigrationSpec

@Database(
    entities = [
        EncryptedPayslipEntity::class,
        PayslipPdfEntity::class,
        AppSettingsEntity::class,
        LedgerRecordEntity::class,
        FinancialInsightEntity::class,
        RepresentationDraftEntity::class,
    ],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 5, to = 6, spec = PayslipDatabase.DeleteGeminiApiKeySpec::class),
        AutoMigration(from = 6, to = 7)
    ],
)
@ConstructedBy(PayslipDatabaseConstructor::class)
abstract class PayslipDatabase : RoomDatabase() {
    abstract fun payslipDao(): PayslipDao

    /**
     * AutoMigration spec for v5→v6:
     * Explicitly marks `geminiApiKey` as deleted from `app_settings`.
     * The API key now lives exclusively in Firebase Secret Manager (server-side).
     */
    @DeleteColumn(tableName = "app_settings", columnName = "geminiApiKey")
    class DeleteGeminiApiKeySpec : AutoMigrationSpec
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
