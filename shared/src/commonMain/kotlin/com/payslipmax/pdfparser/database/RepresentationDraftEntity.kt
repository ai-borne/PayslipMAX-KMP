package com.payslipmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "representation_drafts")
data class RepresentationDraftEntity(
    @PrimaryKey val id: String,
    // format: "MM/YYYY"
    val disputeMonth: String,
    // "MISSING_HRA", "MISSING_TPTA", "SALARY_DROP", "MISSING_ARREARS", "DEDUCTION_DISPUTE"
    val disputeType: String,
    // "PCDA_O_PUNE", "UNIT_PAY_OFFICE"
    val recipient: String,
    val subject: String,
    val bodyText: String,
    val createdAt: Long,
)
