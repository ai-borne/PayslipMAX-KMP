package com.payslipmax.pdfparser.ui.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    private val points = Regex("(?m)^(\\d+)\\. (.*)$").findAll(LegalStrings.settingsHelpPrivacyContent).map { it.groupValues[1].toInt() to it.groupValues[2] }.toList()

    /** WHY (Phase 11 item 63): the in-app privacy promise said nothing about Part II, whose drafts, scanned orders and reminders are the newest data on the phone. */
    @Test
    fun thePrivacyPointsAreNumberedOneToSevenInOrder() {
        assertEquals((1..7).toList(), points.map { it.first })
    }

    @Test
    fun theSeventhPointTellsTheOfficerPartTwoDataStaysOnTheDeviceAndHowABackupCarriesIt() {
        val seventh = points.last().second
        for (needed in listOf("Part II", "on your device", "encrypted", "never uploaded", "Back Up", "password")) {
            assertTrue(needed in seventh, "the Part II point must say '$needed': $seventh")
        }
    }

    @Test
    fun noPrivacyPointPromisesWhatTheAppDoesNotDo() {
        val all = LegalStrings.settingsHelpPrivacyContent
        for (forbidden in listOf("we can read", "shared with", "sent to", "cloud backup", "Part II data is uploaded")) {
            assertFalse(forbidden in all, "the privacy text must not say '$forbidden'")
        }
        assertTrue(points.last().second.length in 120..420, "one readable point, not a policy: ${points.last().second.length}")
    }
}
