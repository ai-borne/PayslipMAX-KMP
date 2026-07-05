package com.payslipmax.pdfparser.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val isPremiumEnabled: Boolean = false,
    // "light", "dark", "system"
    val appTheme: String = "system",
    val isLockEnabled: Boolean = false,
    // SHA-256 hash of the passcode
    val appPinHash: String = "",
    val profileName: String = "",
    val profileCdaNumber: String = "",
    val profilePanNumber: String = "",
    @ColumnInfo(defaultValue = "0") val useLocalAi: Boolean = false,
)
