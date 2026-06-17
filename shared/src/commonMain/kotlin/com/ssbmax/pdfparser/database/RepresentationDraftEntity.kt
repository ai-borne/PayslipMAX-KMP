package com.ssbmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "representation_drafts")
data class RepresentationDraftEntity(
    @PrimaryKey val id: String,
    val disputeMonth: String, // format: "MM/YYYY"
    val disputeType: String, // "MISSING_HRA", "MISSING_TPTA", "SALARY_DROP", "MISSING_ARREARS", "DEDUCTION_DISPUTE"
    val recipient: String, // "PCDA_O_PUNE", "UNIT_PAY_OFFICE"
    val subject: String,
    val bodyText: String,
    val createdAt: Long
)
