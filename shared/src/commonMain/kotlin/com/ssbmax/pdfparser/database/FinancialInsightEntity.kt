package com.ssbmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "financial_insights")
data class FinancialInsightEntity(
    @PrimaryKey val id: String,
    val monthStr: String, // foreign key reference to ledger_records or payslips
    val category: String, // "TAX", "ALLOWANCE", "SALARY_LOSS", "RETIREMENT"
    val title: String,
    val contentMarkdown: String,
    val severity: String, // "INFO", "SUCCESS", "WARNING", "CRITICAL"
    val createdAt: Long,
    val isArchived: Boolean = false
)
