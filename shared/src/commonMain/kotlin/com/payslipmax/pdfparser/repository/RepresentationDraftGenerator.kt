package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.database.RepresentationDraftEntity
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.insights.TaxLedgerAggregator

object RepresentationDraftGenerator {
    /** Indian digit grouping (SSOT: [TaxLedgerAggregator.formatIndianCurrency]); "Rs." not "₹" because the letter is exported as a PDF. */
    private fun grouped(amount: Double) = TaxLedgerAggregator.formatIndianCurrency(amount)

    fun generateRepresentationDraft(
        disputeMonth: String,
        disputeType: String,
        amount: Double,
        officer: Officer,
        expected: Double,
        actual: Double,
        authority: String,
    ): RepresentationDraftEntity {
        val id = CryptoHelper.sha256("$disputeMonth-$disputeType-${CryptoHelper.getCurrentTimeMillis()}")
        val componentName =
            when (disputeType) {
                "MISSING_ALLOWANCE" -> "HRA / Allowance"
                "TPTA_ENTITLEMENT" -> "Transport Allowance (TPTA)"
                "SALARY_LOSS" -> "Net Pay"
                "INCREMENT_MISSED" -> "Basic Pay (annual increment)"
                "MSP_SHORTFALL" -> "Military Service Pay (MSP)"
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
                (c) Amount due         : Rs. ${grouped(expected)}
                (d) Amount credited    : Rs. ${grouped(actual)}
                (e) Shortfall          : Rs. ${grouped(amount)}

            4.  This is admissible under: $authority

            5.  It is requested that the admissibility of the above component may please be verified and the necessary arrears credited to my account.

            6.  Thanking you.

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
