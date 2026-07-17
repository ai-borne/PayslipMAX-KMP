package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.domain.Officer

object RepresentationDraftGenerator {
    fun generateRepresentationDraft(
        disputeMonth: String,
        disputeType: String,
        amount: Double,
        officer: Officer,
    ): RepresentationDraftEntity {
        val id = CryptoHelper.sha256("$disputeMonth-$disputeType-${CryptoHelper.getCurrentTimeMillis()}")
        val componentName =
            when (disputeType) {
                "MISSING_ALLOWANCE" -> "HRA / Allowance"
                "TPTA_ENTITLEMENT" -> "Transport Allowance (TPTA)"
                "SALARY_LOSS" -> "Net Pay"
                else -> disputeType
            }
        val subject = "Representation regarding Non-Admissibility of $componentName"
        val body =
            """
            To,
            The Principal Controller of Defence Accounts (Officers)
            Golibar Maidan, Pune - 411001
            
            SUBJECT: REPRESENTATION REGARDING NON-ADMISSIBILITY OF $componentName FOR THE MONTH OF $disputeMonth
            
            Sir/Madam,
            
            1.  I have the honour to submit that my monthly payslip for $disputeMonth indicates that my $componentName has not been correctly credited / has been adjusted.
            
            2.  My service particular details are as follows:
                (a) Personal Number    : [Service Number]
                (b) Rank               : [Rank]
                (c) Name               : [Officer Name]
                (d) CDA Account Number : [CDA Account No]
            
            3.  Discrepancy Details:
                (a) Component name     : $componentName
                (b) Discrepancy month  : $disputeMonth
                (c) Estimated amount   : Rs. ${amount.toInt()}
            
            4.  It is requested that the admissibility of the above component may please be verified and the necessary arrears credited to my account.
            
            5.  Thanking you.
            
            Yours faithfully,
            
            [Officer Name]
            [Rank]
            """.trimIndent()

        return RepresentationDraftEntity(
            id = id,
            disputeMonth = disputeMonth,
            disputeType = disputeType,
            recipient = "PCDA_O_PUNE",
            subject = subject,
            bodyText = body,
            createdAt = CryptoHelper.getCurrentTimeMillis(),
        )
    }
}
