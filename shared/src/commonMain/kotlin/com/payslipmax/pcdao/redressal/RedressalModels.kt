package com.payslipmax.pcdao.redressal

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import kotlinx.serialization.Serializable

@Serializable
enum class ExportFormat {
    TXT,
    MARKDOWN,
    PDF,
}

@Serializable
data class DiffLineItem(
    val serialNo: Int,
    val lineItemName: String,
    val entitledAmount: Double,
    val creditedAmount: Double,
    val netDue: Double = entitledAmount - creditedAmount,
    val statutoryAuthority: String,
    val ruleId: String? = null,
)

@Serializable
data class RedressalRequest(
    val officerName: String,
    val serviceNumber: String,
    val rank: String,
    val cdaAccountNo: String,
    val ledgerSection: String = "Section R (Regimental Officers)",
    val disputeMonth: String,
    val lineItems: List<DiffLineItem> = emptyList(),
    val prayerRemarks: String? = null,
    val maskPii: Boolean = false,
    val pan: String? = null,
    val exportFormat: ExportFormat = ExportFormat.TXT,
)

@Serializable
data class RedressalLetter(
    val id: String,
    val recipient: String,
    val attention: String,
    val subject: String,
    val officerDetailsHeader: String,
    val discrepancyTableText: String,
    val statutoryCitationsText: String,
    val fullBodyText: String,
    val totalNetDue: Double,
    val generatedDateStr: String,
    val isPiiMasked: Boolean,
    val exportFormat: ExportFormat = ExportFormat.TXT,
    val disputeMonth: String = "",
) {
    fun toRepresentationDraftEntity(): RepresentationDraftEntity {
        return RepresentationDraftEntity(
            id = id,
            disputeMonth = disputeMonth.ifBlank { "N/A" },
            disputeType = "PCDAO_AUDIT_DISCREPANCY",
            recipient = "PCDA_O_PUNE",
            subject = subject,
            bodyText = fullBodyText,
            createdAt = CryptoHelper.getCurrentTimeMillis(),
        )
    }
}
