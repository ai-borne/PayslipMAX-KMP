package com.payslipmax.pdfparser.database

import androidx.room.Entity

/**
 * A letter the officer deleted, kept as (month, dispute type) so a re-audit or re-import does not draft it
 * again while the same finding is still proven. Cleared when that finding stops applying, so a genuinely new
 * finding later drafts a fresh letter. No payslip data, no PII.
 */
@Entity(tableName = "dismissed_drafts", primaryKeys = ["disputeMonth", "disputeType"])
data class DismissedDraftEntity(
    // format: "MM/YYYY"
    val disputeMonth: String,
    val disputeType: String,
)
