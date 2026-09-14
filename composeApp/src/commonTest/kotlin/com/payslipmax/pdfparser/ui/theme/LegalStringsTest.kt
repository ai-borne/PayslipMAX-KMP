package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class LegalStringsTest {
    // Regression guard: privacyPolicyUrl was previously set to the Support page
    // ("https://ai-borne.in/support") instead of the actual privacy policy, so the "Privacy Policy"
    // link in the purchase sheet's legal footer (UpgradeLegalFooter) silently opened the wrong page.
    @Test
    fun privacyPolicyUrlPointsToThePrivacyPolicyPageNotSupport() {
        assertEquals("https://www.ai-borne.in/privacy-policy", LegalStrings.privacyPolicyUrl)
        assertFalse(LegalStrings.privacyPolicyUrl.contains("/support"))
    }

    @Test
    fun termsOfUseUrlPointsToAppleStandardEula() {
        assertEquals("https://www.apple.com/legal/internet-services/itunes/dev/stdeula/", LegalStrings.termsOfUseUrl)
    }
}
