package com.ssbmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "ai_insight_reports")
data class AiInsightReportEntity(
    @PrimaryKey val id: String,
    val payslipMonth: String,
    val generatedDate: Long,
    val reportJSON: String,
    val reportVersion: String,
)
