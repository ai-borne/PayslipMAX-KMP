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

        assertTrue(draft.bodyText.contains("Rs. 3960"))
        assertTrue(draft.bodyText.contains("Rs. 0"))
        assertTrue(draft.bodyText.contains(PayAuthorities.TRANSPORT_ALLOWANCE))
    }
}
