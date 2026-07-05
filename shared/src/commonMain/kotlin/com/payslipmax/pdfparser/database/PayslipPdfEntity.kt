package com.payslipmax.pdfparser.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "payslip_pdfs")
data class PayslipPdfEntity(
    @PrimaryKey
    val dateStr: String,
    val pdfData: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PayslipPdfEntity
        if (dateStr != other.dateStr) return false
        if (!pdfData.contentEquals(other.pdfData)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = dateStr.hashCode()
        result = 31 * result + pdfData.contentHashCode()
        return result
    }
}
