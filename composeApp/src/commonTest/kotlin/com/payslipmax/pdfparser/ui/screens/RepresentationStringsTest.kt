package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.ui.theme.AppStringsPremium
import kotlin.test.Test
import kotlin.test.assertEquals

class RepresentationStringsTest {
    @Test
    fun representationTitleMatchesClaimGeneratorSSOT() {
        assertEquals("Claim Generator", AppStringsPremium.representationTitle)
        assertEquals(AppStringsPremium.proCatalogClaimTitle, AppStringsPremium.representationTitle)
    }
}
