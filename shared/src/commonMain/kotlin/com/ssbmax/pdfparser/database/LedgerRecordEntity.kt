package com.ssbmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ledger_records")
data class LedgerRecordEntity(
    // format: "MM/YYYY", e.g., "05/2026"
    @PrimaryKey val dateStr: String,
    val year: Int,
    val monthNum: Int,
    val basicPay: Double,
    val dearnessAllowance: Double,
    val militaryServicePay: Double,
    val transportAllowance: Double,
    val transportAllowanceDa: Double,
    val houseRentAllowance: Double,
    val grossPay: Double,
    val dsopSubscription: Double,
    val incomeTax: Double,
    val netPay: Double,
)
