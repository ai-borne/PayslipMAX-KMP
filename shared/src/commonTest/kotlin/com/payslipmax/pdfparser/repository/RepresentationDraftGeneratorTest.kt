package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.insights.PayAuthorities
import kotlin.test.Test
import kotlin.test.assertTrue

class RepresentationDraftGeneratorTest {
    @Test
    fun draftCitesExpectedActualAndAuthority() {
        val draft =
            RepresentationDraftGenerator.generateRepresentationDraft(
                disputeMonth = "05/2026",
                disputeType = "TPTA_ENTITLEMENT",
                amount = 3960.0,
                officer = Officer(name = "Test Officer", accountNo = "12345", pan = "ABCDE1234F"),
                expected = 3960.0,
                actual = 0.0,
                authority = PayAuthorities.TRANSPORT_ALLOWANCE,
            )

        assertTrue(draft.bodyText.contains("Rs. 3,960"))
        assertTrue(draft.bodyText.contains("Rs. 0"))
        assertTrue(draft.bodyText.contains(PayAuthorities.TRANSPORT_ALLOWANCE))
    }

    // WHY: the letter is read by PCDA(O) staff and quoted back, so a lakh amount must read as "Rs. 1,23,100", not "Rs. 123100".
    @Test
    fun draftGroupsRupeesInTheIndianWay() {
        val draft =
            RepresentationDraftGenerator.generateRepresentationDraft(
                disputeMonth = "05/2026",
                disputeType = "TPTA_ENTITLEMENT",
                amount = 123100.0,
                officer = Officer(name = "Test Officer", accountNo = "12345", pan = "ABCDE1234F"),
                expected = 123100.0,
                actual = 0.0,
                authority = PayAuthorities.TRANSPORT_ALLOWANCE,
            )
        assertTrue(draft.bodyText.contains("Amount due         : Rs. 1,23,100"), draft.bodyText)
        assertTrue(draft.bodyText.contains("Shortfall          : Rs. 1,23,100"), draft.bodyText)
    }

    // WHY: found on the Pixel with the Phase 8 seed: the letter subject read "Non-Admissibility of INCREMENT_MISSED".
    // The officer sends this to PCDA(O), so a code name must never reach the letter.
    @Test
    fun everyLetterEligibleTypeIsNamedInPlainWords() {
        listOf("INCREMENT_MISSED" to "Basic Pay", "MSP_SHORTFALL" to "Military Service Pay").forEach { (type, plain) ->
            val draft =
                RepresentationDraftGenerator.generateRepresentationDraft(
                    disputeMonth = "07/2022",
                    disputeType = type,
                    amount = 2600.0,
                    officer = Officer(name = "Test Officer", accountNo = "12345", pan = "ABCDE1234F"),
                    expected = 87900.0,
                    actual = 85300.0,
                    authority = PayAuthorities.ANNUAL_INCREMENT,
                )
            assertTrue(draft.subject.contains(plain), "${draft.subject} should name $plain")
            assertTrue(!draft.subject.contains(type) && !draft.bodyText.contains(type), "$type must not appear in the letter")
        }
    }
}
