package com.payslipmax.pdfparser.database

import androidx.room.ColumnInfo
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
    // Added in schema v12 (P7-18, docs/Plan/09_PayAudit_PhasePlan.md) so the Pay Audit timeline auditors
    // (DaArrearsAuditor, TptaEntitlementAuditor, MspAuditor, MarriedQuartersRiskAuditor) see the same
    // fields on the Insights-tab (ledger-backed) path that PayAuditScreen already reads directly from
    // ParsedPayslip. Defaulted so existing rows backfill cleanly on migration.
    @ColumnInfo(defaultValue = "0.0") val riskHardshipAllowance: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val fieldAllowance: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val licenseFee: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val furnitureRent: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val arrearsDa: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val arrearsTpta: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val arrearsTptaDa: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val adjTpta: Double = 0.0,
    @ColumnInfo(defaultValue = "0.0") val adjMsp: Double = 0.0,
    @ColumnInfo(defaultValue = "0") val needsReview: Boolean = false,
)
