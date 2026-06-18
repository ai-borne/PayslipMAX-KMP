package com.ssbmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "financial_insights")
data class FinancialInsightEntity(
    @PrimaryKey val id: String,
    val monthStr: String,
    // "TAX", "ALLOWANCE", "SALARY_LOSS", "RETIREMENT"
    val category: String,
    val title: String,
    val contentMarkdown: String,
    // "INFO", "SUCCESS", "WARNING", "CRITICAL"
    val severity: String,
    val createdAt: Long,
    val isArchived: Boolean = false,
)
